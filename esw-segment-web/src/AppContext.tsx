import React, {createContext, useContext} from "react"
import {Auth} from "@tmtsoftware/esw-ts";

// Application context: Holds values and functions that are shared by different components in the app

export type AppContextState = {
  // Reference date, the date for the segments being displayed
  refDate: Date
  setRefDate: (value: Date) => void

  // Signals to update the display after a change in the database
  updateDisplay: () => void

  // View mode, set in the side bar from the menu
  viewMode: React.Key
  setViewMode: (value: React.Key) => void
  jiraMode: boolean

  // Optionally display segment ids instead of positions
  showSegmentIds: boolean
  setShowSegmentIds: (value: boolean) => void

  // Optionally show only the spare sector
  showSpares: boolean
  setShowSpares: (value: boolean) => void

  // Keycloak authorization
  auth: Auth | null

  // true if auth is enabled/required
  authEnabled: boolean
}

export const appContextDefaultValue: AppContextState = {
  refDate: new Date(),
  setRefDate: (_: Date) => {
  },
  updateDisplay: () => {
  },
  viewMode: "installed",
  setViewMode: (_: React.Key) => {
  },
  jiraMode: false,
  showSegmentIds: false,
  setShowSegmentIds: (_: boolean) => {
  },
  showSpares: false,
  setShowSpares: (_: boolean) => {
  },
  auth: null,
  authEnabled: false
}

export const appContext = createContext<AppContextState>(appContextDefaultValue)
export const useAppContext = () => useContext(appContext)

