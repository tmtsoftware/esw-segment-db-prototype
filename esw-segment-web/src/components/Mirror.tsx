import React from 'react'
import './Mirror.css'
import {Sector} from "./Sector";
import {Config} from "./Config";
import {SegmentToM1Pos} from "./SegmentData";

type MirrorProps = {
  showSegmentIds: boolean,
  posMap: Map<string, SegmentToM1Pos>,
  mostRecentChange: number,
  updateDisplay: () => void
}

/**
 * Represents the TMT mirror
 *
 * @param showSegmentIds if true, display segment ids instead of positions in the segments
 * @param refDate the reference date to use for the display (default: current date)
 * @constructor
 */
export const Mirror = ({showSegmentIds, posMap, mostRecentChange, updateDisplay}: MirrorProps): JSX.Element => {

  // const [posMap, setPosMap] = useState<Map<string, SegmentToM1Pos>>(new Map());
  // const [mostRecentChange, setMostRecentChange] = useState<number>(0);
  //
  // console.log(`XXX Mirror: refDate = ${refDate}`)
  //
  // const requestOptions = {
  //   method: 'POST',
  //   headers: {'Content-Type': 'application/json'},
  //   body: JSON.stringify(refDate.getTime())
  // }
  //
  // async function fetchData() {
  //   const [mostRecentChangeResponse, positionsOnDateResponse] = await Promise.all([
  //     fetch(`${SegmentData.baseUri}/mostRecentChange`, requestOptions),
  //     fetch(`${SegmentData.baseUri}/positionsOnDate`, requestOptions)
  //   ]);
  //
  //   const mostRecentChange: number = await mostRecentChangeResponse.json()
  //   const positionsOnDate: Array<SegmentToM1Pos> = await positionsOnDateResponse.json()
  //
  //   return {
  //     mostRecentChange,
  //     positionsOnDate
  //   };
  // }
  //
  // function updateDisplay() {
  //   fetchData().then(({mostRecentChange, positionsOnDate}) => {
  //     const posMap: Map<string, SegmentToM1Pos> = positionsOnDate
  //       .reduce((map, obj) => map.set(obj.position, obj), new Map())
  //     setPosMap(posMap)
  //     setMostRecentChange(mostRecentChange)
  //     console.log(`XXX Mirror: mostRecentChange = ${new Date(mostRecentChange)}`)
  //   });
  //
  // }
  //
  // useEffect(() => {
  //   updateDisplay()
  // }, []);

  if (posMap.size == 0 || mostRecentChange == 0) {
    return <div/>
  } else {
    const d = Config.mirrorDiameter
    return (
      <div className="mirror-container">
        <svg className="mirror-svg" viewBox={`0 0 ${d} ${d}`} preserveAspectRatio="xMidYMin slice">
          <g className="sectors">
            <circle
              cx={Config.xOrigin}
              cy={Config.yOrigin}
              r={Config.segmentRadius * 2 * 10.5}
              fill="none"
              stroke="black"
              strokeWidth="0.5"
            />
            <Sector sector="A" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="B" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="C" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="D" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="E" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
            <Sector sector="F" posMap={posMap} mostRecentChange={mostRecentChange} showSegmentIds={showSegmentIds}
                    updateDisplay={updateDisplay}/>
          </g>
        </svg>
      </div>
    )
  }
}

