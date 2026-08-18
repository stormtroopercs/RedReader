#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## CLEAN ERROR LIST ##########"
grep '^e:.*reddit/RedditAPI.kt' /tmp/compile.log | sed 's|.*RedditAPI.kt:||'
echo ""
echo "########## L1030-1050 (@IntDef annotation + imports head) ##########"
awk 'NR>=1030 && NR<=1050 {printf "%d| %s\n", NR, $0}' $f
echo "########## imports: Retention/Target/IntDef ##########"
grep -n 'import.*Retention\|import.*Target\|import.*IntDef\|import java.lang.annotation' $f
echo "########## L138-150 (fromJsonList + flair) ##########"
awk 'NR>=138 && NR<=150 {printf "%d| %s\n", NR, $0}' $f
echo "########## L168-182 (flair list add) ##########"
awk 'NR>=168 && NR<=182 {printf "%d| %s\n", NR, $0}' $f
