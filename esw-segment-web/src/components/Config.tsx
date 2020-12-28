/**
 * Basic configuration for the mirror display
 */
export class Config {
  // Radius of a single segment
  static segmentRadius = 12

  static mirrorDiameter = 23 * Config.segmentRadius * 2

  // Center of the mirror
  static xOrigin = Config.mirrorDiameter / 2
  static yOrigin = Config.mirrorDiameter / 2 - Config.segmentRadius

  // Calculated points for a hexagon with the given radius
  static segmentPoints = [...Array(6).keys()]
    .map((i) => {
      const px = Config.segmentRadius * Math.cos((i * 60 * Math.PI) / 180.0)
      const py = Config.segmentRadius * Math.sin((i * 60 * Math.PI) / 180.0)
      return `${px},${py}`
    })
    .join(' ')

  // An inner hexagon used to highlight a segment
  static innerSegmentPoints = [...Array(6).keys()]
    .map((i) => {
      const px =
        (Config.segmentRadius - 1) * Math.cos((i * 60 * Math.PI) / 180.0)
      const py =
        (Config.segmentRadius - 1) * Math.sin((i * 60 * Math.PI) / 180.0)
      return `${px},${py}`
    })
    .join(' ')

  // Returns the angle for the given sector (each one is rotated by 60 deg)
  static sectorAngle(sector: string): number {
    return -60 * (sector.charCodeAt(0) - 'A'.charCodeAt(0) + 1)
  }

  static sectorColors = new Map([
    ["A", "#eeda00"],
    ["B", "#ffcc99"],
    ["C", "#bbe0e3"],
    ["D", "#eeda00"],
    ["E", "#ffcc99"],
    ["F", "#bbe0e3"],
    ["G", "#e3e0e0"]
  ])

  static sectorEmptyColors = new Map([
    ["A", "#f8ee92"],
    ["B", "#ffefe0"],
    ["C", "#e2ecec"],
    ["D", "#f8ee92"],
    ["E", "#ffefe0"],
    ["F", "#e2ecec"],
    ["G", "#f3e8e8"]
  ])

  static undefinedColor = "#e3e0e0"

  static segmentAllocationColors = new Map([
    ["US", "#26d8ef"],
    ["Japan", "#5a80c6"],
    ["TBD", Config.undefinedColor],
    ["China", "#f60966"],
    ["India", "#f8ee92"],
  ])

  static itemLocationColors = new Map([
    ["Ohara", "#f60966"],
    ["Coherent", "#26d8ef"],
    ["Canon", "#5a80c6"],
    ["TBD", Config.undefinedColor],
    ["India", "#f8ee92"],
    ["JIT", "#26d8ef"],
  ])


  // case "riskOfLoss":
  // case "components":
  // case "status":


}
