//package esw.segment.server
//
//import akka.NotUsed
//import akka.actor.typed.ActorSystem
//import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
//import akka.http.scaladsl.model.StatusCodes.*
//import akka.http.scaladsl.model.sse.ServerSentEvent
//import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
//import akka.http.scaladsl.server.{Directive0, Directives, ExceptionHandler, RejectionHandler, Route}
//import akka.stream.OverflowStrategy
//import akka.stream.scaladsl.Source
//import esw.segment.db.{JiraSegmentDataTable, SegmentToM1PosTable}
//import esw.segment.shared.EswSegmentData.*
//import esw.segment.shared.JsonSupport
//import ch.megard.akka.http.cors.scaladsl.CorsDirectives.*
//import csw.logging.api.scaladsl.Logger
//
//import java.time.LocalDate
//import scala.async.Async.{async, await}
//import scala.concurrent.{ExecutionContext, Future}
//
//class Routes(posTable: SegmentToM1PosTable, jiraSegmentDataTable: JiraSegmentDataTable, logger: Logger)(implicit
//    ec: ExecutionContext,
//    sys: ActorSystem[_]
//) extends Directives
//    with JsonSupport {
//
//  val logRequest: HttpRequest => Unit = req => {
//    logger.info(s"${req.method.value} ${req.uri.toString()}")
//  }
//
//  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))
//
//  private def availableSegmentIds(f: Future[List[String]]): Future[List[String]] =
//    async {
//      val list    = await(f)
//      val results = await(Future.sequence(list.map(posTable.currentSegmentPosition)))
//      list.zip(results).filter(p => p._2.isEmpty || p._2.get.position.head == 'G').map(_._1)
//    }
//
//  // Convert callback to stream for progress on sync
//  private def syncWithJiraStream(): Source[Int, NotUsed] = {
//    val sourceDecl      = Source.queue[Int](bufferSize = 2, OverflowStrategy.backpressure)
//    val (queue, source) = sourceDecl.preMaterialize()
//    def callback(percent: Int): Unit = {
//      queue.offer(percent)
//    }
//    jiraSegmentDataTable.syncWithJira(callback)
//    source
//  }
//
//  implicit def myExceptionHandler: ExceptionHandler =
//    ExceptionHandler {
//      case ex: Exception =>
//        extractUri { uri =>
//          println(s"Request to $uri could not be handled normally")
//          ex.printStackTrace()
//          complete(HttpResponse(InternalServerError, entity = "Internal error"))
//        }
//    }
//
//  implicit def myRejectionHandler: RejectionHandler =
//    RejectionHandler.default
//      .mapRejectionResponse {
//        case res @ HttpResponse(_, _, ent: HttpEntity.Strict, _) =>
//          // since all Akka default rejection responses are Strict this will handle all rejections
//          val message = ent.data.utf8String.replaceAll("\"", """\"""")
//
//          // we copy the response in order to keep all headers and status code, wrapping the message as hand rolled JSON
//          // you could the entity using your favourite marshalling library (e.g. spray json or anything else)
//          res.withEntity(HttpEntity(ContentTypes.`application/json`, s"""{"rejection": "$message"}"""))
//
//        case x => x // pass through all other types of responses
//      }
//
//  val route: Route = cors() {
//    routeLogger {
//      post {
//        // Insert/update segment to M1 positions
//        path("setPosition") {
//          entity(as[SegmentToM1Pos]) { segmentToM1Pos =>
//            complete(posTable.setPosition(segmentToM1Pos).map(if (_) OK else BadRequest))
//          }
//        } ~
//        // Set positions of a number of segments on a given date
//        path("setPositions") {
//          entity(as[MirrorConfig]) { config =>
//            complete(posTable.setPositions(config).map(if (_) OK else BadRequest))
//          }
//        } ~
//        // Set all segment positions
//        path("setAllPositions") {
//          entity(as[AllSegmentPositions]) { p =>
//            complete(posTable.setAllPositions(p.date, p.allPositions).map(if (_) OK else BadRequest))
//          }
//        } ~
//        // Gets a list of segments positions for the given segment id in the given date range.
//        path("segmentPositions" / Segment) { segmentId =>
//          entity(as[DateRange]) { dateRange =>
//            complete(posTable.segmentPositions(dateRange, segmentId))
//          }
//        } ~
//        // Gets a list of segment ids that were in the given location in the given date range.
//        path("segmentIds" / Segment) { position =>
//          entity(as[DateRange]) { dateRange =>
//            complete(posTable.segmentIds(dateRange, position))
//          }
//        } ~
//        // Returns a list of segments that were installed since the given date
//        path("newlyInstalledSegments") {
//          entity(as[LocalDate]) { date =>
//            complete(posTable.newlyInstalledSegments(date))
//          }
//        } ~
//        // Returns a list of segment exchanges since the given date.
//        path("segmentExchanges") {
//          entity(as[LocalDate]) { date =>
//            complete(posTable.segmentExchanges(date))
//          }
//        } ~
//        // Returns the segment positions as they were on the given date, sorted by position
//        path("positionsOnDate") {
//          entity(as[LocalDate]) { date =>
//            complete(posTable.positionsOnDate(date))
//          }
//        } ~
//        // Gets the segment position for the given segment id on the given date.
//        path("segmentPositionOnDate" / Segment) { segmentId =>
//          entity(as[LocalDate]) { date =>
//            complete(posTable.segmentPositionOnDate(date, segmentId))
//          }
//        } ~
//        // Gets the id of the segment that was installed in the given location on the given date
//        path("segmentAtPositionOnDate" / Segment) { position =>
//          entity(as[LocalDate]) { date =>
//            complete(posTable.segmentAtPositionOnDate(date, position))
//          }
//        } ~
//        // Drops and recreates the database tables (for testing)
//        path("resetTables") {
//          val f1     = jiraSegmentDataTable.resetJiraSegmentDataTable()
//          val f2     = posTable.resetSegmentToM1PosTable()
//          val result = Future.sequence(List(f1, f2)).map(_.forall(b => b))
//          complete(result.map(if (_) OK else BadRequest))
//        } ~
//        path("resetJiraSegmentDataTable") {
//          complete(jiraSegmentDataTable.resetJiraSegmentDataTable().map(if (_) OK else BadRequest))
//        } ~
//        path("resetSegmentToM1PosTable") {
//          complete(posTable.resetSegmentToM1PosTable().map(if (_) OK else BadRequest))
//        } ~
//        // Returns the most recent date that segments were changed up to the given date, or the current date
//        path("mostRecentChange") {
//          entity(as[LocalDate]) { date =>
//            complete(posTable.mostRecentChange(date))
//          }
//        } ~
//        // Returns the next date after the given one where segments were changed, or the current date, if there are no newer changes.
//        path("nextChange") {
//          entity(as[LocalDate]) { date =>
//            complete(posTable.nextChange(date))
//          }
//        } ~
//        // Returns the previous date before the given one where segments were changed, or the first date, if there are no older changes.
//        path("prevChange") {
//          entity(as[LocalDate]) { date =>
//            complete(posTable.prevChange(date))
//          }
//        }
//      } ~
//      get {
//        // Returns the current segment positions, sorted by position
//        path("currentPositions") {
//          complete(posTable.currentPositions())
//        } ~
//        path("plannedPositions") {
//          complete(jiraSegmentDataTable.plannedPositions())
//        } ~
//        path("segmentData") {
//          complete(jiraSegmentDataTable.segmentData())
//        } ~
//        // Gets the current segment position for the given segment id.
//        path("currentSegmentPosition" / Segment) { segmentId =>
//          complete(posTable.currentSegmentPosition(segmentId))
//        } ~
//        // Gets the id of the segment currently in the given location
//        path("currentSegmentAtPosition" / Segment) { position =>
//          complete(posTable.currentSegmentAtPosition(position))
//        } ~
//        // Gets a list of segment-ids that can be installed at the given position
//        path("availableSegmentIdsForPos" / Segment) { position =>
//          complete(availableSegmentIds(jiraSegmentDataTable.availableSegmentIdsForPos(position)))
//        } ~
//        // Gets a list of all segment ids that were in the given location.
//        path("allSegmentIds" / Segment) { position =>
//          complete(posTable.allSegmentIds(position))
//        } ~
//        path("syncWithJira") {
//          import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling.*
//          complete {
//            syncWithJiraStream()
//              .map(p => ServerSentEvent(p.toString))
//          }
//        }
//      }
//    }
//  }
//}
