import React from 'react'
import {Segment} from "./Segment";
import {Config} from "./Config";

type SectorProps = { sector: string }

export const Sector = ({sector}: SectorProps): JSX.Element => {
  const xInc = 3 * Config.segmentRadius / 2.0
  const yInc = Config.segmentRadius * Math.sin(60 * Math.PI / 180.0)

  const xOrigin = 200
  const yOrigin = 200
  const xStart = xOrigin + xInc * 2
  const yStart = yOrigin - yInc * 2

  const angle = Config.sectorAngle(sector)

  return (
    <g id={sector} transform={`rotate(${angle}, ${xOrigin}, ${yOrigin})`}>
      <Segment id="SN001" pos={`${sector}1`} x={xStart} y={yStart}/>
      <Segment id="SN002" pos={`${sector}2`} x={xStart} y={yStart + yInc * 2}/>

      <Segment id="SN001" pos={`${sector}3`} x={xStart + xInc} y={yStart - yInc}/>
      <Segment id="SN001" pos={`${sector}4`} x={xStart + xInc} y={yStart + yInc}/>
      <Segment id="SN001" pos={`${sector}5`} x={xStart + xInc} y={yStart + yInc * 3}/>

      <Segment id="SN001" pos={`${sector}6`} x={xStart + xInc * 2} y={yStart - yInc * 2}/>
      <Segment id="SN001" pos={`${sector}7`} x={xStart + xInc * 2} y={yStart}/>
      <Segment id="SN001" pos={`${sector}8`} x={xStart + xInc * 2} y={yStart + yInc * 2}/>
      <Segment id="SN001" pos={`${sector}9`} x={xStart + xInc * 2} y={yStart + yInc * 4}/>

      <Segment id="SN001" pos={`${sector}10`} x={xStart + xInc * 3} y={yStart - yInc * 3}/>
      <Segment id="SN001" pos={`${sector}11`} x={xStart + xInc * 3} y={yStart - yInc}/>
      <Segment id="SN001" pos={`${sector}12`} x={xStart + xInc * 3} y={yStart + yInc}/>
      <Segment id="SN001" pos={`${sector}13`} x={xStart + xInc * 3} y={yStart + yInc * 3}/>
      <Segment id="SN001" pos={`${sector}14`} x={xStart + xInc * 3} y={yStart + yInc * 5}/>

      <Segment id="SN001" pos={`${sector}15`} x={xStart + xInc * 4} y={yStart - yInc * 4}/>
      <Segment id="SN001" pos={`${sector}16`} x={xStart + xInc * 4} y={yStart - yInc * 2}/>
      <Segment id="SN001" pos={`${sector}17`} x={xStart + xInc * 4} y={yStart}/>
      <Segment id="SN001" pos={`${sector}18`} x={xStart + xInc * 4} y={yStart + yInc * 2}/>
      <Segment id="SN001" pos={`${sector}19`} x={xStart + xInc * 4} y={yStart + yInc * 4}/>
      <Segment id="SN001" pos={`${sector}20`} x={xStart + xInc * 4} y={yStart + yInc * 6}/>
    </g>
  )
}

