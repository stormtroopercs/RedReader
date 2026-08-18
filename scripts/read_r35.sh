#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. WeakCache.kt (class decl + performRequest + type params) ##########"
find src -name 'WeakCache.kt' | head -1 | xargs sed -n '1,140p' | grep -nE 'class WeakCache|performRequest|performWrite|<K|<T|<E|typealias|abstract|interface' | head -20
echo "########## B. WeakCache full performRequest signatures ##########"
find src -name 'WeakCache.kt' | head -1 | xargs grep -n 'fun performRequest' -A6
echo "########## C. original Java WeakCache.performRequest ##########"
git show $B:src/main/java/org/quantumbadger/redreader/common/WeakCache.java 2>/dev/null | grep -n 'class WeakCache\|void performRequest\|void performWrite' | head
echo "########## D. RedditSubredditManager full (1-130) ##########"
sed -n '40,130p' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
echo "########## E. ReportReason.toPostFields (ReportReason.kt) ##########"
FF=$(find src -name 'ReportReason.kt' | head -1); echo "$FF"
grep -n 'toPostFields' "$FF"
echo "########## F. original Java getSubreddit (RedditSubredditManager.java) full ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | awk 'NR>=95 && NR<=130 {printf "%d| %s\n", NR, $0}'
echo "########## G. all callers of getSubreddit (to check their anon handler type args) ##########"
grep -rn '\.getSubreddit(' src/main/java --include='*.kt' | grep -v 'RedditSubredditManager.kt\|getSubredditPost\|SubredditPostListURL\|RedditURL'
echo "########## H. the PostListingFragment.getSubreddit call (348) context ##########"
awk 'NR>=344 && NR<=360 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt
