import sbt.*
import sbt.Keys

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
}
