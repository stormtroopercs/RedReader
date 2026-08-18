#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. RedditAPI L122-175 (choices var + onSuccess context) ====="
awk 'NR>=122 && NR<=175 {printf "%d| %s\n", NR, $0}' $f
echo "===== 2. Objects.requireNonNull definition ====="
grep -rn 'fun <T' src/main/java/org/quantumbadger/redreader/common/*.kt 2>/dev/null | grep -i 'requireNonNull\|nonnull' 
grep -rn 'requireNonNull' src/main/java/org/quantumbadger/redreader/common/ 2>/dev/null | head -5
grep -rn 'requireNonNull' src/main/java/org/quantumbadger/redreader/io/ 2>/dev/null | head -5
echo "===== 3. getSubreddit signature ====="
grep -n 'fun getSubreddit' -A12 src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
echo "===== 4. requestSubredditList signature ====="
grep -n 'private fun requestSubredditList\|fun requestSubredditList' -A12 $f
echo "===== 5. SubredditListResponse class ====="
grep -n 'class SubredditListResponse' -A12 $f
echo "===== 6. submit inner handler (mResponseHandler + SubmitResponseHandler) ====="
grep -n 'mResponseHandler\|SubmitResponseHandler\|fun onSuccess' $f | sed -n '1,40p'
