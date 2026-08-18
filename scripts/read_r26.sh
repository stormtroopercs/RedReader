#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. RedditFlairChoice.kt full (55-130) ##########"
awk 'NR>=55 && NR<=130 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
echo "########## B. callers of fromJson( (RedditFlairChoice) ##########"
grep -rn 'RedditFlairChoice.Companion.fromJson\b\|FlairChoice.fromJson\b' src/main/java --include='*.kt'
echo "########## C. function containing L478: show 440-510 ##########"
awk 'NR>=440 && NR<=510 {printf "%d| %s\n", NR, $0}' $f
echo "########## D. getRequest signature (the handler param) ##########"
grep -n 'fun getRequest(' $f
awk '/fun getRequest\(/,/^    \)/' $f | head -20
echo "########## E. SubmitResponseHandler in APIResponseHandler.kt ##########"
awk '/class SubmitResponseHandler|interface SubmitResponseHandler/,/^    }/' src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
echo "########## F. original Java APIResponseHandler.java SubmitResponseHandler ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.java 2>/dev/null | awk '/SubmitResponseHandler/{f=1} f{print NR": "$0} f&&/^        }$/{exit}' | head -20
echo "########## G. external callers of subscribedSubreddits/searchSubreddits/requestSubreddits ##########"
grep -rn '\.subscribedSubreddits(\|\.searchSubreddits(\|\.requestSubreddits(' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt'
echo "########## H. postFields.add calls with nullable 2nd args ##########"
grep -n 'postFields.add(PostField(' $f
echo "########## I. L836-861 (output/after decls) ##########"
awk 'NR>=836 && NR<=861 {printf "%d| %s\n", NR, $0}' $f
echo "########## J. external files' current error count (MainMenuFragment, PostListingFragment, SubredditListFragment, etc.) ##########"
grep '^e:.*MainMenuFragment' /tmp/compile.log | wc -l
grep '^e:.*SubredditList' /tmp/compile.log | sed 's|.*redreader/||' | sort -u
