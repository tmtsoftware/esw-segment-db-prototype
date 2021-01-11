package esw.segment.client

import java.util.Date
import akka.actor.ActorSystem
import buildinfo.BuildInfo
import esw.segment.client.EswSegmentClientOptions.defaultPort
import esw.segment.shared.EswSegmentData._
import esw.segment.shared.JsonSupport
import scopt.Read.reads
import scopt.Read
import spray.json._

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.Source

object EswSegmentDbClient extends App with JsonSupport {
  val dateFormat                   = new java.text.SimpleDateFormat("yyyy-MM-dd")
  implicit val system: ActorSystem = ActorSystem()

  import system._

  implicit val durationRead: Read[Date] =
    reads {
      dateFormat.parse
    }

  // Parser for the command line options
  private val parser = new scopt.OptionParser[EswSegmentClientOptions]("esw-segment-db-client") {
    head("esw-segment-db-client", BuildInfo.version)

    opt[String]("host") valueName "<hostname>" action { (x, c) =>
      c.copy(host = x)
    } text "The host name where the ESW Segment DB HTTP server is running (default: localhost)"

    opt[Int]("port") valueName "<number>" action { (x, c) =>
      c.copy(port = x)
    } text s"The port number to use for the server (default: $defaultPort)"

    opt[Date]("date") valueName dateFormat.toPattern action { (x, c) =>
      c.copy(date = x)
    } text "The date to use (default: current date)"

    opt[Date]("from") valueName dateFormat.toPattern action { (x, c) =>
      c.copy(from = x)
    } text "The starting date to use for a date range (default: current date)"

    opt[Date]("to") valueName dateFormat.toPattern action { (x, c) =>
      c.copy(to = x)
    } text "The ending date to use for a date range (default: current date)"

    opt[String]('s', "segmentId") valueName "<id>" action { (x, c) =>
      c.copy(segmentId = Some(x))
    } text "The segment id to use"

    opt[String]('p', "position") valueName "<A1 to F82>" action { (x, c) =>
      c.copy(position = Some(x))
    } text "The segment position to use (A1 to F82)"

    opt[Unit]("setPosition") action { (_, c) =>
      c.copy(setPosition = Some(()))
    } text "Sets or updates the date and position of the given segment (Requires --position, --segmentId if segment is present)"

    opt[Unit]("segmentPositions") action { (_, c) =>
      c.copy(segmentPositions = Some(()))
    } text "Gets a list of segments positions for the given segment id in the given date range (Requires --segmentId)"

    opt[Unit]("segmentIds") action { (_, c) =>
      c.copy(segmentIds = Some(()))
    } text "Gets a list of segment ids that were in the given position in the given date range (Requires --position)"

    opt[Unit]("newlyInstalledSegments") action { (_, c) =>
      c.copy(newlyInstalledSegments = Some(()))
    } text "Gets a list of segments that were installed since the given date (Requires --date)"

    opt[Unit]("currentPositions") action { (_, c) =>
      c.copy(currentPositions = Some(()))
    } text "Gets the current segment positions, sorted by position"

    opt[Unit]("currentSegmentPosition") action { (_, c) =>
      c.copy(currentSegmentPosition = Some(()))
    } text "Gets the current segment position for the given segment id (Requires --segmentId)"

    opt[Unit]("currentSegmentAtPosition") action { (_, c) =>
      c.copy(currentSegmentAtPosition = Some(()))
    } text "Gets the id of the segment currently in the given position (Requires --position)"

    opt[Unit]("positionsOnDate") action { (_, c) =>
      c.copy(positionsOnDate = Some(()))
    } text "Gets the segment positions as they were on the given date, sorted by position"

    opt[Unit]("mostRecentChange") action { (_, c) =>
      c.copy(mostRecentChange = Some(()))
    } text "Gets the most recent date that segments were changed"

    opt[Unit]("segmentPositionOnDate") action { (_, c) =>
      c.copy(segmentPositionOnDate = Some(()))
    } text "Gets the segment position for the given segment id on the given date (Requires --segmentId)"

    opt[Unit]("segmentAtPositionOnDate") action { (_, c) =>
      c.copy(segmentAtPositionOnDate = Some(()))
    } text "Gets the id of the segment that was installed in the given position on the given date (Requires --position)"

    opt[Unit]("resetTables") action { (_, c) =>
      c.copy(resetTables = Some(()))
    } text "Drops and recreates the database tables (for testing)"

    opt[Unit]("resetSegmentToM1PosTable") action { (_, c) =>
      c.copy(resetSegmentToM1PosTable = Some(()))
    } text "Drops and recreates the segment_to_m1_pos database table (for testing)"

    opt[Unit]("resetJiraSegmentDataTable") action { (_, c) =>
      c.copy(resetJiraSegmentDataTable = Some(()))
    } text "Drops and recreates the jira_segment_data database table (for testing)"

    opt[File]('i', "import") valueName "<mirror-file.json>" action { (x, c) =>
      c.copy(importFile = Some(x))
    } text "Import the given JSON file with date and segment position assignments that was previously exported"

    opt[File]('e', "export") valueName "<mirror-file.json>" action { (x, c) =>
      c.copy(exportFile = Some(x))
    } text "Export the segment position assignments for the current or given date to the given JSON file (use with --date option)"

    opt[File]('e', "exportPlan") valueName "<mirror-file.json>" action { (x, c) =>
      c.copy(exportPlan = Some(x))
    } text "Export the planned segment position assignments (according to JIRA) to the given JSON file"

    help("help")
    version("version")
  }

