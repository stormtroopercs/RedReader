#!/bin/bash
cd /opt/data/redreader-project/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. Objects.requireNonNull definition (all of them) ====="
grep -rn 'fun.*requireNonNull' src/main/java --include='*.kt'
echo "===== 1b. original Java L~ for getFlairSelectorChoices (the asObject().isEmpty) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'asObject\|isEmpty\|requireNonNull\|getFlairSelectorChoices' | head -20
echo "===== 2. fromJsonList CALLERS (whole repo) ====="
grep -rn 'fromJsonList' src/main/java --include='*.kt'
echo "===== 3. getSubreddit / getSubreddits CALLERS (whole repo) ====="
grep -rn '\.getSubreddit(\|\.getSubreddits(' src/main/java --include='*.kt'
echo "===== 4. subredditCache declaration (RedditSubredditManager) ====="
grep -n 'subredditCache' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
echo "===== 5. requestSubredditList CALLERS ====="
grep -rn 'requestSubredditList\|searchSubreddits\|subscribedSubreddits\|popularSubreddits' src/main/java --include='*.kt' | grep -v 'fun '
echo "===== 6. SubredditListResponse.after / .subreddits usages ====="
grep -rn '\.after\b\|SubredditListResponse' src/main/java --include='*.kt' | grep -v 'class SubredditListResponse'
echo "===== 7. SubmitResponseHandler onSuccess CALLERS/implementers ====="
grep -rn 'SubmitResponseHandler\|onSuccess(' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt | head
echo "===== 8. WeakCache single-key performRequest full sig (L110-125) ====="
awk 'NR>=108 && NR<=128 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/io/WeakCache.kt
echo "===== 9. original Java: getSubreddit handler param + subredditCache type ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | grep -n 'subredditCache\|WeakCache' | head
echo "===== 10. original Java requestSubredditList handler param (L827-870) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | sed -n '827,870p'
