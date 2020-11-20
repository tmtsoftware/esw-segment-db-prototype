import React, { useEffect, useState } from 'react'
import './App.css'
import { Topbar } from './components/Topbar'
import { Mirror } from './components/Mirror'
import { SegmentData, SegmentToM1Pos } from './components/SegmentData'

const App = (): JSX.Element => {
  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false)
  const [posMap, setPosMap] = useState<Map<string, SegmentToM1Pos>>(new Map())
  const [mostRecentChange, setMostRecentChange] = useState<number>(0)

  async function fetchData(refDate: Date) {
    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
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

  function updateDataNow(refDate: Date) {
    fetchData(refDate).then(({ mostRecentChange, positionsOnDate }) => {
      const posMap: Map<string, SegmentToM1Pos> = positionsOnDate.reduce(
        (map, obj) => map.set(obj.position, obj),
        new Map()
      )
      setPosMap(posMap)
      setMostRecentChange(mostRecentChange)
    })
  }

  function updateData() {
    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(new Date().getTime())
    }
    fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        updateDataNow(date)
      })
  }

  function updateDisplay(showSegmentIds: boolean, refDate: Date) {
    setShowSegmentIds(showSegmentIds)
    updateDataNow(refDate)
  }

  useEffect(() => {
    updateData()
  }, [])

  if (mostRecentChange == 0) return <div />
  else
    return (
      <div className='App'>
        <Topbar
          mostRecentChange={new Date(mostRecentChange)}
          updateDisplay={updateDisplay}
        />
        <Mirror
          showSegmentIds={showSegmentIds}
          posMap={posMap}
          mostRecentChange={mostRecentChange}
          updateDisplay={updateData}
        />
      </div>
    )
}
export default App
