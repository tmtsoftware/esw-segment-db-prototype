import sbt.Keys._
import sbt._

import Dependencies._
import Settings._

lazy val `esw-segment-shared` = project
  .settings(buildSettings: _*)
  .settings(libraryDependencies ++= `esw-segment-shared-deps`)

lazy val `esw-segment-db` = project
  .enablePlugins(DeployApp)
  .enablePlugins(BuildInfoPlugin)
  .settings(appSettings: _*)
  .settings(libraryDependencies ++= `esw-segment-db-deps`)
  .dependsOn(`esw-segment-shared` % "compile->compile;test->test")

lazy val `esw-segment-client` = project
  .enablePlugins(DeployApp)
  .enablePlugins(BuildInfoPlugin)
  .settings(appSettings: _*)
  .settings(libraryDependencies ++= `esw-segment-client-deps`)
  .dependsOn(`esw-segment-shared` % "compile->compile;test->test")
