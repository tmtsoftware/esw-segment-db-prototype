package esw.segment.client

import akka.actor.ActorSystem
import esw.segment.shared.SegmentToM1ApiTestBase
import EswSegmentClientTest._

object EswSegmentClientTest {
  implicit val system: ActorSystem = ActorSystem()
  import system._
  val client = new EswSegmentHttpClient()
}

class EswSegmentClientTest extends SegmentToM1ApiTestBase(client)
