import React, {useEffect, useState} from 'react'
import {JiraSegmentData, SegmentData, SegmentToM1Pos} from './SegmentData'
import {PositionHistory} from './PositionHistory'
import {Button, DatePicker, Divider, Drawer, Form, Select, Typography} from "antd"
import moment from 'moment'
import {format} from "date-fns";
import {Auth} from "@tmtsoftware/esw-ts";

const {Option} = Select;

/**
 * Segment-id, pos (A1 to F82), date installed
 */
type SegmentDetailsProps = {
  id?: string
  pos: string
  date?: Date
  open: boolean,
  closeDialog: () => void
  updateDisplay: () => void
  viewMode: React.Key
  segmentData: JiraSegmentData
  auth: Auth | null
  authEnabled: boolean
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
 * @param auth login authorization from Keycloak
 * @param authEnabled true if login authorization via Keycloak is enabled
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
                                 segmentData,
                                 auth,
                                 authEnabled
                               }: SegmentDetailsProps): JSX.Element => {
  const emptyId = 'empty'
  const [availableSegmentIds, setAvailableSegmentIds] = useState<Array<string>>(
    []
  )
  const [selectedSegmentId, setSelectedSegmentId] = useState(id || emptyId)
  const [errorMessage, setErrorMessage] = useState('')
  const [selectedDate, setSelectedDate] = useState<Date>(
    date ? date : new Date()
  )
  const [saveEnabled, setSaveEnabled] = useState(false)
  const [changed, setChanged] = useState(1)

  function isAuthenticated(): boolean {
    if (authEnabled) {
      if (auth)
        return (auth.isAuthenticated() || false)
      return false
    } else return true
  }

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
        setSelectedSegmentId(id || emptyId)
      })
  }

  useEffect(() => {
    if (open)
      updateAvailableSegmentIds()
  }, [changed, open, id, date])

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
    if ((date ? date : new Date()).getTime() == selectedDate.getTime()) {
      setSelectedDate(new Date())
    }
    setSaveEnabled(isAuthenticated())
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
        <Select
          value={selectedSegmentId}
          onChange={changeSegmentId}
          disabled={!isAuthenticated()}
        >
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
    setSaveEnabled(isAuthenticated())
  }

  // Display/edit the installation date
  function datePicker(): JSX.Element {
    const helpText = id ? `Installed on` : 'Date installed'
    // XXX TODO FIXME: Display current value again after edit/cancel
    return (
      <Form.Item name={`${pos}-date-picker`} label={helpText}>
        <DatePicker
          format={"ddd ll"}
          showToday={true}
          onChange={handleDateChange}
          value={moment(selectedDate)}
          disabled={!isAuthenticated()}
          // defaultValue={moment(date)}
        />
      </Form.Item>
    )
  }

  const [form] = Form.useForm();

  function cancel(): void {
    if (viewMode == "installed") {
      setSaveEnabled(false)
      setSelectedDate(date ? date : new Date())
      setSelectedSegmentId(id || emptyId)
    }
    closeDialog()
    form.resetFields()
  }

  function saveChanges() {
    const maybeId = selectedSegmentId == emptyId ? undefined : selectedSegmentId
    const segmentToM1Pos = {
      date: format(selectedDate, 'yyyy-MM-dd'),
      maybeId: maybeId,
      position: pos
    }
    const token = auth?.token()
    const requestOptions = {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
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
          closeDialog()
        }
      })
  }

  const {Title} = Typography;

  const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
  };

  function installedFormItems(): JSX.Element {
    return (
      <>
        {segmentIdSelector()}
        {datePicker()}
        <Divider/>
        {plannedFormItems(false)}
        <Divider/>
        <div>
          <Title level={5}>
            History
          </Title>
          <PositionHistory pos={pos} changed={changed}/>
        </div>
      </>
    )
  }

  function plannedFormItems(includeSegmentId: boolean): JSX.Element {
    const sector = segmentData.position[0]
    const sectorMsg = sector == 'G' ? ' (Spare)' : ''
    return (
      <>
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
      </>
    )
  }

  function installedLayout(): JSX.Element {
    return (
      <Drawer
        // mask={false}
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
        <Form
          form={form}
          size={'small'}
          initialValues={{
            [`${pos}-date-picker`]: moment(date)
          }}
          {...layout}>
          {installedFormItems()}
        </Form>
      </Drawer>
    )
  }

  function plannedLayout(): JSX.Element {
    return (
      <Drawer
        // mask={false}
        title={`Segment ${pos}`}
        width={550}
        placement="right"
        closable={false}
        onClose={cancel}
        visible={open}
        bodyStyle={{paddingBottom: 80}}
      >
        <Form
          form={form}
          size={'small'}
          {...layout}>
          {plannedFormItems(true)}
        </Form>
      </Drawer>
    )
  }

  if (availableSegmentIds.length != 0) {
    return (viewMode == "installed" ? installedLayout() : plannedLayout())
  } else
    return (<div/>)
}
