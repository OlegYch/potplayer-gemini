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

  @link("kernel32")
  @extern
  object Kernel32 {
    def AllocConsole(): CBool      = extern
    def GetCurrentProcessId: DWord = extern
    def OpenProcess(
        dwDesiredAccess: AccessToken,
        bInheritHandle: CBool,
        dwProcessId: DWord,
    ): Handle = extern
  }
//  Kernel32.AllocConsole()
  Future {
    blocking {
      try {
        val current = Kernel32.GetCurrentProcessId.toString
        println(current)
        val processes = new ProcessBuilder("wmic process get ParentProcessID, ProcessID").start()
        val output = Iterator.continually {
          val o = Iterator.continually(processes.getInputStream.read).takeWhile(_.toChar != '\n')
          val r = o.map(_.toChar).mkString.trim
          println(s"'$r'")
          r
        }
        val parent = output.collectFirst {
          case s if s.endsWith(s" ${current}") => s.takeWhile(_ != ' ').toInt
        }
        println(parent)
        while (true) {
          val h = Kernel32.OpenProcess(AccessToken.TOKEN_READ, false, UInt.valueOf(parent.get))
          if (h != null) {
            Thread.sleep(100)
            HandleApi.CloseHandle(h)
          } else {
            sys.exit(0)
          }
        }
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
              println(s"result: ${output}")
            }
          )
    )
  }
}
