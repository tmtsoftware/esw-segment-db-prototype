import React, {useState} from 'react'
import {Config} from "./Config";
import 'react-responsive-modal/styles.css';
import { Modal } from 'react-responsive-modal';
import {SegmentModal} from "./SegmentModal";


type SegmentProps = { id: string, pos: string, date: string, x: number, y: number}

/**
 * Represents one of the 492 segments of the mirror
 * @param id the segment id
 * @param pos A1 to F82
 * @param date date the segment was installed
 * @param x x offset
 * @param y y offset
 * @constructor
 */
export const Segment = ({id, pos, date, x, y}: SegmentProps): JSX.Element => {

  const sector = pos.charAt(0)
  const classNames = id ? `segment ${sector}` : `segment ${sector} empty`
  const labelXOffset = pos.length == 2 ? -3 : -5

  const [open, setOpen] = useState<boolean>(false);

  function onOpenModal() {
    setOpen(true)
  }
  function onCloseModal() {
    setOpen(false)
  }

  function mousePressed() {
    console.log(`Selected segment: id=${id}, pos=${pos}, sector=${sector}`)
    onOpenModal()
  }

  function toolTip(): string {
    if (id)
      return `Pos: ${pos}, Segment ID: ${id}, Date: ${date}`
    return `Pos: ${pos}: Empty`
  }

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
      <Modal open={open} onClose={onCloseModal} center>
        <SegmentModal id={id} pos={pos} date={date}/>
      </Modal>
    </g>
  )
}

