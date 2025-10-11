import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import sttp.client4.*
import sttp.client4.circe.*

import java.nio.charset.Charset
import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Future, Promise, blocking}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}

object Gemini extends Gemini
class Gemini {
  case class GeminiRequest(
      safety_settings: Seq[SafetySetting],
      generation_config: GenerationConfig,
      system_instruction: SystemInstruction,
      contents: Seq[Content]
  ) derives Encoder.AsObject,
        Decoder
  case class SafetySetting(category: String, threshold: String) derives Encoder.AsObject, Decoder
  case class GenerationConfig(temperature: Double, seed: Int) derives Encoder.AsObject, Decoder
  case class Content(role: String, parts: Seq[Text]) derives Encoder.AsObject, Decoder
  case class SystemInstruction(parts: Seq[Text]) derives Encoder.AsObject, Decoder
  case class Text(text: String) derives Encoder.AsObject, Decoder

  case class GeminiResponse(candidates: Seq[Candidate]) derives Encoder.AsObject, Decoder
  case class Candidate(content: Content) derives Encoder.AsObject, Decoder

  val defaultPrompt =
    "You are an expert subtitle translator, you can use profane language if it is present in the source, output only the translation"

  // #replaced by build
  val MaxContextLines = 50
  val Models = Vector(
    "gemini-2.5-flash",
    "gemini-2.0-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.0-flash-lite"
  )

  case class TimedResult(res: Result, d: Duration, ts: Instant)
  private case class ModelInfo(results: List[TimedResult]) {
    def addResult(result: Result, delay: Duration) = copy(results = (TimedResult(result, delay, Instant.now) :: results).take(100))

    def delays         = results.view.filter(_.res.result.isRight).map(_.d)
    def succeses       = results.view.collect { case TimedResult(Result(result = Right(e)), d, ts) => e -> ts }
    def recentSucceses = succeses.filter(_._2.isAfter(Instant.now.minusSeconds(10)))
    def errors         = results.view.collect { case TimedResult(Result(result = Left(e)), d, ts) => e -> ts }

    val recentErrors = errors.filter(_._2.isAfter(Instant.now.minusSeconds(10)))
    val dead         = results.size > 100 && recentSucceses.isEmpty
    val averageDelay = delays.map(_.toMillis).sum / delays.size.max(1)

    override def toString =
      s"Calls: ${results.size}, Success: ${succeses.size}, Avg delay: $averageDelay, errors in last 10 secs: ${recentErrors.size}"
  }
  private val ModelsInfo = TrieMap[(String, Int), ModelInfo]()
  private def timed[T](f: => Future[T]) = {
    val start = System.currentTimeMillis()
    f.map(_ -> Duration(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS))
  }
  case class WithDelay[T](delay: Promise[Unit], result: Future[T]) {
    def cancel = delay.tryFailure(new Exception("cancelled"))
  }
  private def delayed[T](label: String, d: Duration)(f: => Future[T]) = {
    val delay = Promise[Unit]()
    Future {
      blocking {
        Thread.sleep(d.toMillis)
        delay.trySuccess(())
      }
    }
    val withDelay = for {
      _ <- delay.future.recover {
        case e =>
          Logger.println(s"Cancelled $label")
          throw e
      }
      _ = Logger.println(s"Running $label after $d")
      f <- f
    } yield f
    WithDelay(delay, withDelay)
  }
  private def firstSuccess[T](f: Seq[Future[T]]): Future[T] = {
    if (f.isEmpty) Future.failed(new Exception("couldn't find success"))
    else
      Future.firstCompletedOf(f).recoverWith {
        case e =>
          firstSuccess(f.filterNot(_.value.exists(_.isFailure)))
      }
  }
  @transient private var untranslated = Vector[String]()
  @transient private var context      = Vector[(String, String)]()
  def translate(text: String, prompt: Option[String], from: Option[String], to: String, apiKeys: Seq[String]): Future[String] = {
    val toTranslate   = untranslated :+ text
    val defaultModels = Models.flatMap(model => (0 until apiKeys.size).map(model -> _))
    val bestModels =
      ModelsInfo.filter(i => !i._2.dead && i._2.recentErrors.size < 2 && i._2.succeses.nonEmpty).toVector.sortBy(_._2.averageDelay)
    Logger.println(bestModels.mkString("\n"))
    val currentModels = (bestModels.map(_._1) ++ defaultModels).distinct
//    Logger.println(currentModels.mkString("\n"))
    val attempts = currentModels.zipWithIndex.map {
      case ((model, apiKeyIndex), idx) =>
        val modelKey = (model, apiKeyIndex)
        val apiKey   = apiKeys(apiKeyIndex)
        delayed(modelKey.toString, (700 * idx).millis) {
          timed(translateImpl(toTranslate.mkString("\n"), context, prompt, from, to, apiKey, model)).map {
            case (result, d) =>
              val old = ModelsInfo.get(modelKey).getOrElse(ModelInfo(Nil))
              ModelsInfo.update(modelKey, old.addResult(result, d))
              result.result.fold(
                e => {
                  Logger.println(s"${model}:${apiKeyIndex} error")
//                  Logger.println(s"${model}:${apiKeyIndex} error '$e'")
//            Logger.println(s"${model}:${apiKeyIndex} error '$e' for request:\n${request.asJson}\ncaused by:\n${r.body.toString}")
                  sys.error(e)
                },
                res => res
              )
          }
        }
    }
    firstSuccess(attempts.map(_._2))
      .map { r =>
        untranslated = Vector()
        context = (context :+ (toTranslate.mkString("\n"), r)).takeRight(MaxContextLines)
        r
      }
      .recover {
        case e =>
          untranslated = toTranslate.takeRight(10)
          Logger.println(e)
          "."
      }
      .map { r =>
        attempts.foreach(_.cancel)
//        Logger.println(ModelsInfo.toVector.sortBy(_._1).mkString("\n"))
        r
      }
  }

  private val backend = sttp.client4.DefaultSyncBackend()
  case class Result(req: GeminiRequest, resp: Response[?], result: Either[String, String])
  def translateImpl(
      text: String,
      context: Seq[(String, String)],
      prompt: Option[String],
      from: Option[String],
      to: String,
      apiKey: String,
      model: String
  ): Future[Result] = Future {
    blocking {
      val url       = uri"https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}"
      val thePrompt = prompt.getOrElse(defaultPrompt) + from.fold(", translate to " + to)(from => ", translate from " + from + " to " + to)
      val request = GeminiRequest(
        List(
          SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
          SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
          SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
          SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE"),
          SafetySetting("HARM_CATEGORY_CIVIC_INTEGRITY", "BLOCK_NONE"),
        ),
        GenerationConfig(temperature = 0.1, seed = 100500),
        SystemInstruction(List(Text(thePrompt))),
        context.flatMap { (user, model) =>
          List(
            Content("user", List(Text(user))),
            Content("model", List(Text(model))),
          )
        } ++ List(
          Content("user", List(Text(text)))
        ),
      )
      val r = basicRequest.post(url).body(asJson(request)).header("Content-Type", "application/json").send(backend)
      val response = for {
        body <- r.body
        res  <- deserializeJson[GeminiResponse].apply(body).left.map(_.toString)
        c    <- res.candidates.headOption.toRight("No candidates")
      } yield c.content.parts.map(_.text).mkString(", ")
      Result(request, r, response)
    }
  }
}
