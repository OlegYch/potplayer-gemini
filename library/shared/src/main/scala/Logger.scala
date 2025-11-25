import java.io.File
import java.nio.file.{Files, StandardOpenOption}

object Logger {
  private val logFile = Files.createTempFile("potplayer-gemini", ".txt")
  private def writeLog(a: Any) = {
    Files.writeString(logFile, Option(a).getOrElse("null").toString + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE)
  }
  def println(s: Throwable): Unit = {
    writeLog(s.getMessage)
    s.getStackTrace.foreach(writeLog)
    s.printStackTrace(System.out)
  }
  def println(s: Any): Unit = {
    writeLog(s)
    System.out.println(s)
  }
}
