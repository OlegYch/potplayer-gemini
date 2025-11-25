import scala.scalanative.unsafe.{CBool, Ptr, extern, link}
import scala.scalanative.windows.DWord
import scala.scalanative.windows.HandleApi.Handle
import scala.scalanative.windows.winnt.AccessToken

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

  def GetExitCodeProcess(
      hProcess: Handle,
      lpExitCode: Ptr[DWord],
  ): CBool = extern
}
