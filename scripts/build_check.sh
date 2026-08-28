#!/bin/bash
# MaterialReader compile check: run, capture, print total + optional per-file filter
# Usage: bash scripts/build_check.sh [FILE_FILTER]
export JAVA_HOME=/opt/data/tools/jdk-17.0.17+10
cd /opt/data/redreader-project/MaterialReader
./gradlew compileDebugKotlin --no-daemon > /tmp/compile.log 2>&1
echo "exit=$?"
echo "TOTAL: $(grep -c '^e:' /tmp/compile.log)"
if [ -n "$1" ]; then
  grep '^e:' /tmp/compile.log | grep "$1" | sed 's|file:///opt/data/redreader-project/MaterialReader/src/main/java/com/stormtroopercs/materialreader/||'
fi
