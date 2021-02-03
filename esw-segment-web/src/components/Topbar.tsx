import React, {useContext, useState} from 'react'
import {TopbarDateChooser} from './TopbarDateChooser'
import {PageHeader} from "antd"
import {AuthContext, Logout, Login} from '@tmtsoftware/esw-ts'

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

  const {auth} = useContext(AuthContext)

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
          <span style={{marginLeft: 20}}>
                     {!auth ? (
                       <span>Loading...</span>
                     ) : auth.isAuthenticated() ? (
                       <Logout/>
                     ) : (
                       <Login/>
                     )}
          </span>
        </span>
      }
    >
    </PageHeader>
  )
}
