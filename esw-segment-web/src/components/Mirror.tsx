import React, {useEffect, useState} from 'react'
import './Mirror.css'
import {Sector} from "./Sector";
import {Config} from "./Config";
import {SegmentData, SegmentToM1Pos} from "./SegmentData";

type MirrorProps = {
  showSegmentIds: boolean,
}

/**
 * Represents the TMT mirror
 * @constructor
 */
export const Mirror = ({showSegmentIds}: MirrorProps): JSX.Element => {

  const [posMap, setPosMap] = useState<Map<string, SegmentToM1Pos>>(new Map());
  const [mostRecentChange, setMostRecentChange] = useState<number>(0);


  async function fetchData() {
    const [mostRecentChangeResponse, currentPositionsResponse] = await Promise.all([
      fetch(`${SegmentData.baseUri}/mostRecentChange`),
      fetch(`${SegmentData.baseUri}/currentPositions`)
    ]);

    const mostRecentChange: number = await mostRecentChangeResponse.json()
    const currentPositions: Array<SegmentToM1Pos> = await currentPositionsResponse.json()

    return {
      mostRecentChange,
      currentPositions
    };
  }

  function updateDisplay() {
    fetchData().then(({mostRecentChange, currentPositions}) => {
      const posMap: Map<string, SegmentToM1Pos> = currentPositions
        .reduce((map, obj) => map.set(obj.position, obj), new Map())
      setPosMap(posMap)
      setMostRecentChange(mostRecentChange)
    });

  }

  useEffect(() => {
    updateDisplay()
  }, []);

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

