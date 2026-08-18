#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. ORIGINAL JsonValue: Optional return types ##########"
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.java > /tmp/JsonValue.java 2>/dev/null
grep -n 'getArrayAtPath\|getStringAtPath\|getObjectAtPath\|getAtPath' /tmp/JsonValue.java | head
echo "---- bodies ----"
awk '/public Optional getArrayAtPath/,/^\t}/' /tmp/JsonValue.java | head -14
awk '/public Optional getStringAtPath/,/^\t}/' /tmp/JsonValue.java | head -14
echo "########## B. ORIGINAL JsonObject: properties map + iterator ##########"
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.java > /tmp/JsonObject.java 2>/dev/null
grep -n 'HashMap\|Iterable\|iterator\|public JsonValue get\|Entry' /tmp/JsonObject.java | head
echo "########## C. ORIGINAL getUriBuilder (already seen: non-null Uri.Builder) ##########"
echo "########## D. ORIGINAL getSubreddit + fromJsonList + requestSubredditList ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java > /tmp/RSM.java 2>/dev/null
grep -n -A6 'public void getSubreddit(' /tmp/RSM.java | head -12
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.java > /tmp/RFC.java 2>/dev/null
grep -n -A3 'fromJsonList' /tmp/RFC.java | head -8
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java > /tmp/RAPI.java 2>/dev/null
grep -n 'requestSubredditList\|ValueResponseHandler' /tmp/RAPI.java | head -12
echo "########## E. JsonArray.kt: size() + full head ##########"
sed -n '1,40p' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.kt
echo "########## F. working @IntDef examples elsewhere (files with 0 errors?) ##########"
grep -rn '@IntDef' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt' | head -8
echo "########## G. requestSubredditList call sites ##########"
grep -rn 'requestSubredditList(' src/main/java --include='*.kt'
echo "########## H. 985-1008 full makeRequest call ##########"
awk 'NR>=985 && NR<=1008 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## I. CacheRequest ctor / createPostRequest ##########"
grep -n 'fun createPostRequest\|fun createGetRequest' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
grep -n 'class CacheRequest\|constructor' src/main/java/org/quantumbadger/redreader/cache/CacheRequest.kt | head -6
