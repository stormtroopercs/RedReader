#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. PostField ctor + PostFields ctor ##########"
grep -rn 'class PostField' src/main/java/org/quantumbadger/redreader/http/PostField.kt
awk 'NR>=1 && /class PostField/ {print NR": "$0}' src/main/java/org/quantumbadger/redreader/http/PostField.kt
echo "--- PostField body (ctor) ---"
sed -n "$(grep -n 'class PostField' src/main/java/org/quantumbadger/redreader/http/PostField.kt | head -1 | cut -d: -f1),+12p" src/main/java/org/quantumbadger/redreader/http/PostField.kt
echo "--- PostFields class (search) ---"
grep -rln 'class PostFields' src/main/java --include='*.kt'
echo "########## B. SubredditListResponse class ##########"
grep -rln 'class SubredditListResponse' src/main/java --include='*.kt'
F=$(grep -rln 'class SubredditListResponse' src/main/java --include='*.kt' | head -1)
sed -n '1,60p' "$F" | grep -nE 'class SubredditListResponse|after|subreddits|Optional'
echo "########## C. createPostRequest signature (L985-1010) ##########"
awk 'NR>=985 && NR<=1010 {printf "%d| %s\n", NR, $0}' $f
echo "########## D. L1100-1145 containing fn (handler type) ##########"
awk 'NR>=1095 && NR<=1145 {printf "%d| %s\n", NR, $0}' $f
