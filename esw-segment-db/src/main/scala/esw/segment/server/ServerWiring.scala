package esw.segment.server

import esw.segment.db.DbWiring

class ServerWiring(maybePort: Option[Int] = None) extends DbWiring {
  lazy val routes = new Routes(segmentToM1PosTable)
  lazy val configs = new Configs(maybePort)
  lazy val server = new Server(configs, routes)
}
