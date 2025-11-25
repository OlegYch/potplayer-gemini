import sbt.{Keys, *}
import sbt.Keys.*
import sbtnativeimage.NativeImagePlugin.autoImport.nativeImage

import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport.nativeLink

object Build {
  lazy val build        = taskKey[Seq[(File, String)]]("build")
  lazy val deploy       = taskKey[Unit]("deploy")
  lazy val deployTarget = settingKey[File]("potplayer dir")

  case class Config(DefaultPause: Int, MaxContextLines: Int, Name: String)

  val libs = Seq(
    ("unistring", "unistring-5.dll"),
    ("idn2", "idn2-0.dll"),
    ("libcrypto", "libcrypto-3-x64.dll"),
    ("zlib", "zlib1.dll"),
    ("libcurl", "libcurl.dll"),
  )
  val translatorDlls = settingKey[Seq[File]]("translatorDlls")
  val copyDlls       = taskKey[Seq[File]]("copyDlls")

  val useGraal = false
  def settings(loader: Project, translatorNative: Project, translatorJVM: Project) = Seq(
    deployTarget                                         := file("""d:\program files\PotPlayer\"""),
    Compile / Keys.compile / Keys.skip                   := true,
    Compile / Keys.packageBin / Keys.artifactName        := ((_, _, _) => "potplayer-gemini.zip"),
    Compile / Keys.packageBin / Keys.mappings            := build.value,
    Compile / Keys.unmanagedSourceDirectories            := Seq((Compile / Keys.sourceDirectory).value / "as"),
    Compile / Keys.unmanagedSources / Keys.includeFilter := ("*.as"),
    Compile / Keys.packageTimestamp                      := None,
    build := {
      val debug = true // todo disable on release
//      val debug = deployTarget.value.exists()
      val target     = (Compile / Keys.productDirectories).value.head
      val source     = IO.read((Compile / Keys.sources).value.head)
      val sourceIcon = (Compile / Keys.resourceDirectory).value / "gemini.ico"
      val configs = List(
        Config(300, 50, "Gemini-Free"),
        Config(0, 100, "Gemini-Paid"),
      )
      val translatorFiles = Def.taskIf {
        if (useGraal)
          Seq(file((translatorJVM / Compile / nativeImage).value.getAbsolutePath + ".exe"))
        else
          (translatorNative / translatorDlls).value :+ (translatorNative / Compile / nativeLink).value
      }.value
      val loaderLib = (loader / Compile / nativeLink).value
      IO.copyFile(loaderLib, target / loaderLib.name)
      val targetLibs = (loaderLib -> loaderLib.name) +: translatorFiles.map { lib =>
        val targeFile = target / "potplayer-gemini" / lib.name
        IO.copyFile(lib, targeFile)
        targeFile -> s"potplayer-gemini/${lib.name}"
      }
      val files = configs.flatMap { config =>
        val libsArray = Seq(loaderLib.name).map(f => s"'${f}'").mkString(", ")
        val compiled = source
          .replaceFirst("uint DefaultPause =.*", "uint DefaultPause = " + config.DefaultPause + ";")
          .replaceFirst("uint MaxContextLines =.*", "uint MaxContextLines = " + config.MaxContextLines + ";")
          .replaceFirst("string Name =.*", "string Name = \"" + config.Name + "\";")
          .replaceFirst("bool debug =.*", "bool debug = " + debug + ";")
          .replaceFirst("array<string> libs = .*", s"array<string> libs = {$libsArray};")
        val compiledAs = target / s"SubtitleTranslate - ${config.Name}.as"
        IO.write(compiledAs, compiled)
        val compiledIcon = target / s"SubtitleTranslate - ${config.Name}.ico"
        IO.copyFile(sourceIcon, compiledIcon)
        Seq(compiledAs, compiledIcon).map(f => f -> f.name)
      }
      files ++ targetLibs
    },
    deploy := {
      import scala.sys.process.*
      val built = build.value
      (deployTarget.value / "KillPot64.exe").absolutePath.!
      IO.copy(built.map(f => (f._1, deployTarget.value / "Extension/Subtitle/Translate" / f._2)), CopyOptions().withOverwrite(true))
      (deployTarget.value / "PotPlayerMini64.exe").absolutePath.run()
    },
  )

  import scala.scalanative.build.*
  import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport.*

  def baseSettings = Seq(
    scalaVersion := "3.7.3",
    scalacOptions ++= Seq(
      "-deprecation",
    ),
  )
  def librarySettings = Seq(
    translatorDlls := {
      val base = (ThisBuild / baseDirectory).value.absolutePath
      libs.map {
        case (name, dll) =>
          file(s"$base/vcpkg_installed/x64-windows/bin/$dll")
      }
    },
    copyDlls := {
      val t = (Compile / target).value
      translatorDlls.value.map { f =>
        val r = t / s"scala-${scalaVersion.value}" / f.name
        sbt.IO.copyFile(f, r)
        r
      }
    },
    Compile / runMain           := (Compile / runMain).dependsOn(copyDlls).evaluated,
    Compile / run               := (Compile / run).dependsOn(copyDlls).evaluated,
    Test / test / testExecution := (Test / test / testExecution).dependsOn(copyDlls).value,
    nativeConfig := {
      val c    = nativeConfig.value
      val base = (ThisBuild / baseDirectory).value.absolutePath
      c.withLTO(LTO.none)     // thin
        .withMode(Mode.debug) // releaseFast
        .withGC(GC.immix)     // commix
        .withCompileOptions(
          _ ++ Seq(
            s"-static",
            "-DGC_INITIAL_HEAP_SIZE=5m",
            "-DGC_MAXIMUM_HEAP_SIZE=100m",
            "-D_CRT_SECURE_NO_WARNINGS=1",
            "-Wno-macro-redefined",
            s"-I$base/vcpkg_installed/x64-windows/include",
          )
        )
        .withLinkingOptions(
          _ ++ Seq(
            s"-static",
          ) ++ libs.flatMap {
            case (name, _) =>
              Seq(
                s"-l$name",
                s"-L$base/vcpkg_installed/x64-windows/lib",
              )
          }
        )
    },
  )
}
