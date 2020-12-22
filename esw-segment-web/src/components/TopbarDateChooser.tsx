import React, {useState} from 'react'
import {SegmentData} from './SegmentData'
import {Button, Tooltip, Typography} from "antd";
import {LeftOutlined, RightOutlined, BorderOutlined} from '@ant-design/icons'

const {Text} = Typography;

type TopbarDateChooserProps = {
  mostRecentChange: Date
  updateDisplay: (refDate: Date) => void
}

export const TopbarDateChooser = ({
                                    mostRecentChange,
                                    updateDisplay
                                  }: TopbarDateChooserProps): JSX.Element => {
  const [selectedDate, setSelectedDate] = useState<Date>(mostRecentChange)

  // const classes = useStyles()

  function nextDate() {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
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
      headers: {'Content-Type': 'application/json'},
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
      headers: {'Content-Type': 'application/json'},
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
    <span className={'topbarDateChooser'}>
      <Tooltip placement="bottom" title='Go back to the previous segment change'>
      <Button
        type="ghost"
        icon={<LeftOutlined/>}
        size={"large"}
        onClick={() => prevDate()}
      />
      </Tooltip>
      <Tooltip placement="bottom" title='Display changes up to the current date (default)'>
        <Button
          type="ghost"
          icon={<BorderOutlined/>}
          size={"large"}
          onClick={() => today()}
        />
      </Tooltip>
      <Tooltip placement="bottom" title='Go forward to the next segment change'>
        <Button
          type="ghost"
          icon={<RightOutlined/>}
          size={"large"}
          onClick={() => nextDate()}
        />
      </Tooltip>

      {/*// XXX TODO FIXME: Doesn't display most recent date if segment changed and this item was on previous most recent date */}
      <Text className={'topbarDateChooserText'}>{selectedDate.toLocaleDateString('en-US')}</Text>
    </span>
  )
}
