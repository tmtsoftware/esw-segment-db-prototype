import React from 'react'
import {SegmentData} from './SegmentData'
import {Button, DatePicker, Tooltip} from "antd";
import {LeftOutlined, RightOutlined} from '@ant-design/icons'
import moment from "moment";
import {format} from "date-fns";
import {useAppContext} from "../AppContext";

export const TopbarDateChooser = (): JSX.Element => {
  const {refDate, setRefDate, jiraMode} = useAppContext()

  function nextDate() {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(format(refDate, 'yyyy-MM-dd'))

  }
    fetch(`${SegmentData.baseUri}/nextChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        setRefDate(date)
      })
  }

  function prevDate() {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(format(refDate, 'yyyy-MM-dd'))
    }
    fetch(`${SegmentData.baseUri}/prevChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        setRefDate(date)
      })
  }

  const handleDateChange = (value: moment.Moment | null) => {
    const newDate = value ? value.toDate() : new Date()
    setRefDate(newDate)

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
          defaultValue={moment(refDate)}
          value={moment(refDate)}
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
