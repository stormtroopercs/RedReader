#!/bin/bash
# MaterialReader compile + error count helper
# Usage: bash compile_count.sh [logname]
LOG="${1:-compile}"
cd /opt/data/redreader-project/MaterialReader
export JAVA_HOME=/opt/data/tools/jdk-17.0.17+10
./gradlew compileDebugKotlin --no-daemon > "/tmp/${LOG}.log" 2>&1
EC=$?
TOTAL=$(grep -c '^e:' "/tmp/${LOG}.log")
FILES=$(grep '^e:' "/tmp/${LOG}.log" | sed 's/.*redreader\///; s/:.*//' | sort -u | wc -l)
echo "EXIT=$EC TOTAL=$TOTAL FILES=$FILES"
echo "---- top 15 files ----"
grep '^e:' "/tmp/${LOG}.log" | sed 's/.*redreader\///; s/:.*//' | sort | uniq -c | sort -rn | head -15
