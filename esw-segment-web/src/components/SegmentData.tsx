// These should match the JSON output of the classes on the server side in EswSegmentData.scala
export class SegmentData {
  // TODO: Configure
  static baseUri = 'http://localhost:9192'
}

/**
 * The object returned from the server for each segment
 */
export interface SegmentToM1Pos {
  date: number
  maybeId?: string
  position: string
}

/**
 * Segment data extracted from JIRA
 */
export interface JiraSegmentData {
  position: string,
  segmentId: string,
  jiraKey: string,
  jiraUri: string,
  sector: number,
  segmentType: number,
  partNumber: string,
  originalPartnerBlankAllocation: string,
  itemLocation: string,
  riskOfLoss: string,
  components: string,
  status: string,
  workPackages: string,
  acceptanceCertificates: string,
  acceptanceDateBlank: string,
  shippingAuthorizations: string
}

