import sbt.*
import sbt.Keys
import sbt.Keys.*

object Build {
  lazy val build        = taskKey[Seq[File]]("build")
  lazy val deploy       = taskKey[Unit]("deploy")
  lazy val deployTarget = settingKey[File]("potplayer dir")

  case class Config(DefaultPause: Int, MaxContextLines: Int, Name: String)

  def settings = Seq(
    deployTarget                                  := file("""d:\program files\PotPlayer\"""),
    Compile / Keys.compile / Keys.skip            := true,
    Compile / Keys.packageBin / Keys.artifactName := ((_, _, _) => "potplayer-gemini.zip"),
    Compile / Keys.productDirectories             := Seq(file("target/potplayer-gemini")),
    Compile / Keys.products := {
      build.value ++
        (Compile / Keys.productDirectories).value
    },
    Compile / Keys.unmanagedSourceDirectories            := Seq((Compile / Keys.sourceDirectory).value / "as"),
    Compile / Keys.unmanagedSources / Keys.includeFilter := ("*.as"),
    Compile / Keys.packageTimestamp                      := None,
    build := {
      val debug      = deployTarget.value.exists()
      val target     = (Compile / Keys.productDirectories).value.head
      val source     = IO.read((Compile / Keys.sources).value.head)
      val sourceIcon = (Compile / Keys.resourceDirectory).value / "gemini.ico"
      val configs = List(
        Config(300, 50, "Gemini-Free"),
        Config(0, 100, "Gemini-Paid"),
      )
      val files = configs.flatMap { config =>
        val compiled = source
          .replaceFirst("uint DefaultPause =.*", "uint DefaultPause = " + config.DefaultPause + ";")
          .replaceFirst("uint MaxContextLines =.*", "uint MaxContextLines = " + config.MaxContextLines + ";")
          .replaceFirst("string Name =.*", "string Name = \"" + config.Name + "\";")
          .replaceFirst("bool debug =.*", "bool debug = " + debug + ";")
        val compiledAs = target / s"SubtitleTranslate - ${config.Name}.as"
        IO.write(compiledAs, compiled)
        val compiledIcon = target / s"SubtitleTranslate - ${config.Name}.ico"
        IO.copyFile(sourceIcon, compiledIcon)
        Seq(compiledAs, compiledIcon)
      }
      files
    },
    deploy := {
      val built = build.value
      IO.copy(built.map(f => (f, deployTarget.value / "Extension/Subtitle/Translate" / f.name)), CopyOptions().withOverwrite(true))
      import scala.sys.process.*
      (deployTarget.value / "KillPot64.exe").absolutePath.!
      (deployTarget.value / "PotPlayerMini64.exe").absolutePath.run()
    },
  )

  import scala.scalanative.build.*
  import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport.*
  def librarySettings = Seq(
    scalaVersion := "3.7.3",
    nativeConfig := {
      val c = nativeConfig.value
      c.withLTO(LTO.none)     // thin
        .withMode(Mode.debug) // releaseFast
        .withGC(GC.immix)     // commix
        .withCompileOptions(
          _ ++ Seq(
            s"-static",
            "-D_CRT_SECURE_NO_WARNINGS=1",
            "-Wno-macro-redefined",
            s"-I${(ThisBuild / baseDirectory).value.absolutePath}/vcpkg_installed/vcpkg/pkgs/curl_x64-windows/include",
          )
        )
        .withLinkingOptions(
          _ ++ Seq(
            s"-static",
            s"-llibcurl",
            s"-L${(ThisBuild / baseDirectory).value.absolutePath}/vcpkg_installed/vcpkg/pkgs/curl_x64-windows/lib",
            s"-lidn2",
            s"-L${(ThisBuild / baseDirectory).value.absolutePath}/vcpkg_installed/vcpkg/pkgs/libidn2_x64-windows/lib",
            s"-lunistring",
            s"-L${(ThisBuild / baseDirectory).value.absolutePath}/vcpkg_installed/vcpkg/pkgs/libunistring_x64-windows/lib",
          )
        )
    },
  )
}
