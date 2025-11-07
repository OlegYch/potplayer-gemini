import Gemini.translateImpl
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import sttp.client4.Response

import scala.concurrent.Future

class GeminiTest(using ExecutionEnv) extends Specification {
  private val keys = sys.env("GEMINI_KEYS")
//  private val model = "gemini-2.0-flash"
  private val model = Gemini.Models.head
  "Hello" in {
    translateImpl("Hello", Nil, None, None, "Italian", keys, model).map(_ must beLike {
      case Gemini.Result(result = Right("Ciao")) =>
        ok
    })
  }
  "many lines" in {
    translateImpl("Hello\nGoodbye", Nil, None, None, "Italian", keys, model).map(_ must beLike {
      case Gemini.Result(result = Right("Ciao\nArrivederci")) =>
        ok
    })
  }
  // todo find a more reliable test
  "Hello with context" in {
    for {
      resp <- translateImpl(
        "You're very beatiful",
        List("Hi i'm Annie, what do you think of me" -> "Ciao, sono Annie, cosa pensi di me?"),
        None,
        None,
        "Italian",
        keys,
        model
      )
      _ = resp must beLike {
        case Gemini.Result(result = Right("Sei molto bella.")) =>
          ok
      }
      resp <- translateImpl(
        "You're very beatiful",
        List("Hi i'm Bob, what do you think of me" -> "Ciao, sono Bob, cosa pensi di me?"),
        None,
        None,
        "Italian",
        keys,
        model
      )
      _ = resp must beLike {
        case Gemini.Result(result = Right("Sei molto bello.")) =>
          ok
      }
    } yield ok
  }
//  val fakeGemini = new Gemini {
//    override def translateImpl(
//        text: String,
//        context: List[(String, String)],
//        prompt: Option[String],
//        from: Option[String],
//        to: String,
//        apiKey: String,
//        model: String
//    ): Future[(GeminiRequest, Response[?], Either[String, String])] = Future(text)
//  }
}
