import {createContext, useContext} from "react"

// Application context: Holds values and functions that are shared by different components in the app

type AppContextState = {
  // Reference date, the date for the segments being displayed
  refDate: Date

  // Sets the reference date
  setRefDate: (value: Date) => void

  // Signals to update the display after a change in the database
  updateDisplay: () => {}
}

export const appContextDefaultValue = {
  refDate: new Date(),
  setRefDate: (_: Date) => {},
  updateDisplay: () => {}
}

export const appContext = createContext(appContextDefaultValue)
export const useAppContext = () => useContext(appContext)

