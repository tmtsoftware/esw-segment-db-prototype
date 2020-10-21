package esw.segment.db

import java.net.InetAddress

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.{GenericLoggerFactory, LoggingSystemFactory}
import org.jooq.DSLContext

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._

class Wiring(maybePort: Option[Int] = None) {
  private lazy val host = InetAddress.getLocalHost.getHostName
  implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "EswSegmentDb")
  implicit lazy val ec: ExecutionContextExecutor = typedSystem.executionContext
  LoggingSystemFactory.start("EswSegmentDb", "0.1", host, typedSystem)
  lazy val log: Logger = GenericLoggerFactory.getLogger
  private lazy val locationService = HttpLocationServiceFactory.makeLocalClient(typedSystem)
  private lazy val dbName = "esw_segment_db"
  private lazy val dbFactory = new DatabaseServiceFactory(typedSystem)
  lazy val timeout: FiniteDuration = 60.seconds
  private lazy val dsl: DSLContext = Await.result(dbFactory.makeDsl(locationService, dbName,
    "DB_WRITE_USERNAME", "DB_WRITE_PASSWORD"), timeout)

  lazy val segmentDb = new SegmentDb(dsl)
  lazy val routes = new Routes(segmentDb)
  lazy val configs = new Configs(maybePort)
  lazy val server = new Server(configs, routes)
}
