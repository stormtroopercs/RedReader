#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. Objects.requireNonNull signature ##########"
grep -rn 'fun.*requireNonNull' src/main/java/org/quantumbadger/redreader/common/Objects.kt
echo "########## B. SubredditListResponse (1165-1185) ##########"
awk 'NR>=1160 && NR<=1190 {printf "%d| %s\n", NR, $0}' $f
echo "########## C. SubmitResponseHandler (1040-1085) ##########"
awk 'NR>=1040 && NR<=1085 {printf "%d| %s\n", NR, $0}' $f
echo "########## D. PostFields full class ##########"
awk 'NR>=39 && NR<=70 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/http/body/HTTPRequestBody.kt
echo "########## E. choices decl (150-176) + fromJsonList sig ##########"
awk 'NR>=150 && NR<=176 {printf "%d| %s\n", NR, $0}' $f
grep -rn 'fun.*fromJsonList' src/main/java --include='*.kt' | head -5
echo "########## F. all PostField?> decls in RedditAPI.kt ##########"
grep -n 'PostField?>' $f
echo "########## G. RedditSubreddit.name (current + original) ##########"
grep -n 'name' src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.kt | head -6
git show 1d35f61e:src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.java 2>/dev/null | grep -n 'name' | head -6
echo "########## H. GenericResponseHandler decl ##########"
grep -rn 'class GenericResponseHandler' -A4 src/main/java --include='*.kt' | head -8
echo "########## I. requestSubredditList public wrapper (700-762) ##########"
awk 'NR>=700 && NR<=762 {printf "%d| %s\n", NR, $0}' $f
