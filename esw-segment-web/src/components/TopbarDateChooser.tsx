import React, {useState} from 'react'
import {SegmentData} from './SegmentData'
import {Button, DatePicker, Tooltip, Typography} from "antd";
import {LeftOutlined, RightOutlined} from '@ant-design/icons'
import moment from "moment";
import {format} from "date-fns";

type TopbarDateChooserProps = {
  mostRecentChange: Date
  updateDisplay: (refDate: Date) => void
  jiraMode: boolean
}

export const TopbarDateChooser = ({
                                    mostRecentChange,
                                    updateDisplay,
                                    jiraMode
                                  }: TopbarDateChooserProps): JSX.Element => {
  const [selectedDate, setSelectedDate] = useState<Date>(mostRecentChange)

  function nextDate() {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(format(selectedDate, 'yyyy-MM-dd'))

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
      body: JSON.stringify(format(selectedDate, 'yyyy-MM-dd'))
    }
    fetch(`${SegmentData.baseUri}/prevChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        setSelectedDate(date)
        updateDisplay(date)
      })
  }

  const handleDateChange = (value: moment.Moment | null) => {
    const newDate = value ? value.toDate() : new Date()
    setSelectedDate(newDate)
    updateDisplay(newDate)

  }

  return (
    jiraMode ? <span/> :
      <span className={'topbarDateChooser'}>
      <Tooltip placement="bottom" title='Go back to the previous segment change'>
      <Button
        type="text"
        icon={<LeftOutlined/>}
        size={"large"}
        onClick={() => prevDate()}
      />
      </Tooltip>
      <Tooltip placement="bottom" title='Display changes up to the selected date'>
        <DatePicker
          style={{backgroundColor: '#b2c4db', border: 0}}
          allowClear={false}
          format={"ddd ll"}
          showToday={true}
          onChange={handleDateChange}
          defaultValue={moment(selectedDate)}
          value={moment(selectedDate)}
        />
      </Tooltip>
      <Tooltip placement="bottom" title='Go forward to the next segment change'>
        <Button
          type="text"
          icon={<RightOutlined/>}
          size={"large"}
          onClick={() => nextDate()}
        />
      </Tooltip>
    </span>
  )
}
