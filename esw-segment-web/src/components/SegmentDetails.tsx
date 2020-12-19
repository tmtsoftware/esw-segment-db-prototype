import React, { useEffect, useState } from 'react'
import { SegmentData, SegmentToM1Pos } from './SegmentData'
import { PositionHistory } from './PositionHistory'
import {Button, DatePicker, Form, Select, Typography} from "antd"
import moment from 'moment'

const { Option } = Select;

/**
 * Segment-id, pos (A1 to F82), date installed
 */
type SegmentDetailsProps = {
  id?: string
  pos: string
  date?: number
  open: boolean
  closeDialog: () => void
  updateDisplay: () => void
}

// const useStyles = makeStyles((theme: Theme) =>
//   createStyles({
//     formControl: {
//       // margin: theme.spacing(1),
//       marginLeft: 1,
//       paddingBottom: 20,
//       minWidth: 120
//     },
//     selectEmpty: {
//       marginTop: theme.spacing(2)
//     },
//     history: {
//       marginBottom: 3
//     },
//     datePicker: {
//       // maxWidth: 150
//       paddingBottom: 20
//     }
//   })
// )

// const styles = (theme: Theme) =>
//   createStyles({
//     root: {
//       margin: 0,
//       padding: theme.spacing(2)
//     },
//     closeButton: {
//       position: 'absolute',
//       right: theme.spacing(1),
//       top: theme.spacing(1),
//       color: theme.palette.grey[500]
//     }
//   })

// export interface DialogTitleProps extends WithStyles<typeof styles> {
//   id: string
//   children: React.ReactNode
//   onClose: () => void
// }

// const DialogTitle = withStyles(styles)((props: DialogTitleProps) => {
//   const { children, classes, onClose, ...other } = props
//   return (
//     <MuiDialogTitle disableTypography className={classes.root} {...other}>
//       <Typography variant='h6'>{children}</Typography>
//       {onClose ? (
//         <IconButton
//           aria-label='close'
//           className={classes.closeButton}
//           onClick={onClose}>
//           <CloseIcon />
//         </IconButton>
//       ) : null}
//     </MuiDialogTitle>
//   )
// })

// const DialogContent = withStyles((theme: Theme) => ({
//   root: {
//     padding: theme.spacing(2)
//   }
// }))(MuiDialogContent)

// const DialogActions = withStyles((theme: Theme) => ({
//   root: {
//     margin: 0,
//     padding: theme.spacing(1)
//   }
// }))(MuiDialogActions)

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
export const SegmentDetails = ({
  id,
  pos,
  date,
  open,
  closeDialog,
  updateDisplay
}: SegmentDetailsProps): JSX.Element => {
  const emptyId = 'empty'
  const [availableSegmentIds, setAvailableSegmentIds] = useState<Array<string>>(
    []
  )
  const [selectedSegmentId, setSelectedSegmentId] = useState(id || emptyId)
  const [errorMessage, setErrorMessage] = useState('')
  const [selectedDate, setSelectedDate] = useState<Date>(
    date ? new Date(date) : new Date()
  )
  const [saveEnabled, setSaveEnabled] = useState(false)

  // const classes = useStyles()
  console.log(`XXX open = ${open}`)

  // Gets the list of available segment ids for this position
  function updateAvailableSegmentIds() {
    fetch(`${SegmentData.baseUri}/availableSegmentIdsForPos/${pos}`)
      .then((response) => response.json())
      .then((data) => {
        const ids: Array<string> = id
          ? [...data, id, emptyId]
          : [...data, emptyId]
        const uniqueIds = [...new Set(ids)]
        setAvailableSegmentIds(uniqueIds)
      })
  }

  useEffect(() => {
    updateAvailableSegmentIds()
  }, [])

  // Gets the date of the most recent segment change
  function updateSelectedDate() {
    fetch(`${SegmentData.baseUri}/currentSegmentAtPosition/${pos}`)
      .then((response) => response.json())
      .then((data) => {
        const segmentToM1Pos: SegmentToM1Pos = data
        setSelectedDate(new Date(segmentToM1Pos.date))
      })
  }

  // Called when a new segment id is selected from the menu: Update the DB with the new id
  const changeSegmentId = (selectedId: string) => {
    setSelectedSegmentId(selectedId)
    // Default to current date, since the segment id changed
    if (date == selectedDate.getTime()) {
      setSelectedDate(new Date())
    }
    setSaveEnabled(true)
  }

  // A menu of available segment ids for this position
  function segmentIdSelector(): JSX.Element {
    const validateStatus = errorMessage.length == 0 ? "success" : "error"
    return (
      <Form.Item
        label="Segment ID"
        validateStatus={validateStatus}
        help={errorMessage}
      >
        <Select defaultValue={selectedSegmentId} onChange={changeSegmentId}>
          {availableSegmentIds.map((segId) => {
            return (
              <Option key={segId} value={segId}>
                {segId}
              </Option>
            )
          })}
        </Select>
      </Form.Item>
    )
  }

  const handleDateChange = (value: moment.Moment | null) => {
    const newDate = value ? value.toDate() : new Date()
    setSelectedDate(newDate)
    setSaveEnabled(true)
  }

  // Display/edit the installation date
  function datePicker(): JSX.Element {
    const helpText = id ? `Installed on` : 'Date installed'
    return (
      <Form.Item name="date-picker" label={helpText}>
        <DatePicker
          onChange={handleDateChange}
        />
      </Form.Item>


      // <MuiPickersUtilsProvider utils={DateFnsUtils}>
      //   <Grid container>
      //     <KeyboardDatePicker
      //       className={classes.datePicker}
      //       disableToolbar
      //       variant='inline'
      //       format='MM/dd/yyyy'
      //       id='date-picker-inline'
      //       label={helpText}
      //       value={selectedDate}
      //       onChange={handleDateChange}
      //       KeyboardButtonProps={{
      //         'aria-label': 'change date'
      //       }}
      //     />
      //   </Grid>
      // </MuiPickersUtilsProvider>
    )
  }

  function saveChanges() {
    const date = selectedDate.getTime()
    const maybeId = selectedSegmentId == emptyId ? undefined : selectedSegmentId
    const segmentToM1Pos: SegmentToM1Pos = {
      date: date,
      maybeId: maybeId,
      position: pos
    }
    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(segmentToM1Pos)
    }
    fetch(`${SegmentData.baseUri}/setPosition`, requestOptions)
      .then((response) => response.status)
      .then((status) => {
        setErrorMessage(
          status == 200 ? '' : 'Error: Failed to update the database'
        )
        updateDisplay()
        if (status == 200) {
          closeDialog()
          updateSelectedDate()
        }
      })
  }

  // function cancelChanges() {
  //   setSelectedSegmentId(id || emptyId)
  //   setSelectedDate(date ? new Date(date) : new Date())
  //   closeDialog()
  // }

  const {Title} = Typography;

  const layout = {
    labelCol: { span: 8 },
    wrapperCol: { span: 16 },
  };
  const tailLayout = {
    wrapperCol: { offset: 8, span: 16 },
  };

  if (availableSegmentIds.length != 0)
    return (
      <Form {...layout}>
        <Title level={5}>
          Segment {pos}
        </Title>
          {segmentIdSelector()}
          {datePicker()}
          <div>
            <Title level={5}>
              History
            </Title>
            <PositionHistory pos={pos} />
          </div>
        <Form.Item {...tailLayout}>
          <Button
            type="primary"
            htmlType="submit"
            onClick={saveChanges}
            disabled={!saveEnabled}>
            Save changes
          </Button>
        </Form.Item>

      </Form>
    )
  else return <div/>
}
