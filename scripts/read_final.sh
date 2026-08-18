#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. findFailureType signature ##########"
grep -n 'fun findFailureType' -A2 $f
echo "########## B. function containing L1007 (postFields decl) - L975-1012 ##########"
awk 'NR>=975 && NR<=1012 {printf "%d| %s\n", NR, $0}' $f
echo "########## C. getSubreddit signature (RedditSubredditManager) ##########"
grep -n 'fun getSubreddit' -A8 src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head -20
echo "########## D. ValueResponseHandler base (APIResponseHandler) onSuccess + class decl ##########"
grep -n 'class ValueResponseHandler\|abstract.*onSuccess\|fun onSuccess' src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
echo "########## E. PostField constructor ##########"
grep -rn 'class PostField' -A8 src/main/java/org/quantumbadger/redreader/http/PostField.kt | head -14
echo "########## F. fromJsonList current + ORIGINAL ##########"
grep -n 'fun fromJsonList' -A3 src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.java 2>/dev/null | grep -n 'fromJsonList' -A3
echo "########## G. ALL getAtPathInternal overrides in codebase ##########"
grep -rn 'override fun getAtPathInternal\|fun getAtPathInternal' src/main/java --include='*.kt'
echo "########## H. JsonObject.get(name) external callers (passing possibly-nullable) ##########"
grep -rn '\.get(' src/main/java --include='*.kt' | grep -vE 'Optional|HashMap|properties|\.get\(' | grep -iE 'json|\.asObject\(\)' | head
echo "########## I. who iterates JsonObject (for v in ...asObject) ##########"
grep -rn 'for (.* in .*asObject\|for (.* in .*JsonObject' src/main/java --include='*.kt' | head
