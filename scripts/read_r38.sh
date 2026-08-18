#!/bin/bash
cd /opt/data/RedReader
echo "########## A. CURRENT RedditAPI.kt error list ##########"
grep '^e:.*reddit/RedditAPI.kt' /tmp/compile.log | sed 's|.*RedditAPI.kt:||'
echo "########## B. SubredditListResponse external usages ##########"
grep -rn 'SubredditListResponse' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt'
echo "########## C. custom Optional: apply / orElse / ifPresent signatures ##########"
grep -n 'fun apply\|fun orElse\|fun ifPresent\|fun isPresent\|fun isEmpty\|fun ofNullable\|fun of\b\|fun empty\|fun <T> ' src/main/java/org/quantumbadger/redreader/common/Optional.kt
echo "########## D. FunctionOneArgNoReturn body ##########"
sed -n '19,40p' src/main/java/org/quantumbadger/redreader/common/FunctionOneArgNoReturn.kt
echo "########## E. L287 anon SubmitResponseHandler onSuccess (285-300) ##########"
awk 'NR>=285 && NR<=300 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## F. L1160-1175 SubredditListResponse class ##########"
awk 'NR>=1158 && NR<=1178 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
