#!/bin/bash
# RedReader compile check: run, capture, print total + optional per-file filter
export JAVA_HOME=/opt/data/jdk-17.0.17+10
cd /opt/data/RedReader
./gradlew compileDebugKotlin --no-daemon > /tmp/compile.log 2>&1
echo "TOTAL: $(grep -c '^e:' /tmp/compile.log)"
if [ -n "$1" ]; then
  grep '^e:' /tmp/compile.log | grep "$1" | sed 's|file:///opt/data/RedReader/src/main/java/org/quantumbadger/redreader/||'
fi
