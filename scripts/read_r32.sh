#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. RedditIdAndType.value type ##########"
grep -rn 'class RedditIdAndType\|val value\|var value' src/main/java/org/quantumbadger/redreader/reddit/RedditIdAndType.kt | head
echo "########## B. the local vars feeding PostField 2nd args ##########"
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "--- usernameToUnblock / currentUserFullname (585-600) ---"
awk 'NR>=585 && NR<=600 {printf "%d| %s\n", NR, $0}' $f
echo "--- fullname / state (650-660) ---"
awk 'NR>=650 && NR<=660 {printf "%d| %s\n", NR, $0}' $f
echo "--- sendReplies param fullname: String (L647) confirmed; check call site commentFullname!! (L302) ---"
echo "########## C. original Java PostFields ctor + PostField (HTTPRequestBody.java) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/http/body/HTTPRequestBody.java 2>/dev/null | grep -n 'class PostFields\|PostField\[\]\|Collection<PostField>\|ArrayList<PostField>' | head
echo "########## D. PostSubmitContentFragment 495-520 (the SubmitResponseHandler onSuccess override) ##########"
awk 'NR>=495 && NR<=525 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "########## E. StringUtils.join signature (does it take Collection?) ##########"
grep -rn 'fun join' src/main/java/org/quantumbadger/redreader/common/StringUtils.kt 2>/dev/null | head
echo "########## F. PostSubmitContentFragment error list ##########"
grep '^e:.*PostSubmitContentFragment' /tmp/compile.log | sed 's|.*PostSubmitContentFragment.kt:||'
echo "########## G. ReportDialog.kt (report caller) - reasonFields type it passes ##########"
grep -n 'reasonFields\|toPostFields\|MutableList<PostField' src/main/java/org/quantumbadger/redreader/fragments/ReportDialog.kt | head
grep -rn 'fun toPostFields' src/main/java --include='*.kt' | head
