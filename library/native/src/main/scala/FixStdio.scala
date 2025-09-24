import scala.scalanative.unsafe.*

import java.io.{OutputStream, PrintStream}
import scala.scalanative.libc.stdio
import scala.scalanative.unsafe.{Zone, toCString}

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
    ): scala.scalanative.windows.crt.time.errno_t = extern
  }
  def fix(using Zone) = {
    val ATTACH_PARENT_PROCESS: CUnsignedInt = scala.scalanative.unsigned.UInt.MaxValue
    val CP_UTF8: CUnsignedInt               = scala.scalanative.unsigned.UInt.valueOf(65001)
    Kernel32.SetConsoleOutputCP(CP_UTF8)
    val stdout      = alloc[Ptr[stdio.FILE]]()
    val openedError = Kernel32.freopen_s(stdout, toCString("CONOUT$"), toCString("w"), stdio.stdout)
    if (openedError == 0) {
      System.setOut(
        new PrintStream(
          new OutputStream {
            def write(b: CInt): Unit = stdio.fputc(b, !stdout)
          },
          true,
          "utf-8"
        ) {
          override def println(x: String): Unit = Zone.acquire { implicit zone =>
            stdio.fputs(toCString(x + "\n"), !stdout)
          }
          override def println(x: Any): Unit = println(String.valueOf(x))
        }
      )
    }
  }
}
