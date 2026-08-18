#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. ValueResponseHandler class (APIResponseHandler.kt) ##########"
awk '/class ValueResponseHandler|abstract class ValueResponseHandler/,/^    }/' src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
echo "########## B. L280-315: the anonymous SubmitResponseHandler at L287 ##########"
awk 'NR>=280 && NR<=315 {printf "%d| %s\n", NR, $0}' $f
echo "########## C. submitJSON handlers (193, 268) context ##########"
awk 'NR>=190 && NR<=200 {printf "%d| %s\n", NR, $0}' $f
awk 'NR>=264 && NR<=296 {printf "%d| %s\n", NR, $0}' $f
echo "########## D. other SubmitResponseHandler.onSuccess overrides (all files) ##########"
grep -rn 'override fun onSuccess' src/main/java --include='*.kt' | grep -i 'submit\|Optional' | head
echo "########## E. who implements SubmitResponseHandler (all) ##########"
grep -rn 'SubmitResponseHandler' src/main/java --include='*.kt' | grep -v 'import\|mResponseHandler\|: SubmitResponseHandler' | head
echo "########## F. original Java getSubreddit handler (565-575) to confirm type args ##########"
git show 1d35f61e:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | awk 'NR>=565 && NR<=575 {printf "%d| %s\n", NR, $0}'
