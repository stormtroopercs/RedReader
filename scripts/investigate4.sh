#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## 1. ALL PostField( sites in RedditAPI.kt + the value arg (to decide postFields de-null safety) ##########"
grep -n 'PostField(' $f
echo "########## 2. postFields declarations in RedditAPI.kt ##########"
grep -n 'postFields = \|val postFields\|var postFields' $f
echo "########## 3. createPostRequest call sites + whether they pass postFields (L990-1010 region) ##########"
grep -n 'createPostRequest(' $f
echo "########## 4. subreddit-list external callers (popularSubreddits/searchSubreddits/subscribedSubreddits/requestSubredditList) ##########"
grep -rn 'popularSubreddits(\|searchSubreddits(\|subscribedSubreddits(\|requestSubredditList(' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt'
echo "########## 5. ALL anon ValueResponseHandler<SubredditListResponse in RedditAPI (type-arg sites) ##########"
grep -n 'ValueResponseHandler<SubredditListResponse' $f
echo "########## 6. subscribedSubredditsInternal full signature ##########"
grep -n 'fun subscribedSubredditsInternal' -A12 $f
echo "########## 7. subreddit manager: offerRawSubredditData original Java + getSubreddit current ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | grep -n 'offerRawSubredditData\|void getSubreddit\|void getSubreddits' -A4 | head -30
echo "#### current manager getSubreddit + offerRawSubredditData ####"
grep -n 'fun getSubreddit\|fun getSubreddits\|fun offerRawSubredditData' -A6 src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt
echo "########## 8. PostListingFragment subredditHandler decl ##########"
grep -n 'subredditHandler' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt
echo "########## 9. original Java: does getSubreddit use @NonNull? (the full signature) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | grep -n 'public void getSubreddit' -A6 | head
echo "########## 10. flair: all onSuccess( ...FlairChoice) call sites + any external FlairSelectorResponseHandler impls ##########"
grep -rn 'FlairSelectorResponseHandler' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt'
grep -n 'onSuccess(mutableListOf<RedditFlairChoice' $f
echo "########## 11. original Java createPostRequestUnprocessedResponse postFields type ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'createPostRequestUnprocessedResponse\|createPostRequest(' -A2 | grep -i 'List<PostField\|postFields' | head
echo "########## 12. original Java subscriptionPrepareActionUri postFields type ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'subscriptionPrepareActionUri\|subscriptionActionUri' -A4 | grep -i 'List<PostField\|postFields' | head
