#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. JsonObject.kt FULL ##########"
cat src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.kt
echo "########## B. L1000-1012 (the createPostRequest call at 1007) ##########"
awk 'NR>=1000 && NR<=1012 {printf "%d| %s\n", NR, $0}' $f
