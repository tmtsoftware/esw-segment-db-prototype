import React, {useState} from 'react'
import {Config} from "./Config";
import 'react-responsive-modal/styles.css';
import {Modal} from 'react-responsive-modal';


type SegmentModalProps = { id: string, pos: string, date: string }

export const SegmentModal = ({id, pos, date}: SegmentModalProps): JSX.Element => {
  if (id)
    return (
      <div>
        <strong>Segment {pos}</strong>
        <ul>
          <li>
            id: {id}
          </li>
          <li>
            Installed on: {date}
          </li>
        </ul>
      </div>
    )
  else return (
    <div>
      <strong>Segment {pos}</strong>
      <p><em>Currently empty</em></p>
    </div>
  )
}

