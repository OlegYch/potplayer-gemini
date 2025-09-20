import scala.scalanative.unsafe.*
import sttp.client4.*

import scala.concurrent.Future

object PotplayerGemini {
  @exported
  def add(a: Int, b: Int): Int = a + b

  @exported
  def sayHello(i: Int): Int = {
//    val t = new Thread({ () =>
//      Thread.sleep(1000)
    println(s"Hello from Scala Native DLL! ${i}")
//    })
//    t.start
//    t
    i + 2
  }
  @exported
  def translate(Text: String) = {
    val Models = List(
      "gemini-2.5-flash"
      , "gemini-2.0-flash"
      , "gemini-2.5-flash-lite"
      , "gemini-2.0-flash-lite"
    )

    val api_key = ""
    val prompt = "You are an expert subtitle translator, you can use profane language if it is present in the source, output only the translation";

    val Model = Models.last
    val header =   "Content-Type: application/json";

    val Untranslated = ""
    val context = "";
    val Post =
      """{
    "safety_settings":[
        {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
        {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"},
        {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
        {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"},
        {"category": "HARM_CATEGORY_CIVIC_INTEGRITY", "threshold": "BLOCK_NONE"}
      ],
    "generation_config": {"temperature": 0.1, "seed": 100500},
    "system_instruction": { "parts": { "text": " """ + prompt +
        """ "}},
    "contents": [ """ +
        context +
        """{"role":"user", "parts":[{"text": " """ + Untranslated + Text +
        """ "}]}
    ]
    }""";

//    val b = sttp.client4.DefaultFutureBackend()
    val b = sttp.client4.DefaultSyncBackend()
//
    val url =   uri"https://generativelanguage.googleapis.com/v1beta/models/${Model}:generateContent?key=${api_key}"
    import scala.concurrent.ExecutionContext.Implicits.global
    Future(basicRequest.get(url).header("Content-Type", "application/json").send(b)).map(println)
//    import sttp.client4.quick.*
//    println(quickRequest.get(uri"http://httpbin.org/ip").send())
  }
}
