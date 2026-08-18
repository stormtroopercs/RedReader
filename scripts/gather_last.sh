#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. RequestResponseHandler base class FULL ====="
grep -rn 'class RequestResponseHandler' src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
AF=src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
LN=$(grep -n 'class RequestResponseHandler' $AF | head -1 | cut -d: -f1)
awk -v s=$LN 'NR>=s-2 && NR<=s+25 {printf "%d| %s\n", NR, $0}' $AF
echo "===== 2. RedditFlairChoice class header + constructor (1-30) ====="
awk 'NR>=1 && NR<=30 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
echo "===== 3. PostFields constructor (HTTPRequestBody.kt 35-70) ====="
awk 'NR>=35 && NR<=75 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/http/body/HTTPRequestBody.kt
echo "===== 4. SubmitJSONListener: L287-300 ====="
awk 'NR>=287 && NR<=300 {printf "%d| %s\n", NR, $0}' $f
echo "===== 5. getSubreddit full body (RedditSubredditManager 92-120) ====="
awk 'NR>=92 && NR<=120 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
echo "===== 6. getStringAtPath signature (JsonValue.kt) ====="
grep -n 'fun getStringAtPath' -A4 src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
echo "===== 7. getStringAtPath callers in RedditAPI that hit Optional<String?> param ====="
grep -rn 'getStringAtPath' $f
echo "===== 8. original Java: PostField ctor + getSubreddit handler type ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'PostField(\|new RequestResponseHandler<\|RequestResponseHandler<RedditSubreddit' | head -20
echo "===== 9. PostField.java original (name/value types) ====="
git show $B:src/main/java/org/quantumbadger/redreader/http/PostField.java 2>/dev/null | head -30
