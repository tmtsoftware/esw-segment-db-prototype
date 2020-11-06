
export class Config {
  static segmentRadius = 12

  static xOrigin = 300
  static yOrigin = 300

  static segmentPoints = [...Array(6).keys()].map(i => {
      const px = Config.segmentRadius * Math.cos(i * 60 * Math.PI / 180.0)
      const py = Config.segmentRadius * Math.sin(i * 60 * Math.PI / 180.0)
      return `${px},${py}`
    }
  ).join(" ")

  static sectorAngle(sector: string) {
    return -60 * (sector.charCodeAt(0)-"A".charCodeAt(0) + 1)
  }


}
