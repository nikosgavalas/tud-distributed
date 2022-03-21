#!/bin/bash

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"
CSV_DIR="$SCRIPT_DIR/csv"
CLOCKS=( "VC" "EVC" "REVC" "DMTREVC" )

mkdir -p "$CSV_DIR"

cd "$SCRIPT_DIR/.." || { echo "cd failed"; exit 1; }

# build
sbt clean assembly > /dev/null || { echo "build failed"; exit 1; }

for clock in "${CLOCKS[@]}"; do
  : > "$CSV_DIR/$clock.csv"
  for processes in $(seq 2 2 50); do
    echo running $clock with $processes processes
    scala target/scala-2.13/revc-implementation-assembly-0.1.0-SNAPSHOT.jar $processes $clock 2>/dev/null | grep duration | awk -v p="$processes" '{printf "%s,%s\n", p, $2}' >> "$CSV_DIR/$clock.csv"
  done
done

cd "$SCRIPT_DIR" || { echo "cd failed"; exit 1; }

python plot.py
