Global / onChangedBuildSource := ReloadOnSourceChanges
lazy val root = (project in file("."))
  .settings(
    organization := "io.github.olegych",
    name         := "potplayer-gemini",
    Build.settings
  )
