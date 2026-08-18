#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. JsonObject.kt current 212-250 (TYPE + properties + iterator) ##########"
awk 'NR>=212 && NR<=250 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.kt
echo "########## B. ORIGINAL Java JsonObject: properties decl + reflective TYPE checks ##########"
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.java 2>/dev/null | grep -n 'properties\s*=\|properties;\|Long.TYPE\|Double.TYPE\|Float.TYPE\|Int.TYPE\|Boolean.TYPE\|long.class\|int.class\|double.class\|float.class\|boolean.class\|HashMap<String'
echo "########## C. RedditFlairChoice.fromJsonList return + ORIGINAL ##########"
grep -n 'fun fromJsonList' -A3 src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.java 2>/dev/null | grep -n 'fromJsonList' -A2
echo "########## D. SubredditListResponse (1168-1171) + ORIGINAL ##########"
awk 'NR>=1168 && NR<=1171 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'class SubredditListResponse' -A9
echo "########## E. RedditAPI L1130-1145 (onSuccess at 1139/1141) - the mResponseHandler receiver ##########"
awk 'NR>=1125 && NR<=1145 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## F. what is mResponseHandler's onSuccess signature? ##########"
grep -n 'mResponseHandler\|class.*ResponseHandler\|onSuccess(' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt | grep -i 'onSuccess\|mResponseHandler =' | head
