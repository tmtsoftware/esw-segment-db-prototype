import React, {ChangeEvent, useEffect, useState} from 'react'
import {DatePicker, Form, Input, Layout, Menu, Modal, Popconfirm, Progress, Select, Upload} from "antd";
import {MenuInfo, SelectInfo} from 'rc-menu/lib/interface';
import {SegmentData, SegmentToM1Pos} from "./SegmentData";
import SubMenu from "antd/es/menu/SubMenu";
import { format } from 'date-fns'
import {UploadChangeParam} from "antd/es/upload";
import moment from "moment";
import ValueType = WebAssembly.ValueType;
import {useAppContext} from "../AppContext"

const {Sider} = Layout;

export const Sidebar = (): JSX.Element => {

  const {updateDisplay, setViewMode, setShowSegmentIds, setShowSpares, auth, authEnabled, posMap, mostRecentChange} = useAppContext()
  const [syncing, setSyncing] = useState<boolean>(false)
  const [syncProgress, setSyncProgress] = useState<number>(0)
  const [syncPopupVisible, setSyncPopupVisible] = useState<boolean>(false)
  const [fileMenuSelectedKeys, setFileMenuSelectedKeys] = useState<Array<string>>([])
  const [isExportModalVisible, setExportModalVisible] = useState(false);
  const [selectedExportDate, setSelectedExportDate] = useState(mostRecentChange);
  const [selectedExportBaseFileName, setSelectedExportBaseFileName] = useState('mirror');
  const [selectedExportOpt, setSelectedExportOpt] = useState('recent');

  useEffect(() => {
    setSelectedExportDate(mostRecentChange)
  }, [mostRecentChange])

  function isAuthenticated(): boolean {
    if (authEnabled) {
      if (auth)
        return (auth.isAuthenticated() || false)
      return false
    } else return true
  }

  function menuItemSelected(info: MenuInfo) {
    if (info.key == "syncWithJira")
      setSyncPopupVisible(true)
    else
      setViewMode(info.key)
  }

  function viewMenuOptionSelected(info: SelectInfo) {
    switch (info.key) {
      case 'showSegmentIds':
        setShowSegmentIds(true)
        break
      case 'showSpares':
        setShowSpares(true)
        break
    }
  }

  async function syncWithJira() {
    const eventSource = new EventSource(`${SegmentData.baseUri}/syncWithJira`)
    eventSource.onerror = _ => {
      setSyncing(false)
      setSyncPopupVisible(false)
      console.log("Failed to sync with JIRA")
      alert("Failed to sync with JIRA")
    }
    eventSource.onmessage = e => {
      const progress: number = +e.data
      setSyncing(progress < 100)
      setSyncProgress(progress)
      setSyncPopupVisible(false)
    }
  }

  function viewMenuOptionDeselected(info: SelectInfo) {
    switch (info.key) {
      case 'showSegmentIds':
        setShowSegmentIds(false)
        break
      case 'showSpares':
        setShowSpares(false)
        break
    }
  }

  function toDbPosition(loc: String): number {
    const sectorOffset = loc.charCodeAt(0) - 'A'.charCodeAt(0)
    const n: number = +loc.substr(1)
    return sectorOffset * 82 + n
  }

  interface SegmentConfig {
    position: string,
    segmentId?: string
  }

  interface MirrorConfig {
    date: string,
    segments: Array<SegmentConfig>
  }

  function exportFilter(pos: SegmentToM1Pos): Boolean {
    const spare = pos.position.charAt(0) == 'G'
    if (spare) return false
    return (selectedExportOpt == 'recent') ? new Date(pos.date) >= mostRecentChange : true
  }

  function exportMirrorConfigToFile() {
    const values: Array<SegmentToM1Pos> = [...posMap.values()]
      .filter(exportFilter)
      .sort((pos1, pos2) => {
      const a = toDbPosition(pos1.position)
      const b = toDbPosition(pos2.position)
      if (a > b) {
        return 1;
      } else if (a < b) {
        return -1;
      } else {
        return 0;
      }
    })
    const jsonObject: MirrorConfig = {
      date: format(mostRecentChange, 'yyyy-MM-dd'),
      segments: values.map((value: SegmentToM1Pos) => {
        return {
          position: value.position,
          ...value.maybeId && {segmentId: value.maybeId}
        }
      })
    }

    const a = document.createElement("a");
    a.href = URL.createObjectURL(new Blob([JSON.stringify(jsonObject, null, 2)], {
      type: "text/plain"
    }));
    const selectedDate = format(selectedExportDate, 'yyyy-MM-dd')
    a.setAttribute("download", `${selectedExportBaseFileName}-${selectedDate}.json`);
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  function fileMenuOptionSelected(info: SelectInfo) {
    switch (info.key) {
      case 'export':
        setExportModalVisible(true);
        break
      case 'import':
        setFileMenuSelectedKeys([])
        break
    }
  }

  function setPositions(config: MirrorConfig) {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(config)
    }
    fetch(`${SegmentData.baseUri}/setPositions`, requestOptions)
      .then((response) => response.status)
      .then((status) => {
        if (status != 200) alert(`Error: Failed to update the database`)
        setFileMenuSelectedKeys([])
        updateDisplay()
      })
  }

  const importProps = {
    accept: ".json",
    name: 'file',
    onChange(info: UploadChangeParam) {
      if (info.file.status !== 'uploading') {
        let reader = new FileReader();
        reader.onload = (e) => {
          if (e.target && e.target.result) {
            const json: string = e.target.result.toString()
            const config: MirrorConfig = JSON.parse(json)
            setPositions(config)
          }
        }
        if (info.file.originFileObj)
          reader.readAsText(info.file.originFileObj);
      }
      // TODO: Add busy, error or green check icon to menu item as feedback?
      if (info.file.status === 'done') {
        console.log(`XXX ${info.file.name} file uploaded successfully`);
      } else if (info.file.status === 'error') {
        alert(`${info.file.name} file upload failed.`);
      }
    },
  }

  const doExport = () => {
    exportMirrorConfigToFile()
    setFileMenuSelectedKeys([])
    setExportModalVisible(false);
  };

  const cancelExport = () => {
    setExportModalVisible(false);
  };

  const [exportForm] = Form.useForm();
  const exportLayout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
  };

  const handleExportDateChange = (value: moment.Moment | null) => {
    const newDate = value ? value.toDate() : mostRecentChange
    console.log(`XXX 2 setSelectedExportDate(${mostRecentChange})`)
    setSelectedExportDate(newDate)
  }

  const handleExportBaseFileNameChange = (event: ChangeEvent<HTMLInputElement>) => {
    setSelectedExportBaseFileName(event.target.value)
  }

  const handleExportOptChange = (value: ValueType) => {
    setSelectedExportOpt(value)
  }

  return (
    <Sider>
      <Menu
        theme="dark"
        onClick={menuItemSelected}
        defaultSelectedKeys={['installed']}
        defaultOpenKeys={['jira']}
        mode="inline">
        <Menu.Item key="installed">
          Installed
        </Menu.Item>
        <SubMenu key="jira" title="Planning">
          <Menu.Item key="planned">
            Segments
          </Menu.Item>
          <Menu.Item key="segmentAllocation">
            Segment Allocation
          </Menu.Item>
          <Menu.Item key="itemLocation">
            Item Location
          </Menu.Item>
          <Menu.Item key="riskOfLoss">
            Risk Of Loss
          </Menu.Item>
          <Menu.Item key="components">
            Components
          </Menu.Item>
          <Menu.Item key="status">
            Status
          </Menu.Item>
          <Menu.Item key="syncWithJira" disabled={syncing || !isAuthenticated()}>
            Sync with JIRA
          </Menu.Item>
        </SubMenu>
      </Menu>
      <Popconfirm
        placement="right"
        title="Really Sync with JIRA?"
        visible={syncPopupVisible}
        onConfirm={syncWithJira}
        onCancel={() => setSyncPopupVisible(false)}
      />
      <div style={{width: 140}}>
        <Progress
          percent={syncProgress}
          size="small"
          className="syncWithJiraProgress"
          style={{margin: '0 0 0 20px', display: syncing ? 'block' : 'none'}}/>
      </div>
      {/*<Divider dashed style={{backgroundColor: '#b2c4db'}}/>*/}
      <Menu
        multiple={false}
        theme="dark"
        onSelect={fileMenuOptionSelected}
        defaultOpenKeys={['file']}
        selectedKeys={fileMenuSelectedKeys}
        mode="inline">
        <SubMenu key="file" title="File">
          <Menu.Item key="export">
            Export
          </Menu.Item>
          <Modal
            centered
            title="Export Currently Displayed Segment Allocation"
            visible={isExportModalVisible}
            onOk={doExport}
            onCancel={cancelExport}>
            <Form
              form={exportForm}
              initialValues={{
                ['export-option']: 'recent',
                'date-picker': moment(selectedExportDate),
                'base-file-name': selectedExportBaseFileName
              }}
              size={'small'}
              {...exportLayout}>
              <Form.Item name="date-picker" label={'Date'} rules={[{ required: true }]}>
                <DatePicker
                  format={"ddd ll"}
                  showToday={true}
                  onChange={handleExportDateChange}
                  value={moment(selectedExportDate)}
                />
              </Form.Item>
              <Form.Item name="base-file-name" label={'Base file name'} rules={[{ required: true }]}>
                <Input
                  onChange={handleExportBaseFileNameChange}
                />
              </Form.Item>
              <Form.Item name="export-option" label={'File contents'} rules={[{ required: false }]}>
                <Select onChange={handleExportOptChange}>
                  <Select.Option value="all">All positions</Select.Option>
                  <Select.Option value="recent">Only most recent changes</Select.Option>
                </Select>
              </Form.Item>
            </Form>
          </Modal>
          <Menu.Item key="import" disabled={!isAuthenticated()}>
            <Upload {...importProps}>
              <div style={{color: '#ffffffa6'}}>Import</div>
            </Upload>
          </Menu.Item>
        </SubMenu>
      </Menu>
      <Menu
        multiple={true}
        theme="dark"
        defaultOpenKeys={['view']}
        onSelect={viewMenuOptionSelected}
        onDeselect={viewMenuOptionDeselected}
        mode="inline">
        <SubMenu key="view" title="View">
          <Menu.Item key="showSegmentIds">
            Show Segment IDs
          </Menu.Item>
          <Menu.Item key="showSpares">
            Show Spares
          </Menu.Item>
        </SubMenu>
      </Menu>
    </Sider>
  )
}
