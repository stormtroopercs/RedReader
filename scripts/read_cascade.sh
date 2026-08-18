#!/bin/bash
cd /opt/data/RedReader
echo "########## JsonValue.kt: getAtPath family (56-96) ##########"
awk 'NR>=56 && NR<=96 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
echo "########## JsonArray.kt: getAtPathInternal (112-134) ##########"
awk 'NR>=112 && NR<=134 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.kt
echo "########## JsonObject.kt: imports (1-12) + getLong/getDouble (88-104) + TYPE region (210-225) + iterator/getAtPathInternal (248-266) ##########"
awk 'NR<=12 || (NR>=88 && NR<=104) || (NR>=210 && NR<=225) || (NR>=248 && NR<=266) {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.kt
