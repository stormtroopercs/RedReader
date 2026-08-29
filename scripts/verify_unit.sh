#!/bin/bash
cd /home/storm/.hermes/redreader-project/RedReader
export JAVA_HOME=/home/storm/.hermes/tools/jdk-17.0.17+10
./gradlew compileDebugKotlin --no-daemon > /tmp/compile.log 2>&1
echo "EXIT=$?"
echo "TOTAL: $(grep -c '^e:' /tmp/compile.log)"
echo '=== unit files with residual errors ==='
for f in $(git diff --name-only); do
  c=$(grep -c "^e:.*$f:" /tmp/compile.log)
  [ "$c" -gt 0 ] && echo "$c  $f"
done
echo '(none above = unit clean)'
echo '=== top 10 files overall ==='
grep '^e:' /tmp/compile.log | sed 's|.*com/stormtroopercs/materialreader/||; s|\.kt:.*||' | sort | uniq -c | sort -rn | head -10
