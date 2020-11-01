import sbt._

object Dependencies {

  val `esw-segment-db-deps` = Seq(
    CSW.`csw-location-client`,
    CSW.`csw-database`,
    AkkaHttp.`akka-http`,
    AkkaHttp.`akka-http-core`,
    AkkaHttp.`akka-http-spray-json`,
    Libs.`scopt`,
    Libs.`scalaTest` % Test
  )

  val `esw-segment-shared-deps` = Seq(
    AkkaHttp.`akka-http-spray-json`,
    Libs.`scala-async` % Test,
    Libs.`scalaTest` % Test,
    Akka.`akka-stream`
  )

  val `esw-segment-client-deps` = Seq(
    AkkaHttp.`akka-http`,
    AkkaHttp.`akka-http-core`,
    AkkaHttp.`akka-http-spray-json`,
    Akka.`akka-actor`,
    Akka.`akka-stream`,
    Libs.`scala-async`,
    Libs.`scopt`,
    Libs.`scalaTest` % Test
  )
}
