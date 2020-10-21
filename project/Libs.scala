import sbt._

object Libs {
  val `scopt` = "com.github.scopt" %% "scopt" % "3.7.1" //MIT License
  val `scalaTest` = "org.scalatest" %% "scalatest" % "3.1.4" // ApacheV2
}

object CSW {
  private val Org = "com.github.tmtsoftware.csw"
//  private val Version = "1.0.0"
  private val Version = "0.1.0-SNAPSHOT"

  val `csw-logging-client` = Org %% "csw-logging-client" % Version
  val `csw-commons` = Org %% "csw-commons" % Version
  val `csw-location-client` = Org %% "csw-location-client" % Version
  val `csw-database` = Org %% "csw-database" % Version
}

object AkkaHttp { //ApacheV2
  val Version = "10.2.1"
  val `akka-http` = "com.typesafe.akka" %% "akka-http" % Version
  val `akka-http-core` = "com.typesafe.akka" %% "akka-http-core" % Version
  val `akka-http-testkit` = "com.typesafe.akka" %% "akka-http-testkit" % Version
  val `akka-http-spray-json` = "com.typesafe.akka" %% "akka-http-spray-json" % Version
}
