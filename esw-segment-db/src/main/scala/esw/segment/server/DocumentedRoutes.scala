package esw.segment.server

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.{complete, extractUri}
import akka.http.scaladsl.server.{Directive0, ExceptionHandler, Route}
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import buildinfo.BuildInfo
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.aas.http.SecurityDirectives
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import esw.segment.db.{JiraSegmentDataTable, SegmentToM1PosTable}
import esw.segment.shared.EswSegmentData.{AllSegmentPositions, DateRange, MirrorConfig, SegmentConfig, SegmentToM1Pos, currentDate}
import sttp.tapir._
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.json.spray._
import esw.segment.shared.{JiraSegmentData, JsonSupport}
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
import scala.util.{Failure, Success}

/**
 * Defines the routes and documentation for the ESW Segment DB HTTP server,
 * based on Tapir and akka-http.
 */
object DocumentedRoutes extends JsonSupport {
  // Type returned for errors (plain text error message)
  private type ErrorInfo = String
  private val errorOutWithDoc = plainBody[ErrorInfo].example("Error message.")

  // See https://tapir.softwaremill.com/en/latest/endpoint/customtypes.html?highlight=path%5B#customising-derived-schemas
  // Add descriptions to schema fields here so we don't need to add tapir as a dependency to the esw-segment-shared project that
  // contains the case classes.
  implicit val customSegmentToM1PosSchema: Schema[SegmentToM1Pos] = implicitly[Derived[Schema[SegmentToM1Pos]]].value
    .modify(_.date)(_.description("date of record in the format yyyy-mm-dd"))
    .modify(_.maybeId)(_.description("the segment id, if the segment is present"))
    .modify(_.position)(_.description("position of segment (For example: A32, B19, F82)"))

  implicit val customSegmentConfigSchema: Schema[SegmentConfig] = implicitly[Derived[Schema[SegmentConfig]]].value
    .modify(_.position)(_.description("segment poosition (A1 to G82)"))
    .modify(_.segmentId)(_.description("the segment id, if the segment is present"))

  implicit val customMirrorConfigSchema: Schema[MirrorConfig] = implicitly[Derived[Schema[MirrorConfig]]].value
    .modify(_.date)(_.description("the date for the configuration in the format yyyy-mm-dd"))
    .modify(_.segments)(_.description("list of segment assignments"))

  implicit val customAllSegmentPositionsSchema: Schema[AllSegmentPositions] =
    implicitly[Derived[Schema[AllSegmentPositions]]].value
      .modify(_.date)(_.description("the date corresponding to the positions"))
      .modify(_.allPositions)(
        _.description("list of all 574 segment positions from A1 to G82. Missing segments have the value null.")
      )

  implicit val customDateRangeSchema: Schema[DateRange] = implicitly[Derived[Schema[DateRange]]].value
    .modify(_.from)(_.description("start of the date range in the format yyyy-mm-dd"))
    .modify(_.to)(_.description("end of the date range in the format yyyy-mm-dd"))

  // --- Schema Descriptions ---

  private val today = currentDate()

