import React, {useEffect, useRef, useState} from 'react'
import {SegmentData, SegmentToM1Pos} from './SegmentData'
import {Table} from "antd"


type PositionHistoryProps = {
  pos: string
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
 * @constructor
 */
export const PositionHistory = ({pos}: PositionHistoryProps): JSX.Element => {
  // const classes = useStyles()
  const [data, setData] = useState<Array<SegmentToM1Pos>>([])

  useEffect(() => {
    // Gets the list of available segment ids for this position
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
    console.log(`XXX getHistoryData ${pos}`)
    getHistoryData()
  })

  const dataSource = data.map((row) => {
    return {
      key: row.date,
      date: new Date(row.date).toLocaleDateString('en-US'),
      segmentId: row.maybeId ? row.maybeId : <em>removed</em>
    };
  });

  return (
    <Table
      dataSource={dataSource}
      columns={columns}
      pagination={false}
      scroll={{y: 200}}
    />
  )
}
