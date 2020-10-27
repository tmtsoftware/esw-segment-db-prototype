package esw.segment.server

object Main extends App {
  val wiring = new ServerWiring()
  wiring.server.start()
}
