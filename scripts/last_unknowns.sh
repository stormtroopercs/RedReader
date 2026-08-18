#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. RedditAPI L475-510 (full getSubreddit call incl 4th arg) ====="
awk 'NR>=475 && NR<=510 {printf "%d| %s\n", NR, $0}' $f
echo "===== 2. RedditSubredditSubscriptionManager L315-350 (subscriptionAction callers + subredditId arg) ====="
awk 'NR>=315 && NR<=350 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/api/RedditSubredditSubscriptionManager.kt
echo "===== 3. Optional.kt FULL ====="
cat src/main/java/org/quantumbadger/redreader/common/Optional.kt
echo "===== 4. RedditAPI L1100-1132 (SubmitJSONListener errors building) ====="
awk 'NR>=1100 && NR<=1132 {printf "%d| %s\n", NR, $0}' $f
echo "===== 5. sendReplies signature ====="
grep -n 'fun sendReplies' -A12 $f | head -16
echo "===== 6. CURRENT error state of direct callers (from compile log) ====="
echo "--- ReportDialog ---"; grep '^e:.*ReportDialog' /tmp/compile.log | sed 's|.*ReportDialog.kt:||'
echo "--- PostSubmitContentFragment ---"; grep '^e:.*PostSubmitContentFragment' /tmp/compile.log | sed 's|.*PostSubmitContentFragment.kt:||'
echo "--- PostListingFragment ---"; grep '^e:.*PostListingFragment' /tmp/compile.log | sed 's|.*PostListingFragment.kt:||'
echo "--- RedditSubredditManager ---"; grep '^e:.*RedditSubredditManager' /tmp/compile.log | sed 's|.*RedditSubredditManager.kt:||'
echo "--- RedditSubredditSubscriptionManager ---"; grep '^e:.*RedditSubredditSubscriptionManager' /tmp/compile.log | sed 's|.*RedditSubredditSubscriptionManager.kt:||'
echo "--- APIResponseHandler ---"; grep '^e:.*APIResponseHandler' /tmp/compile.log | sed 's|.*APIResponseHandler.kt:||'
echo "--- RedditFlairChoice ---"; grep '^e:.*RedditFlairChoice' /tmp/compile.log | sed 's|.*RedditFlairChoice.kt:||'
echo "--- PostPropertiesDialog / other PostField callers ---"; grep '^e:.*PostPropertiesDialog' /tmp/compile.log | sed 's|.*PostPropertiesDialog.kt:||' | head
echo "===== 7. getAll errors total (sanity) ====="
grep -c '^e:' /tmp/compile.log
echo "===== 8. getSubreddit 4th arg in RedditAPI: search 'updatedVersionListener\|null' near L475-507 ====="
awk 'NR>=475 && NR<=510 {printf "%d| %s\n", NR, $0}' $f | grep -i 'listener\|null\|)'
