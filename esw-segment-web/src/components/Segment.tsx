import React from 'react'
import {Config} from "./Config";

type SegmentProps = { id: string, pos: string, x: number, y: number }

export const Segment = ({id, pos, x, y}: SegmentProps): JSX.Element => {
  const sector = pos.charAt(0)
  const classNames = `segment ${sector}`
  const labelXOffset = pos.length == 2 ? -3 : -5

  return (
    <g id={pos} key={pos} className={classNames} transform={`translate(${x}, ${y})`}>
      <title>{id}</title>
      <polygon
        stroke="white"
        strokeWidth="0.5"
        points={Config.segmentPoints}/>
      <text
        x={labelXOffset}
        y="2"
        transform={`rotate(${-Config.sectorAngle(sector)})`}
        fontSize="5"
        fill="white">
        {pos}
      </text>
    </g>
  )
}

