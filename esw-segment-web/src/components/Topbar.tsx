import React, {useState} from "react"
import AppBar from "@material-ui/core/AppBar"
import {createStyles, Theme, Toolbar, Typography} from "@material-ui/core"
import IconButton from "@material-ui/core/IconButton"
import MenuIcon from '@material-ui/icons/Menu'
import FormControlLabel from "@material-ui/core/FormControlLabel"
import Checkbox from "@material-ui/core/Checkbox"
import {makeStyles} from "@material-ui/core/styles"
import {TopbarDatePicker} from "./TopbarDatePicker"

type TopbarProps = {
  updateDisplay: (showSegmentIds: boolean, refDate: Date) => void
}

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    checkbox: {
      color: 'inherit',
      paddingLeft: theme.spacing(7),
    },
    appBar: {
      background : '#2E3B55'
    },
    formControlLabel: {
      fontSize: 14,
      paddingTop: 4
    }
  }),
)

export const Topbar = ({updateDisplay}: TopbarProps): JSX.Element => {

  const [showSegmentIds, setShowSegmentIds] = React.useState(false)
  const [selectedDate, setSelectedDate] = useState<Date>(new Date())

  const showSegmentIdsChanged = (event: React.ChangeEvent<HTMLInputElement>) => {
    setShowSegmentIds(event.target.checked)
    updateDisplay(event.target.checked, selectedDate)
  }

  const refDateChanged = (date: Date) => {
    setSelectedDate(date)
    updateDisplay(showSegmentIds, date)
  }

  const classes = useStyles()

  function showSegmentIdsCheckbox(): JSX.Element {
    return <FormControlLabel
      control={
        <Checkbox
          title="Display the last part of the segment id instead of the segment position"
          size="small"
          checked={showSegmentIds}
          onChange={showSegmentIdsChanged}
          classes={{
            root: classes.checkbox,
          }}
          inputProps={{'aria-label': 'primary checkbox'}}
        />
      }
      label={<Typography className={classes.formControlLabel}>Show Segment IDs</Typography>}
    />
  }

  return (
    <AppBar position="static" className={classes.appBar}>
      <Toolbar>
        <IconButton
          edge="start"
          color="inherit"
          aria-label="open drawer"
        >
          <MenuIcon/>
        </IconButton>
        <Typography variant="h6" noWrap>
          TMT Segment Database
        </Typography>
        <div>{showSegmentIdsCheckbox()}</div>
        <div>
          <TopbarDatePicker updateDisplay={refDateChanged}/>
        </div>
      </Toolbar>
    </AppBar>
  )

}
