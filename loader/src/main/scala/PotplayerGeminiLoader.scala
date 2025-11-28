import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import io.circe.jawn.parse

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.{Charset, StandardCharsets}
import scala.scalanative.unsafe
import scala.scalanative.unsafe.{CChar, CString, CWideString, Zone, exported, extern, fromCString, fromCWideString, link, toCString}
import scala.scalanative.unsigned.UInt
import scala.scalanative.windows.HandleApi.Handle
import scala.scalanative.windows.{CWString, DWord, WChar}

object PotplayerGeminiLoader {
  @link("kernel32")
  @extern
  object Kernel32 {
    def GetModuleFileNameW(
        hModule: Handle,
        lpFilename: CWString,
        nSize: DWord
    ): DWord = extern
    def GetModuleFileNameA(
        hModule: Handle,
        lpFilename: CString,
        nSize: DWord
    ): DWord = extern
  }

  private given Zone = Zone.open()
  FixStdio.fix
  val potplayerExe = unsafe.alloc[WChar](10000)
  Kernel32.GetModuleFileNameW(null, potplayerExe, UInt.valueOf(10000))
  val potplayerExeString = fromCWideString(potplayerExe, StandardCharsets.UTF_16LE)
  println(potplayerExeString)
  private val translatorExe = potplayerExeString.toLowerCase.replaceAll("\\w+.exe", "") + "\\Extension\\Subtitle\\Translate\\potplayer-gemini\\potplayer-gemini-library.exe"
  println(translatorExe)
  val process = new ProcessBuilder(translatorExe).start()
  val reader  = new BufferedReader(new InputStreamReader(process.getInputStream))

  case class Input(text: String, prompt: String, from: String, to: String, apiKeys: String) derives Encoder
  case class Output(text: String) derives Decoder
  @exported
  def translate(text: CString, prompt: CString, from: CString, to: CString, apiKeys: CString): CString = try {
    import io.circe.*
    val input = Input(
      text = fromCString(text),
      prompt = fromCString(prompt),
      from = fromCString(from),
      to = fromCString(to),
      apiKeys = fromCString(apiKeys),
    ).asJson.noSpaces + "\n"
    process.getOutputStream.write(input.getBytes)
    val output = Iterator.continually(reader.readLine().trim)
    val result = output.map(output => output -> parse(output).flatMap(_.as[Output])).flatMap {
      case (_, Right(output)) => Some(output.text)
      case (r, _) =>
        println(r)
        None
    }
    toCString(result.next())
  } catch {
    case e: Throwable =>
      e.printStackTrace(Console.out)
      toCString(e.getMessage)
  }
}
