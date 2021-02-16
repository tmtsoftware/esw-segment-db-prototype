import React, {useState} from 'react'
import {Config} from './Config'
import {SegmentDetails} from './SegmentDetails'
import {JiraSegmentData} from "./SegmentData"
import {useAppContext} from "../AppContext"

type SegmentProps = {
  id?: string
  pos: string
  segmentData: JiraSegmentData
  date?: Date
  x: number
  y: number
}

/**
 * Represents one of the 492 segments of the mirror
 * @param id the segment id
 * @param pos A1 to F82
 * @param segmentData data from JIRA task for segment
 * @param date date the segment was installed
 * @param x x offset of segment in the display
 * @param y y offset of segment in the display
 * @constructor
 */
export const Segment = ({
                          id,
                          pos,
                          segmentData,
                          date,
                          x,
                          y
                        }: SegmentProps): JSX.Element => {
  const {showSegmentIds, viewMode, mostRecentChange} = useAppContext()
  const sector = pos.charAt(0)
  const fill = getFillColor()
  const labelXOffset = pos.length == 2 ? -4 : -6
  const dateStr = date ? date.toDateString() : ''
  const idStr = id ? id : ''
  const label = showSegmentIds ? idStr.substr(3) : pos
  const fontSize = showSegmentIds ? 6 : 7

  const [open, setOpen] = useState<boolean>(false)

  function getFillColor(): string | undefined {
    let c
    switch (viewMode) {
      case "segmentAllocation":
        c = Config.segmentAllocationColors.get(segmentData.originalPartnerBlankAllocation)
        break
      case "itemLocation":
        c = Config.itemLocationColors.get(segmentData.itemLocation)
        break
      case "riskOfLoss":
        c = Config.riskOfLossColors.get(segmentData.riskOfLoss)
        break
      case "components":
        c = Config.componentColors.get(segmentData.components)
        break
      case "status":
        c = Config.statusColors.get(segmentData.status)
        break
      default:
        c = id ? Config.sectorColors.get(sector) : Config.sectorEmptyColors.get(sector)
        break
    }
    return c ? c : Config.undefinedColor
  }

  function openDialog() {
    setOpen(true)
  }

  function closeDialog() {
    setOpen(false)
  }

  // Pop up a modal dialog on mouse press
  function mousePressed() {
    openDialog()
  }

  // Tool tip to display over a segment
  function toolTip(): string {
    if (id) return `Pos: ${pos}, Segment ID: ${id}, Installed: ${dateStr}`
    return `Pos: ${pos}: Empty since: ${dateStr}`
  }

  // Returns a hexagon figure to display marking recently changed segments
  function innerHexagon(): JSX.Element | undefined {
    if (!date || date < mostRecentChange) return undefined
    else {
      return (
        <polygon
          stroke='black'
          strokeWidth='1.0'
          onClick={mousePressed}
          points={Config.innerSegmentPoints}
        />
      )
    }
  }

  return (
    <g
      id={pos}
      key={pos}
      className={'segment'}
      fill={fill}
      transform={`translate(${x}, ${y})`}>
      <title>{toolTip()}</title>
      <polygon
        stroke='white'
        strokeWidth='1.0'
        onClick={mousePressed}
        points={Config.segmentPoints}
      />
      {innerHexagon()}
      <text
        x={labelXOffset}
        y='2'
        onClick={mousePressed}
        transform={`rotate(${-Config.sectorAngle(sector)})`}
        fontSize={fontSize}
        fill={'black'}>
        {label}
      </text>
      <SegmentDetails
        id={id}
        pos={pos}
        date={date}
        open={open}
        closeDialog={closeDialog}
        segmentData={segmentData}
      />
    </g>
  )
}
