Global / onChangedBuildSource := ReloadOnSourceChanges

import scala.scalanative.build.{Build => *, *}

lazy val library = crossProject(JVMPlatform, NativePlatform)
  .settings(Build.librarySettings)
  .settings(
    name := "potplayer-gemini-library",
    libraryDependencies ++= Seq(
      "io.github.cquiroz"             %%% "scala-java-time" % "2.6.0",
      "com.softwaremill.sttp.client4" %%% "circe"           % "4.0.11",
      "io.circe"                      %%% "circe-generic"   % "0.14.14",
    ),
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "5.6.4" % Test,
    ),
  )
  .enablePlugins(ScalaNativeJUnitPlugin)
lazy val loader = project
  .enablePlugins(ScalaNativePlugin)
  .settings(
    scalaVersion := "3.7.3",
    name         := "potplayer-gemini-loader",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-jawn" % "0.14.14",
    ),
    nativeConfig := {
      val c = nativeConfig.value
      c.withBuildTarget(BuildTarget.libraryDynamic)
        .withLTO(LTO.none)
        .withMode(Mode.debug)
        .withGC(GC.none)
        .withMultithreading(false)
        .withCompileOptions(
          _ ++ Seq(
            s"-static",
            "-D_CRT_SECURE_NO_WARNINGS=1",
            "-Wno-macro-redefined",
          )
        )
    },
  )
lazy val root = (project in file("."))
  .settings(
    organization := "io.github.olegych",
    name         := "potplayer-gemini",
    Build.settings(loader, library.native),
  )
  .aggregate(library.native, library.jvm)
