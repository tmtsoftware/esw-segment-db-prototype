package esw.segment.server

import java.sql.Date

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import esw.segment.db.SegmentToM1PosTable.{DateRange, SegmentToM1Pos}
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
      }
    } ~
      get {
        // Gets a list of segments positions for the given segment id in the given date range.
        path("segmentPositions" / Segment) { segmentId =>
          entity(as[DateRange]) { dateRange =>
            complete(posTable.segmentPositions(dateRange, segmentId))
          }
        } ~
          // Gets a list of segment ids that were in the given position in the given date range.
          path("segmentIds" / IntNumber) { pos =>
            entity(as[DateRange]) { dateRange =>
              complete(posTable.segmentIds(dateRange, pos))
            }
          } ~
          // Returns a list of segments that were installed since the given date
          path("newlyInstalledSegments") {
            entity(as[Date]) { date =>
              complete(posTable.newlyInstalledSegments(date))
            }
          }
      }
}
