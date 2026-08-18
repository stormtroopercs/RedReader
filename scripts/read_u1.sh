#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. original getSubreddit/getSubreddits/getMultireddits signatures ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | grep -n 'public void getSubreddit\|public void getSubreddits\|RequestResponseHandler<\|public void getMultireddits' | head -20
echo "########## B. CURRENT getSubreddits + getMultireddits (RedditSubredditManager) ##########"
grep -n 'fun getSubreddits\|fun getMultireddits\|fun getSubreddit' -A8 src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
echo "########## C. MainMenuFragment 100-135 (anon handlers) ##########"
awk 'NR>=100 && NR<=135 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/MainMenuFragment.kt
echo "########## D. MainMenuFragment 138-155 + 175-212 (collections) ##########"
awk 'NR>=138 && NR<=155 || NR>=175 && NR<=212 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/MainMenuFragment.kt
echo "########## E. original MainMenuFragment: getSubreddits/getMultireddits anon handlers + collections ##########"
git show $B:src/main/java/org/quantumbadger/redreader/fragments/MainMenuFragment.java 2>/dev/null | grep -n 'getSubreddits\|getMultireddits\|new RequestResponseHandler\|ArrayList<SubredditCanonicalId>\|ArrayList<String>\|HashSet<' | head -20
