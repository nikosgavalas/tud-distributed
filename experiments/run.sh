#!/bin/bash
# we 're using this script because with akka actors there is no straightforward way to manage actively the number of
# spawned actors (which corresponds to the number of processes for the experiments)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CSV_DIR="$SCRIPT_DIR/csv"
CLOCKS=( "VC" "EVC" "REVC" "DMTREVC" )
MAX_MSG=100

mkdir -p "$CSV_DIR"/{time,bitsizes}

cd "$SCRIPT_DIR/.." || { echo "cd failed"; exit 1; }

# build
sbt clean assembly > /dev/null || { echo "build failed"; exit 1; }

for clock in "${CLOCKS[@]}"; do
  : > "$CSV_DIR/time/$clock.csv"
  for processes in $(seq 2 2 20); do
    echo running $clock with $processes processes
    scala target/scala-2.13/revc-implementation-assembly-0.1.0-SNAPSHOT.jar false $MAX_MSG $processes $clock 2>/dev/null | grep duration | awk -v p="$processes" '{printf "%s,%s\n", p, $2}' >> "$CSV_DIR/time/$clock.csv"
  done
done

for clock in "${CLOCKS[@]}"; do
  : > "$CSV_DIR/bitsizes/$clock.csv"
  echo running $clock with bitsize tracking
  scala target/scala-2.13/revc-implementation-assembly-0.1.0-SNAPSHOT.jar true $MAX_MSG $processes $clock 2>/dev/null | grep eventnum | awk '{printf "%s,%s\n", $2, $3}' >> "$CSV_DIR/bitsizes/$clock.csv"
done

cd "$SCRIPT_DIR" || { echo "cd failed"; exit 1; }

python plot.py
