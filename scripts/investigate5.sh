#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## 1. SubmitResponseHandler class (find + full) ##########"
grep -rn 'class SubmitResponseHandler\|open class SubmitResponseHandler' src/main/java --include='*.kt' | head -2
FF=$(grep -rln 'class SubmitResponseHandler' src/main/java --include='*.kt' | head -1); echo "file: $FF"
[ -n "$FF" ] && awk '/class SubmitResponseHandler/{found=1} found' "$FF" | head -40
echo "#### original Java SubmitResponseHandler ####"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'class SubmitResponseHandler' -A20 | grep -E 'class|onSuccess|Optional|onSubmitErrors|String|context' | head
echo "########## 2. Consumer interface ##########"
FF=$(find src -name 'Consumer.kt' | head -1); echo "file: $FF"
[ -n "$FF" ] && grep -n 'fun interface Consumer\|interface Consumer\|fun accept\|fun ' "$FF" | head
echo "########## 3. PostListingFragment 290-360 (subredditHandler anon) ##########"
awk 'NR>=290 && NR<=360 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt
echo "########## 4. fromJsonList body (RedditFlairChoice.kt 55-95) ##########"
awk 'NR>=55 && NR<=95 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
echo "#### original fromJsonList body ####"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.java 2>/dev/null | sed -n '45,70p'
echo "########## 5. PostSubmitContentFragment 370-420 (FlairSelectorResponseHandler impl onSuccess) ##########"
awk 'NR>=370 && NR<=420 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "#### original FlairSelectorResponseHandler.onSuccess (Java) ####"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'interface FlairSelectorResponseHandler' -A6 | head
echo "########## 6. WeakCache.performWrite + getSubreddits/offerRawSubredditData external callers ##########"
grep -rn 'offerRawSubredditData(\|getSubreddits(' src/main/java --include='*.kt' | grep -v 'fun offerRawSubredditData\|fun getSubreddits'
echo "########## 7. RedditIdAndType.value type ##########"
FF=$(find src -name 'RedditIdAndType.kt' | head -1); echo "file: $FF"
[ -n "$FF" ] && grep -n 'class RedditIdAndType\|val value\|var value\|value:' "$FF" | head
echo "########## 8. getSubreddit external callers (to check handler de-null cascade) ##########"
grep -rn '\.getSubreddit(' src/main/java --include='*.kt'
echo "########## 9. SendReplies signature (the commentFullname param) ##########"
grep -n 'fun sendReplies' -A8 $f | head -12
echo "########## 10. original Java: sendReplies + getSubreddit subredditCanonicalId nullability ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'void sendReplies' -A6 | head
echo "#### getSubreddit original (subredditCanonicalId) ####"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | grep -n 'public void getSubreddit' -A6 | head
