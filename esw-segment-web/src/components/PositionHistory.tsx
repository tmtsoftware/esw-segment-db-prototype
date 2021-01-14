import React, {useEffect, useState} from 'react'
import {SegmentData, SegmentToM1Pos} from './SegmentData'
import {Table} from "antd"


type PositionHistoryProps = {
  pos: string
  changed: number
}

const columns = [
  {
    title: 'Date',
    dataIndex: 'date',
    key: 'date',
  },
  {
    title: 'Segment ID',
    dataIndex: 'segmentId',
    key: 'segmentId',
  }
];

/**
 * Displays a table showing when segments were added or removed
 * @param pos the segment position
 * @param changed incremented when something changes that could change the contents of the history table
 * @constructor
 */
export const PositionHistory = ({pos, changed}: PositionHistoryProps): JSX.Element => {
  const [data, setData] = useState<Array<SegmentToM1Pos>>([])

  useEffect(() => {
    // Gets the list of segment ids that were in the given position until now
    function getHistoryData() {
      fetch(`${SegmentData.baseUri}/allSegmentIds/${pos}`)
        .then((response) => response.json())
        .then((result) => {
          const d: Array<SegmentToM1Pos> = result
          if (JSON.stringify(d) != JSON.stringify(data)) {
            setData(d)
          }
        })
    }
    getHistoryData()
  }, [pos, changed])

  const dataSource = data.map((row) => {
    return {
      key: row.date,
      date: row.date,
      segmentId: row.maybeId ? row.maybeId : <em>empty</em>
    };
  });

  // margin: 10px;
  // border-style: double;

  return (
    <Table
      style={{borderStyle: 'double', borderColor: 'gray'}}
      dataSource={dataSource}
      columns={columns}
      pagination={false}
      scroll={{y: 200}}
    />
  )
}
