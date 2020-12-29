import React from 'react'
import {Layout, Table, Tag} from "antd";
import {Config} from "./Config";
import {ColumnsType} from "antd/es/table";

const {Sider} = Layout;

type LegendProps = {
  viewMode: React.Key
}

export const Legend = ({viewMode}: LegendProps): JSX.Element => {
  function makeSegmentAllocationLegend(): JSX.Element {
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
        title: 'Partner',
        dataIndex: 'partner',
        key: 'partner',
        render: partner => (
          <Tag color={Config.segmentAllocationColors.get(partner)}>{partner}</Tag>
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

    const dataSource = [...Config.segmentAllocationColors.keys()].map((key: string) => {
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
      <Table
        className={'legend'}
        size={'small'}
        dataSource={dataSource}
        columns={columns}
        pagination={false}
        // scroll={{y: 200}}
      />
    )
  }

  function makeLegend(): JSX.Element {
    switch (viewMode) {
      case "segmentAllocation":
      case "itemLocation":
      case "riskOfLoss":
      case "components":
      case "status":
      default:
        break
    }
    return makeSegmentAllocationLegend()
  }

  return (
    <div style={{flex: 'grow', backgroundColor: '#383b3e'}}>
      {makeLegend()}
    </div>
  )
}
