#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. RedditSubreddit.name: current + original ##########"
grep -n 'name' src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.kt | head -8
git show $B:src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.java 2>/dev/null | grep -n 'getName\|name' | head -8
echo "########## B. RedditAPI 960-1000 (function containing L1007) ##########"
awk 'NR>=960 && NR<=1000 {printf "%d| %s\n", NR, $0}' $f
echo "########## C. fromJsonList full body (RedditFlairChoice) ##########"
sed -n '60,100p' src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
echo "########## D. BezelSwipeOverlay @IntDef region ##########"
sed -n '25,40p' src/main/java/org/quantumbadger/redreader/views/bezelmenu/BezelSwipeOverlay.kt
echo "########## E. the 4 files: any actual use of java Retention/RetentionPolicy? ##########"
for ff in reddit/RedditAPI.kt reddit/url/RedditURLParser.kt fragments/MainMenuFragment.kt views/bezelmenu/BezelSwipeOverlay.kt; do
  echo "--- $ff"
  grep -n 'RetentionPolicy\|java.lang.annotation' src/main/java/org/quantumbadger/redreader/$ff | head -5
done
echo "########## F. RedditURLParser + MainMenuFragment @IntDef/@Retention regions ##########"
grep -n '@IntDef\|@Retention' src/main/java/org/quantumbadger/redreader/reddit/url/RedditURLParser.kt src/main/java/org/quantumbadger/redreader/fragments/MainMenuFragment.kt
echo "########## G. all postFields locals in RedditAPI (the L1007 function) ##########"
grep -n 'val postFields' $f
