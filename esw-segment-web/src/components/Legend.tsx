import React from 'react'
import {Layout, Table, Tag} from "antd";
import {Config} from "./Config";
import {ColumnsType} from "antd/es/table";
import {JiraSegmentData} from "./SegmentData";

const {Sider} = Layout;

type LegendProps = {
  viewMode: React.Key
  segmentMap: Map<string, JiraSegmentData>
}

interface SegmentStats {
  segmentTypes: string,
  totalPrimeSegments: string,
  totalSpareSegments: string,
  totalSegments: string,
}

export const Legend = ({viewMode, segmentMap}: LegendProps): JSX.Element => {
  function makeTable(map: Map<string, string>): JSX.Element {
    interface SegmentAllocation {
      key: string;
      partner: string,
      segmentTypes: string,
      totalPrimeSegments: string,
      totalSpareSegments: string,
      totalSegments: string,
    }

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

    // XXX TODO calculate the stats
    const dataSource = [...map.keys()].map((key: string) => {
      return {
        key: key,
        partner: key,
        segmentTypes: '12',
        totalPrimeSegments: '23',
        totalSpareSegments: '45',
        totalSegments: '345',
      };
    })

    return (
      <Table<SegmentAllocation>
        className={'legend'}
        size={'small'}
        dataSource={dataSource}
        columns={columns}
        pagination={false}
      />
    )
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
        return makeTable(Config.sectorColors)
    }
  }

  return (
    <div style={{flex: 'grow', backgroundColor: '#383b3e'}}>
      {makeLegend()}
    </div>
  )
}
