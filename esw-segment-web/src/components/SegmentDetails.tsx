import React, {useEffect, useState} from 'react'
import 'react-responsive-modal/styles.css';
import {SegmentData} from "./SegmentData";
import InputLabel from '@material-ui/core/InputLabel';
import MenuItem from '@material-ui/core/MenuItem';
import FormControl from '@material-ui/core/FormControl';
import FormHelperText from '@material-ui/core/FormHelperText';
import Select from '@material-ui/core/Select';
import {createStyles, makeStyles, Theme} from '@material-ui/core/styles';


type SegmentDetailsProps = { id: string, pos: string, date: string }

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
);

export const SegmentDetails = ({id, pos, date}: SegmentDetailsProps): JSX.Element => {

  const [availableSegmentIds, setAvailableSegmentIds] = useState<Array<string>>([]);
  const [segmentId, setSegmentId] = React.useState(id ? id : "empty");

  const classes = useStyles();

  function update() {
    useEffect(() => {
      fetch(`${SegmentData.baseUri}/availableSegmentIdsForPos/${pos}`)
        .then(response => response.json())
        .then(data => {
          const list = id ? [...data, id, "empty"] : [...data, "empty"];
          setAvailableSegmentIds(list)
        });
      // empty dependency array means this effect will only run once
    }, []);
  }

  update()

  const changeSegmentId = (event: React.ChangeEvent<{ value: unknown }>) => {
    const newId = event.target.value as string
    setSegmentId(newId)
    console.log(`XXX changeSegmentId to ${newId}`)
  };

  const helpText = id ? `Installed on: ${date}` : "Empty: Select new ID"

  if (availableSegmentIds.length != 0)
    return (
      <div>
        <strong>Segment {pos}</strong>
        <div>
          <FormControl className={classes.formControl}>
            <InputLabel>Segment ID</InputLabel>
            <Select
              value={segmentId}
              onChange={changeSegmentId}
            >
              {availableSegmentIds.map(segId => {
                return <MenuItem key={segId} value={segId}>{segId}</MenuItem>
              })
              }
            </Select>
          </FormControl>
          <FormHelperText>{helpText}</FormHelperText>
        </div>
      </div>
    )
  else return (
    <div>
      <strong>Segment {pos}</strong>
      <p><em>Currently empty</em></p>
    </div>
  )
}

