#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. fromJsonList FULL (RedditFlairChoice.kt 55-95) ====="
awk 'NR>=55 && NR<=95 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
echo "===== 2. PostField + PostFields class defs (search) ====="
grep -rn 'class PostField\|class PostFields' src/main/java --include='*.kt'
echo "===== 3. RequestResponseHandler FULL def (APIResponseHandler.kt) ====="
AF=src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
awk '/class RequestResponseHandler/,/^    }/' $AF | head -40
echo "===== 4. ValueResponseHandler onSuccess ====="
grep -n 'class ValueResponseHandler' -A18 $AF
echo "===== 5. getSubreddit callers (all) ====="
grep -rn '\.getSubreddit(' src/main/java --include='*.kt' | grep -v 'RedditSubredditManager.kt'
echo "===== 6. RedditSubreddit file + name prop ====="
grep -rln 'class RedditSubreddit' src/main/java --include='*.kt'
grep -rn 'class RedditSubreddit' src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.kt 2>/dev/null
grep -rn 'name' src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.kt 2>/dev/null | head -5
echo "===== 7. postFields local near 1006 (createPostRequest 960-1010) ====="
awk 'NR>=960 && NR<=1010 {printf "%d| %s\n", NR, $0}' $f
echo "===== 8. requestSubredditList callers ====="
grep -rn 'requestSubredditList(' src/main/java --include='*.kt' | grep -v 'fun requestSubredditList'
echo "===== 9. WeakCache class decl (type params) ====="
grep -rn 'class WeakCache' src/main/java/org/quantumbadger/redreader/io/WeakCache.kt 2>/dev/null || grep -rln 'class WeakCache' src/main/java --include='*.kt'
