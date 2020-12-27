package esw.segment.server

import java.sql.Date
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Directives, Route}
import esw.segment.db.{JiraSegmentDataTable, SegmentToM1PosTable}
import esw.segment.shared.EswSegmentData._
import esw.segment.shared.JsonSupport
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.logging.api.scaladsl.Logger

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class Routes(posTable: SegmentToM1PosTable, jiraSegmentDataTable: JiraSegmentDataTable, logger: Logger)(implicit
    ec: ExecutionContext
) extends Directives
    with JsonSupport {

  val logRequest: HttpRequest => Unit = req => {
    logger.debug(s"${req.method.value} ${req.uri.toString()}")
  }

  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

  private def availableSegmentIds(f: Future[List[String]]): Future[List[String]] = async {
    val list = await(f)
    val results = await(Future.sequence(list.map(posTable.currentSegmentPosition)))
    list.zip(results).filter(_._2.isEmpty).map(_._1)
  }

  val route: Route = cors() {
    routeLogger {
      post {
        // Insert/update segment to M1 positions
        path("setPosition") {
          entity(as[SegmentToM1Pos]) { segmentToM1Pos =>
            complete(posTable.setPosition(segmentToM1Pos).map(if (_) OK else BadRequest))
          }
        } ~
        // Set positions of a number of segments on a given date
        path("setPositions") {
          entity(as[SegmentToM1Positions]) { p =>
            complete(posTable.setPositions(p.date, p.positions).map(if (_) OK else BadRequest))
          }
        } ~
        // Set all segment positions
        path("setAllPositions") {
          entity(as[AllSegmentPositions]) { p =>
            complete(posTable.setAllPositions(p.date, p.allPositions).map(if (_) OK else BadRequest))
          }
        } ~
        // Gets a list of segments positions for the given segment id in the given date range.
        path("segmentPositions" / Segment) { segmentId =>
          entity(as[DateRange]) { dateRange =>
            complete(posTable.segmentPositions(dateRange, segmentId))
          }
        } ~
        // Gets a list of segment ids that were in the given location in the given date range.
        path("segmentIds" / Segment) { position =>
          entity(as[DateRange]) { dateRange =>
            complete(posTable.segmentIds(dateRange, position))
          }
        } ~
        // Returns a list of segments that were installed since the given date
        path("newlyInstalledSegments") {
          entity(as[Date]) { date =>
            complete(posTable.newlyInstalledSegments(date))
          }
        } ~
        // Returns the segment positions as they were on the given date, sorted by position
        path("positionsOnDate") {
          entity(as[Date]) { date =>
            complete(posTable.positionsOnDate(date))
          }
        } ~
        // Gets the segment position for the given segment id on the given date.
        path("segmentPositionOnDate" / Segment) { segmentId =>
          entity(as[Date]) { date =>
            complete(posTable.segmentPositionOnDate(date, segmentId))
          }
        } ~
        // Gets the id of the segment that was installed in the given location on the given date
        path("segmentAtPositionOnDate" / Segment) { position =>
          entity(as[Date]) { date =>
            complete(posTable.segmentAtPositionOnDate(date, position))
          }
        } ~
        // Drops and recreates the database tables (for testing)
        path("resetTables") {
          complete(posTable.resetTables().map(if (_) OK else BadRequest))
        } ~
        // Returns the most recent date that segments were changed up to the given date, or the current date
        path("mostRecentChange") {
          entity(as[Date]) { date =>
            complete(posTable.mostRecentChange(date))
          }
        } ~
        // Returns the next date after the given one where segments were changed, or the current date, if there are no newer changes.
        path("nextChange") {
          entity(as[Date]) { date =>
            complete(posTable.nextChange(date))
          }
        } ~
        // Returns the previous date before the given one where segments were changed, or the first date, if there are no older changes.
        path("prevChange") {
          entity(as[Date]) { date =>
            complete(posTable.prevChange(date))
          }
        }
      } ~
      get {
        // Returns the current segment positions, sorted by position
        path("currentPositions") {
          complete(posTable.currentPositions())
        } ~
        // Gets the current segment position for the given segment id.
        path("currentSegmentPosition" / Segment) { segmentId =>
          complete(posTable.currentSegmentPosition(segmentId))
        } ~
        // Gets the id of the segment currently in the given location
        path("currentSegmentAtPosition" / Segment) { position =>
          complete(posTable.currentSegmentAtPosition(position))
        } ~
        // Gets a list of segment-ids that can be installed at the given position
        path("availableSegmentIdsForPos" / Segment) { position =>
          complete(availableSegmentIds(jiraSegmentDataTable.availableSegmentIdsForPos(position)))
        } ~
        // Gets a list of all segment ids that were in the given location.
        path("allSegmentIds" / Segment) { position =>
          complete(posTable.allSegmentIds(position))
        }
      }
    }
  }
}
