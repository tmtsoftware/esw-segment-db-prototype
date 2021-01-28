package esw.segment.server

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directive0, ExceptionHandler, RejectionHandler, Route}
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import buildinfo.BuildInfo
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import csw.logging.api.scaladsl.Logger
import esw.segment.db.{JiraSegmentDataTable, SegmentToM1PosTable}
import esw.segment.shared.EswSegmentData.{
  AllSegmentPositions,
  DateRange,
  MirrorConfig,
  SegmentConfig,
  SegmentToM1Pos,
  currentDate
}
import sttp.tapir._
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.json.spray._
import esw.segment.shared.JsonSupport
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.swagger.akkahttp.SwaggerAkka
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.generic.Derived

import java.time.LocalDate
import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

object DocumentedRoutes {
  // See https://tapir.softwaremill.com/en/latest/endpoint/customtypes.html?highlight=path%5B#customising-derived-schemas
  // Add descriptions to schema fields here so we don't need to add tapir as a dependency to the esw-segment-shared project that
  // contains the case classes.
  implicit val customSegmentToM1PosSchema: Schema[SegmentToM1Pos] = implicitly[Derived[Schema[SegmentToM1Pos]]].value
    .modify(_.date)(_.description("date of record"))
    .modify(_.maybeId)(_.description("the segment id, if the segment is present"))
    .modify(_.position)(_.description("position of segment (For example: A32, B19, F82)"))
}

