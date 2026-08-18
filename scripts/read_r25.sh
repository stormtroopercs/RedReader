#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. Objects import in RedditAPI.kt ##########"
grep -n 'import.*Objects\|import.*Objects\.' $f | head -3
echo "########## B. requireNonNull definition anywhere ##########"
grep -rn 'fun.*requireNonNull' src/main/java --include='*.kt' | head -5
echo "########## C. JsonValue.asObject signature ##########"
grep -n 'fun asObject\|fun asString\|fun asArray\|fun asSubreddit' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
echo "########## D. Optional.get body ##########"
grep -n 'fun get\|fun orElse\|fun apply' src/main/java/org/quantumbadger/redreader/common/Optional.kt
echo "########## E. SubmitResponseHandler in current RedditAPI.kt ##########"
grep -n 'SubmitResponseHandler\|onSuccess(' $f | grep -in 'submit' | head -6
echo "---- show the interface ----"
awk '/interface SubmitResponseHandler/,/^    }/' $f
echo "########## F. RequestResponseHandler base (full) ##########"
cat src/main/java/org/quantumbadger/redreader/io/RequestResponseHandler.kt
echo "########## G. original Java SubmitResponseHandler ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'SubmitResponseHandler' | head
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | awk '/interface SubmitResponseHandler/{f=1} f{print NR": "$0} f&&/^    }/{exit}' | head -14
