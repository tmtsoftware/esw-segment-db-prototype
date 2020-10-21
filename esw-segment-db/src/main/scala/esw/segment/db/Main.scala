package esw.segment.db

object Main extends App {
  val wiring = new Wiring()
  wiring.server.start()
}
