import React from 'react'
import {Segment} from "./Segment";
import {Config} from "./Config";

type SectorProps = { sector: string }

export const Sector = ({sector}: SectorProps): JSX.Element => {
  const xInc = 3 * Config.segmentRadius / 2.0
  const yInc = Config.segmentRadius * Math.sin(60 * Math.PI / 180.0)

  const xOrigin = 300
  const yOrigin = 300
  const xStart = xOrigin + xInc * 2
  const yStart = yOrigin - yInc * 2

  const angle = Config.sectorAngle(sector)

  function segmentRow(count: number, firstPos: number): Array<JSX.Element> {
    return [...Array(count).keys()].map(i => {
      const pos = `SN0${sector}${firstPos+i}`
      return <Segment
        id={pos}
        key={pos}
        pos={`${sector}${firstPos + i}`}
        x={xStart + xInc * (count - 2)}
        y={yStart + yInc * ((2 - count) + i * 2)}/>
    })
  }

  function segmentRows(): Array<JSX.Element> {
    return [...Array(10).keys()].flatMap(i => {
      return segmentRow(i + 2, i * (i + 3) / 2 + 1)
    })
  }

  return (
    <g id={sector} transform={`rotate(${angle}, ${xOrigin}, ${yOrigin})`}>
        {segmentRows()}
    </g>
  )
}

