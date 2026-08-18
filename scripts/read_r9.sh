#!/bin/bash
cd /opt/data/RedReader
echo "########## A. JsonArray.kt iterator + tail (100-151) ##########"
sed -n '100,151p' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.kt
echo "########## B. JsonArray ORIGINAL: iterator + iterable ##########"
grep -n 'iterator\|Iterable\|mContents' /tmp/JsonArray.java | head
echo "########## C. JsonValue.kt getAtPathInternal + tail (110-151) ##########"
sed -n '110,151p' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
echo "########## D. any subclass overriding iterator/getAtPathInternal ##########"
grep -rn 'override fun iterator\|override fun getAtPathInternal\|: JsonValue()\|: JsonObject()\|: JsonArray()' src/main/java --include='*.kt'
