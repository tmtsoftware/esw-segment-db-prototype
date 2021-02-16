import React, {useState} from 'react'
import {TopbarDateChooser} from './TopbarDateChooser'
import {PageHeader} from "antd"
import {Logout, Login} from '@tmtsoftware/esw-ts'
import {useAppContext} from "../AppContext"

type TopbarProps = {
  mostRecentChange: Date
}

export const Topbar = ({
                         mostRecentChange
                       }: TopbarProps): JSX.Element => {

  const {setRefDate, jiraMode, auth, authEnabled} = useAppContext()
  const [, setSelectedDate] = useState<Date>(mostRecentChange)

  const refDateChanged = (date: Date) => {
    setSelectedDate(date)
    setRefDate(date)
  }

  const makeLoginItem = () => {
    return (!auth ? (
      <span>Loading...</span>
    ) : auth.isAuthenticated() ? (
      <Logout/>
    ) : (
      <Login/>
    ))
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
            {authEnabled ? makeLoginItem() : <span/>}
          </span>
        </span>
      }
    >
    </PageHeader>
  )
}
