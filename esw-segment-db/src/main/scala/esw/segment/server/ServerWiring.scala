package esw.segment.server

import esw.segment.db.DbWiring
import DbWiring._

class ServerWiring(port: Int, dbName: String = defaultDbName) extends DbWiring(dbName) {
  lazy val routes = new Routes(segmentToM1PosTable, jiraSegmentDataTable, log)
  lazy val server = new Server(port, routes)
}
