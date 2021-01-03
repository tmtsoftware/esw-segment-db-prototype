import React, {useState} from 'react'
import {SegmentData} from './SegmentData'
import {Button, DatePicker, Tooltip, Typography} from "antd";
import {LeftOutlined, RightOutlined} from '@ant-design/icons'
import moment from "moment";

const {Text} = Typography;

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

  // function today() {
  //   const requestOptions = {
  //     method: 'POST',
  //     headers: {'Content-Type': 'application/json'},
  //     body: JSON.stringify(new Date().getTime())
  //   }
  //   fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions)
  //     .then((response) => response.json())
  //     .then((result) => {
  //       const date: Date = new Date(result)
  //       setSelectedDate(date)
  //       updateDisplay(date)
  //     })
  // }

  const handleDateChange = (value: moment.Moment | null) => {
    const newDate = value ? value.toDate() : new Date()
    // const requestOptions = {
    //   method: 'POST',
    //   headers: {'Content-Type': 'application/json'},
    //   body: JSON.stringify(newDate.getTime())
    // }
    // fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions)
    //   .then((response) => response.json())
    //   .then((result) => {
    //     const date: Date = new Date(result)
    //     setSelectedDate(date)
    //     updateDisplay(date)
    //   })
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
          // format={"YYYY-MM-DD"}
          format={"ddd ll"}
          showToday={true}
          onChange={handleDateChange}
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

        {/*// XXX TODO FIXME: Doesn't display most recent date if segment changed and this item was on previous most recent date */}
        {/*<Text className={'topbarDateChooserText'}>{selectedDate.toLocaleDateString('en-US')}</Text>*/}
        {/*<Text className={'topbarDateChooserText'}>{selectedDate.toDateString()}</Text>*/}
    </span>
  )
}
