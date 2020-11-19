import React from 'react'
import './Mirror.css'
import {Sector} from "./Sector";
import {Config} from "./Config";
import {SegmentToM1Pos} from "./SegmentData";

type MirrorProps = {
  showSegmentIds: boolean,
  posMap: Map<string, SegmentToM1Pos>,
  mostRecentChange: number,
  updateDisplay: () => void
}

/**
 * Represents the TMT mirror
 *
 * @param showSegmentIds if true, display segment ids instead of positions in the segments
 * @param refDate the reference date to use for the display (default: current date)
 * @constructor
 */
export const Mirror = ({showSegmentIds, posMap, mostRecentChange, updateDisplay}: MirrorProps): JSX.Element => {
  if (posMap.size == 0 || mostRecentChange == 0) {
    return <div/>
  } else {
    const d = Config.mirrorDiameter
    return (
      <div className="mirror-container">
        <svg className="mirror-svg" viewBox={`0 0 ${d} ${d}`} preserveAspectRatio="xMidYMin slice">
          <g className="sectors">
            <circle
              cx={Config.xOrigin}
              cy={Config.yOrigin}
              r={Config.segmentRadius * 2 * 10.5}
              fill="none"
              stroke="black"
              strokeWidth="0.5"
            />
            <Sector sector="A" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="B" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="C" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="D" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="E" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="F" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
          </g>
        </svg>
      </div>
    )
  }
}

