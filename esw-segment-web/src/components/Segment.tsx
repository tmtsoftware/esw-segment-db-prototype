import React from 'react'
import {Config} from "./Config";

type SegmentProps = { id: string, pos: string, date: string, x: number, y: number}

export const Segment = ({id, pos, date, x, y}: SegmentProps): JSX.Element => {
  const sector = pos.charAt(0)
  const classNames = id ? `segment ${sector}` : `segment ${sector} empty`
  const labelXOffset = pos.length == 2 ? -3 : -5

  function mousePressed() {
    console.log(`Selected segment: id=${id}, pos=${pos}, sector=${sector}`)
  }

  function toolTip(): string {
    if (id)
      return `Pos: ${pos}, Segment ID: ${id}, Date: ${date}`
    return `Pos: ${pos}: Empty`
  }

  // function fill(): string {
  //   return id ? "black" : "red"
  // }

  return (
    <g id={pos} key={pos} className={classNames} transform={`translate(${x}, ${y})`}>
      <title>{toolTip()}</title>
      <polygon
        stroke="white"
        strokeWidth="0.5"
        onClick={mousePressed}
        points={Config.segmentPoints}/>
      <text
        x={labelXOffset}
        y="2"
        onClick={mousePressed}
        transform={`rotate(${-Config.sectorAngle(sector)})`}
        fontSize="5"
        fill={"black"}>
        {pos}
      </text>
    </g>
  )
}

