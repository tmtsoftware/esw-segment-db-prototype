import React, {useEffect, useState} from 'react'
import 'react-responsive-modal/styles.css'
import {SegmentData, SegmentToM1Pos} from "./SegmentData"
import InputLabel from '@material-ui/core/InputLabel'
import MenuItem from '@material-ui/core/MenuItem'
import FormControl from '@material-ui/core/FormControl'
import FormHelperText from '@material-ui/core/FormHelperText'
import Select from '@material-ui/core/Select'
import {createStyles, makeStyles, Theme} from '@material-ui/core/styles'
import {MuiPickersUtilsProvider, KeyboardDatePicker} from '@material-ui/pickers'
import Grid from '@material-ui/core/Grid'
import DateFnsUtils from '@date-io/date-fns'

/**
 * Segment-id, pos (A1 to F82), date installed
 */
type SegmentDetailsProps = {
  id?: string,
  pos: string,
  date?: number,
  updateDisplay: () => void
}

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    formControl: {
      margin: theme.spacing(1),
      minWidth: 120,
    },
    selectEmpty: {
      marginTop: theme.spacing(2),
    },
  }),
)

/**
 * Displays details about the selected segment
 *
 * @param id segment id
 * @param pos A1 to F82
 * @param date date the segment was installed
 * @param updateDisplay function to update the display after a DB change
 * @constructor
 */
export const SegmentDetails = ({id, pos, date, updateDisplay}: SegmentDetailsProps): JSX.Element => {

  const emptyId = "empty"
  const [availableSegmentIds, setAvailableSegmentIds] = useState<Array<string>>([])
  const [selectedSegmentId, setSelectedSegmentId] = useState(id ? id : emptyId)
  const [errorMessage, setErrorMessage] = useState("")
  const [selectedDate, setSelectedDate] = useState<Date>(date ? new Date(date) : new Date())

  const classes = useStyles()

  // Gets the list of available segment ids for this position
  function update() {
    fetch(`${SegmentData.baseUri}/availableSegmentIdsForPos/${pos}`)
      .then(response => response.json())
      .then(data => {
        const ids: Array<string> = id ? [...data, id, emptyId] : [...data, emptyId]
        const uniqueIds = [...new Set(ids)]
        setAvailableSegmentIds(uniqueIds)
      })
  }

  useEffect(() => {
    update()
  }, [])

  // Called when a new segment id is selected from the menu: Update the DB with the new id
  const changeSegmentId = (event: React.ChangeEvent<{ value: unknown }>) => {
    const selectedId = (event.target.value as string).trim()
    const newId = selectedId == emptyId ? undefined : selectedId
    setSelectedSegmentId(selectedId)
    const today = new Date().getTime()
    // XXX TODO: Get from datepicker
    const segmentToM1Pos: SegmentToM1Pos = {date: today, maybeId: newId, position: pos}
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(segmentToM1Pos)
    }
    fetch(`${SegmentData.baseUri}/setPosition`, requestOptions)
      .then(response => response.status)
      .then(status => {
        setErrorMessage(status == 200 ? "" : "Error: Failed to update the database")
        updateDisplay()
      })
  }

  function segmentIdSelector(): JSX.Element {
    return <div>
      <FormControl className={classes.formControl}>
        <InputLabel>Segment ID</InputLabel>
        <Select
          value={selectedSegmentId}
          onChange={changeSegmentId}
        >
          {availableSegmentIds.map(segId => {
            return <MenuItem key={segId} value={segId}>{segId}</MenuItem>
          })
          }
        </Select>
      </FormControl>
      <FormHelperText>{errorMessage}</FormHelperText>
    </div>
  }

  const handleDateChange = (date: Date | null) => {
    setSelectedDate(date || new Date())
  }

  function datePicker(): JSX.Element {
    const helpText = id ? `Installed on` : "Select date installed"
    return <MuiPickersUtilsProvider utils={DateFnsUtils}>
      <Grid container justify="space-around">
        <KeyboardDatePicker
          disableToolbar
          variant="inline"
          format="MMM dd, yyyy"
          margin="normal"
          id="date-picker-inline"
          label={helpText}
          value={selectedDate}
          onChange={handleDateChange}
          KeyboardButtonProps={{
            'aria-label': 'change date',
          }}
        />
      </Grid>
    </MuiPickersUtilsProvider>
  }

  if (availableSegmentIds.length != 0)
    return (
      <div>
        <h3>Segment {pos}</h3>
        {segmentIdSelector()}
        {datePicker()}
      </div>
    )
  else return (
    <div>
    </div>
  )
}

