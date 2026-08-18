#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. subscribedSubreddits + subscribedSubredditsInternal + searchSubreddits + popularSubreddits (L673-835) ====="
awk 'NR>=673 && NR<=835 {printf "%d| %s\n", NR, $0}' $f
echo "===== 2. PostListingFragment subredditHandler decl + type ====="
grep -n 'subredditHandler\|RequestResponseHandler<\|onRequestSuccess\|onRequestFailed' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt | head -20
echo "===== 3. RedditAPIIndividualSubredditDataRequester class decl + type params ====="
grep -n 'class RedditAPIIndividualSubredditDataRequester\|: CacheDataSource\|RequestResponseHandler<\|<K\|<V\|<F\|fun performRequest\|onRequestSuccess\|onRequestFailed' src/main/java/org/quantumbadger/redreader/reddit/api/RedditAPIIndividualSubredditDataRequester.kt | head -20
echo "===== 4. offerRawSubredditData callers ====="
grep -rn 'offerRawSubredditData' src/main/java --include='*.kt'
echo "===== 5. getSubreddits callers ====="
grep -rn 'getSubreddits' src/main/java --include='*.kt' | grep -v 'fun getSubreddits'
echo "===== 6. original Java: requestSubredditList/searchSubreddits/popularSubreddits wrappers (L700-760) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | awk 'NR>=700 && NR<=742'
echo "===== 7. ReportDialog L100-120 (report reasonFields) ====="
awk 'NR>=100 && NR<=120 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/ReportDialog.kt
echo "===== 8. original Java report signature ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'public static void report\|reasonFields' | head
echo "===== 9. RawObjectDB ctor (L49-75) ====="
awk 'NR>=49 && NR<=75 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/io/RawObjectDB.kt
