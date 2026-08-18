#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. isEmpty: property or method? (JsonObject/JsonArray/JsonValue) ====="
grep -rn 'val isEmpty\|fun isEmpty' src/main/java/org/quantumbadger/redreader/jsonwrap/*.kt
echo "===== 2. Optional.orElse + get + orElseNull signatures ====="
grep -n 'fun orElse\|fun get\|fun orElseNull\|fun isPresent\|fun ifPresent\|fun apply\|companion object\|fun <T> empty\|fun <T> of\|val isEmpty\|fun isEmpty' src/main/java/org/quantumbadger/redreader/common/Optional.kt
echo "===== 3. ReportDialog.kt L95-120 (report call site) ====="
awk 'NR>=95 && NR<=120 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/ReportDialog.kt
echo "===== 4. PostSubmitContentFragment L368-400 (FlairSelector onSuccess body) ====="
awk 'NR>=368 && NR<=400 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "===== 5. RedditSubredditSubscriptionManager L315-350 (subscriptionAction callers) ====="
awk 'NR>=315 && NR<=350 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/api/RedditSubredditSubscriptionManager.kt
echo "===== 6. RedditAPI subscriptionAction FULL call L475-507 (4th arg?) ====="
awk 'NR>=475 && NR<=507 {printf "%d| %s\n", NR, $0}' $f
echo "===== 7. PostListingFragment L290-360 (subredditHandler) ====="
awk 'NR>=290 && NR<=360 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt
echo "===== 8. EXACT external callers of RedditAPI static fns ====="
grep -rn 'RedditAPI\.\(popularSubreddits\|searchSubreddits\|subscribedSubreddits\|subscriptionAction\|flairSelectorForNewLink\)' src/main/java --include='*.kt'
echo "===== 9. UpdatedVersionListener def ====="
grep -rn 'interface UpdatedVersionListener\|fun onVersionUpdated\|fun onUpdate' src/main/java --include='*.kt' | grep -i updatedversion | head
FF=$(grep -rln 'interface UpdatedVersionListener' src/main/java --include='*.kt' | head -1); echo "FILE=$FF"
grep -n 'interface UpdatedVersionListener' -A6 "$FF" 2>/dev/null
echo "===== 10. RawObjectDB + ThreadedRawObjectDB class headers (type params) ====="
grep -n 'class RawObjectDB\|class ThreadedRawObjectDB' src/main/java/org/quantumbadger/redreader/io/RawObjectDB.kt src/main/java/org/quantumbadger/redreader/io/ThreadedRawObjectDB.kt
echo "===== 11. WeakCache header (20-40) ====="
awk 'NR>=20 && NR<=40 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/io/WeakCache.kt
echo "===== 12. subredditDb decl RedditSubredditManager L55-72 ====="
awk 'NR>=55 && NR<=72 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
