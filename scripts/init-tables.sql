/*
 * Initialize the ESW Segment Database tables
 */

CREATE TABLE segment_to_m1_pos (date DATE NOT NULL PRIMARY KEY, positions CHARACTER(6)[492]);
