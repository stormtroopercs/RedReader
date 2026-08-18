#!/bin/bash
export JAVA_HOME=/opt/data/jdk-17.0.17+10
cd /opt/data/RedReader
./gradlew compileDebugKotlin --no-daemon > /tmp/compile.log 2>&1
echo "exit=$?"
echo "total: $(grep -c '^e:' /tmp/compile.log)"
