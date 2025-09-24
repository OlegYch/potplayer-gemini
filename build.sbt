Global / onChangedBuildSource := ReloadOnSourceChanges

import scala.scalanative.build.{Build => *, *}

lazy val library = crossProject(JVMPlatform, NativePlatform)
  .settings(Build.librarySettings)
  .settings(
    name := "potplayer-gemini-library",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client4" %%% "circe"         % "4.0.11",
      "io.circe"                      %%% "circe-generic" % "0.14.14",
    ),
    nativeConfig := {
      val c = nativeConfig.value
      c.withBuildTarget(BuildTarget.libraryDynamic)
    },
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "5.6.4" % Test,
    ),
  )
lazy val app = project
  .enablePlugins(ScalaNativePlugin)
//  .dependsOn(library.native)
  .settings(Build.librarySettings)
lazy val root = (project in file("."))
  .settings(
    organization := "io.github.olegych",
    name         := "potplayer-gemini",
    Build.settings(library.native),
  )
  .aggregate(library.native, library.jvm, app)
