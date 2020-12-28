import React, {useState} from 'react'
import {TopbarDateChooser} from './TopbarDateChooser'
import {PageHeader} from "antd"

type TopbarProps = {
  mostRecentChange: Date
  updateDisplay: (refDate: Date) => void
  jiraMode: boolean
}

export const Topbar = ({
                         mostRecentChange,
                         updateDisplay,
                         jiraMode
                       }: TopbarProps): JSX.Element => {
  const [selectedDate, setSelectedDate] = useState<Date>(mostRecentChange)

  const refDateChanged = (date: Date) => {
    setSelectedDate(date)
    updateDisplay(date)
  }

  return (
    <PageHeader
      style={{backgroundColor: '#b2c4db', height: '45px', paddingTop: '0'}}
      ghost={true}
      className={'topbarPageHeader'}
      title="TMT Segment Database"
      extra={
        <span>
          <TopbarDateChooser
            mostRecentChange={mostRecentChange}
            updateDisplay={refDateChanged}
            jiraMode={jiraMode}
          />
        </span>
      }
    >
    </PageHeader>
  )
}
