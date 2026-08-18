#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. getSubreddit signature (RedditSubredditManager) ##########"
grep -n 'fun getSubreddit' -A10 src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head -16
echo "########## B. requestSubredditList signature ##########"
grep -n 'private fun requestSubredditList\|fun requestSubredditList' -A12 src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt | head -20
echo "########## C. SubredditListResponse class (after field) + original ##########"
grep -rn 'class SubredditListResponse\|SubredditListResponse(' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt | head
grep -n 'SubredditListResponse' -A8 src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt | grep -A8 'class SubredditListResponse'
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'class SubredditListResponse' -A10 | head -14
echo "########## D. RedditURLParser @IntDef body (199-204) ##########"
awk 'NR>=199 && NR<=204 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/url/RedditURLParser.kt
echo "########## E. MainMenuFragment @IntDef body (57-61) ##########"
awk 'NR>=57 && NR<=61 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/MainMenuFragment.kt
echo "########## F. BezelSwipeOverlay @IntDef (31-32) exact ##########"
awk 'NR>=31 && NR<=32 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/views/bezelmenu/BezelSwipeOverlay.kt
echo "########## G. original Java: were postFields List<PostField> (non-null elements)? ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'List<PostField>\|LinkedList<PostField>' | head
echo "########## H. original getSubreddit handler type ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | grep -n 'getSubreddit' -A6 | head -10
