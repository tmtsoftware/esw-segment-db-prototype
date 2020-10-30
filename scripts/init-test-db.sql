/*
 * Initialize the Test Segment Database
 */

DROP DATABASE IF EXISTS test_segment_db;
CREATE DATABASE test_segment_db;
CREATE TABLE test_segment_db (date DATE NOT NULL PRIMARY KEY, positions CHARACTER(6)[492]);
