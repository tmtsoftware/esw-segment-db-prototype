package esw.segment.server

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directive0, ExceptionHandler, RejectionHandler}
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.Directives._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import buildinfo.BuildInfo
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import csw.logging.api.scaladsl.Logger
import esw.segment.db.{JiraSegmentDataTable, SegmentToM1PosTable}
import esw.segment.shared.EswSegmentData.{MirrorConfig, SegmentConfig, SegmentToM1Pos, currentDate}
import sttp.tapir._
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.json.spray._
import esw.segment.shared.JsonSupport
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.swagger.akkahttp.SwaggerAkka
import sttp.tapir.openapi.circe.yaml._

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

//noinspection TypeAnnotation
class Server2(posTable: SegmentToM1PosTable, jiraSegmentDataTable: JiraSegmentDataTable, logger: Logger)(implicit
    ec: ExecutionContext,
    sys: ActorSystem[_]
) extends JsonSupport {

  private val today = currentDate()

  private def availableSegmentIds(f: Future[List[String]]): Future[List[String]] =
    async {
      val list    = await(f)
      val results = await(Future.sequence(list.map(posTable.currentSegmentPosition)))
      list.zip(results).filter(p => p._2.isEmpty || p._2.get.position.head == 'G').map(_._1)
    }

  // Convert callback to stream for progress on sync
  private def syncWithJiraStream(): Source[Int, NotUsed] = {
    val sourceDecl      = Source.queue[Int](bufferSize = 2, OverflowStrategy.backpressure)
    val (queue, source) = sourceDecl.preMaterialize()
    def callback(percent: Int): Unit = {
      queue.offer(percent)
    }
    jiraSegmentDataTable.syncWithJira(callback)
    source
  }

  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case ex: Exception =>
        extractUri { uri =>
          println(s"Request to $uri could not be handled normally")
          ex.printStackTrace()
          complete(HttpResponse(InternalServerError, entity = "Internal error"))
        }
    }

  implicit def myRejectionHandler: RejectionHandler =
    RejectionHandler.default
      .mapRejectionResponse {
        case res @ HttpResponse(_, _, ent: HttpEntity.Strict, _) =>
          // since all Akka default rejection responses are Strict this will handle all rejections
          val message = ent.data.utf8String.replaceAll("\"", """\"""")

          // we copy the response in order to keep all headers and status code, wrapping the message as hand rolled JSON
          // you could the entity using your favourite marshalling library (e.g. spray json or anything else)
          res.withEntity(HttpEntity(ContentTypes.`application/json`, s"""{"rejection": "$message"}"""))

        case x => x // pass through all other types of responses
      }

  // Tapir description of SegmentToM1Pos JSON argument
  private val segmentToM1PosBody = jsonBody[SegmentToM1Pos]
    .description(
      "Position of a segment on a given date. The the segment id can be empty if no segment is installed at the position."
    )
    .example(SegmentToM1Pos(today, Some("SN-513"), "A2"))

  // Tapir description of MirrorConfig JSON argument
  private val mirrorConfigBody = jsonBody[MirrorConfig]
    .description(
      "Holds a number of segment-id assignments for the mirror. The the segment id can be empty if no segment is installed at the position."
    )
    .example(
      MirrorConfig(
        today,
        List(
          SegmentConfig("A78", Some("SN-483")),
          SegmentConfig("B78", Some("SN-484")),
          SegmentConfig("D78", Some("SN-486"))
        )
      )
    )

  // Convert a Boolean result to an Either
  private def booleanToEither(b: Boolean) = if (b) Right(()) else Left(())

  // --- Endpoints ---

  // Note: Endpoint[I, E, O, S], where:
  //    I is the type of the input parameters
  //    E is the type of the error-output parameters
  //    O is the type of the output parameters
  //    S is the type of streams that are used by the endpoint’s inputs/outputs

  val setPosition: Endpoint[SegmentToM1Pos, Unit, Unit, Any] =
    endpoint
      .description("Insert/update segment to M1 positions mapping")
      .post
      .in("setPosition")
      .in(segmentToM1PosBody)
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode(StatusCode.BadRequest))

  def setPositionImpl(segmentToM1Pos: SegmentToM1Pos): Future[Either[Unit, Unit]] = {
    posTable.setPosition(segmentToM1Pos).map(booleanToEither)
  }

  val setPositionRoute = AkkaHttpServerInterpreter.toRoute(setPosition)(setPositionImpl)

  // ---

  val setPositions: Endpoint[MirrorConfig, Unit, Unit, Any] =
    endpoint
      .description("Set positions of a number of segments on a given date")
      .post
      .in("setPositions")
      .in(mirrorConfigBody)
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode(StatusCode.BadRequest))

  def setPositionsImpl(mirrorConfig: MirrorConfig): Future[Either[Unit, Unit]] = {
    posTable.setPositions(mirrorConfig).map(booleanToEither)
  }

  val setPositionsRoute =
    AkkaHttpServerInterpreter.toRoute(setPosition)(setPositionImpl)

  // ---

  val openApiDocs: OpenAPI =
    OpenAPIDocsInterpreter.toOpenAPI(List(setPosition, setPositions), "The ESW Segment DB API", BuildInfo.version)
  val openApiYml: String = openApiDocs.toYaml

  val logRequest: HttpRequest => Unit = req => {
    logger.info(s"${req.method.value} ${req.uri.toString()}")
  }

  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

  val routes = {
    cors() {
      routeLogger {
        concat(
          setPositionRoute,
          setPositionsRoute,
          new SwaggerAkka(openApiYml).routes)
      }
    }
  }
}
