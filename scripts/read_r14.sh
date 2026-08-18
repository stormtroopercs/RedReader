#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. JsonArray.kt: iterator / element type (40-110) ##########"
awk 'NR>=40 && NR<=110 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.kt
echo "########## B. JsonObject.kt: iterator / entry type ##########"
grep -n 'iterator\|Iterable\|Map.Entry\|asSequence\|operator fun\|class JsonEntry\|val key\|val value' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.kt
echo "########## C. original JsonArray iterator + JsonObject Entry (element nullability) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.java 2>/dev/null | grep -n 'iterator\|Iterable\|JsonArrayIterator\|JsonEntry\|class '
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.java 2>/dev/null | grep -n 'iterator\|Iterable\|Map.Entry\|JsonEntry\|class \|getKey\|getValue'
