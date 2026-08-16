#!/bin/bash
# RedReader compile check: runs compileDebugKotlin and reports error count
cd /opt/data/RedReader
export JAVA_HOME=/opt/data/jdk-17.0.17+10
./gradlew compileDebugKotlin --no-daemon > /tmp/compile.log 2>&1
rc=$?
total=$(grep -c '^e:' /tmp/compile.log)
echo "exit=$rc total_errors=$total"
