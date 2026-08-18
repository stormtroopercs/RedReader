#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. RequestResponseHandler base: find file + full def ====="
grep -rln 'class RequestResponseHandler' src/main/java --include='*.kt'
grep -rn 'class RequestResponseHandler' src/main/java --include='*.kt'
FF=$(grep -rln 'class RequestResponseHandler' src/main/java --include='*.kt' | head -1)
LN=$(grep -n 'class RequestResponseHandler' "$FF" | head -1 | cut -d: -f1)
echo "FILE=$FF LINE=$LN"
awk -v s=$LN 'NR>=s && NR<=s+30 {printf "%d| %s\n", NR, $0}' "$FF"
echo "===== 2. ORIGINAL Java: RequestResponseHandler def ====="
for c in APIResponseHandler RequestResponseHandler; do
  JF=$(git show $B:src/main/java/org/quantumbadger/redreader/reddit/$c.java 2>/dev/null)
  if [ -n "$JF" ]; then echo "--- $c.java ---"; echo "$JF" | grep -n 'class RequestResponseHandler' -A15 | head -25; fi
done
echo "===== 3. ORIGINAL Java: RedditSubreddit.java name field ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubreddit.java 2>/dev/null | grep -n 'name\|class RedditSubreddit\|implements\|@NonNull' | head -20
echo "===== 4. ORIGINAL Java: getSubreddit + WeakCache type args ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | grep -n 'subredditCache\|WeakCache<\|void getSubreddit\|RequestResponseHandler<RedditSubreddit' | head -15
echo "===== 5. ORIGINAL Java: fromJsonList + FlairSelectorResponseHandler ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.java 2>/dev/null | grep -n 'fromJsonList\|Optional<List<RedditFlairChoice' | head
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'interface FlairSelectorResponseHandler' -A8 | head -15
echo "===== 6. ORIGINAL Java: SubmitResponseHandler + onSuccess(redirect,thing) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.java 2>/dev/null | grep -n 'class SubmitResponseHandler' -A15 | head -25
echo "===== 7. ORIGINAL Java: subscribedSubreddits + after Optional<String> ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'void subscribedSubreddits\|subscribedSubredditsInternal\|Optional<String> after\|new SubredditListResponse\|ArrayList<RedditSubreddit> results' | head
echo "===== 8. WritableObject: does RedditSubreddit implement it? current ====="
grep -rn 'WritableObject' src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.kt | head
grep -rn 'interface WritableObject' src/main/java --include='*.kt' | head -3