  // Tapir description of SegmentToM1Pos JSON argument
  private val segmentToM1PosBody = jsonBody[SegmentToM1Pos]
    .description(
      "Position of a segment on a given date. The segment id is optional and can be missing if no segment is installed at the position."
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

  // Tapir description of List[JiraSegmentData] JSON result
  private val jiraSegmentDataListBody = jsonBody[List[JiraSegmentData]]
    .description(
      "A list of Data scanned from JIRA issues"
    )

  // Tapir description of Option[SegmentToM1Pos] JSON result
  private val segmentToM1PosOptionBody = jsonBody[Option[SegmentToM1Pos]]
    .description(
      "An optional object mapping segment id to position on a given date"
    )
    .example(
      Some(
        SegmentToM1Pos(today, Some("SN-484"), "B78")
      )
    )

  // Tapir description of MirrorConfig JSON argument
  private val mirrorConfigBody = jsonBody[MirrorConfig]
    .description(
      "Holds a number of segment-id assignments for the mirror. A segment id is optional and can be misisng if no segment is installed at the position."
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

  // Tapir description of MirrorConfig List JSON argument
  private val mirrorConfigListBody = jsonBody[List[MirrorConfig]]
    .description(
      "Holds a MirrirConfig, representing segment changes on different dates."
    )
    .example(
      List(
        MirrorConfig(
          LocalDate.parse("2021-01-29"),
          List(
            SegmentConfig("A78", Some("SN-483")),
            SegmentConfig("B78", Some("SN-484")),
            SegmentConfig("D78", Some("SN-486"))
          )
        ),
        MirrorConfig(
          LocalDate.parse("2021-01-30"),
          List(
            SegmentConfig("A5", Some("SN-007")),
            SegmentConfig("B78", None)
          )
        )
      )
    )

  // Tapir description of AllSegmentPositions JSON argument
  private val allSegmentPositionsBody = jsonBody[AllSegmentPositions]
    .description(
      "Holds all 574 segment-id assignments (A1 to G8, including spares) for the mirror. A segment id can be null if no segment is installed at the position."
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

  // Tapir description of a LocalDate JSON argument
  private val localDateBody = jsonBody[LocalDate]
    .description(
      "A date in the format yyyy-mm-dd"
    )
    .example(
      LocalDate.parse("2020-10-23")
    )

  // ---

  // Convert a Boolean result to an Either
  private def booleanToEither(b: Boolean): Either[ErrorInfo, Unit] = if (b) Right(()) else Left("Internal error")

}

//noinspection TypeAnnotation
/**
 * Defines the routes and documentation for the ESW Segment DB HTTP server,
 * based on Tapir and akka-http.
 *
 * @param posTable object managing the ESW segment database position table
 * @param jiraSegmentDataTable object managing the database table containing the JIRA info
 * @param logger used to log messages
 * @param ec akka execution context
 * @param actorSystem actor system used for async and futures
 */
class DocumentedRoutes(posTable: SegmentToM1PosTable, jiraSegmentDataTable: JiraSegmentDataTable, logger: Logger)(implicit
    ec: ExecutionContext,
    actorSystem: ActorSystem[_]
) {
  import DocumentedRoutes._

  val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val config  = actorSystem.settings.config
  private val role = config.getString("esw-segment-db.role")
  private val directives      = SecurityDirectives(config, locationService)
  import directives._

  private val myExceptionHandler = ExceptionHandler {
    case e: Throwable =>
      extractUri { uri =>
        println(s"Request to $uri could not be handled normally")
        complete(HttpResponse(InternalServerError, entity = e.getMessage))
      }
  }

  // Combines Tapir/OpenAPI doc with akka-http route
  private trait DocRoute[I, O] {
    private def handleErrors[T](f: Future[Either[ErrorInfo, T]]): Future[Either[ErrorInfo, T]] =
      f.transform {
        case Success(v) => Success(v)
        case Failure(e) =>
          Success(Left(e.getMessage))
      }

    // Documents the route
    def doc: Endpoint[I, ErrorInfo, O, AkkaStreams with WebSockets]

    // Implements the server route
    def impl(i: I): Future[Either[ErrorInfo, O]]

    // Set to true if the route is protected by Auth/Keycloak
    def isProtected: Boolean = false

    // Returns the akka-http route for this endpoint
    final def route: Route = {
      val innerRoute = AkkaHttpServerInterpreter.toRoute(doc) { i =>
        handleErrors(impl(i))
      }
      if (isProtected)
        sPost(RealmRolePolicy(role))(innerRoute)
      else
        innerRoute
    }
  }

  // Returns
  private def availableSegmentIds(f: Future[List[String]]): Future[List[String]] =
    async {
      val list    = await(f)
      val results = await(Future.sequence(list.map(posTable.currentSegmentPosition)))
      list.zip(results).filter(p => p._2.isEmpty || p._2.get.position.head == 'G').map(_._1)
    }

  // Convert callback to stream for progress on sync
  private def syncWithJiraStream(): Future[Source[Int, NotUsed]] =
    async {
      val sourceDecl      = Source.queue[Int](bufferSize = 2, OverflowStrategy.backpressure)
      val (queue, source) = sourceDecl.preMaterialize()
      def callback(percent: Int): Unit = {
        queue.offer(percent)
      }
      if (await(jiraSegmentDataTable.syncWithJira(callback)))
        source
      else
        throw new IllegalAccessException("Failed to sync with JIRA")
    }

  // --- Endpoints ---

  private object setPosition extends DocRoute[SegmentToM1Pos, Unit] {
    override val doc: Endpoint[SegmentToM1Pos, ErrorInfo, Unit, Any] =
      endpoint.post
        .description("Insert/update segment to M1 positions mapping")
        .in("setPosition")
        .in(segmentToM1PosBody)
        .out(statusCode(StatusCode.Ok))
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(segmentToM1Pos: SegmentToM1Pos): Future[Either[ErrorInfo, Unit]] = {
      posTable.setPosition(segmentToM1Pos).map(booleanToEither)
    }

    override val isProtected = true
  }

  private object setPositions extends DocRoute[MirrorConfig, Unit] {
    override val doc: Endpoint[MirrorConfig, ErrorInfo, Unit, Any] =
      endpoint.post
        .description("Set positions of a number of segments on a given date")
        .in("setPositions")
        .in(mirrorConfigBody)
        .out(statusCode(StatusCode.Ok))
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(mirrorConfig: MirrorConfig): Future[Either[ErrorInfo, Unit]] = {
      posTable.setPositions(mirrorConfig).map(booleanToEither)
    }

    override val isProtected = true
  }

  private object setAllPositions extends DocRoute[AllSegmentPositions, Unit] {
    override val doc: Endpoint[AllSegmentPositions, ErrorInfo, Unit, Any] =
      endpoint.post
        .description("Sets all 574 segment positions (A1 to G82) for a given date")
        .in("setAllPositions")
        .in(allSegmentPositionsBody)
        .out(statusCode(StatusCode.Ok))
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(allSegmentPositions: AllSegmentPositions): Future[Either[ErrorInfo, Unit]] = {
      posTable.setAllPositions(allSegmentPositions.date, allSegmentPositions.allPositions).map(booleanToEither)
    }

    override val isProtected = true
  }

  private object segmentPositions extends DocRoute[(String, DateRange), List[SegmentToM1Pos]] {
    override val doc: Endpoint[(String, DateRange), ErrorInfo, List[SegmentToM1Pos], Any] =
      endpoint.post
        .description("Gets a list of segments positions for the given segment id in the given date range")
        .in("segmentPositions" / path[String]("segmentId").description("the segment id to search for").example("SN-484"))
        .in(dateRangeBody.description("The range of dates to search for"))
        .out(segmentToM1PosListBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(p: (String, DateRange)): Future[Either[ErrorInfo, List[SegmentToM1Pos]]] = {
      posTable.segmentPositions(p._2, p._1).map(r => Right(r))
    }
  }

  private object segmentIds extends DocRoute[(String, DateRange), List[SegmentToM1Pos]] {
    override val doc: Endpoint[(String, DateRange), ErrorInfo, List[SegmentToM1Pos], Any] =
      endpoint.post
        .description("Gets a list of segment ids that were in the given position in the given date range.")
        .in("segmentIds" / path[String]("position").description("the segment position to search for (A1 to F82)").example("A32"))
        .in(dateRangeBody)
        .out(segmentToM1PosListBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(p: (String, DateRange)): Future[Either[ErrorInfo, List[SegmentToM1Pos]]] = {
      posTable.segmentIds(p._2, p._1).map(r => Right(r))
    }
  }

  private object newlyInstalledSegments extends DocRoute[LocalDate, List[SegmentToM1Pos]] {
    override val doc: Endpoint[LocalDate, ErrorInfo, List[SegmentToM1Pos], Any] =
      endpoint.post
        .description("Returns a list of segments that were installed since the given date.")
        .in("newlyInstalledSegments")
        .in(localDateBody)
        .out(segmentToM1PosListBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(date: LocalDate): Future[Either[ErrorInfo, List[SegmentToM1Pos]]] = {
      posTable.newlyInstalledSegments(date).map(r => Right(r))
    }
  }

  private object segmentExchanges extends DocRoute[LocalDate, List[MirrorConfig]] {
    override val doc: Endpoint[LocalDate, ErrorInfo, List[MirrorConfig], Any] =
      endpoint.post
        .description("Returns a list of segment exchanges since the given date.")
        .in("segmentExchanges")
        .in(localDateBody)
        .out(mirrorConfigListBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(date: LocalDate): Future[Either[ErrorInfo, List[MirrorConfig]]] = {
      posTable.segmentExchanges(date).map(r => Right(r))
    }
  }

  private object positionsOnDate extends DocRoute[LocalDate, List[SegmentToM1Pos]] {
    override val doc: Endpoint[LocalDate, ErrorInfo, List[SegmentToM1Pos], Any] =
      endpoint.post
        .description("Returns the segment positions as they were on the given date.")
        .in("positionsOnDate")
        .in(localDateBody)
        .out(segmentToM1PosListBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(date: LocalDate): Future[Either[ErrorInfo, List[SegmentToM1Pos]]] = {
      posTable.positionsOnDate(date).map(r => Right(r))
    }
  }

  private object segmentPositionOnDate extends DocRoute[(String, LocalDate), Option[SegmentToM1Pos]] {
    override val doc: Endpoint[(String, LocalDate), ErrorInfo, Option[SegmentToM1Pos], Any] =
      endpoint.post
        .description("Gets the segment position for the given segment id on the given date.")
        .in("segmentPositionOnDate" / path[String]("segmentId").description("the segment id to search for").example("SN-019"))
        .in(localDateBody)
        .out(segmentToM1PosOptionBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(p: (String, LocalDate)): Future[Either[ErrorInfo, Option[SegmentToM1Pos]]] = {
      posTable.segmentPositionOnDate(p._2, p._1).map(r => Right(r))
    }
  }

  private object segmentAtPositionOnDate extends DocRoute[(String, LocalDate), Option[SegmentToM1Pos]] {
    override val doc: Endpoint[(String, LocalDate), ErrorInfo, Option[SegmentToM1Pos], Any] =
      endpoint.post
        .description("Gets the id of the segment that was installed in the given position on the given date.")
        .in("segmentAtPositionOnDate" / path[String]("position").description("the segment position to search for").example("A82"))
        .in(localDateBody)
        .out(segmentToM1PosOptionBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(p: (String, LocalDate)): Future[Either[ErrorInfo, Option[SegmentToM1Pos]]] = {
      posTable.segmentAtPositionOnDate(p._2, p._1).map(r => Right(r))
    }
  }

  private object resetTables extends DocRoute[Unit, Unit] {
    override val doc: Endpoint[Unit, ErrorInfo, Unit, Any] =
      endpoint.post
        .description("Drops and recreates the database tables (for testing)")
        .in("resetTables")
        .out(statusCode(StatusCode.Ok))
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(x: Unit): Future[Either[ErrorInfo, Unit]] = {
      val f1 = jiraSegmentDataTable.resetJiraSegmentDataTable()
      val f2 = posTable.resetSegmentToM1PosTable()
      Future.sequence(List(f1, f2)).map(_.forall(b => b)).map(booleanToEither)
    }

    override val isProtected = true
  }

  private object resetJiraSegmentDataTable extends DocRoute[Unit, Unit] {
    override val doc: Endpoint[Unit, ErrorInfo, Unit, Any] =
      endpoint.post
        .description("Drops and recreates the JIRA segment database table (for testing)")
        .in("resetJiraSegmentDataTable")
        .out(statusCode(StatusCode.Ok))
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(x: Unit): Future[Either[ErrorInfo, Unit]] = {
      jiraSegmentDataTable.resetJiraSegmentDataTable().map(booleanToEither)
    }

    override val isProtected = true
  }

  private object resetSegmentToM1PosTable extends DocRoute[Unit, Unit] {
    override val doc: Endpoint[Unit, ErrorInfo, Unit, Any] =
      endpoint.post
        .description("Drops and recreates the segment database table (for testing)")
        .in("resetSegmentToM1PosTable")
        .out(statusCode(StatusCode.Ok))
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(x: Unit): Future[Either[ErrorInfo, Unit]] = {
      posTable.resetSegmentToM1PosTable().map(booleanToEither)
    }

    override val isProtected = true
  }

  private object mostRecentChange extends DocRoute[LocalDate, LocalDate] {
    override val doc: Endpoint[LocalDate, ErrorInfo, LocalDate, Any] =
      endpoint.post
        .description(
          "Returns the most recent date that segments were changed up to the given date, or the current date, if there are no segments installed yet."
        )
        .in("mostRecentChange")
        .in(localDateBody)
        .out(localDateBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(date: LocalDate): Future[Either[ErrorInfo, LocalDate]] = {
      posTable.mostRecentChange(date).map(r => Right(r))
    }
  }

  private object nextChange extends DocRoute[LocalDate, LocalDate] {
    override val doc: Endpoint[LocalDate, ErrorInfo, LocalDate, Any] =
      endpoint.post
        .description(
          "Returns the next date after the given one where segments were changed, or the current date, if there are no newer changes."
        )
        .in("nextChange")
        .in(localDateBody)
        .out(localDateBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(date: LocalDate): Future[Either[ErrorInfo, LocalDate]] = {
      posTable.nextChange(date).map(r => Right(r))
    }
  }

  private object prevChange extends DocRoute[LocalDate, LocalDate] {
    override val doc: Endpoint[LocalDate, ErrorInfo, LocalDate, Any] =
      endpoint.post
        .description(
          "Returns the previous date before the given one where segments were changed, or the first date, if there are no older changes."
        )
        .in("prevChange")
        .in(localDateBody)
        .out(localDateBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(date: LocalDate): Future[Either[ErrorInfo, LocalDate]] = {
      posTable.prevChange(date).map(r => Right(r))
    }
  }

  private object authEnabled extends DocRoute[Unit, Boolean] {
    override val doc: Endpoint[Unit, ErrorInfo, Boolean, Any] =
      endpoint.get
        .description(
          "Returns true if authorization via Keycloak is enabled."
        )
        .in("authEnabled")
        .out(jsonBody[Boolean])
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(x: Unit): Future[Either[ErrorInfo, Boolean]] = {
      val disabled = config.hasPath("auth-config.disabled") && config.getBoolean("auth-config.disabled")
      Future.successful(Right(!disabled))
    }
  }

  private object currentPositions extends DocRoute[Unit, List[SegmentToM1Pos]] {
    override val doc: Endpoint[Unit, ErrorInfo, List[SegmentToM1Pos], Any] =
      endpoint.get
        .description("Returns the current segment positions.")
        .in("currentPositions")
        .out(segmentToM1PosListBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(x: Unit): Future[Either[ErrorInfo, List[SegmentToM1Pos]]] = {
      posTable.currentPositions().map(r => Right(r))
    }
  }

  private object plannedPositions extends DocRoute[Unit, List[SegmentToM1Pos]] {
    override val doc: Endpoint[Unit, ErrorInfo, List[SegmentToM1Pos], Any] =
      endpoint.get
        .description("Returns the segment positions as defined in the JIRA issues.")
        .in("plannedPositions")
        .out(segmentToM1PosListBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(x: Unit): Future[Either[ErrorInfo, List[SegmentToM1Pos]]] = {
      jiraSegmentDataTable.plannedPositions().map(r => Right(r))
    }
  }

  private object segmentData extends DocRoute[Unit, List[JiraSegmentData]] {
    override val doc: Endpoint[Unit, ErrorInfo, List[JiraSegmentData], Any] =
      endpoint.get
        .description("Gets the JIRA segment data for all segments.")
        .in("segmentData")
        .out(jiraSegmentDataListBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(x: Unit): Future[Either[ErrorInfo, List[JiraSegmentData]]] = {
      jiraSegmentDataTable.segmentData().map(r => Right(r))
    }
  }

  private object currentSegmentPosition extends DocRoute[String, Option[SegmentToM1Pos]] {
    override val doc: Endpoint[String, ErrorInfo, Option[SegmentToM1Pos], Any] =
      endpoint.get
        .description("Gets the current segment position for the given segment id.")
        .in("currentSegmentPosition" / path[String]("segmentId").description("the segment id to search for").example("SN-019"))
        .out(segmentToM1PosOptionBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(segmentId: String): Future[Either[ErrorInfo, Option[SegmentToM1Pos]]] = {
      posTable.currentSegmentPosition(segmentId).map(r => Right(r))
    }
  }

  private object currentSegmentAtPosition extends DocRoute[String, Option[SegmentToM1Pos]] {
    override val doc: Endpoint[String, ErrorInfo, Option[SegmentToM1Pos], Any] =
      endpoint.get
        .description("Gets the id of the segment currently in the given position.")
        .in(
          "currentSegmentAtPosition" / path[String]("position").description("the segment position to search for").example("A82")
        )
        .out(segmentToM1PosOptionBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(position: String): Future[Either[ErrorInfo, Option[SegmentToM1Pos]]] = {
      posTable.currentSegmentAtPosition(position).map(r => Right(r))
    }
  }

  private object availableSegmentIdsForPos extends DocRoute[String, List[String]] {
    override val doc: Endpoint[String, ErrorInfo, List[String], Any] =
      endpoint.get
        .description("Gets a list of segment-ids that can be installed at the given position.")
        .in(
          "availableSegmentIdsForPos" / path[String]("position").description("the segment position to search for").example("A82")
        )
        .out(jsonBody[List[String]].example(List("SN-517")))
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(position: String): Future[Either[ErrorInfo, List[String]]] = {
      availableSegmentIds(jiraSegmentDataTable.availableSegmentIdsForPos(position)).map(r => Right(r))
    }
  }

  private object allSegmentIds extends DocRoute[String, List[SegmentToM1Pos]] {
    override val doc: Endpoint[String, ErrorInfo, List[SegmentToM1Pos], Any] =
      endpoint.get
        .description("Gets a list of segments that were in the given position.")
        .in("allSegmentIds" / path[String]("position").description("the segment position to search for").example("A82"))
        .out(segmentToM1PosListBody)
        .errorOut(statusCode(StatusCode.BadRequest))
        .errorOut(errorOutWithDoc)

    override def impl(position: String): Future[Either[ErrorInfo, List[SegmentToM1Pos]]] = {
      posTable.allSegmentIds(position).map(r => Right(r))
    }
  }

  // Tapir doesn't support SSE, so this is an undocumented route
  // XXX TODO: Add Auth
  private val syncWithJiraRoute: Route = {
    import akka.http.scaladsl.server.Directives._
    get {
      path("syncWithJira") {
        import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
        complete {
          syncWithJiraStream()
            .map(_.map(p => ServerSentEvent(p.toString)))
        }
      }
    }
  }

  // ---

  // All documented routes
  private val docRoutes = List(
    setPosition,
    setPositions,
    setAllPositions,
    segmentPositions,
    segmentIds,
    newlyInstalledSegments,
    segmentExchanges,
    positionsOnDate,
    segmentPositionOnDate,
    segmentAtPositionOnDate,
    resetTables,
    resetJiraSegmentDataTable,
    resetSegmentToM1PosTable,
    mostRecentChange,
    nextChange,
    prevChange,
    authEnabled,
    currentPositions,
    plannedPositions,
    segmentData,
    currentSegmentPosition,
    currentSegmentAtPosition,
    availableSegmentIdsForPos,
    allSegmentIds
  )

  // Automatically log http requests
  private val logRequest: HttpRequest => Unit = req => {
    logger.info(s"${req.method.value} ${req.uri.toString()}")
  }

  private val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

  // Enable the Open API routes
  private val openApiDocs: OpenAPI =
    OpenAPIDocsInterpreter.toOpenAPI(
      docRoutes.map(_.doc),
      "The ESW Segment DB API",
      BuildInfo.version
    )
  private val openApiYml: String = openApiDocs.toYaml

  /**
   * The routes for this server
   */
  val routes: Route = {
    import akka.http.scaladsl.server.Directives._
    val routeList = List(new SwaggerAkka(openApiYml).routes, syncWithJiraRoute) ++ docRoutes.map(_.route)
    cors() {
      routeLogger {
        handleExceptions(myExceptionHandler) {
          concat(routeList: _*)
        }
      }
    }
  }
}
