#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. SubredditListResponse class (L1165-1185) ====="
awk 'NR>=1165 && NR<=1185 {printf "%d| %s\n", NR, $0}' $f
echo "===== 2. PostFields ctor (HTTPRequestBody.kt L35-70) ====="
awk 'NR>=35 && NR<=70 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/http/body/HTTPRequestBody.kt
echo "===== 3. SubmitResponseHandler (APIResponseHandler.kt L70-100) ====="
awk 'NR>=70 && NR<=100 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
echo "===== 4. ValueResponseHandler base (find) ====="
grep -n 'class ValueResponseHandler\|abstract class ValueResponseHandler' src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
echo "===== 5. function with mResponseHandler at L1139: find the enclosing fun (L1020-1050) ====="
awk 'NR>=1020 && NR<=1050 {printf "%d| %s\n", NR, $0}' $f
echo "===== 6. mResponseHandler declaration + onSuccess of SubmitResponseHandler ====="
grep -n 'mResponseHandler' $f | head
echo "===== 7. ORIG Java fromJsonList + SubredditListResponse + createPostRequest ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.java 2>/dev/null | grep -n 'fromJsonList' -A4
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'class SubredditListResponse\|SubredditListResponse(' -A10 | head -20
