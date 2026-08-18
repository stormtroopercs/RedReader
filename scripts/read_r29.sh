#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. getSubreddit signature (current RedditSubredditManager) ##########"
grep -n 'fun getSubreddit' -A14 src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head -20
echo "########## B. original Java getSubreddit handler type ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | grep -n 'void getSubreddit(' -A10 | head -14
echo "########## C. external callers of RedditAPI.searchSubreddits / .subscribedSubreddits (static) ##########"
grep -rn 'RedditAPI\.searchSubreddits\|RedditAPI\.subscribedSubreddits\|\.subscribedSubreddits(\|\.searchSubreddits(' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt' | grep -v 'subredditDao\|subredditRepository\|subredditApi'
echo "########## D. who references SubredditListResponse (all files) ##########"
grep -rn 'SubredditListResponse' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt'
echo "########## E. current errors in files that call these (MainMenuFragment / PostListingFragment / any) ##########"
grep '^e:.*MainMenuFragment' /tmp/compile.log | wc -l
echo "---- do any clean files pass ValueResponseHandler<SubredditListResponse?> as arg? ----"
grep -rn 'ValueResponseHandler<SubredditListResponse' src/main/java --include='*.kt'
echo "########## F. L668-678 (the L673 handler param - which fn) ##########"
awk 'NR>=660 && NR<=678 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
