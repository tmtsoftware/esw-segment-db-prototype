import React, {useContext, useEffect, useState} from 'react'
import './App.css'
import {Topbar} from './components/Topbar'
import {Mirror} from './components/Mirror'
import {JiraSegmentData, SegmentData, SegmentToM1Pos} from './components/SegmentData'
import {Layout} from "antd"
import 'antd/dist/antd.css'
import {Sidebar} from "./components/Sidebar";
import {Legend} from "./components/Legend";
import {format} from "date-fns";
import {AuthContext} from '@tmtsoftware/esw-ts'
import {appContext} from './context'

const {Content} = Layout

const App = (): JSX.Element => {
  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false)
  const [showSpares, setShowSpares] = useState<boolean>(false)
  const [posMap, setPosMap] = useState<Map<string, SegmentToM1Pos>>(new Map())
  const [segmentMap, setSegmentMap] = useState<Map<string, JiraSegmentData>>(new Map())
  const [mostRecentChange, setMostRecentChange] = useState<Date>(new Date(0))
  const [viewMode, setViewMode] = useState<React.Key>("installed")
  const [jiraMode, setJiraMode] = useState<boolean>(false)
  const [authEnabled, setAuthEnabled] = useState<boolean>(true)
  const [refDate, setRefDate] = useState<Date>(new Date())

  const {auth} = useContext(AuthContext)

  async function fetchData(refDate: Date) {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(format(refDate, 'yyyy-MM-dd'))
    }

    const [
      mostRecentChangeResponse,
      positionsOnDateResponse,
      segmentDataResponse,
      authEnabledResponse
    ] = await Promise.all([
      fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions),
      fetch(`${SegmentData.baseUri}/positionsOnDate`, requestOptions),
      fetch(`${SegmentData.baseUri}/segmentData`),
      fetch(`${SegmentData.baseUri}/authEnabled`),
    ])

    const dateStr: string = await mostRecentChangeResponse.json()
    const mostRecentChange: Date = new Date(dateStr)
    const positionsOnDate: Array<SegmentToM1Pos> = await positionsOnDateResponse.json()
    const segmentData: Array<JiraSegmentData> = await segmentDataResponse.json()
    const authEnabled: boolean = await authEnabledResponse.json()

    return {
      mostRecentChange,
      positionsOnDate,
      segmentData,
      authEnabled
    }
  }

  async function fetchPlannedData() {
    const [
      plannedPositionsResponse,
      segmentDataResponse
    ] = await Promise.all([
      fetch(`${SegmentData.baseUri}/plannedPositions`),
      fetch(`${SegmentData.baseUri}/segmentData`),
    ])

    const plannedPositions: Array<SegmentToM1Pos> = await plannedPositionsResponse.json()
    const segmentData: Array<JiraSegmentData> = await segmentDataResponse.json()

    return {
      plannedPositions,
      segmentData
    }
  }

  function updateDataNow(refDate: Date) {
    if (!jiraMode) {
      fetchData(refDate).then(({mostRecentChange, positionsOnDate, segmentData, authEnabled}) => {
        const posMap: Map<string, SegmentToM1Pos> = positionsOnDate.reduce(
          (map, obj) => map.set(obj.position, obj),
          new Map()
        )
        setPosMap(posMap)
        const segMap: Map<string, JiraSegmentData> = segmentData.reduce(
          (map, obj) => map.set(obj.position, obj),
          new Map()
        )
        setSegmentMap(segMap)
        setMostRecentChange(mostRecentChange)
        setAuthEnabled(authEnabled)
      })
    } else {
      fetchPlannedData().then(({plannedPositions, segmentData}) => {
        const posMap: Map<string, SegmentToM1Pos> = plannedPositions.reduce(
          (map, obj) => map.set(obj.position, obj),
          new Map()
        )
        setPosMap(posMap)
        const segMap: Map<string, JiraSegmentData> = segmentData.reduce(
          (map, obj) => map.set(obj.position, obj),
          new Map()
        )
        setSegmentMap(segMap)
      })
    }
  }

  function updateData() {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(format(new Date(), 'yyyy-MM-dd'))
    }
    fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const dateStr: string = result
        const date: Date = new Date(dateStr)
        updateDataNow(date)
      })
  }

  function updateDisplay() {
    updateDataNow(refDate)
  }

  // Update the segment data being displayed whenever the JIRA mode changes
  useEffect(() => {
    updateData()
  }, [jiraMode])

  // Update the display whenever the reference date changes
  useEffect(() => {
    updateDisplay()
  }, [refDate])

  function sidebarOptionsChanged(viewMode: string | number, showSegmentIds: boolean, showSpares: boolean) {
    setJiraMode(viewMode != 'installed')
    setViewMode(viewMode)
    setShowSegmentIds(showSegmentIds)
    setShowSpares(showSpares)
  }

  if (mostRecentChange.getTime() == 0) return <div/>
  else
    return (
      <appContext.Provider value={{refDate, setRefDate, updateDisplay: updateData}}>
        <Layout className='App'>
          <Topbar
            mostRecentChange={mostRecentChange}
            jiraMode={jiraMode}
            auth={auth}
            authEnabled={authEnabled}
          />
          <Layout>
            <Sidebar
              sidebarOptionsChanged={sidebarOptionsChanged}
              posMap={posMap}
              date={mostRecentChange}
              auth={auth}
              authEnabled={authEnabled}
            />
            <Content>
              <Mirror
                showSegmentIds={showSegmentIds}
                showSpares={showSpares}
                posMap={posMap}
                segmentMap={segmentMap}
                mostRecentChange={mostRecentChange}
                viewMode={viewMode}
                auth={auth}
                authEnabled={authEnabled}
              />
            </Content>
            <Legend viewMode={viewMode} segmentMap={segmentMap}/>
          </Layout>
        </Layout>
      </appContext.Provider>
    )
}
export default App
