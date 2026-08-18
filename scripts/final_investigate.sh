#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. FlairSelectorResponseHandler interface (RedditAPI.kt L1091-1110) ====="
awk 'NR>=1091 && NR<=1112 {printf "%d| %s\n", NR, $0}' $f
echo "===== 2. original Java FlairSelectorResponseHandler ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'FlairSelectorResponseHandler' -A8 | head -20
echo "===== 3. createPostRequest CALLERS (who passes postFields) ====="
grep -n 'createPostRequest\|createPostRequestUnprocessedResponse' $f
echo "===== 4. all postFields decls in RedditAPI.kt ====="
grep -n 'postFields\|PostField?\|PostField>' $f
echo "===== 5. subscriptionPrepareActionUri + L487-492 (postFields in getSubreddit anon) ====="
awk 'NR>=485 && NR<=500 {printf "%d| %s\n", NR, $0}' $f
grep -n 'subscriptionPrepareActionUri' $f
echo "===== 6. SubredditListResponse internal: output + after locals (L840-862) ====="
awk 'NR>=838 && NR<=862 {printf "%d| %s\n", NR, $0}' $f
echo "===== 7. asSubreddit() return type ====="
grep -rn 'fun asSubreddit' src/main/java --include='*.kt'
git show $B:src/main/java/org/quantumbadger/redreader/reddit/things/RedditThing.java 2>/dev/null | grep -n 'asSubreddit' -A3
echo "===== 8. subredditDbWrapper type (RedditSubredditManager) ====="
grep -n 'subredditDbWrapper\|RawObjectDB\|SubredditDbWrapper' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head
echo "===== 9. SubmitResponseHandler implementers onSuccess (PostSubmitContentFragment L498-515) ====="
awk 'NR>=498 && NR<=515 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "===== 10. original Java subscription postFields (around subscriptionPrepareActionUri / prepareActionUri) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'prepareActionUri\|PostField\|LinkedList' | head -20
echo "===== 11. PostField class (ctor) ====="
grep -rn 'class PostField' src/main/java --include='*.kt'
grep -n 'class PostField' -A14 src/main/java/org/quantumbadger/redreader/http/body/HTTPRequestBody.kt 2>/dev/null | head -18
echo "===== 12. original Java getSubreddit caller (subscriptionAction in RedditAPI) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'getSubreddit\|subscriptionPrepareActionUri\|subreddit.name\|prepareActionUri' -A3 | head -30
