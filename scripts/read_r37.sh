#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. popularSubreddits body (675-703) ##########"
awk 'NR>=675 && NR<=703 {printf "%d| %s\n", NR, $0}' $f
echo "########## B. asSubreddit return type (current + original) ##########"
grep -n 'fun asSubreddit' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.java 2>/dev/null | grep -n 'asSubreddit'
echo "########## C. FunctionOneArgNoReturn (definition) ##########"
grep -rn 'FunctionOneArgNoReturn' src/main/java/org/quantumbadger/redreader/common/ --include='*.kt' | grep -i 'fun interface\|typealias\|interface' | head -3
FF=$(grep -rln 'fun interface FunctionOneArgNoReturn\|typealias FunctionOneArgNoReturn' src/main/java --include='*.kt' | head -1); echo "def file: $FF"
grep -n 'FunctionOneArgNoReturn' "$FF" 2>/dev/null | head -3
echo "########## D. original Java subscribedSubreddits + popularSubreddits + searchSubreddits signatures ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'void subscribedSubreddits(\|void popularSubreddits(\|void searchSubreddits(\|void requestSubredditList(' -A6 | grep -E 'void |ValueResponseHandler|Optional' | head -20
echo "########## E. all 'value: String?' lambda sites in RedditAPI.kt ##########"
grep -n 'value: String?' $f
echo "########## F. all 'Optional<String?>' sites in RedditAPI.kt ##########"
grep -n 'Optional<String?>' $f
echo "########## G. all 'RedditSubreddit?>' sites in RedditAPI.kt ##########"
grep -n 'RedditSubreddit?>' $f
echo "########## H. Consumer import in RedditAPI.kt ##########"
grep -n 'import.*Consumer' $f
