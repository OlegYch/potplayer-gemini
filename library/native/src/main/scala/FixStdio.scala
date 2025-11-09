import java.io.{OutputStream, PrintStream}
import scala.scalanative.*
import scala.scalanative.libc.stdio
import scala.scalanative.unsafe.*

object FixStdio {
  @link("kernel32")
  @extern
  object Kernel32 {
    def AllocConsole(): CBool                                = extern
    def AttachConsole(dwProcessId: CUnsignedInt): CBool      = extern
    def SetConsoleOutputCP(wCodePageID: CUnsignedInt): CBool = extern
    def freopen_s(
        stream: Ptr[Ptr[stdio.FILE]],
        fileName: CString,
        mode: CString,
        oldStream: Ptr[stdio.FILE]
    ): windows.crt.time.errno_t = extern
  }
  def fix(using Zone) = {
    val ATTACH_PARENT_PROCESS: CUnsignedInt = unsigned.UInt.MaxValue
    val CP_UTF8: CUnsignedInt               = unsigned.UInt.valueOf(65001)
    Kernel32.SetConsoleOutputCP(CP_UTF8)
    def reopen(name: String, stream: Ptr[libc.stdio.FILE]): Option[PrintStream] = {
      val handle      = alloc[Ptr[libc.stdio.FILE]]()
      val openedError = Kernel32.freopen_s(handle, toCString(name), toCString("w"), stream)
      Option(openedError).filter(_ == 0).map { _ =>
        val out = new OutputStream {
          def write(b: CInt): Unit = stdio.fputc(b, !handle)
        }
        new PrintStream(out, true, "utf-8") {
          override def println(x: String): Unit = Zone.acquire { implicit zone =>
            stdio.fputs(toCString(x + "\n"), !handle)
          }
          override def println(x: Any): Unit = println(String.valueOf(x))
        }
      }
    }
    reopen("CONOUT$", stdio.stdout).foreach(System.setOut)
//    reopen("CONERR$", stdio.stderr).foreach(System.setErr)
  }
}
