import React, {useEffect, useState} from 'react'
import './App.css'
import {Topbar} from './components/Topbar'
import {Mirror} from './components/Mirror'
import {SegmentData, SegmentToM1Pos} from './components/SegmentData'
import {Layout} from "antd"
import 'antd/dist/antd.css'
import {Sidebar} from "./components/Sidebar";

const {Content} = Layout;

const App = (): JSX.Element => {
  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false)
  const [showSpares, setShowSpares] = useState<boolean>(false)
  const [posMap, setPosMap] = useState<Map<string, SegmentToM1Pos>>(new Map())
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
      positionsOnDateResponse
    ] = await Promise.all([
      fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions),
      fetch(`${SegmentData.baseUri}/positionsOnDate`, requestOptions)
    ])

    const mostRecentChange: number = await mostRecentChangeResponse.json()
    const positionsOnDate: Array<SegmentToM1Pos> = await positionsOnDateResponse.json()

    return {
      mostRecentChange,
      positionsOnDate
    }
  }

  async function fetchPlannedData() {
    const positionsResponse = await fetch(`${SegmentData.baseUri}/plannedPositions`)
    const positions: Array<SegmentToM1Pos> = await positionsResponse.json()
    return {
      positions
    }
  }

  function updateDataNow(refDate: Date) {
    if (!jiraMode) {
      fetchData(refDate).then(({mostRecentChange, positionsOnDate}) => {
        const posMap: Map<string, SegmentToM1Pos> = positionsOnDate.reduce(
          (map, obj) => map.set(obj.position, obj),
          new Map()
        )
        setPosMap(posMap)
        setMostRecentChange(mostRecentChange)
      })
    } else {
      fetchPlannedData().then(({positions}) => {
        const posMap: Map<string, SegmentToM1Pos> = positions.reduce(
          (map, obj) => map.set(obj.position, obj),
          new Map()
        )
        setPosMap(posMap)
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

  function sidebarOptionsChanged(viewMode: string|number, showSegmentIds: boolean, showSpares: boolean) {
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
          <Sidebar sidebarOptionsChanged={sidebarOptionsChanged}/>
          <Content>
            <Mirror
              showSegmentIds={showSegmentIds}
              showSpares={showSpares}
              posMap={posMap}
              mostRecentChange={mostRecentChange}
              updateDisplay={updateData}
            />
          </Content>
        </Layout>
      </Layout>
    )
}
export default App
