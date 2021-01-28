import sbt._

object Libs {
  val `scopt` = "com.github.scopt" %% "scopt" % "4.0.0" //MIT License
  val `scalaTest` = "org.scalatest" %% "scalatest" % "3.2.3" // ApacheV2
  val `scala-async` = "org.scala-lang.modules" %% "scala-async" % "1.0.0-M1" //BSD 3-clause "New" or "Revised" License
}

object Tapir {
  private val Version = "0.17.7"
  val `tapir-json-spray` = "com.softwaremill.sttp.tapir" %% "tapir-json-spray" % Version
  val `tapir-core` = "com.softwaremill.sttp.tapir" %% "tapir-core" % Version
  val `tapir-akka-http-server` = "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % Version
  val `tapir-openapi-docs` = "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % Version
  val `tapir-openapi-circe-yaml` = "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % Version
  val `tapir-swagger-ui-akka-http` = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http" % Version
  val `tapir-redoc-akka-http` = "com.softwaremill.sttp.tapir" %% "tapir-redoc-akka-http" % Version
}

object CSW {
  private val Org = "com.github.tmtsoftware.csw"
//  private val Version = "0.1.0-SNAPSHOT"
  private val Version = "3.0.1"

  val `csw-logging-client` = Org %% "csw-logging-client" % Version
  val `csw-commons` = Org %% "csw-commons" % Version

  val `csw-location-client` = Org %% "csw-location-client" % Version
  val `csw-database` = Org %% "csw-database" % Version
}

object AkkaHttp { //ApacheV2
  val Version = "10.2.3"
  val `akka-http` = "com.typesafe.akka" %% "akka-http" % Version
  val `akka-http-core` = "com.typesafe.akka" %% "akka-http-core" % Version
  val `akka-http-testkit` = "com.typesafe.akka" %% "akka-http-testkit" % Version
  val `akka-http-spray-json` = "com.typesafe.akka" %% "akka-http-spray-json" % Version

  val `akka-http-cors` = "ch.megard" %% "akka-http-cors" % "1.1.1"
}

object Akka {
  val Version = "2.6.11" //all akka is Apache License 2.0

  val `akka-stream`              = "com.typesafe.akka" %% "akka-stream"              % Version
  val `akka-stream-typed`        = "com.typesafe.akka" %% "akka-stream-typed"        % Version
  val `akka-remote`              = "com.typesafe.akka" %% "akka-remote"              % Version
  val `akka-stream-testkit`      = "com.typesafe.akka" %% "akka-stream-testkit"      % Version
  val `akka-actor`               = "com.typesafe.akka" %% "akka-actor"               % Version
  val `akka-actor-typed`         = "com.typesafe.akka" %% "akka-actor-typed"         % Version
  val `akka-actor-testkit-typed` = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version
  val `akka-distributed-data`    = "com.typesafe.akka" %% "akka-distributed-data"    % Version
  val `akka-multi-node-testkit`  = "com.typesafe.akka" %% "akka-multi-node-testkit"  % Version
  val `akka-cluster-tools`       = "com.typesafe.akka" %% "akka-cluster-tools"       % Version
  val `akka-cluster`             = "com.typesafe.akka" %% "akka-cluster"             % Version
  val `akka-cluster-typed`       = "com.typesafe.akka" %% "akka-cluster-typed"       % Version
  val `akka-slf4j`               = "com.typesafe.akka" %% "akka-slf4j"               % Version
  val `cluster-sharding`         = "com.typesafe.akka" %% "akka-cluster-sharding"    % Version
  val `akka-persistence`         = "com.typesafe.akka" %% "akka-persistence"         % Version

  //, akka-pki
}
