#!/bin/bash
# requirements: pandoc, pandoc-citeproc

pandoc --filter pandoc-citeproc \
  -V colorlinks \
  --number-sections \
  -V urlcolor=NavyBlue \
  -V linkcolor=NavyBlue \
  -V geometry:"top=2cm, bottom=1.5cm, left=2cm, right=2cm" \
  -o report.pdf \
  Report.md
