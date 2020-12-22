import React, {useState} from 'react'
import {Config} from './Config'
import {SegmentDetails} from './SegmentDetails'

type SegmentProps = {
  id?: string
  pos: string
  date?: number
  mostRecentChange: number
  showSegmentIds: boolean
  x: number
  y: number
  updateDisplay: () => void
}

/**
 * Represents one of the 492 segments of the mirror
 * @param id the segment id
 * @param pos A1 to F82
 * @param date date the segment was installed
 * @param mostRecentChange date of most recent segment change
 * @param showSegmentIds if true, display the segment id, otherwise the position
 * @param x x offset of segment in the display
 * @param y y offset of segment in the display
 * @param updateDisplay function to update the display after a DB change
 * @constructor
 */
export const Segment = ({
                          id,
                          pos,
                          date,
                          mostRecentChange,
                          showSegmentIds,
                          x,
                          y,
                          updateDisplay
                        }: SegmentProps): JSX.Element => {
  const sector = pos.charAt(0)
  const classNames = `segment ${sector}` + (id ? '' : ' empty')
  const labelXOffset = pos.length == 2 ? -4 : -6
  // const dateStr = date ? new Date(date).toLocaleDateString('en-US') : ''
  const dateStr = date ? new Date(date).toDateString() : ''
  const idStr = id ? id : ''
  const label = showSegmentIds ? idStr.substr(2) : pos
  const fontSize = showSegmentIds ? 6 : 7

  const [open, setOpen] = useState<boolean>(false)

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
      className={classNames}
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
        updateDisplay={updateDisplay}
      />
    </g>
  )
}
