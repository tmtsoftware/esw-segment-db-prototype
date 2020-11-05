import React from 'react'
import './Mirror.css'
import {Sector} from "./Sector";
import {Config} from "./Config";

export const Mirror = (): JSX.Element => {

  return (
      <div>
        <svg viewBox="0 0 600 600">
          <g className="sectors">
            <circle
              cx={Config.xOrigin}
              cy={Config.yOrigin}
              r={Config.segmentRadius*2*10.5}
              fill="none"
              stroke="black"
              strokeWidth="0.5"
            />
            <Sector sector="A"/>
            <Sector sector="B"/>
            <Sector sector="C"/>
            <Sector sector="D"/>
            <Sector sector="E"/>
            <Sector sector="F"/>
          </g>
        </svg>
      </div>
  )
}

