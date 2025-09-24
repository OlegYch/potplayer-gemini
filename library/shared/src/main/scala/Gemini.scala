import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import sttp.client4.*
import sttp.client4.circe.*

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Future, blocking}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

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
  val Models = List(
    "gemini-2.5-flash",
    "gemini-2.0-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.0-flash-lite"
  )

  private case class ModelInfo(delay: Duration, errors: Int)
  private val ModelsInfo = TrieMap[(String, Int), ModelInfo]()
  private def timed[T](f: => Future[T]) = {
    val start = System.currentTimeMillis()
    f.map(_ -> Duration(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS))
  }
  private def firstSuccess[T](f: List[Future[T]]): Future[T] = f match {
    case _ :: tail =>
      Future.firstCompletedOf(f).recoverWith { case e =>
        firstSuccess(tail)
      }
    case Nil => Future.failed(new Exception("couldn't find success") with scala.util.control.NoStackTrace)
  }
  @transient private var untranslated = Vector[String]()
  @transient private var context      = Vector[(String, String)]()
  def translate(text: String, prompt: Option[String], from: Option[String], to: String, apiKeys: Seq[String]): Future[String] = {
    val toTranslate = untranslated :+ text
    val attempts = Models.map { model =>
      val apiKeyIndex = Random.nextInt(apiKeys.size)
      val apiKey      = apiKeys(apiKeyIndex)
      timed(translateImpl(toTranslate.mkString("\n"), context, prompt, from, to, apiKey, model)).map { case ((request, r, res), d) =>
        val old = ModelsInfo.get((model, apiKeyIndex))
        res.fold(
          e => {
            ModelsInfo.update((model, apiKeyIndex), old.fold(ModelInfo(d, 1))(old => old.copy(errors = old.errors + 1)))
            Logger.println(s"${model}:${apiKeyIndex} error '$e' for request:\n${request.asJson}\ncaused by:\n${r.body.toString}")
            sys.error(e)
          },
          res => {
            ModelsInfo.update((model, apiKeyIndex), old.fold(ModelInfo(d, 0))(_.copy(delay = d)))
            res
          }
        )
      }
    }
    firstSuccess(attempts)
      .map { r =>
        untranslated = Vector()
        context = (context :+ (toTranslate.mkString("\n"), r)).takeRight(MaxContextLines)
        r
      }
      .recover { case e =>
        untranslated = toTranslate.takeRight(10)
        Logger.println(e)
        "."
      }
      .andThen { _ =>
        Logger.println(ModelsInfo.toVector.sortBy(_._1).mkString("\n"))
      }
  }

  private val backend = sttp.client4.DefaultSyncBackend()
  def translateImpl(
      text: String,
      context: Seq[(String, String)],
      prompt: Option[String],
      from: Option[String],
      to: String,
      apiKey: String,
      model: String
  ): Future[(GeminiRequest, Response[?], Either[String, String])] = Future {
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
      (request, r, response)
    }
  }
}
