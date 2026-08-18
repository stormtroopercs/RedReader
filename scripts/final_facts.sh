#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. Optional.kt: get/orElse/empty/of signatures ====="
grep -n 'fun get\|fun orElse\|fun empty\|fun of\|fun isPresent\|fun isEmpty\|fun apply\|fun map\|fun filter\|class Optional' src/main/java/org/quantumbadger/redreader/common/Optional.kt
echo "===== 2. PostListingFragment L330-375 (direct getSubreddit caller) ====="
awk 'NR>=330 && NR<=375 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt
echo "===== 3. RedditSubredditManager.kt L40-130 (ctor + getSubreddit + getSubreddits) ====="
awk 'NR>=40 && NR<=130 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
echo "===== 4. RawObjectDB class decl + Requester interface ====="
grep -n 'class RawObjectDB\|interface Requester\|fun performRequest\|fun load\|<K\|<V\|<F' src/main/java/org/quantumbadger/redreader/io/RawObjectDB.kt | head -25
echo "===== 5. ThreadedRawObjectDB class decl ====="
grep -n 'class ThreadedRawObjectDB\|fun performRequest\|<K\|<V\|<F\|Requester' src/main/java/org/quantumbadger/redreader/io/ThreadedRawObjectDB.kt | head -25
echo "===== 6. ORIGINAL Java RedditSubredditManager (full, it is small) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null
echo "===== 7. ORIGINAL Java: function enclosing the getSubreddit call (subscriptionAction, L540-570) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | awk 'NR>=540 && NR<=575'
echo "===== 8. ORIGINAL Java: report (reasonFields) + subreddit-list wrappers (L740-880) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | awk 'NR>=740 && NR<=826'
echo "===== 9. Kotlin callers of report( / getSubreddits( ====="
grep -rn '\.report(\|\.getSubreddits(\|RedditAPI.Companion.report\|Companion\.getSubreddits' src/main/java --include='*.kt' | head
echo "===== 10. PostSubmitContentFragment L481-535 (SubmitResponseHandler anon) ====="
awk 'NR>=481 && NR<=535 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "===== 11. OTHER SubmitResponseHandler implementers ====="
grep -rn ': SubmitResponseHandler\|SubmitResponseHandler(' src/main/java --include='*.kt' | grep -v 'import'
echo "===== 12. OTHER FlairSelectorResponseHandler implementers ====="
grep -rn 'FlairSelectorResponseHandler' src/main/java --include='*.kt' | grep -v 'import\|fun flairSelector\|responseHandler: FlairSelector'
