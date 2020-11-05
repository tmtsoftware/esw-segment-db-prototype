
export class Config {
  static segmentRadius = 10

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
