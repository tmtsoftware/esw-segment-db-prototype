import React, {useEffect, useState} from 'react'
import {Divider, Layout, Menu, Progress} from "antd";
import {MenuInfo, SelectInfo} from 'rc-menu/lib/interface';
import {SegmentData} from "./SegmentData";

const {Sider} = Layout;

type SidebarProps = {
  sidebarOptionsChanged: (viewMode: React.Key, showSegmentIds: boolean, showSpares: boolean) => void
}

export const Sidebar = ({sidebarOptionsChanged}: SidebarProps): JSX.Element => {

  const [viewMode, setViewMode] = useState<string | number>("installed")
  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false)
  const [showSpares, setShowSpares] = useState<boolean>(false)
  const [syncing, setSyncing] = useState<boolean>(false)
  const [syncProgress, setSyncProgress] = useState<number>(0)

  useEffect(() => {
    sidebarOptionsChanged(viewMode, showSegmentIds, showSpares)
  }, [viewMode, showSegmentIds, showSpares])

  function menuItemSelected(info: MenuInfo) {
    if (info.key == "syncWithJira")
      syncWithJira()
    else
      setViewMode(info.key)
  }

  function menuOptionSelected(info: SelectInfo) {
    switch (info.key) {
      case 'showSegmentIds':
        setShowSegmentIds(true)
        break
      case 'showSpares':
        setShowSpares(true)
        break
    }
  }

  function menuOptionDeselected(info: SelectInfo) {
    switch (info.key) {
      case 'showSegmentIds':
        setShowSegmentIds(false)
        break
      case 'showSpares':
        setShowSpares(false)
        break
    }
  }

  async function syncWithJira() {
    const eventSource = new EventSource(`${SegmentData.baseUri}/syncWithJira`)
    eventSource.onmessage = e => {
      const progress: number = +e.data
      console.log(`XXX SSE: ${progress}`)
      setSyncing(progress < 100)
      setSyncProgress(progress)
    }
  }

  return (
    <Sider>
      <Menu
        theme="dark"
        onClick={menuItemSelected}
        defaultSelectedKeys={['installed']}
        mode="inline">
        <Menu.Item key="installed">
          Installed
        </Menu.Item>
        <Menu.Item key="planned">
          Planned
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
        <Menu.Item key="syncWithJira" disabled={syncing}>
          Sync with JIRA
        </Menu.Item>
      </Menu>
      <div style={{width: 140}}>
        <Progress
          percent={syncProgress}
          size="small"
          className="syncWithJiraProgress"
          style={{margin: '0 0 0 20px', display: syncing ? 'block' : 'none'}}/>
      </div>
      <Divider dashed style={{backgroundColor: '#b2c4db'}}/>
      <Menu
        multiple={true}
        theme="dark"
        onSelect={menuOptionSelected}
        onDeselect={menuOptionDeselected}
        mode="inline">
        <Menu.Item key="showSegmentIds">
          Show Segment IDs
        </Menu.Item>
        <Menu.Item key="showSpares">
          Show Spares
        </Menu.Item>
      </Menu>
    </Sider>
  )
}
