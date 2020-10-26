package esw.segment.db

import esw.segment.server.ServerWiring

object Main extends App {
  val wiring = new ServerWiring()
  wiring.server.start()
}
