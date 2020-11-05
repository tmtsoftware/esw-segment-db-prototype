
export class Config {
  static segmentRadius = 12

  static segmentPoints = [0, 1, 2, 3, 4, 5].map(i => {
      const px = Config.segmentRadius * Math.cos(i * 60 * Math.PI / 180.0)
      const py = Config.segmentRadius * Math.sin(i * 60 * Math.PI / 180.0)
      return `${px},${py}`
    }
  ).join(" ")

  static sectorAngle(sector: string) {
    switch (sector) {
      case "A":
        return -60
      case "B":
        return -60 * 2
      case "C":
        return -60 * 3
      case "D":
        return -60 * 4
      case "E":
        return -60 * 5
    }
    return 0
  }


}
