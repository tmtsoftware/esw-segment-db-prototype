import React, {useEffect, useState} from 'react'
import {SegmentData, SegmentToM1Pos} from "./SegmentData"
import InputLabel from '@material-ui/core/InputLabel'
import MenuItem from '@material-ui/core/MenuItem'
import FormControl from '@material-ui/core/FormControl'
import FormHelperText from '@material-ui/core/FormHelperText'
import Select from '@material-ui/core/Select'
import {MuiPickersUtilsProvider, KeyboardDatePicker} from '@material-ui/pickers'
import Grid from '@material-ui/core/Grid'
import DateFnsUtils from '@date-io/date-fns'
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialogActions from '@material-ui/core/DialogActions';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';
import {createStyles, makeStyles, Theme, withStyles, WithStyles} from '@material-ui/core/styles';
import Typography from '@material-ui/core/Typography';

/**
 * Segment-id, pos (A1 to F82), date installed
 */
type SegmentDetailsProps = {
  id?: string,
  pos: string,
  date?: number,
  open: boolean,
  closeDialog: () => void,
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

const styles = (theme: Theme) =>
  createStyles({
    root: {
      margin: 0,
      padding: theme.spacing(2),
    },
    closeButton: {
      position: 'absolute',
      right: theme.spacing(1),
      top: theme.spacing(1),
      color: theme.palette.grey[500],
    },
  });

export interface DialogTitleProps extends WithStyles<typeof styles> {
  id: string;
  children: React.ReactNode;
  onClose: () => void;
}

const DialogTitle = withStyles(styles)((props: DialogTitleProps) => {
  const { children, classes, onClose, ...other } = props;
  return (
    <MuiDialogTitle disableTypography className={classes.root} {...other}>
      <Typography variant="h6">{children}</Typography>
      {onClose ? (
        <IconButton aria-label="close" className={classes.closeButton} onClick={onClose}>
          <CloseIcon />
        </IconButton>
      ) : null}
    </MuiDialogTitle>
  );
});

const DialogContent = withStyles((theme: Theme) => ({
  root: {
    padding: theme.spacing(2),
  },
}))(MuiDialogContent);

const DialogActions = withStyles((theme: Theme) => ({
  root: {
    margin: 0,
    padding: theme.spacing(1),
  },
}))(MuiDialogActions);


/**
 * Displays details about the selected segment
 *
 * @param id segment id
 * @param pos A1 to F82
 * @param date date the segment was installed
 * @param open true if dialog is open
 * @param closeDialog function to close the dialog
 * @param updateDisplay function to update the display after a DB change
 * @constructor
 */
export const SegmentDetails = ({id, pos, date, open, closeDialog, updateDisplay}: SegmentDetailsProps): JSX.Element => {
  const emptyId = "empty"
  const [availableSegmentIds, setAvailableSegmentIds] = useState<Array<string>>([])
  const [selectedSegmentId, setSelectedSegmentId] = useState(id || emptyId)
  const [errorMessage, setErrorMessage] = useState("")
  const [selectedDate, setSelectedDate] = useState<Date>(date ? new Date(date) : new Date())
  const [saveEnabled, setSaveEnbled] = useState(false)

  const classes = useStyles()

  // Gets the list of available segment ids for this position
  function updateAvailableSegmentIds() {
    fetch(`${SegmentData.baseUri}/availableSegmentIdsForPos/${pos}`)
      .then(response => response.json())
      .then(data => {
        const ids: Array<string> = id ? [...data, id, emptyId] : [...data, emptyId]
        const uniqueIds = [...new Set(ids)]
        setAvailableSegmentIds(uniqueIds)
      })
  }

  useEffect(() => {
    updateAvailableSegmentIds()
  }, [])

  // Called when a new segment id is selected from the menu: Update the DB with the new id
  const changeSegmentId = (event: React.ChangeEvent<{ value: unknown }>) => {
    const selectedId = (event.target.value as string).trim()
    setSelectedSegmentId(selectedId)
    // Default to current date, since the segment id changed
    if (date == selectedDate.getTime()) {
      setSelectedDate(new Date())
    }
    setSaveEnbled(true)
  }

  // A menu of available segment ids for this position
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

  const handleDateChange = (newDate: Date | null) => {
    setSelectedDate(newDate || new Date())
    setSaveEnbled(true)
  }

  // Display/edit the installation date
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

  function saveChanges() {
    const date = selectedDate.getTime()
    const maybeId = selectedSegmentId == emptyId ? undefined : selectedSegmentId
    const segmentToM1Pos: SegmentToM1Pos = {date: date, maybeId: maybeId, position: pos}
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
        if (status == 200)
          closeDialog()
      })
  }

  function cancelChanges() {
    setSelectedSegmentId(id || emptyId)
    setSelectedDate(date ? new Date(date) : new Date())
    closeDialog()
  }

  if (availableSegmentIds.length != 0)
    return (
      <Dialog onClose={cancelChanges} aria-labelledby="customized-dialog-title" open={open}>
        <DialogTitle id="customized-dialog-title" onClose={cancelChanges}>
          Segment {pos}
        </DialogTitle>
        <DialogContent dividers>
            {segmentIdSelector()}
            {datePicker()}
        </DialogContent>
        <DialogActions>
          <Button autoFocus onClick={saveChanges} color="primary" disabled={!saveEnabled}>
            Save changes
          </Button>
        </DialogActions>
      </Dialog>
    )
  else return (
    <div>
    </div>
  )
}

