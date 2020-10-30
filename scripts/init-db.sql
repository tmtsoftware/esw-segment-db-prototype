/*
 * Initialize the ESW Segment Database
 */

DROP DATABASE IF EXISTS esw_segment_db;
CREATE DATABASE esw_segment_db;
CREATE TABLE segment_to_m1_pos (date DATE NOT NULL PRIMARY KEY, positions CHARACTER(6)[492]);
