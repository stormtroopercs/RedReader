#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. RedditIdAndType.value type ##########"
FF=$(find src -name 'RedditIdAndType.kt' | head -1); echo "file: $FF"
grep -n 'value\|class RedditIdAndType' "$FF" | head -6
echo "########## B. usernameToUnblock / usernameToBlock param types ##########"
grep -n 'usernameToUnblock\|usernameToBlock' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt | head
echo "########## C. original Java: usernameToUnblock/Block + report reasonFields + getSubreddit handler ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'usernameToUnblock\|usernameToBlock\|reasonFields\|subreddit.name' | head
echo "########## D. WeakCache.performRequest (the cache base) ##########"
grep -rn 'fun performRequest' src/main/java/org/quantumbadger/redreader/common/WeakCache.kt 2>/dev/null
find src -name 'WeakCache.kt' | head -1 | xargs grep -n 'fun performRequest' 2>/dev/null
echo "########## E. getSubreddit's updatedVersionListener param + performRequest arg (92-108) ##########"
awk 'NR>=92 && NR<=108 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
