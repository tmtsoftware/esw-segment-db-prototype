import React from 'react'
import './Mirror.css'
import {Sector} from './Sector'
import {Config} from './Config'
import {useAppContext} from "../AppContext"

/**
 * Represents the TMT mirror
 */
export const Mirror = (): JSX.Element => {
  const {showSpares, posMap, mostRecentChange} = useAppContext()
  if (posMap.size == 0 || mostRecentChange.getTime() == 0) {
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
              />
            ))}
          </g>
        </svg>
      </div>
    )
  }
}
