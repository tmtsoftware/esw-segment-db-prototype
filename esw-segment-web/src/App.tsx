import React, {useEffect, useState} from 'react'
import './App.css'
import {Topbar} from './components/Topbar'
import {Mirror} from './components/Mirror'
import {SegmentData, SegmentToM1Pos} from './components/SegmentData'
import {Layout, Menu} from "antd"
import 'antd/dist/antd.css'
import { UserOutlined, LaptopOutlined, NotificationOutlined } from '@ant-design/icons';

const {Sider, Content} = Layout;
const { SubMenu } = Menu;

const App = (): JSX.Element => {
  const [showSegmentIds, setShowSegmentIds] = useState<boolean>(false)
  const [posMap, setPosMap] = useState<Map<string, SegmentToM1Pos>>(new Map())
  const [mostRecentChange, setMostRecentChange] = useState<number>(0)

  async function fetchData(refDate: Date) {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(refDate.getTime())
    }

    const [
      mostRecentChangeResponse,
      positionsOnDateResponse
    ] = await Promise.all([
      fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions),
      fetch(`${SegmentData.baseUri}/positionsOnDate`, requestOptions)
    ])

    const mostRecentChange: number = await mostRecentChangeResponse.json()
    const positionsOnDate: Array<SegmentToM1Pos> = await positionsOnDateResponse.json()

    return {
      mostRecentChange,
      positionsOnDate
    }
  }

  function updateDataNow(refDate: Date) {
    fetchData(refDate).then(({mostRecentChange, positionsOnDate}) => {
      const posMap: Map<string, SegmentToM1Pos> = positionsOnDate.reduce(
        (map, obj) => map.set(obj.position, obj),
        new Map()
      )
      setPosMap(posMap)
      setMostRecentChange(mostRecentChange)
    })
  }

  function updateData() {
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(new Date().getTime())
    }
    fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions)
      .then((response) => response.json())
      .then((result) => {
        const date: Date = new Date(result)
        updateDataNow(date)
      })
  }

  function updateDisplay(showSegmentIds: boolean, refDate: Date) {
    setShowSegmentIds(showSegmentIds)
    updateDataNow(refDate)
  }

  useEffect(() => {
    updateData()
  }, [])

  if (mostRecentChange == 0) return <div/>
  else
    return (
      <Layout className='App'>
        <Topbar
          mostRecentChange={new Date(mostRecentChange)}
          updateDisplay={updateDisplay}
        />
        <Layout>
          <Sider className={'sidebar'}>
            <Menu
              className={'sidebarMenu'}
              mode="inline"
              defaultSelectedKeys={['1']}
              defaultOpenKeys={['sub1']}
            >
              <SubMenu key="sub1" icon={<UserOutlined />} title="subnav 1">
                <Menu.Item key="1">option1</Menu.Item>
                <Menu.Item key="2">option2</Menu.Item>
                <Menu.Item key="3">option3</Menu.Item>
                <Menu.Item key="4">option4</Menu.Item>
              </SubMenu>
              <SubMenu key="sub2" icon={<LaptopOutlined />} title="subnav 2">
                <Menu.Item key="5">option5</Menu.Item>
                <Menu.Item key="6">option6</Menu.Item>
                <Menu.Item key="7">option7</Menu.Item>
                <Menu.Item key="8">option8</Menu.Item>
              </SubMenu>
              <SubMenu key="sub3" icon={<NotificationOutlined />} title="subnav 3">
                <Menu.Item key="9">option9</Menu.Item>
                <Menu.Item key="10">option10</Menu.Item>
                <Menu.Item key="11">option11</Menu.Item>
                <Menu.Item key="12">option12</Menu.Item>
              </SubMenu>
            </Menu>          </Sider>
          <Content>
            <Mirror
              showSegmentIds={showSegmentIds}
              posMap={posMap}
              mostRecentChange={mostRecentChange}
              updateDisplay={updateData}
            />
          </Content>
        </Layout>
      </Layout>
    )
}
export default App
