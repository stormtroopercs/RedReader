#!/bin/bash
cd /opt/data/RedReader
echo "########## A. callers of popularSubreddits / requestSubredditList / subscribedSubreddits / searchSubreddits (static, on RedditAPI) ##########"
grep -rn 'popularSubreddits(\|requestSubredditList(\|subscribedSubreddits(' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt'
echo "########## B. Consumer type (java.util.function?) ##########"
grep -rn 'import java.util.function.Consumer' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt | head -2
grep -rn 'typealias Consumer\|fun <T> Consumer' src/main/java/org/quantumbadger/redreader/common/ --include='*.kt' | head
echo "########## C. the Consumer at L297 region + sendReplies param (647) ##########"
awk 'NR>=295 && NR<=308 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## D. PostSubmitContentFragment 390-410 (Consumer refs) ##########"
awk 'NR>=390 && NR<=410 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "########## E. who passes Optional<String?> to popularSubreddits after (external) ##########"
grep -rn 'Optional.*empty.*String\|Optional.*of.*String' src/main/java --include='*.kt' | grep -i 'subreddit\|popular' | head
echo "########## F. L508-522 subscriptionPrepareActionUri (postFields param type) ##########"
awk 'NR>=508 && NR<=525 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
