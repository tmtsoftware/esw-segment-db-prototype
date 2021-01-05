package esw.segment.client

import java.util.Date

object EswSegmentClientOptions {
  val defaultPort = 9192
}

case class EswSegmentClientOptions(
    host: String = "localhost",
    port: Int = EswSegmentClientOptions.defaultPort,
    date: Date = new Date(),
    from: Date = new Date(),
    to: Date = new Date(),
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
)
