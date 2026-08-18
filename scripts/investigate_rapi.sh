#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. RequestResponseHandler base (io/) ##########"
FF=$(find src -name 'RequestResponseHandler.kt' | head -1); echo "file: $FF"
awk '/(abstract |open )?(class|interface) RequestResponseHandler/{print NR": "$0; found=1} found' "$FF" | head -40
echo "---- all abstract/open funs in it ----"
grep -n 'abstract fun\|open fun\|fun onRequestSuccess\|fun onRequestFailed\|fun onDownloadStarted\|fun onSubmitErrors' "$FF" | head
echo "########## B. RedditSubreddit.name ##########"
FF=$(find src -name 'RedditSubreddit.kt' | head -1); echo "file: $FF"
grep -n 'name\|class RedditSubreddit' "$FF" | head
echo "########## C. PostField ctor + PostFields class ##########"
FF=$(find src -name 'PostField.kt' | head -1); echo "file: $FF"; cat "$FF" | sed -n '1,80p' | grep -n 'class PostField\|class PostFields\|constructor\|fun PostField\|PostField(\|name:\|value:\|MutableList\|ArrayList\|Collection' | head
echo "#### PostFields file ####"
FF2=$(find src -name 'PostFields.kt' | head -1); echo "file: $FF2"
[ -n "$FF2" ] && grep -n 'class PostFields\|constructor\|fun \|MutableList\|ArrayList\|Collection\|PostField' "$FF2" | head
echo "########## D. asSubreddit return (current + original) ##########"
grep -rn 'fun asSubreddit' src/main/java/org/quantumbadger/redreader/jsonwrap/ 2>/dev/null
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.java 2>/dev/null | grep -n 'asSubreddit'
echo "########## E. RedditAPIIndividualSubredditDataRequester type ##########"
FF=$(find src -name 'RedditAPIIndividualSubredditDataRequester.kt' | head -1); echo "file: $FF"
[ -n "$FF" ] && grep -n 'class RedditAPIIndividualSubredditDataRequester\|: \|Requester\|SubredditCanonicalId\|RedditSubreddit\|RRError' "$FF" | head
echo "########## F. RawObjectDB / ThreadedRawObjectDB / WeakCache generic decls ##########"
grep -rn 'class RawObjectDB<' src/main/java/org/quantumbadger/redreader/cache/RawObjectDB.kt | head
grep -rn 'class ThreadedRawObjectDB<' src/main/java/org/quantumbadger/redreader/cache/ThreadedRawObjectDB.kt | head
grep -rn 'class WeakCache<' src/main/java/org/quantumbadger/redreader/cache/WeakCache.kt | head
echo "########## G. 'choices' type in RedditAPI (L100-175) ##########"
awk 'NR>=95 && NR<=176 {printf "%d| %s\n", NR, $0}' $f | grep -i 'choices\|MutableList\|ArrayList\|get()'
echo "########## H. createPostRequest signature (L980-1010) ##########"
awk 'NR>=980 && NR<=1010 {printf "%d| %s\n", NR, $0}' $f
