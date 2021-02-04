import React from 'react'
import {render} from 'react-dom'
import './index.css'
import App from './App'
import {AuthContextProvider} from "@tmtsoftware/esw-ts";
import {AppConfig} from "./config/AppConfig";

render(
  <AuthContextProvider config={AppConfig}>
    <App/>
  </AuthContextProvider>,
  document.getElementById('root'))
