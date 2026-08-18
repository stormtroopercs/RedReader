#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
AF=src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
echo "===== 1. RequestResponseHandler FULL (find + 30 lines) ====="
LN=$(grep -n 'class RequestResponseHandler' $AF | head -1 | cut -d: -f1); echo "at $LN"
awk -v s=$LN 'NR>=s && NR<=s+30 {printf "%d| %s\n", NR, $0}' $AF
echo "===== 2. FlairSelectorResponseHandler interface (RedditAPI L1085-1100) ====="
awk 'NR>=1085 && NR<=1100 {printf "%d| %s\n", NR, $0}' $f
echo "===== 3. ALL postFields local decls in RedditAPI ====="
grep -n 'postFields = \|val postFields\|var postFields\|postFields: ' $f
echo "===== 4. subscribedSubreddits results decl + subscribedSubredditsInternal (L745-818) ====="
awk 'NR>=745 && NR<=818 {printf "%d| %s\n", NR, $0}' $f
echo "===== 5. original Java subscriptionPrepareAction (subreddit var) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'subscriptionPrepareAction\|void subscriptionAction\|subreddit.name\|new PostField("sr"' | head
echo "===== 6. requestSubredditList call sites L690-700 & L730-742 ====="
awk 'NR>=690 && NR<=700 {printf "%d| %s\n", NR, $0}' $f
awk 'NR>=730 && NR<=742 {printf "%d| %s\n", NR, $0}' $f
echo "===== 7. JsonValue.asObject return type ====="
grep -n 'fun asObject' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
echo "===== 8. getSubreddit callers detail (RedditAPI L475 + PostListingFragment L348) ====="
awk 'NR>=475 && NR<=490 {printf "%d| %s\n", NR, $0}' $f
awk 'NR>=344 && NR<=360 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt
echo "===== 9. SubredditListResponse (L1163-1169) ====="
awk 'NR>=1163 && NR<=1169 {printf "%d| %s\n", NR, $0}' $f
