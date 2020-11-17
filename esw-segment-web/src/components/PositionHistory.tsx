import React, {useEffect, useState} from 'react'
import {makeStyles} from '@material-ui/core/styles'
import Paper from '@material-ui/core/Paper'
import Table from '@material-ui/core/Table'
import TableBody from '@material-ui/core/TableBody'
import TableCell from '@material-ui/core/TableCell'
import TableContainer from '@material-ui/core/TableContainer'
import TableHead from '@material-ui/core/TableHead'
import TablePagination from '@material-ui/core/TablePagination'
import TableRow from '@material-ui/core/TableRow'
import {SegmentData, SegmentToM1Pos} from "./SegmentData";


type PositionHistoryProps = {
  pos: string
}

interface Column {
  id: 'date' | 'segmentId' | 'position'
  label: string
  minWidth?: number
  align?: 'right'
  format?: (value: Date) => string
}

const columns: Column[] = [
  {id: 'date', label: 'Date', minWidth: 80, format: (value: Date) => value.toDateString()},
  {id: 'segmentId', label: 'Segment ID', minWidth: 60},
  {id: 'position', label: 'Position', minWidth: 50},
]

const useStyles = makeStyles({
  root: {
    width: '100%',
  },
  container: {
    maxHeight: 230,
  },
  tableCell:  {
    paddingTop: 5,
    paddingBottom: 5,
  },
})

/**
 * Displays a table showing when segments were added or removed
 * @param pos the segment position
 * @constructor
 */
export const PositionHistory = ({pos}: PositionHistoryProps): JSX.Element => {
  const classes = useStyles()
  const [data, setData] = useState<Array<SegmentToM1Pos>>([])

  // Gets the list of available segment ids for this position
  function getHistoryData() {
    fetch(`${SegmentData.baseUri}/allSegmentIds/${pos}`)
      .then(response => response.json())
      .then(result => {
        const d: Array<SegmentToM1Pos> = result
        setData(d)
      })
  }

  useEffect(() => {
    getHistoryData()
  }, [])


  return (
    <Paper className={classes.root}>
      <TableContainer className={classes.container}>
        <Table stickyHeader aria-label="sticky table">
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  className={classes.tableCell}
                  key={column.id}
                  align={column.align}
                  style={{minWidth: column.minWidth}}
                >
                  <strong>{column.label}</strong>
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {data.map((row) => {
              return (
                <TableRow hover key={row.date}>
                  <TableCell key="date" className={classes.tableCell}>
                    {new Date(row.date).toDateString()}
                  </TableCell>
                  <TableCell key="segmentId" className={classes.tableCell}>
                    {row.maybeId ? row.maybeId : <em>removed</em>}
                  </TableCell>
                  <TableCell key="position" className={classes.tableCell}>
                    {row.position}
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  )
}
