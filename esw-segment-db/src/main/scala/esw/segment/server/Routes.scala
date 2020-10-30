package esw.segment.server

import java.sql.Date

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import esw.segment.db.SegmentToM1PosTable.{AllSegmentPositions, DateRange, SegmentToM1Pos, SegmentToM1Positions}
import esw.segment.db.{JsonSupport, SegmentToM1PosTable}

import scala.concurrent.ExecutionContext

class Routes(posTable: SegmentToM1PosTable)(implicit ec: ExecutionContext) extends Directives with JsonSupport {
  val route: Route =
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
        }
    } ~
      get {
        // Gets a list of segments positions for the given segment id in the given date range.
        path("segmentPositions" / Segment) {
          segmentId =>
            entity(as[DateRange]) {
              dateRange =>
                complete(posTable.segmentPositions(dateRange, segmentId))
            }
        } ~
          // Gets a list of segment ids that were in the given position in the given date range.
          path("segmentIds" / IntNumber) {
            pos =>
              entity(as[DateRange]) {
                dateRange =>
                  complete(posTable.segmentIds(dateRange, pos))
              }
          } ~
          // Returns a list of segments that were installed since the given date
          path("newlyInstalledSegments") {
            entity(as[Date]) {
              date =>
                complete(posTable.newlyInstalledSegments(date))
            }
          } ~
          // Returns the current segment positions, sorted by position
          path("currentPositions") {
            complete(posTable.currentPositions())
          } ~
          // Gets the current segment position for the given segment id.
          path("currentSegmentPosition" / Segment) {
            segmentId =>
              complete(posTable.currentSegmentPosition(segmentId))
          } ~
          // Gets the id of the segment currently in the given position
          path("currentSegmentAtPosition" / IntNumber) {
            pos =>
              complete(posTable.currentSegmentAtPosition(pos))
          } ~
          // Returns the segment positions as they were on the given date, sorted by position
          path("positionsOnDate") {
            entity(as[Date]) {
              date =>
                complete(posTable.positionsOnDate(date))
            }
          } ~
          get {
            // Gets the segment position for the given segment id on the given date.
            path("segmentPositionOnDate" / Segment) {
              segmentId =>
                entity(as[Date]) {
                  date =>
                    complete(posTable.segmentPositionOnDate(date, segmentId))
                }
            } ~
              // Gets the id of the segment that was installed in the given position on the given date
              path("segmentAtPositionOnDate" / IntNumber) {
                pos =>
                  entity(as[Date]) {
                    date =>
                      complete(posTable.segmentAtPositionOnDate(date, pos))
                  }
              }
          }
      }
}
