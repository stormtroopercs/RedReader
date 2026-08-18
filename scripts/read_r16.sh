#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. WORKING @IntDef example (BezelSwipeOverlay) ##########"
grep -n '@IntDef\|annotation class\|import.*IntDef\|import.*Retention\|Retention(' src/main/java/org/quantumbadger/redreader/views/bezelmenu/BezelSwipeOverlay.kt | head
echo ""
echo "########## B. BLAST RADIUS: call sites of getUriBuilder ##########"
grep -rn 'getUriBuilder' src/main/java --include='*.kt' | grep -v 'Constants.kt'
echo ""
echo "########## C. BLAST RADIUS: getArrayAtPath / getStringAtPath / getObjectAtPath call sites (count) ##########"
grep -rn '\.getArrayAtPath\|\.getStringAtPath\|\.getObjectAtPath\|\.getAtPath(' src/main/java --include='*.kt' | wc -l
echo "--- files ---"
grep -rln '\.getArrayAtPath\|\.getStringAtPath\|\.getObjectAtPath\|\.getAtPath(' src/main/java --include='*.kt'
echo ""
echo "########## D. JsonValue.kt getAtPath family full (50-100) ##########"
awk 'NR>=50 && NR<=100 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