//noinspection TypeAnnotation
class DocumentedRoutes(posTable: SegmentToM1PosTable, jiraSegmentDataTable: JiraSegmentDataTable, logger: Logger)(implicit
    ec: ExecutionContext,
    sys: ActorSystem[_]
) extends JsonSupport {

  import DocumentedRoutes._

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

  private implicit def myExceptionHandler: ExceptionHandler = {
    import akka.http.scaladsl.server.Directives._
    ExceptionHandler {
      case ex: Exception =>
        extractUri { uri =>
          println(s"Request to $uri could not be handled normally")
          ex.printStackTrace()
          complete(HttpResponse(InternalServerError, entity = "Internal error"))
        }
    }
  }

  private implicit def myRejectionHandler: RejectionHandler =
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
      "Position of a segment on a given date. A segment id can be empty if no segment is installed at the position."
    )
    .example(SegmentToM1Pos(today, Some("SN-513"), "A2"))

  // Tapir description of List[SegmentToM1Pos] JSON result
  private val segmentToM1PosListBody = jsonBody[List[SegmentToM1Pos]]
    .description(
      "A list of objects mapping segment id to position on a given date"
    )
    .example(
      List(
        SegmentToM1Pos(today, Some("SN-484"), "B78"),
        SegmentToM1Pos(today, None, "D78")
      )
    )

  // Tapir description of MirrorConfig JSON argument
  private val mirrorConfigBody = jsonBody[MirrorConfig]
    .description(
      "Holds a number of segment-id assignments for the mirror. A segment id can be empty if no segment is installed at the position."
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

  // Tapir description of AllSegmentPositions JSON argument
  private val allSegmentPositionsBody = jsonBody[AllSegmentPositions]
    .description(
      "Holds all 574 segment-id assignments (A1 to G8, including spares) for the mirror. A segment id can be empty if no segment is installed at the position."
    )
    .example(
      AllSegmentPositions(
        today,
        List(
          Some("SN-013"),
          Some("SN-450"),
          Some("SN-019"),
          None,
          Some("SN-007"),
          Some("etc...")
        )
      )
    )

  // Tapir description of DateRange JSON argument
  private val dateRangeBody = jsonBody[DateRange]
    .description(
      "A range of dates, from, to, where each date has the format yyyy-mm-dd"
    )
    .example(
      DateRange(
        LocalDate.parse("2020-10-23"),
        today
      )
    )

  // Convert a Boolean result to an Either
  private def booleanToEither(b: Boolean) = if (b) Right(()) else Left(())

  // Note: Tapir Endpoint[I, E, O, S], where:
  //    I is the type of the input parameters
  //    E is the type of the error-output parameters
  //    O is the type of the output parameters
  //    S is the type of streams that are used by the endpoint’s inputs/outputs

  // Combines Tapir/OpenAPI doc with akka-http route
  private trait DocRoute[I, E, O] {
    // Documents the route
    def doc: Endpoint[I, E, O, AkkaStreams with WebSockets]

    // Implements the route
    def impl(i: I): Future[Either[E, O]]

    // Returns the akka-http route for this endpoint
    def route: Route = AkkaHttpServerInterpreter.toRoute(doc)(i => impl(i))
  }

  // --- Endpoints ---

  private object setPosition extends DocRoute[SegmentToM1Pos, Unit, Unit] {
    override val doc: Endpoint[SegmentToM1Pos, Unit, Unit, Any] =
      endpoint.post
        .description("Insert/update segment to M1 positions mapping")
        .in("setPosition")
        .in(segmentToM1PosBody)
        .out(statusCode(StatusCode.Ok))
        .errorOut(statusCode(StatusCode.BadRequest))

    override def impl(segmentToM1Pos: SegmentToM1Pos): Future[Either[Unit, Unit]] = {
      posTable.setPosition(segmentToM1Pos).map(booleanToEither)
    }
  }

  private object setPositions extends DocRoute[MirrorConfig, Unit, Unit] {
    override val doc: Endpoint[MirrorConfig, Unit, Unit, Any] =
      endpoint.post
        .description("Set positions of a number of segments on a given date")
        .in("setPositions")
        .in(mirrorConfigBody)
        .out(statusCode(StatusCode.Ok))
        .errorOut(statusCode(StatusCode.BadRequest))

    override def impl(mirrorConfig: MirrorConfig): Future[Either[Unit, Unit]] = {
      posTable.setPositions(mirrorConfig).map(booleanToEither)
    }
  }

  private object setAllPositions extends DocRoute[AllSegmentPositions, Unit, Unit] {
    override val doc: Endpoint[AllSegmentPositions, Unit, Unit, Any] =
      endpoint.post
        .description("Sets all 574 segment positions (A1 to G82) for a given date")
        .in("setAllPositions")
        .in(allSegmentPositionsBody)
        .out(statusCode(StatusCode.Ok))
        .errorOut(statusCode(StatusCode.BadRequest))

    override def impl(allSegmentPositions: AllSegmentPositions): Future[Either[Unit, Unit]] = {
      posTable.setAllPositions(allSegmentPositions.date, allSegmentPositions.allPositions).map(booleanToEither)
    }
  }

  private object segmentPositions extends DocRoute[(String, DateRange), Unit, List[SegmentToM1Pos]] {
    override val doc: Endpoint[(String, DateRange), Unit, List[SegmentToM1Pos], Any] =
      endpoint.post
        .description("Gets a list of segments positions for the given segment id in the given date range")
        .in("segmentPositions" / path[String]("segmentId").description("the segment id to search for").example("SN-484"))
        .in(dateRangeBody.description("The range of dates to search for"))
        .out(segmentToM1PosListBody)

    override def impl(p: (String, DateRange)): Future[Either[Unit, List[SegmentToM1Pos]]] = {
      posTable.segmentPositions(p._2, p._1).map(r => Right(r))
    }
  }

  private object segmentIds extends DocRoute[(String, DateRange), Unit, List[SegmentToM1Pos]] {
    override val doc: Endpoint[(String, DateRange), Unit, List[SegmentToM1Pos], Any] =
      endpoint.post
        .description("Gets a list of segment ids that were in the given position in the given date range.")
        .in("segmentIds" / path[String]("position").description("the segment position to search for (A1 to F82)").example("A32"))
        .in(dateRangeBody)
        .out(segmentToM1PosListBody)

    override def impl(p: (String, DateRange)): Future[Either[Unit, List[SegmentToM1Pos]]] = {
      posTable.segmentIds(p._2, p._1).map(r => Right(r))
    }
  }

  // ---
  // ---
  // ---
  // ---
  // ---
  // ---
  // ---
  // ---
  // ---

  private val logRequest: HttpRequest => Unit = req => {
    logger.info(s"${req.method.value} ${req.uri.toString()}")
  }

  private val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

  private val openApiDocs: OpenAPI =
    OpenAPIDocsInterpreter.toOpenAPI(
      List(setPosition.doc, setPositions.doc, setAllPositions.doc, segmentPositions.doc, segmentIds.doc),
      "The ESW Segment DB API",
      BuildInfo.version
    )
  private val openApiYml: String = openApiDocs.toYaml

  val routes: Route = {
    import akka.http.scaladsl.server.Directives._
    cors() {
      routeLogger {
        concat(
          setPosition.route,
          setPositions.route,
          setAllPositions.route,
          segmentPositions.route,
          segmentIds.route,
          new SwaggerAkka(openApiYml).routes
        )
      }
    }
  }
}
