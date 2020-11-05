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

  function segmentRow(row: number, count: number, firstPos: number, offset: number = 0): Array<JSX.Element> {
    return [...Array(count).keys()].map(i => {
      const id = `SN0${sector}${firstPos + i}`
      return <Segment
        id={id}
        key={id}
        pos={`${sector}${firstPos + i}`}
        x={xStart + xInc * row}
        y={yStart + yInc * ((2 - count) + (i+offset/2.0) * 2)}
      />
    })
  }

  function segmentRows(): Array<JSX.Element> {
    return [...Array(12).keys()].flatMap(i => {
      switch (i) {
        case 10:
          return segmentRow(10, 11, 66, 1)
        case 11:
          return segmentRow(11, 6, 77, 1)
        default:
          return segmentRow(i, i + 2, i * (i + 3) / 2 + 1)
      }
    })
  }

  return (
    <g id={sector} transform={`rotate(${angle}, ${xOrigin}, ${yOrigin})`}>
      {segmentRows()}
    </g>
  )
}

