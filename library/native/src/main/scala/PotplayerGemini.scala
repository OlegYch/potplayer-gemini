import sttp.client4.*

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.scalanative.libc.stdio
import scala.scalanative.unsafe.*

object PotplayerGemini {
  var count          = 0
  private given Zone = Zone.open()
  FixStdio.fix

  @exported
  def translate(text: CString, prompt: CString, from: CString, to: CString, apiKeys: CString): CString = {
    count += 1
    val result = Gemini.translate(
      text = fromCString(text),
      prompt = Option(fromCString(prompt)).filter(_.nonEmpty),
      from = Option(fromCString(from)).filter(_.nonEmpty),
      to = fromCString(to),
      apiKeys = fromCString(apiKeys).split(" "),
    )
    toCString {
      try
        Await.result(result, 9.seconds)
      catch {
        case e: Throwable =>
          Logger.println(e)
          ".."
      }
    }
  }
}
