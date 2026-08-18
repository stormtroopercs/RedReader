#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## 850-920 (for-loops + key/value + 916) ##########"
sed -n '850,920p' $f
echo "########## 1032-1046 (@IntDef annotation) ##########"
sed -n '1032,1046p' $f
echo "########## requestSubredditList signature ##########"
grep -n 'fun requestSubredditList' $f
