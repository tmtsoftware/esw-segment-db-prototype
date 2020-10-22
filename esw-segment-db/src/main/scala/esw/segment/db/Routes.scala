package esw.segment.db

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import esw.segment.db.SegmentToM1PosTable.SegmentToM1Pos

class Routes(db: SegmentToM1PosTable) extends Directives with JsonSupport {
  val route: Route =
    pathPrefix("segmentToM1Pos") {
      // Insert/update segment to M1 positions
      post {
        entity(as[SegmentToM1Pos]) { segmentToM1Pos =>
          complete(OK)
        }
      }
    }
}
