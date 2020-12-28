import React from 'react'
import {Layout, Menu} from "antd";
import {MenuClickEventHandler} from 'rc-menu/lib/interface';

const {Sider} = Layout;

type SidebarProps = {
  menuItemSelected: MenuClickEventHandler
}

export const Sidebar = ({menuItemSelected}: SidebarProps): JSX.Element => {
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
    </Sider>
  )
}
