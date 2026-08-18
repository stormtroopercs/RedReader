#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. function containing L475 (L450-478) ##########"
awk 'NR>=450 && NR<=478 {printf "%d| %s\n", NR, $0}' $f
echo "#### original Java: that function (subscriptionActionUri / prepareActionUri) ####"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'prepareActionUri\|subscriptionActionUri\|void prepareAction\|prepareAction(' -A8 | grep -iE 'void |SubredditCanonicalId|getSubreddit|@NonNull' | head
echo "#### the actual function name at L455-475 ####"
awk 'NR>=452 && NR<=475 {print NR": "$0}' $f | grep -iE 'fun |private|public|void'
echo "########## B. who calls that function (the one with subredditId)? ##########"
echo "(search for the function name once known)"
echo "########## C. original Java: is the subredditId/subreddit param @NonNull? (the action-prepare function) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'SubredditCanonicalId' | head
echo "########## D. original Java full: the function that calls getSubreddit (subscriptionAction) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'getSubreddit(' -B14 | grep -iE 'void |static|SubredditCanonicalId|action|context|user' | head
