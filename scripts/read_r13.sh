#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. CURRENT Constants.getUriBuilder + original ##########"
grep -n 'fun getUriBuilder' -A4 src/main/java/org/quantumbadger/redreader/common/Constants.kt
echo "--- original ---"
git show $B:src/main/java/org/quantumbadger/redreader/common/Constants.java 2>/dev/null | grep -n 'getUriBuilder' -A4
echo ""
echo "########## B. getArrayAtPath / getStringAtPath CURRENT def ##########"
grep -rn 'fun getArrayAtPath\|fun getStringAtPath\|fun getObjectAtPath\|fun getAtPath' src/main/java --include='*.kt'
echo ""
echo "########## C. original JsonValue getArrayAtPath/getPath (nullability) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.java 2>/dev/null | grep -n 'getArrayAtPath\|getStringAtPath\|getObjectAtPath\|Optional<JsonArray\|Optional<String\|Optional<JsonObject\|getPath\|getAtPath' 
echo ""
echo "########## D. CURRENT RequestResponseHandler interface (onRequestFailed/onRequestSuccess) ##########"
grep -rn 'interface RequestResponseHandler' -A12 src/main/java --include='*.kt' | head -30
