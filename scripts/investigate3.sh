#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## 1. Objects.requireNonNull signature (custom) ##########"
FF=$(grep -rln 'fun <T> requireNonNull\|fun requireNonNull' src/main/java/org/quantumbadger/redreader/common/ --include='*.kt' | head -1)
[ -z "$FF" ] && FF=$(grep -rln 'requireNonNull' src/main/java --include='*.kt' | grep -i 'objects\|general' | head -1)
echo "file: $FF"
[ -n "$FF" ] && grep -n 'requireNonNull' "$FF"
echo "#### how imported in RedditAPI ####"
grep -n 'import.*Objects\|Objects' $f | head
echo "########## 2. L800-865 (subscribedSubredditsInternal: after source) ##########"
awk 'NR>=800 && NR<=865 {printf "%d| %s\n", NR, $0}' $f
echo "########## 3. L1090-1160 (L1139/1141 onSuccess + getStringAtPath) ##########"
awk 'NR>=1090 && NR<=1160 {printf "%d| %s\n", NR, $0}' $f
echo "########## 4. fromJsonList original Java ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.java 2>/dev/null | grep -n 'fromJsonList' -A4 | head
echo "#### find it if not there ####"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/things/RedditFlairChoice.java 2>/dev/null | grep -n 'fromJsonList' -A4 | head
echo "########## 5. FlairSelectorResponseHandler.onSuccess signature ##########"
grep -rn 'fun onSuccess' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt | grep -i 'flair\|MutableList<RedditFlairChoice' 
grep -rn 'FlairSelectorResponseHandler\|interface.*ResponseHandler' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt | head
echo "#### find FlairSelectorResponseHandler def ####"
grep -rn 'class FlairSelectorResponseHandler\|interface FlairSelectorResponseHandler' src/main/java --include='*.kt' | head
