#!/bin/bash
# MaterialReader compile check: run, capture, print total + optional per-file filter
# Usage: bash scripts/build_check.sh [FILE_FILTER]
export JAVA_HOME=/home/storm/.hermes/tools/jdk-17.0.17+10
cd /home/storm/.hermes/redreader-project/RedReader
./gradlew compileDebugKotlin --no-daemon > /tmp/compile.log 2>&1
echo "exit=$?"
echo "TOTAL: $(grep -c '^e:' /tmp/compile.log)"
if [ -n "$1" ]; then
  grep '^e:' /tmp/compile.log | grep "$1" | sed 's|file:///home/storm/.hermes/redreader-project/RedReader/src/main/java/com/stormtroopercs/materialreader/||'
fi
