import React, {useEffect, useState} from 'react'
import './App.css'
import {Topbar} from "./components/Topbar";
import {Mirror} from "./components/Mirror";
import {SegmentData, SegmentToM1Pos} from "./components/SegmentData";

const App = (): JSX.Element => {

  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false);
  const [selectedDate, setSelectedDate] = useState<Date>(new Date())
  const [posMap, setPosMap] = useState<Map<string, SegmentToM1Pos>>(new Map());
  const [mostRecentChange, setMostRecentChange] = useState<number>(0);

  async function fetchData() {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(selectedDate.getTime())
    }

    const [mostRecentChangeResponse, positionsOnDateResponse] = await Promise.all([
      fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions),
      fetch(`${SegmentData.baseUri}/positionsOnDate`, requestOptions)
    ]);

    const mostRecentChange: number = await mostRecentChangeResponse.json()
    const positionsOnDate: Array<SegmentToM1Pos> = await positionsOnDateResponse.json()

    return {
      mostRecentChange,
      positionsOnDate
    };
  }

  function updateData() {
    fetchData().then(({mostRecentChange, positionsOnDate}) => {
      const posMap: Map<string, SegmentToM1Pos> = positionsOnDate
        .reduce((map, obj) => map.set(obj.position, obj), new Map())
      setPosMap(posMap)
      setMostRecentChange(mostRecentChange)
      console.log(`XXX mostRecentChange = ${new Date(mostRecentChange)}`)
    });
  }

  function updateDisplay(showSegmentIds: boolean, refDate: Date) {
    setShowSegmentIds(showSegmentIds)
    setSelectedDate(refDate)
    updateData()
  }

  useEffect(() => {
    updateData()
  }, []);

  return (
    <div className="App">
      <Topbar updateDisplay={updateDisplay}/>
      <Mirror showSegmentIds={showSegmentIds} posMap={posMap} mostRecentChange={mostRecentChange} updateDisplay={updateData}/>
    </div>
  )
}
export default App
