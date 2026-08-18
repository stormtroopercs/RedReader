#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. L287 anon SubmitResponseHandler override (287-330) ##########"
awk 'NR>=287 && NR<=330 {printf "%d| %s\n", NR, $0}' $f
echo "########## B. ALL SubmitResponseHandler impls + their onSuccess/onSubmitErrors overrides (all files) ##########"
grep -rn 'SubmitResponseHandler' src/main/java --include='*.kt' | grep -v 'import'
echo "########## C. JsonArray.getString return type ##########"
grep -n 'fun getString' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.kt
echo "########## D. JsonObject.getString return type ##########"
grep -n 'fun getString' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.kt
echo "########## E. external callers of searchSubreddits/subscribedSubreddits on RedditAPI ##########"
grep -rn 'RedditAPI\.\|\.searchSubreddits(\|\.subscribedSubreddits(' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt' | grep -i 'subreddit'
echo "########## F. L800-818 (recursive call arg for after) ##########"
awk 'NR>=800 && NR<=818 {printf "%d| %s\n", NR, $0}' $f
echo "########## G. L1113 errors decl + L1119 ##########"
awk 'NR>=1111 && NR<=1124 {printf "%d| %s\n", NR, $0}' $f
