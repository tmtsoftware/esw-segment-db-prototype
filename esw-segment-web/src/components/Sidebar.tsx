import React, {useEffect, useState} from 'react'
import {Divider, Layout, Menu} from "antd";
import {MenuInfo, SelectInfo} from 'rc-menu/lib/interface';

const {Sider} = Layout;

type SidebarProps = {
  sidebarOptionsChanged: (viewMode: React.Key, showSegmentIds: boolean, showSpares: boolean) => void
}

export const Sidebar = ({sidebarOptionsChanged}: SidebarProps): JSX.Element => {

  const [viewMode, setViewMode] = useState<string|number>("installed")
  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false)
  const [showSpares, setShowSpares] = useState<boolean>(false)

  useEffect(() => {
    sidebarOptionsChanged(viewMode, showSegmentIds, showSpares)
  }, [viewMode, showSegmentIds, showSpares])

  function menuItemSelected(info: MenuInfo) {
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
        <Menu.Item key="syncWithJira">
          Sync with JIRA
        </Menu.Item>
      </Menu>
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
