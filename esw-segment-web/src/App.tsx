import React, {useState} from 'react'
import './App.css'
import {Topbar} from "./components/Topbar";
import {Mirror} from "./components/Mirror";

const App = (): JSX.Element => {

  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false);

  function updateDisplay(showSegmentIds: boolean) {
    setShowSegmentIds(showSegmentIds)
  }

  return (
    <div className="App">
      <Topbar updateDisplay={updateDisplay}/>
      <Mirror showSegmentIds={showSegmentIds}/>
    </div>
  )
}
export default App
