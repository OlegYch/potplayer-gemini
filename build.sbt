Global / onChangedBuildSource := ReloadOnSourceChanges
import scala.scalanative.build.{Build => *, *}
lazy val library = project
  .enablePlugins(ScalaNativePlugin)
  .settings(Build.librarySettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client4" %%% "core" % "4.0.10",
    ),
    nativeConfig := {
      val c = nativeConfig.value
      c.withBuildTarget(BuildTarget.libraryDynamic)
    },
  )
lazy val app = project
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(library)
  .settings(Build.librarySettings)
lazy val root = (project in file("."))
  .settings(
    organization := "io.github.olegych",
    name         := "potplayer-gemini",
    Build.settings,
  )
  .dependsOn(library)
  .aggregate(library, app)
