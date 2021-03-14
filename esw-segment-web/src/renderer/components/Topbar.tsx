import React from 'react'
import {TopbarDateChooser} from './TopbarDateChooser'
import {PageHeader} from "antd"
import {Logout, Login} from '@tmtsoftware/esw-ts'
import {useAppContext} from "../AppContext"

export const Topbar = (): JSX.Element => {

  const {auth, authEnabled} = useAppContext()

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
          <TopbarDateChooser/>
          <span style={{marginLeft: 20}}>
            {authEnabled ? makeLoginItem() : <span/>}
          </span>
        </span>
      }
    >
    </PageHeader>
  )
}
