#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. ALL getSubreddit call sites (anon handler type args) ##########"
grep -rn 'getSubreddit(' src/main/java --include='*.kt'
echo "########## B. requestSubredditList call sites + anon ValueResponseHandler type args ##########"
grep -n 'ValueResponseHandler<SubredditListResponse' $f
echo "########## C. L690-800 the three requestSubredditList anon objects ##########"
awk 'NR>=690 && NR<=795 {printf "%d| %s\n", NR, $0}' $f | grep -n 'ValueResponseHandler\|onSuccess\|requestSubredditList\|object :'
echo "########## D. working @IntDef example (BezelSwipeOverlay) - full annot ##########"
sed -n '25,40p' src/main/java/org/quantumbadger/redreader/views/bezelmenu/BezelSwipeOverlay.kt
echo "########## E. does BezelSwipeOverlay import java.lang.annotation.Retention? ##########"
grep -n 'java.lang.annotation' src/main/java/org/quantumbadger/redreader/views/bezelmenu/BezelSwipeOverlay.kt
echo "########## F. CacheRequest constructors (param types) ##########"
awk 'NR>=110 && NR<=200 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/cache/CacheRequest.kt
echo "########## G. fromJsonList full (RedditFlairChoice) ##########"
sed -n '55,90p' src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
echo "########## H. ORIGINAL RedditSubreddit.name nullability ##########"
git show 1d35f61e:src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.java > /tmp/RS.java 2>/dev/null
grep -n 'String name\|public String getName\|this.name' /tmp/RS.java | head
