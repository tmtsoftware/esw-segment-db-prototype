import React from 'react'
import './App.css'
import {Topbar} from "./components/Topbar";
import {Mirror} from "./components/Mirror";

const App = (): JSX.Element => {
  return (
    <div className="App">
      <Topbar/>
      <Mirror/>
    </div>
  )
}
export default App
