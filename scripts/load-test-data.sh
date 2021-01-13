#!/bin/sh

# Resets the database and then loads some test data into the database (Run from top level dir)

dir=./esw-segment-shared/src/test/resources

./target/universal/stage/bin/esw-segment-client --resetTables
for i in $dir/*.json
do
  echo "Importing $i"
  ./target/universal/stage/bin/esw-segment-client -i $i
done
