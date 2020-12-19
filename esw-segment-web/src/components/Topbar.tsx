import React, {useState} from 'react'
import {TopbarDateChooser} from './TopbarDateChooser'
import {Checkbox, Layout, Menu, Tooltip, Typography} from "antd"
import {CheckboxChangeEvent} from "antd/lib/checkbox"
const {Title} = Typography;
const {SubMenu} = Menu;
const {Header} = Layout;

type TopbarProps = {
  mostRecentChange: Date
  updateDisplay: (showSegmentIds: boolean, refDate: Date) => void
}

// const useStyles = makeStyles((theme: Theme) =>
//   createStyles({
//     checkbox: {
//       color: 'inherit',
//       marginLeft: theme.spacing(10)
//     },
//     appBar: {
//       background: '#2E3B55'
//     },
//     toolbar: {
//     },
//     formControlLabel: {
//       fontSize: 14,
//       paddingTop: 4,
//       paddingRight: theme.spacing(10)
//     }
//   })
// )

export const Topbar = ({
                         mostRecentChange,
                         updateDisplay
                       }: TopbarProps): JSX.Element => {
  const [showSegmentIds, setShowSegmentIds] = useState(false)
  const [selectedDate, setSelectedDate] = useState<Date>(mostRecentChange)

  const showSegmentIdsChanged = (event: CheckboxChangeEvent) => {
    setShowSegmentIds(event.target.checked)
    updateDisplay(event.target.checked, selectedDate)
  }

  const refDateChanged = (date: Date) => {
    setSelectedDate(date)
    updateDisplay(showSegmentIds, date)
  }

  function showSegmentIdsCheckbox(): JSX.Element {
    return (
      <Tooltip placement="bottom" title='Display the last part of the segment id instead of the segment position'>
        <Checkbox
          style={{color: '@menu-item-color'}}
          checked={showSegmentIds}
          onChange={showSegmentIdsChanged}>
          Show Segment IDs
        </Checkbox>
      </Tooltip>
    )
  }

  return (
    <Header>
      <div style={{color: "#d9d9d9", float: "left"}}>
        TMT Segment Database
        {/*<Title*/}
        {/*  style={{color: "white"}}*/}
        {/*  level={5}>*/}
        {/*  TMT Segment Database*/}
        {/*</Title>*/}
      </div>
      <Menu theme="dark" mode="horizontal">
        <SubMenu key="view" title="View">
          <Menu.Item key="showSegmentIds">{showSegmentIdsCheckbox()}</Menu.Item>
        </SubMenu>
      </Menu>
    </Header>

    // <Header>
    //   <Title level={5}>TMT Segment Database</Title>
    //   <div>{showSegmentIdsCheckbox()}</div>
    //   <div>
    //     {/*<TopbarDateChooser*/}
    //     {/*  mostRecentChange={mostRecentChange}*/}
    //     {/*  updateDisplay={refDateChanged}*/}
    //     {/*/>*/}
    //   </div>
    // </Header>
  )
}
