package esw.segment.shared

/**
 * Data scanned from JIRA issues
 *
 * @param segmentId the segment id ("SN-072")
 * @param jiraKey base name of JIRA task ("M1ST-72")
 * @param jiraUri link to JIRA issue
 * @param sector 1 to 6, or 7 for spare
 * @param segmentType 1 to 82, indicates where it can be used
 * @param partNumber string from issue
 * @param originalPartnerBlankAllocation US, Japan, India, China, ...
 * @param itemLocation JIT, Canon, India, Ohara, ...
 * @param riskOfLoss TIO, NINS, DST, ...
 * @param components Planned, In-Work Blank, Accepted Blank, In-Work Roundel, Acceptance View Roundel
 * @param status TO DO, In Progress, Under Review, In TIO Storage, Disposed, ...
 */
case class JiraSegmentData(
    segmentId: String,
    jiraKey: String,
    jiraUri: String,
    sector: Int,
    segmentType: Int,
    partNumber: String,
    originalPartnerBlankAllocation: String,
    itemLocation: String,
    riskOfLoss: String,
    components: String,
    status: String,
    workPackages: String,
    acceptanceCertificates: String,
    acceptanceDateBlank: String
)
