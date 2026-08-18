#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== A. is prepareActionUri / subscriptionPrepareActionUri private? ====="
grep -n 'private fun prepareActionUri\|private fun subscriptionPrepareActionUri\|fun prepareActionUri\|fun subscriptionPrepareActionUri' $f
echo "===== B. report() callers (external) ====="
grep -rn '\.report(\|RedditAPI.report(' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt'
echo "===== C. prepareActionUri / subscriptionPrepareActionUri callers ====="
grep -n 'prepareActionUri(\|subscriptionPrepareActionUri(' $f
echo "===== D. report() body L437-466 ====="
awk 'NR>=437 && NR<=466 {printf "%d| %s\n", NR, $0}' $f
echo "===== E. subscriptionPrepareActionUri body L508-526 ====="
awk 'NR>=508 && NR<=526 {printf "%d| %s\n", NR, $0}' $f
echo "===== F. who calls report with reasonFields (to see arg type) ====="
grep -rn 'reasonFields\|MutableList<PostField\|ArrayList<PostField' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt' | head
echo "===== G. createPostRequest callers (all) ====="
grep -n 'createPostRequest(' $f
echo "===== H. sendReplies / unblockUser / blockUser / hidePost postFields (L582-667) ====="
awk 'NR>=582 && NR<=667 {printf "%d| %s\n", NR, $0}' $f | grep -n 'postFields\|fun \|PostField('
echo "===== I. L95-100 (first postFields, blockUser?) ====="
awk 'NR>=88 && NR<=101 {printf "%d| %s\n", NR, $0}' $f
echo "===== J. current error counts (sanity) ====="
grep -c '^e:' /tmp/compile.log
echo "RedditAPI:"; grep -c '^e:.*reddit/RedditAPI.kt' /tmp/compile.log
