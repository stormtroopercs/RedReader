#!/bin/bash
cd /opt/data/RedReader
echo "########## A. JsonArray.kt full (40-151) ##########"
sed -n '40,151p' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.kt
echo "########## B. ORIGINAL JsonArray: Iterable + mContents ##########"
git show 1d35f61e:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.java > /tmp/JsonArray.java 2>/dev/null
grep -n 'Iterable\|ArrayList<JsonValue\|Iterator<JsonValue\|implements' /tmp/JsonArray.java | head
echo "########## C. ORIGINAL JsonValue getAtPathInternal + get*AtPath full (150-200) ##########"
awk 'NR>=150 && NR<=200 {printf "%d| %s\n", NR, $0}' /tmp/JsonValue.java
echo "########## D. explicit Optional<Json*?> val types at external callers ##########"
grep -rn 'Optional<Json\|Optional<JsonValue\|Optional<JsonArray\|Optional<JsonObject' src/main/java --include='*.kt' | grep -v 'jsonwrap/'
echo "########## E. current error counts in the affected external files ##########"
for x in compose/net/NetWrapper receivers/announcements/AnnouncementDownloader image/ImageInfo image/RedgifsAPIV2; do printf "%s: " "$x"; grep -c "^e:.*$x.kt" /tmp/compile.log; done
echo "########## F. JsonArray.getAtPathInternal override? ##########"
grep -n 'getAtPathInternal' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.kt src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
