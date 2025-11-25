import Logger.println

import scala.scalanative.unsafe
import scala.scalanative.unsigned.UInt
import scala.scalanative.windows.{DWord, HandleApi}
import scala.scalanative.windows.winnt.AccessToken

object ProcessUtils {
  def getParentProcessId = {
    val current = Kernel32.GetCurrentProcessId.toString
//    println(current)
    val processes = new ProcessBuilder("wmic process get ParentProcessID, ProcessID").start()
    val output = Iterator.continually {
      val o = Iterator.continually(processes.getInputStream.read).takeWhile(_.toChar != '\n')
      val r = o.map(_.toChar).mkString.trim
//      println(s"'$r'")
      r
    }
    val parent = output.collectFirst {
      case s if s.endsWith(s" ${current}") => s.takeWhile(_ != ' ').toInt
    }
//    println(parent)
    parent
  }

  def exitAfterParentProcess(): Unit = {
    val parent = getParentProcessId
    while (true) {
      val PROCESS_QUERY_INFORMATION = UInt.valueOf(0x0400)
      val h                         = Kernel32.OpenProcess(PROCESS_QUERY_INFORMATION, false, UInt.valueOf(parent.get))
      if (h != null) {
        try {
          val STILL_ACTIVE = 259
          val exitCode     = unsafe.stackalloc[DWord]()
          val eok          = Kernel32.GetExitCodeProcess(h, exitCode)
          if (!eok) {
            val error = scala.scalanative.windows.ErrorHandlingApi.GetLastError()
            println("!eok")
            println(error)
            println(scala.scalanative.windows.ErrorHandlingApiOps.errorMessage(error))
            sys.exit(0)
          } else if (!exitCode != STILL_ACTIVE) {
            println("exitCode: " + !exitCode)
            sys.exit(0)
          } else {
            HandleApi.CloseHandle(h)
            Thread.sleep(100)
          }
        } catch {
          case e =>
            println(e)
            sys.exit(-1)
        }
      } else {
        sys.exit(0)
      }
    }
  }
}
