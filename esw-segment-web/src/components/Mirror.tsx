import React from 'react'
import './Mirror.css'
import {Sector} from './Sector'
import {Config} from './Config'
import {JiraSegmentData, SegmentToM1Pos} from './SegmentData'

type MirrorProps = {
  showSegmentIds: boolean
  showSpares: boolean
  posMap: Map<string, SegmentToM1Pos>
  segmentMap: Map<string, JiraSegmentData>
  mostRecentChange: number
  updateDisplay: () => void
  viewMode: React.Key
}

/**
 * Represents the TMT mirror
 *
 * @param showSegmentIds if true, display segment ids instead of positions in the segments
 * @param showSpares if true, only display the spare segments
 * @param posMap map pos ("A23") to object containing segment id and date
 * @param segmentMap maps pos ("A23") to data from JIRA task
 * @param mostRecentChange date of most recent change to installed segments
 * @param updateDisplay function to update the display
 * @param viewMode string indicating the selected view (from the Sidebar menu)
 * @constructor
 */
export const Mirror = ({
                         showSegmentIds,
                         showSpares,
                         posMap,
                         segmentMap,
                         mostRecentChange,
                         updateDisplay,
                         viewMode
                       }: MirrorProps): JSX.Element => {
  if (posMap.size == 0 || mostRecentChange == 0) {
    return <div/>
  } else {
    const sectors = showSpares ? ['G'] : ['A', 'B', 'C', 'D', 'E', 'F']
    const d = Config.mirrorDiameter
    return (
      <div className='mirror-container'>
        <svg
          className='mirror-svg'
          viewBox={`0 0 ${d} ${d}`}
          preserveAspectRatio='xMidYMin slice'>
          <g className='sectors'>
            <circle
              cx={Config.xOrigin}
              cy={Config.yOrigin}
              r={Config.segmentRadius * 2 * 10.5}
              fill='none'
              stroke='white'
              // stroke='black'
              strokeWidth='0.5'
            />
            {sectors.map((sector) => (
              <Sector
                sector={sector}
                key={sector}
                posMap={posMap}
                segmentMap={segmentMap}
                mostRecentChange={mostRecentChange}
                showSegmentIds={showSegmentIds}
                updateDisplay={updateDisplay}
                viewMode={viewMode}
              />
            ))}
          </g>
        </svg>
      </div>
    )
  }
}
