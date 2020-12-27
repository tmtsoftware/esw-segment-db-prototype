package esw.segment.db

import java.net.InetAddress
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.{GenericLoggerFactory, LoggingSystemFactory}
import org.jooq.DSLContext

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import DbWiring._
import esw.segment.jira.JiraClient

object DbWiring {
  val defaultDbName = "esw_segment_db"
  val testDbName    = "test_segment_db"
}

/**
 * Sets up the connection to the database.
 *
 * @param dbName The database name
 */
class DbWiring(dbName: String = defaultDbName) {
  lazy val host: String                                             = InetAddress.getLocalHost.getHostName
  implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "EswSegmentDb")
  implicit lazy val ec: ExecutionContextExecutor                    = typedSystem.executionContext

  LoggingSystemFactory.start("EswSegmentDb", "0.1", host, typedSystem)
  lazy val log: Logger                      = GenericLoggerFactory.getLogger
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(typedSystem)
  lazy val dbFactory                        = new DatabaseServiceFactory(typedSystem)
  lazy val timeout: FiniteDuration          = 60.seconds
  lazy val dsl: DSLContext =
    Await.result(dbFactory.makeDsl(locationService, dbName, "DB_WRITE_USERNAME", "DB_WRITE_PASSWORD"), timeout)
  lazy val segmentToM1PosTable = new SegmentToM1PosTable(dsl)
  val jiraClient = new JiraClient()
  lazy val jiraSegmentDataTable = new JiraSegmentDataTable(dsl, jiraClient)
}
