import sbt._

object Dependencies {

  //akka-actor-typed, akka-remote, akka-pki, akka-stream-typed
  val `esw-segment-db-deps` = Seq(
    CSW.`csw-location-client`,
    CSW.`csw-database`,
    CSW.`csw-aas-http`,
    Akka.`akka-actor-typed`,
    Akka.`akka-slf4j`,
    Akka.`akka-remote`,
    Akka.`akka-stream-typed`,
    // XXX fix indirect dependency conflict
    Akka.`akka-pki`,
    Akka.`akka-protobuf-v3`,

    Tapir.`tapir-json-spray`,
    Tapir.`tapir-core`,
    Tapir.`tapir-akka-http-server`,
    Tapir.`tapir-openapi-docs`,
    Tapir.`tapir-openapi-circe-yaml`,
    Tapir.`openapi-circe-yaml`,
    Tapir.`tapir-swagger-ui-akka-http`,
    Tapir.`tapir-redoc-akka-http`,
    AkkaHttp.`akka-http`,
    AkkaHttp.`akka-http-core`,
    AkkaHttp.`akka-http-spray-json`,
    AkkaHttp.`akka-http-cors`,
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
