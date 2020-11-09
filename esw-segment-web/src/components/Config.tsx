/**
 * Basic configuration for the mirror display
 */
export class Config {
  // Radius of a single segment
  static segmentRadius = 12

  // Center of the mirror
  static xOrigin = 300
  static yOrigin = 300

  // Calculated points for a hexagon with the given radius
  static segmentPoints = [...Array(6).keys()].map(i => {
      const px = Config.segmentRadius * Math.cos(i * 60 * Math.PI / 180.0)
      const py = Config.segmentRadius * Math.sin(i * 60 * Math.PI / 180.0)
      return `${px},${py}`
    }
  ).join(" ")

  // Returns the angle for the given sector (each one is rotated by 60 deg)
  static sectorAngle(sector: string) {
    return -60 * (sector.charCodeAt(0)-"A".charCodeAt(0) + 1)
  }
}
