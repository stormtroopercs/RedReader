#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. CacheRequest PRIMARY ctor (40-100) ##########"
awk 'NR>=40 && NR<=100 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/cache/CacheRequest.kt
echo "########## B. ORIGINAL createPostRequestUnprocessedResponse call (Java) ##########"
grep -n -B2 -A15 'createPostRequestUnprocessedResponse\|new CacheRequest(' /tmp/RAPI.java | grep -A15 'UnprocessedResponse' | head -25
echo "########## C. NetWrapper getUriBuilder usage (405-425) ##########"
awk 'NR>=405 && NR<=425 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/compose/net/NetWrapper.kt
echo "########## D. get*AtPath callers (file:line) ##########"
grep -rn 'getArrayAtPath\|getStringAtPath\|getObjectAtPath' src/main/java --include='*.kt'
echo "########## E. L914-918 exact ##########"
awk 'NR>=913 && NR<=918 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## F. ORIGINAL JsonValue: are asObject/asArray/asString nullable? (annotations) ##########"
awk 'NR>=95 && NR<=140 {printf "%d| %s\n", NR, $0}' /tmp/JsonValue.java
echo "########## G. ORIGINAL RedditSubreddit name field + getName ##########"
grep -n 'name' /tmp/RS.java | head
