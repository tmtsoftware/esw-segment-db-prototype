import React, { useEffect, useState } from 'react'
import { SegmentData, SegmentToM1Pos } from './SegmentData'
import {Table} from "antd";

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


// interface Column {
//   id: 'date' | 'segmentId'
//   label: string
//   minWidth?: number
//   align?: 'right'
//   format?: (value: Date) => string
// }
//
// const columns: Column[] = [
//   {
//     id: 'date',
//     label: 'Date',
//     minWidth: 80,
//     format: (value: Date) => value.toLocaleDateString('en-US')
//   },
//   { id: 'segmentId', label: 'Segment ID', minWidth: 60 }
// ]

// const useStyles = makeStyles({
//   root: {
//     width: '100%'
//   },
//   container: {
//     maxHeight: 230
//   },
//   tableCell: {
//     paddingTop: 5,
//     paddingBottom: 5
//   }
// })

/**
 * Displays a table showing when segments were added or removed
 * @param pos the segment position
 * @constructor
 */
export const PositionHistory = ({ pos }: PositionHistoryProps): JSX.Element => {
  // const classes = useStyles()
  const [data, setData] = useState<Array<SegmentToM1Pos>>([])

  // Gets the list of available segment ids for this position
  function getHistoryData() {
    fetch(`${SegmentData.baseUri}/allSegmentIds/${pos}`)
      .then((response) => response.json())
      .then((result) => {
        const d: Array<SegmentToM1Pos> = result
        setData(d)
      })
  }

  useEffect(() => {
    getHistoryData()
  }, [])

  const dataSource = data.map((row) =>  {
    return {
      key: row.position,
      date: new Date(row.date).toLocaleDateString('en-US'),
      segmentId: row.maybeId ? row.maybeId : <em>removed</em>
    };
  });

  return (
    <Table dataSource={dataSource} columns={columns} />
  )
}
