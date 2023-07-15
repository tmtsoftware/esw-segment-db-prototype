package esw.segment.server

import esw.segment.db.DbWiring
import DbWiring.*
import akka.http.scaladsl.server.Route

class ServerWiring(port: Int, dbName: String = defaultDbName) extends DbWiring(dbName) {
  lazy val routes: Route = new DocumentedRoutes(segmentToM1PosTable, jiraSegmentDataTable, log).routes
  lazy val server: Server = new Server(port, routes)
}
