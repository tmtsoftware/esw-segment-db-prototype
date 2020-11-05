import React from 'react'
import {Config} from "./Config";

type SegmentProps = { id: string, pos: string, x: number, y: number }

// export const Segment = ({id, pos, x, y}: SegmentProps): JSX.Element => {
//   const sector = pos.charAt(0)
//   const classNames = `segment ${sector}`
//   const s1 = 5
//   const s2 = s1*2
//   const s3 = s2-1
//   const labelXOffset = pos.length == 2 ? -3 : -5
//
//   return (
//           <g id={pos} className={classNames} transform={`translate(${x}, ${y})`}>
//             <title>{id}</title>
//             <polygon
//               stroke="white"
//               strokeWidth="0.5"
//               points={`${s1},${-s3} ${-s1},${-s3} ${-s2},0 ${-s1},${s3} ${s1},${s3} ${s2},0`}/>
//             <text x={labelXOffset} y="2" fontSize="5" fill="white">{pos}</text>
//           </g>
//   )
// }
export const Segment = ({id, pos, x, y}: SegmentProps): JSX.Element => {
  const sector = pos.charAt(0)
  const classNames = `segment ${sector}`
  const labelXOffset = pos.length == 2 ? -3 : -5

  const points = [0, 1, 2, 3, 4, 5].map(a => {
      const px = Config.segmentRadius * Math.cos(a * 60 * Math.PI / 180.0)
      const py = Config.segmentRadius * Math.sin(a * 60 * Math.PI / 180.0)
      return `${px},${py}`
    }
  ).join(" ")

  return (
    <g id={pos} className={classNames} transform={`translate(${x}, ${y})`}>
      <title>{id}</title>
      <polygon
        stroke="white"
        strokeWidth="0.5"
        points={points}/>
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

