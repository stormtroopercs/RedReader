#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. subredditDbWrapper type + subredditDbWrapper decl (RedditSubredditManager) ====="
grep -n 'subredditDbWrapper\|RawObjectDB\|ThreadedRawObjectDB\|PermanentCache' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head
echo "===== 2. WeakCache class header + performRequest sigs (io/WeakCache.kt) ====="
awk 'NR>=20 && NR<=140 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/io/WeakCache.kt 2>/dev/null | grep -n 'class WeakCache\|fun performRequest\|fun performWrite\|RequestResponseHandler\|RawObjectDB\|ThreadedRawObjectDB'
echo "===== 3. PostListingFragment subredditHandler decl + type (L300-355) ====="
awk 'NR>=300 && NR<=355 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt | grep -n 'subredditHandler\|RequestResponseHandler\|RedditSubreddit'
echo "===== 4. searchSubreddits signature (RedditAPI) ====="
grep -n 'fun searchSubreddits' -A12 $f
echo "===== 5. subscribedSubreddits + searchSubreddits callers (external) ====="
grep -rn '\.searchSubreddits(\|\.subscribedSubreddits(' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt'
echo "===== 6. PostSubmitContentFragment FlairSelector impl (L370-400) ====="
awk 'NR>=370 && NR<=400 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "===== 7. PostSubmitContentFragment SubmitResponseHandler impl (L478-520) ====="
awk 'NR>=478 && NR<=520 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "===== 8. ORIGINAL Java: RedditSubreddit.java (name field + class header) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubreddit.java 2>/dev/null | sed -n '1,60p' | grep -n 'class RedditSubreddit\|name\|@NonNull\|String '
echo "===== 9. ORIGINAL Java: searchSubreddits + subscribedSubreddits handler types ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'void searchSubreddits\|void subscribedSubreddits\|ValueResponseHandler<SubredditListResponse>\|ValueResponseHandler<ArrayList<RedditSubreddit>' | head
echo "===== 10. RawObjectDB / ThreadedRawObjectDB type params ====="
grep -n 'class RawObjectDB\|class ThreadedRawObjectDB' src/main/java/org/quantumbadger/redreader/io/*.kt
echo "===== 11. PostFields: which constructor is used at L1006 (postFields: LinkedList<PostField?>) ====="
grep -n 'PostFields(' $f
echo "===== 12. FunctionOneArgNoReturn + Optional.apply ====="
grep -n 'fun apply' src/main/java/org/quantumbadger/redreader/common/Optional.kt
