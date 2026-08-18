#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. Objects.requireNonNull (find the file) ##########"
grep -rln 'fun <T' src/main/java/org/quantumbadger/redreader/common/ --include='*.kt' | xargs grep -ln 'requireNonNull' 2>/dev/null
grep -rn 'requireNonNull' src/main/java/org/quantumbadger/redreader/common/ --include='*.kt' | head
echo "########## B. SubmitResponseHandler interface (find + body) ##########"
LN=$(grep -n 'interface SubmitResponseHandler' $f | head -1 | cut -d: -f1)
echo "at line $LN"
awk -v s=$LN 'NR>=s && NR<s+14 {printf "%d| %s\n", NR, $0}' $f
echo "########## C. private requestSubredditList signature ##########"
LN=$(grep -n 'fun requestSubredditList(' $f | head -1 | cut -d: -f1)
echo "at line $LN"
awk -v s=$LN 'NR>=s && NR<s+20 {printf "%d| %s\n", NR, $0}' $f
echo "########## D. ALL ValueResponseHandler<SubredditListResponse?> occurrences (all files) ##########"
grep -rn 'ValueResponseHandler<SubredditListResponse' src/main/java --include='*.kt'
echo "########## E. fromJsonList body (RedditFlairChoice 63-90) ##########"
awk 'NR>=63 && NR<=90 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
echo "########## F. original Java fromJsonList + SubredditListResponse + SubmitResponseHandler.onSuccess ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.java 2>/dev/null | grep -n 'fromJsonList' 
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'class SubredditListResponse' -A3
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'interface SubmitResponseHandler' -A5
