import React from 'react'
import './Mirror.css'
import {Sector} from "./Sector";

export const Mirror = (): JSX.Element => {

  return (
      <div>
        <svg viewBox="0 0 600 600">
          <g className="sectors">
            <rect x="0" y="0" width="600" height="600" />
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