  // Parse the command line options
  parser.parse(args, EswSegmentClientOptions()) match {
    case Some(options) =>
      try {
        run(options)
        System.exit(0)
      }
      catch {
        case e: Throwable =>
          e.printStackTrace()
          System.exit(1)
      }
    case None => System.exit(1)
  }

  private def error(msg: String): Unit = {
    println(msg)
    System.exit(1)
  }

  private def showResults(result: List[SegmentToM1Pos]): Unit = {
    result.foreach(r => println(s"${dateFormat.format(r.date)} ${r.maybeId.getOrElse("------")} ${r.position}"))
  }

  private def await[T](future: Future[T]): T = Await.result(future, Duration.Inf)

  // Run the application
  private def run(options: EswSegmentClientOptions): Unit = {
    import options._
    val client = new EswSegmentHttpClient(host, port)

    if (options.setPosition.isDefined) {
      if (position.isEmpty) error("--position option is required")
      val segmentToM1Pos = SegmentToM1Pos(date, segmentId, position.get)
      val result         = await(client.setPosition(segmentToM1Pos))
      if (!result)
        error(s"setPosition failed for date: $date, segmentId: ${segmentId.getOrElse("None")}, position: ${position.get}")
    }

    if (options.resetTables.isDefined) {
      val result = await(client.resetTables())
      if (!result)
        error(s"resetTables failed")
    }

    if (options.resetSegmentToM1PosTable.isDefined) {
      val result = await(client.resetSegmentToM1PosTable())
      if (!result)
        error(s"resetSegmentToM1PosTable failed")
    }

    if (options.resetJiraSegmentDataTable.isDefined) {
      val result = await(client.resetJiraSegmentDataTable())
      if (!result)
        error(s"resetJiraSegmentDataTable failed")
    }

    if (options.segmentPositions.isDefined) {
      if (segmentId.isEmpty) error("--segmentId option is required")
      showResults(await(client.segmentPositions(DateRange(from, to), segmentId.get)))
    }

    if (options.segmentIds.isDefined) {
      if (position.isEmpty) error("--position option is required")
      showResults(await(client.segmentIds(DateRange(from, to), position.get)))
    }

    if (options.newlyInstalledSegments.isDefined) {
      showResults(await(client.newlyInstalledSegments(date)))
    }

    if (options.currentPositions.isDefined) {
      showResults(await(client.currentPositions()))
    }

    if (options.plannedPositions.isDefined) {
      showResults(await(client.plannedPositions()))
    }

    if (options.currentSegmentPosition.isDefined) {
      if (segmentId.isEmpty) error("--segmentId option is required")
      showResults(await(client.currentSegmentPosition(segmentId.get)).toList)
    }

    if (options.currentSegmentAtPosition.isDefined) {
      if (position.isEmpty) error("--position option is required")
      showResults(await(client.currentSegmentAtPosition(position.get)).toList)
    }

    if (options.positionsOnDate.isDefined) {
      showResults(await(client.positionsOnDate(date)))
    }

    if (options.mostRecentChange.isDefined) {
      val result = await(client.mostRecentChange(options.date))
      println(result.toString)
    }

    if (options.segmentPositionOnDate.isDefined) {
      if (segmentId.isEmpty) error("--segmentId option is required")
      showResults(await(client.segmentPositionOnDate(date, segmentId.get)).toList)
    }

    if (options.segmentAtPositionOnDate.isDefined) {
      if (position.isEmpty) error("--position option is required")
      showResults(await(client.segmentAtPositionOnDate(date, position.get)).toList)
    }

    if (options.availableSegmentIdsForPos.isDefined) {
      if (position.isEmpty) error("--position option is required")
      val result = await(client.availableSegmentIdsForPos(position.get))
      result.foreach(println)
    }

    if (options.importFile.isDefined) {
      val file = options.importFile.get
      if (!file.exists()) error(s"File $file does not exist")
      val source = Source.fromFile(file)
      val json   = source.getLines().mkString("\n")
      source.close()
      val mirrorConfig = json.parseJson.convertTo[MirrorConfig]
      val result       = await(client.setPositions(mirrorConfig))
      if (!result)
        error(s"Import file failed")
    }

    def saveMirrorConfig(file: File, segments: List[SegmentConfig]): Unit = {
      val config = MirrorConfig(dateFormat.format(date), segments)
      val json   = config.toJson.prettyPrint
      Files.write(file.toPath, json.getBytes(StandardCharsets.UTF_8))

    }

    if (options.exportFile.isDefined) {
      val file = options.exportFile.get
      if (file.exists()) error(s"File $file exists")
      val segments = await(client.positionsOnDate(date)).map(s => SegmentConfig(s.position, s.maybeId))
      saveMirrorConfig(file, segments)
    }

    if (options.exportPlan.isDefined) {
      val file = options.exportPlan.get
      if (file.exists()) error(s"File $file exists")
      val segments = await(client.plannedPositions()).map(s => SegmentConfig(s.position, s.maybeId))
      saveMirrorConfig(file, segments)
    }
  }
}
