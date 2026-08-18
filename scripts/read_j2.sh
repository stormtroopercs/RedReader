#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## JsonValue.kt full ##########"
cat src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
echo "########## JsonArray.kt: class decl + mContents + get(id) + iterator (1-40 + 77-80) ##########"
awk 'NR<=40 || (NR>=73 && NR<=80) {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.kt
echo "########## JsonObject.kt: class decl + properties field (1-45) ##########"
awk 'NR<=45 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.kt
echo "########## ORIGINAL Java JsonValue.getAtPath family (150-195) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.java 2>/dev/null | awk 'NR>=150 && NR<=195 {printf "%d| %s\n", NR, $0}'
