#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## 78-80 (Retention import) ##########"
sed -n '76,82p' $f
echo "########## 1000-1012 (makeRequest candidates) ##########"
sed -n '1000,1012p' $f
echo "########## ACTION_UPVOTE etc defs ##########"
grep -n 'ACTION_UPVOTE\|ACTION_UNVOTE\|SUBSCRIPTION_ACTION_SUBSCRIBE' $f | head -6
echo "########## local IntDef? ##########"
grep -rn 'IntDef' $f | head
echo "########## requestSubredditList sig (820-835) ##########"
sed -n '820,835p' $f
echo "########## makeRequest overloads in CacheManager ##########"
grep -n 'fun makeRequest' src/main/java/org/quantumbadger/redreader/cache/CacheManager.kt
echo "########## JsonArray size + Iterable + asArray ##########"
find src -name 'JsonArray.kt' | head -1 | xargs grep -n 'class JsonArray\|Iterable\|fun size\|asArray\|fun get\|element' | head
echo "########## exact 897-917 numbered ##########"
awk 'NR>=897 && NR<=917 {printf "%d| %s\n", NR, $0}' $f
