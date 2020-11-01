package esw.segment.client

import akka.actor.ActorSystem
import esw.segment.shared.SegmentToM1ApiTestBase
import EswSegmentClientTest._
import esw.segment.db.DbWiring
import esw.segment.server.ServerWiring

object EswSegmentClientTest {
  implicit val system: ActorSystem = ActorSystem()
  import system._

  // Start a test server (Assumes CSW Database Service is running - There is no DB test kit)
  val wiring = new ServerWiring(9192, DbWiring.testDbName)
  wiring.server.start()

  // Get a client for the server
  val client = new EswSegmentHttpClient()
}

class EswSegmentClientTest extends SegmentToM1ApiTestBase(client)
