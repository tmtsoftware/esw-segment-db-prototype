#!/bin/sh

#curl \
#   -D- \
#   -u ${JIRA_USER}:${JIRA_API_TOKEN} \
#   -X GET \
#   -H "Content-Type: application/json" \
#   https://tmt-project.atlassian.net/rest/api/latest/issue/M1ST-72?expand=schema

# customfield_12000 = sector
# customfield_11909 = segment type
# customfield_11905 = Part Number
# customfield_11907 = Original Partner Blank Allocation
# customfield_11906 = Item Location (India, Canon, ...)
# customfield_11912 = Risk of Loss
# components = Components (Planned, In-Work Blank, Accepted Blank, In-Work Roundel, Acceptance View Roundel)
# status = Status (TO DO, In Progress, ...) // JIRA Status
# summary = title (includes segment id: ex: "M1 Segment SN-072")

# Segment no: 1 to 574
# XXX Need to get a list of all JIRA tasks, since some blanks were discarded and new issues created!
segmentNumber=1

curl \
   -D- \
   -u ${JIRA_USER}:${JIRA_API_TOKEN} \
   -X GET \
   -H "Content-Type: application/json" \
   https://tmt-project.atlassian.net/rest/api/latest/issue/M1ST-${segmentNumber}?fields=customfield_12000,customfield_11909,customfield_11905,customfield_11907,customfield_11906,customfield_11912,components,status,summary
