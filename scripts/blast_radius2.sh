#!/bin/bash
cd /opt/data/redreader-project/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 0. CURRENT error counts in downstream files ====="
for fn in PostListingFragment PostSubmitContentFragment RedditSubredditManager RedditSubredditSubscriptionManager MainMenuFragment RedditAPIV2 RedgifsAPIV2 ImageInfo AnnouncementDownloader NetWrapper; do
  c=$(grep -c "^e:.*$fn" /tmp/compile.log)
  echo "$fn: $c"
done
echo "===== 1. function signatures enclosing postFields in RedditAPI.kt ====="
grep -n 'fun ' $f | awk -F: '$1 >= 185 && $1 <= 680'
echo "===== 2. submitPost sig (L190-210) ====="
awk 'NR>=190 && NR<=212 {printf "%d| %s\n", NR, $0}' $f
echo "===== 3. RedditIdAndType.value ====="
FF=$(find src -name 'RedditIdAndType.kt' | head -1); echo "FILE=$FF"
grep -n 'value\|class RedditIdAndType' "$FF" | head -8
echo "===== 4. PostListingFragment subredditHandler decl ====="
grep -n 'subredditHandler' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt | head
echo "===== 5. external callers of popularSubreddits/searchSubreddits/subscribedSubreddits ====="
grep -rn 'popularSubreddits(\|searchSubreddits(\|subscribedSubreddits(' src/main/java --include='*.kt' | grep -v 'fun \|RedditAPI.kt'
echo "===== 6. SubmitJSONListener errors construction (L1100-1130) ====="
awk 'NR>=1100 && NR<=1130 {printf "%d| %s\n", NR, $0}' $f
echo "===== 7. Consumer import in RedditAPI.kt ====="
grep -n 'import.*Consumer\|Consumer' $f | head -6
echo "===== 8. Optional.ifPresent signature ====="
grep -n 'fun ifPresent' -A4 src/main/java/org/quantumbadger/redreader/common/Optional.kt
echo "===== 9. ORIGINAL Java: subscriptionAction sig + subreddit param ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | sed -n '557,600p'
echo "===== 10. ORIGINAL Java: subscribedSubreddits sig ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | sed -n '746,762p'
echo "===== 11. ORIGINAL Java: RedditSubreddit.java location + name field ====="
git ls-tree -r $B --name-only | grep -i 'RedditSubreddit.java'
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubreddit.java 2>/dev/null | grep -n 'name' | head
echo "===== 12. ORIGINAL Java: submitPost + sendMessage + submitComment param annotations (sample) ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'public static void submitPost\|public static void sendMessage\|public static void submitComment\|public static void blockUser\|public static void unblockUser\|public static void hidePost\|public static void sendReplies\|public static void subscriptionPrepareActionUri\|public static void subscriptionAction' 
echo "===== 13. PostField call at L209-213 (the multiline one) ====="
awk 'NR>=209 && NR<=213 {printf "%d| %s\n", NR, $0}' $f
echo "===== 14. L440-460 (subscriptionPrepareActionUri + its callers) ====="
awk 'NR>=440 && NR<=466 {printf "%d| %s\n", NR, $0}' $f
