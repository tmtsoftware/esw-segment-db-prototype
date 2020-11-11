import React from "react";
import AppBar from "@material-ui/core/AppBar";
import {createStyles, Theme, Toolbar, Typography} from "@material-ui/core";
import IconButton from "@material-ui/core/IconButton";
import MenuIcon from '@material-ui/icons/Menu';
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Checkbox from "@material-ui/core/Checkbox";
import {makeStyles} from "@material-ui/core/styles";

type TopbarProps = {
  updateDisplay: (showSegmentIds: boolean) => void
}

export const Topbar = ({updateDisplay}:TopbarProps): JSX.Element => {

  const [showSegmentIds, setShowSegmentIds] = React.useState(false);

  const showSegmentIdsChanged = (event: React.ChangeEvent<HTMLInputElement>) => {
    setShowSegmentIds(event.target.checked);
    updateDisplay(event.target.checked)
  };

  const useStyles = makeStyles((theme: Theme) =>
    createStyles({
      checkbox: {
        color: 'inherit',
        paddingLeft: theme.spacing(10),
      },
    }),
  );

  const classes = useStyles();

  return (
    <AppBar position="static">
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
        <FormControlLabel
          control={
            <Checkbox
              title="Display the last part of the segment id instead of the segment position"
              checked={showSegmentIds}
              onChange={showSegmentIdsChanged}
              classes={{
                root: classes.checkbox,
              }}
              inputProps={{'aria-label': 'primary checkbox'}}
            />
          }
          label="Show Segment IDs"
        />
      </Toolbar>
    </AppBar>
  );

}
