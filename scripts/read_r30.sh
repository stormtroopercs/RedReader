#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. RedditSubredditManager: subredditCache type + performRequest chain ##########"
grep -n 'subredditCache' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head
F=$(grep -rln 'class RedditSubredditCache\|fun performRequest' src/main/java/org/quantumbadger/redreader/reddit/ --include='*.kt' | head -3)
echo "files: $F"
grep -rn 'fun performRequest' src/main/java/org/quantumbadger/redreader/reddit/ --include='*.kt' | head
echo "########## B. performRequest signature (current) ##########"
FF=$(grep -rln 'fun performRequest' src/main/java/org/quantumbadger/redreader/reddit/ --include='*.kt' | head -1)
grep -n 'fun performRequest' -A8 "$FF"
echo "########## C. original Java performRequest ##########"
git show $B:$(echo "$FF" | sed 's/\.kt$/.java/') 2>/dev/null | grep -n 'void performRequest' -A8 | head -12
echo "########## D. UpdatedVersionListener interface ##########"
grep -rn 'interface UpdatedVersionListener' src/main/java --include='*.kt'
echo "########## E. callers of report( with reasonFields ##########"
grep -rn 'RedditAPI\.report\|\.report(' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt' | grep -iv 'errorreport\|reporterr' | head
echo "########## F. L430-445 (the report fn signature) ##########"
awk 'NR>=430 && NR<=445 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## G. PostSubmitContentFragment 475-515 (SubmitResponseHandler impl) ##########"
awk 'NR>=475 && NR<=515 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "########## H. PostSubmitContentFragment current error count ##########"
grep -c '^e:.*PostSubmitContentFragment' /tmp/compile.log
echo "########## I. sendReplies signature (the commentFullname param) ##########"
grep -n 'fun sendReplies' -A8 src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt | head -12
echo "########## J. all callers of RedditSubredditManager.getSubreddit (handler arg type check) ##########"
grep -rn '\.getSubreddit(' src/main/java --include='*.kt' | grep -v 'RedditSubredditManager.kt' | head
echo "########## K. all callers of getSubreddits( (plural) ##########"
grep -rn '\.getSubreddits(' src/main/java --include='*.kt' | grep -v 'RedditSubredditManager.kt' | head
