/*
 * Initialize the ESW Segment Database tables
 */

CREATE TABLE segment_to_m1_pos
(
    date         DATE NOT NULL PRIMARY KEY,
    positions    TEXT[492],
    install_date DATE[492]
);

CREATE TABLE jira_segment_data
(
    segment_id    TEXT,
    jira_key      TEXT,
    sector        INT,
    segment_type  INT,
    part_number   TEXT,
    partner       TEXT,
    item_location TEXT,
    risk_of_loss  TEXT,
    components    TEXT,
    status        TEXT
);
