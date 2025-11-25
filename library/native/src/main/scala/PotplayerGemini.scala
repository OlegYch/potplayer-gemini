import Logger.println
import io.circe.Decoder
import sttp.client4.*

import java.io.{BufferedReader, InputStreamReader}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future, blocking}
import scala.scalanative.libc.stdio
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UInt
import scala.scalanative.windows.HandleApi.Handle
import scala.scalanative.windows.ProcessThreadsApi.{GetCurrentProcess, OpenProcessToken}
import scala.scalanative.windows.winnt.AccessToken
import scala.scalanative.windows.{DWord, ErrorHandlingApi, ErrorHandlingApiOps, HandleApi, ProcessThreadsApi}

object PotplayerGemini extends App {
  var count          = 0
  private given Zone = Zone.open()

  case class Input(text: String, prompt: String, from: String, to: String, apiKeys: String) derives Decoder
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
        s"<u><b>${Await.result(result, 9.seconds)}</b></u>"
      catch {
        case e: Throwable =>
          println(e)
          ".."
      }
    }
  }

  Future {
    blocking {
      try {
        ProcessUtils.exitAfterParentProcess()
      } catch {
        case e => e.printStackTrace()
      }
    }
  }

  while (true) {
    val input  = Console.in.readLine()
    val parsed = io.circe.jawn.parse(input)
    parsed.fold(
      e => println(e),
      js =>
        js.as[Input]
          .fold(
            e => println(e),
            input => {
              val result = Gemini.translate(
                text = input.text,
                prompt = Option(input.prompt).filter(_.nonEmpty),
                from = Option(input.from).filter(_.nonEmpty),
                to = input.to,
                apiKeys = input.apiKeys.split(" "),
              )
              val output =
                try
                  s"<u><b>${Await.result(result, 9.seconds)}</b></u>"
                catch {
                  case e: Throwable =>
                    println(e)
                    ".."
                }
              println(s"result: ${output.replaceAll("\n", " ")}")
            }
          )
    )
  }
}
