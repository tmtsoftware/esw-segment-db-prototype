import React from 'react'
import './Landing.css'
import {Mirror} from "../Mirror";
import {Topbar} from "../Topbar";

const Landing = (): JSX.Element => {
  return (
    <div className="App">
      <Topbar/>
      <Mirror/>
    </div>
  )
}

export default Landing
