import React from 'react'
import {Segment} from './Segment'
import {Config} from './Config'
import {JiraSegmentData, SegmentToM1Pos} from './SegmentData'
import {Auth} from "@tmtsoftware/esw-ts";

type SectorProps = {
  sector: string
  posMap: Map<string, SegmentToM1Pos>
  segmentMap: Map<string, JiraSegmentData>
  mostRecentChange: Date
  showSegmentIds: boolean
  updateDisplay: () => void
  viewMode: React.Key
  auth: Auth | null
}

/**
 * Represents a sector of the mirror
 * @param sector A to F
 * @param posMap a map of pos (A1 to F82) to SegmentToM1Pos object
 * @param segmentMap maps pos ("A23") to data from JIRA task
 * @param mostRecentChange date of most recent segment change
 * @param showSegmentIds if true display segment ids in the segments instead of the position
 * @param updateDisplay function to update the display after a DB change
 * @param viewMode string indicating the selected view (from the Sidebar menu)
 * @param auth login authorization from Keycloak
 * @constructor
 */
export const Sector = ({
                         sector,
                         posMap,
                         segmentMap,
                         mostRecentChange,
                         showSegmentIds,
                         updateDisplay,
                         viewMode,
                         auth
                       }: SectorProps): JSX.Element => {
  const xInc = (3 * Config.segmentRadius) / 2.0
  const yInc = Config.segmentRadius * Math.sin((60 * Math.PI) / 180.0)

  const xStart = Config.xOrigin + xInc * 2
  const yStart = Config.yOrigin - yInc * 2

  const angle = Config.sectorAngle(sector)

  function segmentRow(
    row: number,
    count: number,
    firstPos: number,
    offset = 0
  ): Array<JSX.Element> {
    if (posMap) {
      return [...Array(count).keys()].map((i) => {
        const pos = `${sector}${firstPos + i}`
        const segmentToM1Pos = posMap.get(pos)
        const segmentData = segmentMap.get(pos) || {
          position: '',
          segmentId: '',
          jiraKey: '',
          jiraUri: '',
          sector: 0,
          segmentType: 0,
          partNumber: '',
          originalPartnerBlankAllocation: '',
          itemLocation: '',
          riskOfLoss: '',
          components: '',
          status: '',
          workPackages: '',
          acceptanceCertificates: '',
          acceptanceDateBlank: '',
          shippingAuthorizations: ''
        }
        const id = segmentToM1Pos ? segmentToM1Pos.maybeId || '' : ''
        const key = pos
        const date = segmentToM1Pos ? new Date(segmentToM1Pos.date) : undefined
        return (
          <Segment
            id={id}
            pos={pos}
            segmentData={segmentData}
            date={date}
            mostRecentChange={mostRecentChange}
            showSegmentIds={showSegmentIds}
            key={key}
            x={xStart + xInc * row}
            y={yStart + yInc * (2 - count + (i + offset / 2.0) * 2)}
            updateDisplay={updateDisplay}
            viewMode={viewMode}
            auth={auth}
          />
        )
      })
    } else {
      return []
    }
  }

  function segmentRows(): Array<JSX.Element> {
    return [...Array(12).keys()].flatMap((i) => {
      switch (i) {
        case 10:
          return segmentRow(10, 11, 66, 1)
        case 11:
          return segmentRow(11, 6, 77, 1)
        default:
          return segmentRow(i, i + 2, (i * (i + 3)) / 2 + 1)
      }
    })
  }

  return (
    <g
      id={sector}
      transform={`rotate(${angle}, ${Config.xOrigin}, ${Config.yOrigin})`}>
      {segmentRows()}
    </g>
  )
}
