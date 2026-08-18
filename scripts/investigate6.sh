#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. createPostRequest full signature (L975-990) ##########"
awk 'NR>=975 && NR<=990 {printf "%d| %s\n", NR, $0}' $f
echo "#### original Java createPostRequest (postFields type) ####"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'private static CacheRequest createPostRequest(' -A8 | head -10
echo "########## B. WeakCache generic decl (does it allow non-null V?) ##########"
grep -n 'class WeakCache' src/main/java/org/quantumbadger/redreader/cache/WeakCache.kt
echo "#### original Java WeakCache decl ####"
git show $B:src/main/java/org/quantumbadger/redreader/cache/WeakCache.java 2>/dev/null | grep -n 'class WeakCache' | head
echo "########## C. subredditCache decl in manager (current) ##########"
grep -n 'subredditCache' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
echo "########## D. manager imports (SubredditCanonicalId, RRError, etc.) ##########"
grep -n 'import' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head
echo "########## E. manager full file ##########"
cat src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
echo "########## F. RedditAPI.kt: does it import Objects? (will be unused after fix) ##########"
grep -n 'import java.util.Objects' $f
echo "#### other Objects. uses in RedditAPI (besides L118/L125) ####"
grep -n 'Objects\.' $f
