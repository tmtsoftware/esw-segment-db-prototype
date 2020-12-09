#!/bin/sh

# "total" field gives number of issues

curl \
   -D- \
   -u ${JIRA_USER}:${JIRA_API_TOKEN} \
   -X GET \
   -H "Content-Type: application/json" \
   'https://tmt-project.atlassian.net/rest/api/latest/search?jql=project=SE-M1SEG&maxResults=0'
