import React, { useState } from 'react'
import { SegmentData } from './SegmentData'
import {Button, Layout, Tooltip, Typography} from "antd";
import { LeftOutlined, RightOutlined, BorderOutlined } from '@ant-design/icons'
const {Title} = Typography;

type TopbarDateChooserProps = {
  mostRecentChange: Date
  updateDisplay: (refDate: Date) => void
}

// const useStyles = makeStyles({
//   icons: {
//     paddingLeft: 5,
//     paddingRight: 5,
//     paddingBottom: 0
//   },
//   formControlLabel: {
//     fontSize: 14,
//     paddingLeft: 15,
//     paddingTop: 2,
//     paddingBottom: 2
//   }
// })

export const TopbarDateChooser = ({
  mostRecentChange,
  updateDisplay
}: TopbarDateChooserProps): JSX.Element => {
  const [selectedDate, setSelectedDate] = useState<Date>(mostRecentChange)
  // const classes = useStyles()

  function nextDate() {
    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(selectedDate.getTime())
    }
    fetch(`${SegmentData.baseUri}/nextChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        setSelectedDate(date)
        updateDisplay(date)
      })
  }

  function prevDate() {
    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(selectedDate.getTime())
    }
    fetch(`${SegmentData.baseUri}/prevChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        setSelectedDate(date)
        updateDisplay(date)
      })
  }

  function today() {
    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(new Date().getTime())
    }
    fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        setSelectedDate(date)
        updateDisplay(date)
      })
  }

  return (
    <div>
      <Tooltip placement="bottom" title='Go back to the previous segment change'>
      <Button
        type="primary"
        icon={<LeftOutlined />}
        size={"large"}
        onClick={() => prevDate()}
      />
      </Tooltip>



      <Tooltip placement="bottom" title='Display changes up to the current date (default)'>
        <Button
          type="primary"
          icon={<BorderOutlined />}
          size={"large"}
          onClick={() => today()}
        />
      </Tooltip>


      <Tooltip placement="bottom" title='Go forward to the next segment change'>
        <Button
          type="primary"
          icon={<RightOutlined />}
          size={"large"}
          onClick={() => nextDate()}
        />
      </Tooltip>

      {/*// XXX TODO FIXME: Doesn't display most recent date if segment changed and this item was on previous most recent date */}
      <Title level={5}>{selectedDate.toLocaleDateString('en-US')}</Title>
    </div>
  )
}
