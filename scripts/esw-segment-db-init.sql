/*
 * Drop and create the segment_to_m1_pos table
 */


-- Segment to M1 Position Assignments Store
-- Note: positions(i) is the segment id for segment i, or "null" if the segment is not present.
DROP TABLE IF EXISTS segment_to_m1_pos;
CREATE TABLE segment_to_m1_pos (date DATE NOT NULL PRIMARY KEY, positions CHARACTER(6)[492]);
