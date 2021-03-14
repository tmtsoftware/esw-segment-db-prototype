import React from 'react'
import {Table, Tag} from "antd"
import {Config} from "./Config"
import {ColumnsType} from "antd/es/table"
import {JiraSegmentData} from "./SegmentData"
import {useAppContext} from "../AppContext"

interface SegmentStats {
  segmentTypes: number,
  totalPrimeSegments: number,
  totalSpareSegments: number,
  totalSegments: number,
}

interface SegmentAllocation {
  key: string;
  partner: string,
  segmentTypes: number | undefined,
  totalPrimeSegments: number,
  totalSpareSegments: number,
  totalSegments: number,
}

export const Legend = (): JSX.Element => {
  const {viewMode, segmentMap} = useAppContext()

  function makeTable(map: Map<string, string>): JSX.Element {
    const columns: ColumnsType<SegmentAllocation> = [
      {
        title: ' ',
        dataIndex: 'partner',
        key: 'partner',
        render: partner => (
          <Tag
            color={map.get(partner)}
            style={{color: 'black', width: '100%'}}
          >
            {partner}
          </Tag>
        ),
      },
      {
        title: 'Segment Types',
        dataIndex: 'segmentTypes',
        key: 'segmentTypes',
      },
      {
        title: 'Total Prime Segments',
        dataIndex: 'totalPrimeSegments',
        key: 'totalPrimeSegments',
      },
      {
        title: 'Total Spare Segments',
        dataIndex: 'totalSpareSegments',
        key: 'totalSpareSegments',
      },
      {
        title: 'Total Segments',
        dataIndex: 'totalSegments',
        key: 'totalSegments',
      }
    ];

    const dataSource: Array<SegmentAllocation> = [...map.keys()].map((key: string) => {
      const stats = getStats(key)
      return {
        key: key,
        partner: key,
        segmentTypes: stats.segmentTypes,
        totalPrimeSegments: stats.totalPrimeSegments,
        totalSpareSegments: stats.totalSpareSegments,
        totalSegments: stats.totalSegments,
      };
    })

    function dataSourceWithTotals(): Array<SegmentAllocation> {
      const totals: SegmentAllocation = {
        key: "total",
        partner: "Total",
        segmentTypes: undefined,
        totalPrimeSegments: dataSource.reduce((sum, current) => sum + current.totalPrimeSegments, 0),
        totalSpareSegments: dataSource.reduce((sum, current) => sum + current.totalSpareSegments, 0),
        totalSegments: dataSource.reduce((sum, current) => sum + current.totalSegments, 0)
      }
      return dataSource.concat([totals])
    }

    return (
      <Table<SegmentAllocation>
        className={'legend'}
        size={'small'}
        dataSource={dataSourceWithTotals()}
        columns={columns}
        pagination={false}
      />
    )
  }

  function getStats(key: string): SegmentStats {
    let f: (data: JiraSegmentData) => string
    switch (viewMode) {
      case "segmentAllocation":
        f = (data: JiraSegmentData) => {
          return data.originalPartnerBlankAllocation
        }
        break
      case "itemLocation":
        f = (data: JiraSegmentData) => {
          return data.itemLocation
        }
        break
      case "riskOfLoss":
        f = (data: JiraSegmentData) => {
          return data.riskOfLoss
        }
        break
      case "components":
        f = (data: JiraSegmentData) => {
          return data.components
        }
        break
      case "status":
        f = (data: JiraSegmentData) => {
          return data.status
        }
        break
    }
    let segmentTypes: Set<number> = new Set()
    let totalPrimeSegments = 0
    let totalSpareSegments = 0
    let totalSegments = 0

    Array.from(segmentMap.values()).forEach(data => {
        const x = f(data)
        if (x == key) {
          segmentTypes.add(data.segmentType)
          if (data.sector < 7)
            totalPrimeSegments++
          else
            totalSpareSegments++
          totalSegments++
        }
      }
    )
    return {
      segmentTypes: segmentTypes.size,
      totalPrimeSegments: totalPrimeSegments,
      totalSpareSegments: totalSpareSegments,
      totalSegments: totalSegments
    }
  }

  function makeLegend(): JSX.Element {
    switch (viewMode) {
      case "segmentAllocation":
        return makeTable(Config.segmentAllocationColors)
      case "itemLocation":
        return makeTable(Config.itemLocationColors)
      case "riskOfLoss":
        return makeTable(Config.riskOfLossColors)
      case "components":
        return makeTable(Config.componentColors)
      case "status":
        return makeTable(Config.statusColors)
      default:
        // return makeTable(Config.sectorColors)
        return (<div/>)
    }
  }

  return (
    <div style={{flex: 'grow', backgroundColor: '#383b3e'}}>
      {makeLegend()}
    </div>
  )
}
