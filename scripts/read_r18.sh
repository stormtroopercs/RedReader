#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. L117-121 (requireNonNull) ##########"
awk 'NR>=117 && NR<=121 {printf "%d| %s\n", NR, $0}' $f
echo "########## B. L174-177 (choices.get()) ##########"
awk 'NR>=174 && NR<=177 {printf "%d| %s\n", NR, $0}' $f
echo "########## C. L858-862 (L861) ##########"
awk 'NR>=858 && NR<=862 {printf "%d| %s\n", NR, $0}' $f
echo "########## D. L895-912 (for loop) ##########"
awk 'NR>=895 && NR<=912 {printf "%d| %s\n", NR, $0}' $f
echo "########## E. L1138-1146 (L1142/L1144) ##########"
awk 'NR>=1138 && NR<=1146 {printf "%d| %s\n", NR, $0}' $f
echo "########## F. PostField ctor ##########"
grep -n 'class PostField' -A6 src/main/java/org/quantumbadger/redreader/http/PostField.kt
echo "########## G. ORIGINAL Java L119 (requireNonNull) + L176 + L861 + L1142 ##########"
B=1d35f61e
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java > /tmp/RA.java 2>/dev/null
grep -n 'requireNonNull\|\.isEmpty()\|choices.get()\|after.get()\|getStringAtPath\|Optional<String>' /tmp/RA.java | head -20
echo "########## H. is 'Objects' imported in RedditAPI.kt? ##########"
grep -n 'import.*Objects\|import.*Objects' $f
