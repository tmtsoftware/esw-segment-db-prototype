import sbt.Keys._
import sbt._

import Dependencies._
import Settings._

lazy val `esw-segment-db` = project
  .enablePlugins(DeployApp)
  .settings(appSettings: _*)
  .settings(libraryDependencies ++= `databaseTests-deps`)
