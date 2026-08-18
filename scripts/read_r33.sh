#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. RedditIdAndType file + value type ##########"
FF=$(find src -name 'RedditIdAndType.kt' | head -1); echo "$FF"
grep -n 'value' "$FF" | head -5
echo "########## B. reasonFields in report(): original Java type ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'reasonFields' | head -3
echo "########## C. L100-108 (L105 createPostRequest call args) ##########"
awk 'NR>=96 && NR<=122 {printf "%d| %s\n", NR, $0}' $f
echo "########## D. ReportReason.toPostFields return ##########"
grep -rn 'fun toPostFields' src/main/java/org/quantumbadger/redreader/reddit/api/ReportReason.kt 2>/dev/null
find src -name 'ReportReason.kt' | head -1 | xargs grep -n 'toPostFields' 2>/dev/null | head -3
echo "########## E. StringUtils.join sig (60-70) ##########"
awk 'NR>=60 && NR<=72 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/common/StringUtils.kt
echo "########## F. PostSubmitContentFragment 370-410 (flair handler + Consumer refs) ##########"
awk 'NR>=370 && NR<=410 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "########## G. original Java: sendReplies param + report reasonFields + PostField usage ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'LinkedList<PostField>\|new PostField(' | head -6
echo "########## H. createPostRequestUnprocessedResponse callers (333, 618) args ##########"
awk 'NR>=325 && NR<=345 {printf "%d| %s\n", NR, $0}' $f
awk 'NR>=610 && NR<=630 {printf "%d| %s\n", NR, $0}' $f
