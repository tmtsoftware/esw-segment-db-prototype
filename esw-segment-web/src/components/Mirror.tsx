import React from 'react'
import './Mirror.css'
import {Sector} from './Sector'
import {Config} from './Config'
import {JiraSegmentData, SegmentToM1Pos} from './SegmentData'
import {useAppContext} from "../AppContext"

type MirrorProps = {
  posMap: Map<string, SegmentToM1Pos>
  segmentMap: Map<string, JiraSegmentData>
  mostRecentChange: Date
}

/**
 * Represents the TMT mirror
 *
 * @param showSegmentIds if true, display segment ids instead of positions in the segments
 * @param posMap map pos ("A23") to object containing segment id and date
 * @param segmentMap maps pos ("A23") to data from JIRA task
 * @param mostRecentChange date of most recent change to installed segments
 * @constructor
 */
export const Mirror = ({
                         posMap,
                         segmentMap,
                         mostRecentChange
                       }: MirrorProps): JSX.Element => {
  if (posMap.size == 0 || mostRecentChange.getTime() == 0) {
    return <div/>
  } else {
    const {showSpares} = useAppContext()
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
              />
            ))}
          </g>
        </svg>
      </div>
    )
  }
}
