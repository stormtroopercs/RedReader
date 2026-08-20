#!/bin/bash
export JAVA_HOME=/opt/data/redreader-project/jdk-17.0.17+10
cd /opt/data/redreader-project/RedReader
./gradlew compileDebugKotlin --no-daemon > /tmp/compile.log 2>&1
echo "exit=$?"
echo "total: $(grep -c '^e:' /tmp/compile.log)"
