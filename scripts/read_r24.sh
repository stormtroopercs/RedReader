#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. Objects.requireNonNull definition (General.kt) ##########"
grep -n 'requireNonNull' src/main/java/org/quantumbadger/redreader/common/General.kt
awk '/fun.*requireNonNull|inline fun.*requireNonNull|fun <T.*requireNonNull/{print NR": "$0; for(i=1;i<=4;i++) getline, print "   cont: "$0}' src/main/java/org/quantumbadger/redreader/common/General.kt 2>/dev/null
echo "--- raw lines around requireNonNull in General.kt ---"
LN=$(grep -n 'requireNonNull' src/main/java/org/quantumbadger/redreader/common/General.kt | head -1 | cut -d: -f1)
awk -v s=$LN 'NR>=s-2 && NR<s+6 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/common/General.kt
echo "########## B. is there a common/Objects.kt? ##########"
find src -name 'Objects.kt' | head
echo "########## C. original Java SubmitResponseHandler.onSuccess + requestSubredditList handler ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'interface SubmitResponseHandler\|void onSuccess\|ValueResponseHandler<SubredditListResponse' 
echo "--- SubmitResponseHandler block ---"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | awk '/interface SubmitResponseHandler/,/^    }/' | head -12
echo "########## D. original postFields type (Java) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'LinkedList<PostField>\|LinkedList<PostField ' | head -4
echo "########## E. original getSubreddit/getSubreddits handler types ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'void getSubreddit(\|void getSubreddits(\|void searchSubreddits(\|RequestResponseHandler<RedditSubreddit' | head
echo "########## F. original L118 region (Objects.requireNonNull) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'requireNonNull' | head -3
