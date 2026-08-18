#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. RequestResponseHandler base (find + class decl + method sigs) ====="
grep -rln 'abstract class RequestResponseHandler\|interface RequestResponseHandler\|open class RequestResponseHandler' src/main/java --include='*.kt'
for ff in $(grep -rln 'class RequestResponseHandler\|interface RequestResponseHandler' src/main/java --include='*.kt'); do echo "FILE: $ff"; grep -n 'class RequestResponseHandler\|interface RequestResponseHandler\|fun onRequestSuccess\|fun onRequestFailed\|out E\|<E\|<F\|<out' "$ff" | head -20; done
echo "===== 2. ValueResponseHandler base (APIResponseHandler.kt L103-160) ====="
awk 'NR>=103 && NR<=170 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
echo "===== 3. Objects.requireNonNull signature ====="
for ff in $(grep -rln 'requireNonNull' src/main/java/org/quantumbadger/redreader/util --include='*.kt'); do echo "FILE: $ff"; grep -n 'fun.*requireNonNull' "$ff"; done
grep -rn 'fun.*requireNonNull' src/main/java --include='*.kt' | head
echo "===== 4. RedditSubreddit.name (current + original Java) ====="
grep -n 'name' src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.kt | head
git show $B:src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.java 2>/dev/null | grep -n 'name\|String name' | head
echo "===== 5. RedditFlairChoice.fromJsonList body (L63-110) ====="
awk 'NR>=63 && NR<=110 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
echo "===== 6. FlairSelectorResponseHandler.onSuccess (find + sig) ====="
grep -rn 'FlairSelectorResponseHandler' src/main/java --include='*.kt' | head -3
echo "===== 7. original Java getSubreddit (RedditSubredditManager) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | grep -n 'getSubreddit' -A12 | head -24
echo "===== 8. WeakCache.performRequest sig (current) ====="
grep -rln 'class WeakCache' src/main/java --include='*.kt'
for ff in $(grep -rln 'class WeakCache' src/main/java --include='*.kt'); do grep -n 'fun performRequest\|class WeakCache\|out E\|<E\|<K' "$ff" | head -15; done
echo "===== 9. SubmitResponseHandler original Java onSuccess ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.java 2>/dev/null | grep -n 'onSuccess' -A4 | head -30
