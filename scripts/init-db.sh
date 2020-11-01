#!/bin/sh

# Initialize the ESW Segment DB database, test database and tables
# (Do this once on installation. Edit ~/.pgpass to avoid retyping Postgres password.)

psql postgres -h localhost -f init-db.sql
psql postgres -h localhost -f init-test-db.sql
psql esw_segment_db -h localhost -f init-tables.sql
psql test_segment_db -h localhost -f init-tables.sql
