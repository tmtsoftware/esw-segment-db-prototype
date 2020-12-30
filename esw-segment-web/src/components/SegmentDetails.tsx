import React, {useEffect, useState} from 'react'
import {JiraSegmentData, SegmentData, SegmentToM1Pos} from './SegmentData'
import {PositionHistory} from './PositionHistory'
import {Button, DatePicker, Divider, Drawer, Form, Select, Typography} from "antd"
import moment from 'moment'

const {Option} = Select;

/**
 * Segment-id, pos (A1 to F82), date installed
 */
type SegmentDetailsProps = {
  id?: string
  pos: string
  date?: number
  open: boolean,
  closeDialog: () => void
  updateDisplay: () => void
  viewMode: React.Key
  segmentData: JiraSegmentData
}

/**
 * Displays details about the selected segment
 *
 * @param id segment id
 * @param pos A1 to F82
 * @param date date the segment was installed
 * @param open true if drawer is open
 * @param closeDialog function to call to close the drawer
 * @param updateDisplay function to update the display after a DB change
 * @param viewMode selected view mode from sidebar
 * @param segmentData segment data from JIRA
 * @constructor
 */
export const SegmentDetails = ({
                                 id,
                                 pos,
                                 date,
                                 open,
                                 closeDialog,
                                 updateDisplay,
                                 viewMode,
                                 segmentData
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
  const [changed, setChanged] = useState(1)

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
  }, [changed, open])

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
        <Select value={selectedSegmentId} onChange={changeSegmentId}>
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
    // XXX TODO FIXME: Display current value again after edit/cancel
    return (
      <Form.Item name="date-picker" label={helpText}>
        <DatePicker
          // format={"YYYY-MM-DD"}
          format={"ddd ll"}
          showToday={true}
          onChange={handleDateChange}
          value={moment(selectedDate)}
        />
      </Form.Item>
    )
  }

  const [form] = Form.useForm();

  function cancel(): void {
    setSaveEnabled(false)
    setSelectedDate(date ? new Date(date) : new Date())
    setSelectedSegmentId(id || emptyId)
    closeDialog()
    form.resetFields()
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
      headers: {'Content-Type': 'application/json'},
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
          setSaveEnabled(false)
          // closeDialog()
          updateSelectedDate()
          setChanged(changed + 1)
        }
      })
  }

  const {Title} = Typography;

  const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
  };

  function installedForm(): JSX.Element {
    return (
      <Form
        form={form}
        initialValues={{
          "date-picker": moment(selectedDate)
        }}
        size={'small'}
        {...layout}>
        {segmentIdSelector()}
        {datePicker()}
        <Divider/>
        {plannedForm(false)}
        <Divider/>
        <div>
          <Title level={5}>
            History
          </Title>
          <PositionHistory pos={pos} changed={changed}/>
        </div>
      </Form>
    )
  }

  function plannedForm(includeSegmentId: boolean): JSX.Element {
    const sector = segmentData.position[0]
    const sectorMsg = sector == 'G' ? ' (Spare)' : ''
    return (
      <Form
        form={form}
        size={'small'}
        {...layout}>
        {includeSegmentId && (
          <Form.Item label="Segment ID">
            {segmentData.segmentId}
          </Form.Item>
        )}
        <Form.Item label="JIRA Task">
          <a href={segmentData.jiraUri} target="_blank" rel="noopener noreferrer">
            {segmentData.jiraKey}
          </a>
        </Form.Item>
        <Form.Item label="Sector">
          {sector} {sectorMsg}
        </Form.Item>
        <Form.Item label="Part Number">
          {segmentData.partNumber}
        </Form.Item>
        <Form.Item label="Partner Blank Allocation">
          {segmentData.originalPartnerBlankAllocation}
        </Form.Item>
        <Form.Item label="Item Location">
          {segmentData.itemLocation}
        </Form.Item>
        <Form.Item label="Risk of Loss">
          {segmentData.riskOfLoss}
        </Form.Item>
        <Form.Item label="Components">
          {segmentData.components}
        </Form.Item>
        <Form.Item label="Status">
          {segmentData.status}
        </Form.Item>
        <Form.Item label="Work Packages">
          {segmentData.workPackages}
        </Form.Item>
        <Form.Item label="Acceptance Certificates">
          {segmentData.acceptanceCertificates}
        </Form.Item>
        <Form.Item label="Acceptance Date Blank">
          {segmentData.acceptanceDateBlank}
        </Form.Item>
        <Form.Item label="Shipping Authorizations">
          {segmentData.shippingAuthorizations}
        </Form.Item>
      </Form>
    )
  }

  function installedLayout(): JSX.Element {
    return (
      <Drawer
        title={`Segment ${pos}`}
        width={550}
        placement="right"
        closable={false}
        onClose={cancel}
        visible={open}
        bodyStyle={{paddingBottom: 80}}
        footer={
          <div
            style={{
              textAlign: 'right',
            }}
          >
            <Button onClick={cancel} style={{marginRight: 8}}>
              Cancel
            </Button>
            <Button onClick={saveChanges} disabled={!saveEnabled} type="primary">
              Submit
            </Button>
          </div>
        }
      >
        {installedForm()}
      </Drawer>
    )
  }

  function plannedLayout(): JSX.Element {
    return (
      <Drawer
        title={`Segment ${pos}`}
        width={550}
        placement="right"
        closable={false}
        onClose={cancel}
        visible={open}
        bodyStyle={{paddingBottom: 80}}
      >
        {plannedForm(true)}
      </Drawer>
    )
  }

  if (availableSegmentIds.length != 0)
    return viewMode == "installed" ? installedLayout() : plannedLayout()

  else return <div/>
}
