#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. SubredditListResponse class (full) ##########"
LN=$(grep -n 'class SubredditListResponse' $f | head -1 | cut -d: -f1)
echo "at line $LN"
awk "NR>=$LN && NR<=$((LN+15)) {printf \"%d| %s\n\", NR, $0}" $f
echo "########## B. SubmitResponseHandler interface (1085-1100) ##########"
awk 'NR>=1085 && NR<=1100 {printf "%d| %s\n", NR, $0}' $f
echo "########## C. requestSubredditList wrapper (760-790) ##########"
awk 'NR>=760 && NR<=790 {printf "%d| %s\n", NR, $0}' $f
echo "########## D. createPostRequest (935-989) ##########"
awk 'NR>=935 && NR<=989 {printf "%d| %s\n", NR, $0}' $f
echo "########## E. PostFields ctor (HTTPRequestBody.kt) ##########"
grep -n 'class PostFields' -A6 src/main/java/org/quantumbadger/redreader/http/body/HTTPRequestBody.kt
echo "########## F. flair choices decl (125-150) ##########"
awk 'NR>=125 && NR<=150 {printf "%d| %s\n", NR, $0}' $f
echo "########## G. createPostRequest call sites with postFields ##########"
grep -n 'createPostRequest\|createPostRequestUnprocessedResponse' $f
