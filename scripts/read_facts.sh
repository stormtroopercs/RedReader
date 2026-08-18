#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
p() { awk -v a=$1 -v b=$2 'NR>=a && NR<=b {printf "%d| %s\n", NR, $0}' $f; }
echo "===== A. choices declaration + fromJsonList (L125-166) ====="; p 125 166
echo "===== B. createPostRequest signature (L960-1000) ====="; p 960 1000
echo "===== C. function containing L1139: mResponseHandler type (L1050-1139 head) ====="; p 1050 1080
echo "===== D. SubredditListResponse class def ====="
grep -n 'class SubredditListResponse' $f
echo "===== E. SubmitResponseHandler def ====="
grep -rn 'interface SubmitResponseHandler\|class SubmitResponseHandler' src/main/java --include='*.kt' | head -3
echo "===== F. PostFields ctor ====="
grep -rn 'class PostFields' src/main/java --include='*.kt' | head -3
grep -n 'class PostFields' -A12 src/main/java/org/quantumbadger/redreader/reddit/PostFields.kt 2>/dev/null | head -16
echo "===== G. getSubreddit current (RedditSubredditManager) ====="
grep -n 'fun getSubreddit' -A16 src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head -20
echo "===== H. fromJsonList (RedditFlairChoice) ====="
grep -rn 'fun fromJsonList' src/main/java --include='*.kt'
echo "===== I. getMultireddits / searchSubreddits handler params in RedditAPI ====="
grep -n 'RequestResponseHandler<\|ValueResponseHandler<' $f
