import React, { useState } from 'react'
import { IconButton, Typography } from '@material-ui/core'
import { makeStyles } from '@material-ui/core/styles'
import {
  ChevronLeftRounded,
  ChevronRightRounded,
  TodayRounded
} from '@material-ui/icons'
import { SegmentData } from './SegmentData'

type TopbarDateChooserProps = {
  mostRecentChange: Date
  updateDisplay: (refDate: Date) => void
}

const useStyles = makeStyles({
  icons: {
    paddingLeft: 5,
    paddingRight: 5,
    paddingBottom: 0
  },
  formControlLabel: {
    fontSize: 14,
    paddingLeft: 15,
    paddingTop: 2,
    paddingBottom: 2
  }
})

export const TopbarDateChooser = ({
  mostRecentChange,
  updateDisplay
}: TopbarDateChooserProps): JSX.Element => {
  const [selectedDate, setSelectedDate] = useState<Date>(mostRecentChange)
  // const [disabled, setDisabled] = useState<boolean>(false)
  const classes = useStyles()

  function nextDate() {
    // setDisabled(true)
    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(selectedDate.getTime())
    }
    fetch(`${SegmentData.baseUri}/nextChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        setSelectedDate(date)
        updateDisplay(date)
        // setDisabled(false)
      })
  }

  function prevDate() {
    // setDisabled(true)
    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(selectedDate.getTime())
    }
    fetch(`${SegmentData.baseUri}/prevChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        setSelectedDate(date)
        updateDisplay(date)
        // setDisabled(false)
      })
  }

  function today() {
    // setDisabled(true)
    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(new Date().getTime())
    }
    fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        setSelectedDate(date)
        updateDisplay(date)
        // setDisabled(false)
      })
  }

  return (
    <div>
      <IconButton
        color='inherit'
        className={classes.icons}
        onClick={() => prevDate()}
        // disabled={disabled}
        title='Go back to the previous segment change'>
        <ChevronLeftRounded />
      </IconButton>
      <IconButton
        color='inherit'
        className={classes.icons}
        onClick={() => today()}
        // disabled={disabled}
        title='Display changes up to the current date (default)'>
        <TodayRounded />
      </IconButton>
      <IconButton
        color='inherit'
        className={classes.icons}
        onClick={() => nextDate()}
        // disabled={disabled}
        title='Go forward to the next segment change'>
        <ChevronRightRounded />
      </IconButton>
      {/*// XXX TODO FIXME: Doesn't display most recent date if segment changed and this item was on previous most recent date */}
      <Typography className={classes.formControlLabel}>
        {selectedDate.toLocaleDateString('en-US')}
      </Typography>
    </div>
  )
}
