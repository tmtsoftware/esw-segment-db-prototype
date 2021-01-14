package esw.segment.client

import java.io.File
import java.time.LocalDate

object EswSegmentClientOptions {
  val defaultPort = 9192
}

case class EswSegmentClientOptions(
    host: String = "localhost",
    port: Int = EswSegmentClientOptions.defaultPort,
    date: LocalDate = LocalDate.now(),
    from: LocalDate = LocalDate.now(),
    to: LocalDate = LocalDate.now(),
    segmentId: Option[String] = None,
    position: Option[String] = None,
    setPosition: Option[Unit] = None,
    segmentPositions: Option[Unit] = None,
    segmentIds: Option[Unit] = None,
    newlyInstalledSegments: Option[Unit] = None,
    currentPositions: Option[Unit] = None,
    plannedPositions: Option[Unit] = None,
    currentSegmentPosition: Option[Unit] = None,
    currentSegmentAtPosition: Option[Unit] = None,
    positionsOnDate: Option[Unit] = None,
    mostRecentChange: Option[Unit] = None,
    segmentPositionOnDate: Option[Unit] = None,
    segmentAtPositionOnDate: Option[Unit] = None,
    availableSegmentIdsForPos: Option[Unit] = None,
    resetTables: Option[Unit] = None,
    resetSegmentToM1PosTable: Option[Unit] = None,
    resetJiraSegmentDataTable: Option[Unit] = None,
    importFile: Option[File] = None,
    exportFile: Option[File] = None,
    exportPlan: Option[File] = None,
    exportJiraData: Option[File] = None,
)
