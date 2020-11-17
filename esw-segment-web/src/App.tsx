import React, {useState} from 'react'
import './App.css'
import {Topbar} from "./components/Topbar";
import {Mirror} from "./components/Mirror";

const App = (): JSX.Element => {

  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false);
  const [selectedDate, setSelectedDate] = useState<Date>(new Date())

  function updateDisplay(showSegmentIds: boolean, refDate: Date) {
    setShowSegmentIds(showSegmentIds)
    setSelectedDate(refDate)
  }

  return (
    <div className="App">
      <Topbar updateDisplay={updateDisplay}/>
      <Mirror showSegmentIds={showSegmentIds} refDate={selectedDate}/>
    </div>
  )
}
export default App
