import React, {useEffect, useState} from 'react'
import './App.css'
import {Topbar} from './components/Topbar'
import {Mirror} from './components/Mirror'
import {JiraSegmentData, SegmentData, SegmentToM1Pos} from './components/SegmentData'
import {Layout} from "antd"
import 'antd/dist/antd.css'
import {Sidebar} from "./components/Sidebar";
import {Legend} from "./components/Legend";

const {Content} = Layout;

const App = (): JSX.Element => {
  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false)
  const [showSpares, setShowSpares] = useState<boolean>(false)
  const [posMap, setPosMap] = useState<Map<string, SegmentToM1Pos>>(new Map())
  const [segmentMap, setSegmentMap] = useState<Map<string, JiraSegmentData>>(new Map())
  const [mostRecentChange, setMostRecentChange] = useState<number>(0)
  const [viewMode, setViewMode] = useState<React.Key>("installed")
  const [jiraMode, setJiraMode] = useState<boolean>(false)

  async function fetchData(refDate: Date) {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(refDate.getTime())
    }

    const [
      mostRecentChangeResponse,
      positionsOnDateResponse,
      segmentDataResponse
    ] = await Promise.all([
      fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions),
      fetch(`${SegmentData.baseUri}/positionsOnDate`, requestOptions),
      fetch(`${SegmentData.baseUri}/segmentData`),
    ])

    const mostRecentChange: number = await mostRecentChangeResponse.json()
    const positionsOnDate: Array<SegmentToM1Pos> = await positionsOnDateResponse.json()
    const segmentData: Array<JiraSegmentData> = await segmentDataResponse.json()

    return {
      mostRecentChange,
      positionsOnDate,
      segmentData
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

  // async function fetchPlannedData() {
  //   const positionsResponse = await fetch(`${SegmentData.baseUri}/plannedPositions`)
  //   const positions: Array<SegmentToM1Pos> = await positionsResponse.json()
  //   return {
  //     positions
  //   }
  // }

  function updateDataNow(refDate: Date) {
    if (!jiraMode) {
      fetchData(refDate).then(({mostRecentChange, positionsOnDate, segmentData}) => {
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
      body: JSON.stringify(new Date().getTime())
    }
    fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        updateDataNow(date)
      })
  }

  function updateDisplay(refDate: Date) {
    updateDataNow(refDate)
  }

  useEffect(() => {
    updateData()
  }, [jiraMode])

  function sidebarOptionsChanged(viewMode: string | number, showSegmentIds: boolean, showSpares: boolean) {
    setJiraMode(viewMode != 'installed')
    setViewMode(viewMode)
    setShowSegmentIds(showSegmentIds)
    setShowSpares(showSpares)
  }

  if (mostRecentChange == 0) return <div/>
  else
    return (
      <Layout className='App'>
        <Topbar
          mostRecentChange={new Date(mostRecentChange)}
          updateDisplay={updateDisplay}
          jiraMode={jiraMode}
        />
        <Layout>
          <Sidebar
            segmentMapSize={segmentMap.size}
            sidebarOptionsChanged={sidebarOptionsChanged}
            posMap={posMap}
            date={new Date(mostRecentChange)}
            updateDisplay={updateData}
          />
          <Content>
            <Mirror
              showSegmentIds={showSegmentIds}
              showSpares={showSpares}
              posMap={posMap}
              segmentMap={segmentMap}
              mostRecentChange={mostRecentChange}
              updateDisplay={updateData}
              viewMode={viewMode}
            />
          </Content>
          <Legend viewMode={viewMode} segmentMap={segmentMap}/>
        </Layout>
      </Layout>
    )
}
export default App
