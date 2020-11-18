import React, {useState} from "react";
import {createStyles, TextField, withStyles} from "@material-ui/core";
import {makeStyles, ThemeProvider} from "@material-ui/core/styles";
import {KeyboardDatePicker, MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import Grid from "@material-ui/core/Grid";
import createMuiTheme from "@material-ui/core/styles/createMuiTheme";

type TopbarDatePickerProps = {
  updateDisplay: (refDate: Date) => void
}

const useStyles = makeStyles(() =>
  createStyles({
    datePicker: {
      color: 'inherit',
      maxWidth: 160,
      paddingTop: 4,
      paddingLeft: 20
    }
  }),
);

export const TopbarDatePicker = ({updateDisplay}: TopbarDatePickerProps): JSX.Element => {

  const [selectedDate, setSelectedDate] = useState<Date>(new Date())

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


  const handleDateChange = (newDate: Date | null) => {
    const d = newDate || new Date()
    setSelectedDate(d)
    updateDisplay(d)
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

  return datePicker()
}
