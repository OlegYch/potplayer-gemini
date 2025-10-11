import Gemini.translateImpl
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import sttp.client4.Response

import scala.concurrent.Future

class GeminiTest(using ExecutionEnv) extends Specification {
  private val keys  = sys.env("GEMINI_KEYS")
  private val model = Gemini.Models.head
  "Hello" in {
    translateImpl("Hello", Nil, None, None, "Italian", keys, model).map(_ must beLike { case Gemini.Result(_, _, Right("Ciao")) =>
      ok
    })
  }
  "many lines" in {
    translateImpl("Hello\nGoodbye", Nil, None, None, "Italian", keys, model).map(_ must beLike { case Gemini.Result(_, _, Right("Ciao\nArrivederci")) =>
      ok
    })
  }
  "Hello with context" in {
    translateImpl("Hello bill", List("I'm bill" -> "joe"), None, None, "Italian", keys, model).map(_ must beLike {
      case Gemini.Result(_, _, Right("Ciao joe")) =>
        ok
    })
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
