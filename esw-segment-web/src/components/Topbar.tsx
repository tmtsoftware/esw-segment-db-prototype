import React, {useState} from "react";
import AppBar from "@material-ui/core/AppBar";
import {createStyles, TextField, Theme, Toolbar, Typography, withStyles} from "@material-ui/core";
import IconButton from "@material-ui/core/IconButton";
import MenuIcon from '@material-ui/icons/Menu';
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Checkbox from "@material-ui/core/Checkbox";
import {makeStyles, ThemeProvider} from "@material-ui/core/styles";
import {KeyboardDatePicker, MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import Grid from "@material-ui/core/Grid";
import createMuiTheme from "@material-ui/core/styles/createMuiTheme";

type TopbarProps = {
  updateDisplay: (showSegmentIds: boolean, refDate: Date) => void
}

export const Topbar = ({updateDisplay}: TopbarProps): JSX.Element => {

  const [showSegmentIds, setShowSegmentIds] = React.useState(false);
  const [selectedDate, setSelectedDate] = useState<Date>(new Date())

  const showSegmentIdsChanged = (event: React.ChangeEvent<HTMLInputElement>) => {
    setShowSegmentIds(event.target.checked);
    updateDisplay(event.target.checked, selectedDate)
  };

  const useStyles = makeStyles((theme: Theme) =>
    createStyles({
      checkbox: {
        color: 'inherit',
        paddingLeft: theme.spacing(7),
      },
      datePicker: {
        color: 'inherit',
        maxWidth: 160,
        paddingTop: 4,
        paddingLeft: 20
      },
      appBar: {
        background : '#2E3B55'
      },
      formControlLabel: {
        fontSize: 14,
        paddingTop: 4
      }
    }),
  );

  const classes = useStyles();

  const datePickerPalette = {
    palette: {
      primary: {  // primary color
        contrastText: "#FFFFFF",
        dark: "#0000FF",
        main: "#0000FF",
        light: "#0000FF"
      }
    }
  };
  const datePickerTheme = createMuiTheme(datePickerPalette);

  const CURRENT_THEME = {
    background: "#111d23",
    el1: "#1B262C",
    el2: "#263137",
    el3: "#2E3B55",
    text: "#210124",
    textInv: "#F0EDEE",
    main: "#019EE2",
    secondary: "#4DBBEB",
    mainShadow: "#13232B",
    danger: "#DB162F",
    warning: "",
    font1: `"Roboto Slab", "Times New Roman", serif`,
    font2: `"Roboto light"`
  };

  const CssTextField = withStyles({
    root: {
      //all
      "& .MuiIconButton-root": {
        color: CURRENT_THEME.textInv
      },
      // filled
      "& .MuiFilledInput-underline": {
        "&:before": {
          borderBottomColor: "transparent"
        },
        "&:after": {
          borderBottomColor: "transparent"
        }
      },
      "& .MuiFilledInput-input": {
        color: CURRENT_THEME.textInv,
        paddingBottom: 22,
      },
      "& .MuiFilledInput-root": {
        borderRadius: "20px 20px 20px 20px",
        backgroundColor: CURRENT_THEME.el3,
        "&.Mui-focused": {
          borderColor: "#1ab5e1",
          backgroundColor: CURRENT_THEME.el2,
          boxShadow: `0px 0px 0px 0px ${CURRENT_THEME.main}, 0px 0px 0px 0px ${CURRENT_THEME.main}, 0px 0px 0px 0px ${CURRENT_THEME.main}`
        }
      },

      "& .MuiInputLabel-formControl": {
        color: CURRENT_THEME.textInv
      }
    }
  })(TextField);


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
      // label="Show Segment IDs"
      label={<Typography className={classes.formControlLabel}>Show Segment IDs</Typography>}
    />
  }

  const handleDateChange = (newDate: Date | null) => {
    setSelectedDate(newDate || new Date())
  }

  function datePicker(): JSX.Element {
    return <ThemeProvider theme={datePickerTheme}>
      <MuiPickersUtilsProvider utils={DateFnsUtils}>
        <Grid container>
          <KeyboardDatePicker
            className={classes.datePicker}
            disableToolbar
            inputVariant="filled"
            variant="inline"
            size="small"
            helperText=""
            // label="Date"
            TextFieldComponent={CssTextField}
            format="MM/dd/yyyy"
            id="date-picker-inline"
            value={selectedDate}
            onChange={handleDateChange}
            KeyboardButtonProps={{
              'aria-label': 'change date',
            }}
          />
        </Grid>
      </MuiPickersUtilsProvider>
    </ThemeProvider>
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
        <div>{datePicker()}</div>
      </Toolbar>
    </AppBar>
  );

}
