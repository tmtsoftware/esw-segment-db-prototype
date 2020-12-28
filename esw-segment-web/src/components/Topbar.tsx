import React, {useState} from 'react'
import {TopbarDateChooser} from './TopbarDateChooser'
import {Checkbox, PageHeader, Tooltip} from "antd"
import {CheckboxChangeEvent} from "antd/lib/checkbox"

type TopbarProps = {
  mostRecentChange: Date
  updateDisplay: (showSegmentIds: boolean, refDate: Date) => void
  jiraMode: Boolean
}

export const Topbar = ({
                         mostRecentChange,
                         updateDisplay,
                         jiraMode
                       }: TopbarProps): JSX.Element => {
  const [showSegmentIds, setShowSegmentIds] = useState(false)
  const [selectedDate, setSelectedDate] = useState<Date>(mostRecentChange)

  const showSegmentIdsChanged = (event: CheckboxChangeEvent) => {
    setShowSegmentIds(event.target.checked)
    updateDisplay(event.target.checked, selectedDate)
  }

  const refDateChanged = (date: Date) => {
    setSelectedDate(date)
    updateDisplay(showSegmentIds, date)
  }

  function showSegmentIdsCheckbox(): JSX.Element {
    return (
      <Tooltip placement="bottom" title='Display the last part of the segment id instead of the segment position'>
        <Checkbox
          style={{paddingTop: '10px'}}
          checked={showSegmentIds}
          onChange={showSegmentIdsChanged}>
          Show Segment IDs
        </Checkbox>
      </Tooltip>
    )
  }

  return (
    <PageHeader
      style={{backgroundColor: '#b2c4db', height: '45px', paddingTop: '0'}}
      ghost={true}
      className={'topbarPageHeader'}
      title="TMT Segment Database"
      extra={
        <span>
          {showSegmentIdsCheckbox()}
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
