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
  {id: 'date', label: 'Date', minWidth: 120, format: (value: Date) => value.toDateString()},
  {id: 'segmentId', label: 'Segment ID', minWidth: 100},
  {id: 'position', label: 'Position', minWidth: 170},
]

const useStyles = makeStyles({
  root: {
    width: '100%',
  },
  container: {
    maxHeight: 230,
  },
})

// export const SegmentDetails = ({id, pos, date, open, closeDialog, updateDisplay}: SegmentDetailsProps): JSX.Element => {
export const PositionHistory = ({pos}: PositionHistoryProps): JSX.Element => {
  const classes = useStyles()
  const [data, setData] = useState<Array<SegmentToM1Pos>>([])
  const [page, setPage] = useState<number>(0)
  const [rowsPerPage, setRowsPerPage] = useState<number>(10)

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


  const handleChangePage = (event: unknown, newPage: number) => {
    // XXX
    const x = event
    setPage(newPage)
  }

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(+event.target.value)
    setPage(0)
  }

  return (
    <Paper className={classes.root}>
      <TableContainer className={classes.container}>
        <Table stickyHeader aria-label="sticky table">
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  key={column.id}
                  align={column.align}
                  style={{minWidth: column.minWidth}}
                >
                  {column.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {data.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).map((row) => {
              return (
                <TableRow hover role="checkbox" tabIndex={-1} key={row.date}>
                  <TableCell key="date">
                    {new Date(row.date).toDateString()}
                  </TableCell>
                  <TableCell key="segmentId">
                    {row.maybeId ? row.maybeId : <em>removed</em>}
                  </TableCell>
                  <TableCell key="position">
                    {row.position}
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        rowsPerPageOptions={[5, 10, 50, 100]}
        component="div"
        count={data.length}
        rowsPerPage={rowsPerPage}
        page={page}
        onChangePage={handleChangePage}
        onChangeRowsPerPage={handleChangeRowsPerPage}
      />
    </Paper>
  )
}
