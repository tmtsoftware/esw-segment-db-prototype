import React from "react";
import AppBar from "@material-ui/core/AppBar";
import {Toolbar, Typography} from "@material-ui/core";
import IconButton from "@material-ui/core/IconButton";
import MenuIcon from '@material-ui/icons/Menu';

export const Topbar = (): JSX.Element => {
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
      </Toolbar>
    </AppBar>
  );

}
