import React, {useEffect, useState} from 'react'
import './Mirror.css'
import {Sector} from "./Sector";
import {Config} from "./Config";
import {SegmentData, SegmentToM1Pos} from "./SegmentData";

export const Mirror = (): JSX.Element => {

  const [posMap, setPosMap] = useState<Map<string, SegmentToM1Pos>>(new Map());

  function update() {
    useEffect(() => {
      const x = fetch(`${SegmentData.baseUri}/currentPositions`)
        .then(response => response.json())
        .then(data => {
          const currentPositions: Array<SegmentToM1Pos> = data
          const posMap: Map<string, SegmentToM1Pos> = currentPositions
            .reduce((map, obj) => map.set(obj.position, obj), new Map())
          setPosMap(posMap)
        });
      // empty dependency array means this effect will only run once
    }, []);
  }

  update()

  return (
    <div>
      <svg viewBox="0 0 600 600">
        <g className="sectors">
          <circle
            cx={Config.xOrigin}
            cy={Config.yOrigin}
            r={Config.segmentRadius * 2 * 10.5}
            fill="none"
            stroke="black"
            strokeWidth="0.5"
          />
          <Sector sector="A" posMap={posMap}/>
          <Sector sector="B" posMap={posMap}/>
          <Sector sector="C" posMap={posMap}/>
          <Sector sector="D" posMap={posMap}/>
          <Sector sector="E" posMap={posMap}/>
          <Sector sector="F" posMap={posMap}/>
        </g>
      </svg>
    </div>
  )
}

