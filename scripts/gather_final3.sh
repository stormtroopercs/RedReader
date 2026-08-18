#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== A. where is RequestResponseHandler declared? ====="
grep -rln 'RequestResponseHandler<' src/main/java --include='*.kt' | head
grep -rn 'interface RequestResponseHandler\|class RequestResponseHandler\|abstract class RequestResponseHandler' src/main/java --include='*.kt'
echo "===== B. PostListingFragment handler decl (search subredditHandler) ====="
grep -n 'subredditHandler\|RequestResponseHandler\|RequestResponseHandler<\|RedditSubreddit' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt | head -20
echo "===== C. Optional.apply / orElse / get / ifPresent (common/Optional.kt) ====="
grep -n 'fun apply\|fun orElse\|fun get\|fun ifPresent\|fun isPresent\|companion object\|fun <T> empty\|fun <T> of' src/main/java/org/quantumbadger/redreader/common/Optional.kt
echo "===== D. RedditSubreddit name usages (setters) ====="
grep -rn '\.name = \|\.name=' src/main/java --include='*.kt' | grep -i subreddit | head
grep -rn 'subreddit.name\|\.name = name\|name = ' src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.kt | head
echo "===== E. subredditDbWrapper type ====="
grep -n 'subredditDbWrapper\|RawObjectDB\|ThreadedRawObjectDB\|PermanentCache\|RawObjectDBWrapper' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head
echo "===== F. WeakCache header + performRequest + RawObjectDB type param ====="
grep -n 'class WeakCache\|fun performRequest\|fun performWrite\|RawObjectDB\|: RawObjectDB\|dbWrapper\|RequestResponseHandler' src/main/java/org/quantumbadger/redreader/io/WeakCache.kt | head -20
echo "===== G. PostSubmitContentFragment FlairSelector (L370-395) ====="
awk 'NR>=370 && NR<=395 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "===== H. PostSubmitContentFragment SubmitResponseHandler impl (L478-515) ====="
awk 'NR>=478 && NR<=515 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "===== I. PostListingFragment L330-355 ====="
awk 'NR>=330 && NR<=355 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt
