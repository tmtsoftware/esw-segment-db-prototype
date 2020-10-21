import sbt._

object Dependencies {

  val `databaseTests-deps` = Seq(
    CSW.`csw-location-client`,
    CSW.`csw-database`,
    AkkaHttp.`akka-http`,
    AkkaHttp.`akka-http-core`,
    AkkaHttp.`akka-http-spray-json`,
    Libs.`scalaTest`       % Test
  )
}
