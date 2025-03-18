import sbt.*
import sbt.Keys

object Build {
  lazy val build  = taskKey[Seq[File]]("build")
  lazy val deploy = taskKey[Unit]("deploy")
  case class Config(DefaultPause: Int, MaxContextLines: Int, Model: String, Name: String)

  def settings = Seq(
    Compile / Keys.compile / Keys.skip                   := true,
    Compile / Keys.packageBin / Keys.artifactName        := ((_, _, _) => "potplayer-gemini.zip"),
    Compile / Keys.productDirectories                    := Seq(file("target/potplayer-gemini")),
    Compile / Keys.products                              := {
      build.value
      (Compile / Keys.productDirectories).value
    },
    Compile / Keys.unmanagedSourceDirectories            := Seq((Compile / Keys.sourceDirectory).value / "as"),
    Compile / Keys.unmanagedSources / Keys.includeFilter := ("*.as"),
    build := {
      val target     = (Compile / Keys.productDirectories).value.head
      val source     = IO.read((Compile / Keys.sources).value.head)
      val sourceIcon = (Compile / Keys.resourceDirectory).value / "gemini.ico"
      val configs = List(
        Config(500, 50, "gemini-2.0-flash", "Gemini-Flash-Free"),
        Config(0, 100, "gemini-2.0-flash", "Gemini-Flash-Paid"),
        Config(200, 50, "gemini-2.0-flash-lite", "Gemini-Lite-Free"),
      )
      val files = configs.flatMap { config =>
        val compiled = source
          .replaceFirst("uint DefaultPause =.*", "uint DefaultPause = " + config.DefaultPause + ";")
          .replaceFirst("uint MaxContextLines =.*", "uint MaxContextLines = " + config.MaxContextLines + ";")
          .replaceFirst("string Model =.*", "string Model = \"" + config.Model + "\";")
          .replaceFirst("string Name =.*", "string Name = \"" + config.Name + "\";")
        val compiledAs = target / s"SubtitleTranslate - ${config.Name}.as"
        IO.write(compiledAs, compiled)
        val compiledIcon = target / s"SubtitleTranslate - ${config.Name}.ico"
        IO.copyFile(sourceIcon, compiledIcon)
        Seq(compiledAs, compiledIcon)
      }
      files
    },
    deploy := {
      val built        = build.value
      val deployTarget = file("""d:\program files\PotPlayer\Extension\Subtitle\Translate""")
      IO.copy(built.map(f => (f, deployTarget / f.name)), CopyOptions().withOverwrite(true))
    },
  )
}
