#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
echo "########## A. ALL FlairSelectorResponseHandler implementers (onSuccess overrides) ##########"
grep -rn 'FlairSelectorResponseHandler' src/main/java --include='*.kt' | grep -v 'import\|interface '
echo "---- their onSuccess lines ----"
for f in $(grep -rln 'FlairSelectorResponseHandler' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt'); do
  echo "[$f]"; grep -n 'override fun onSuccess' "$f"
done
echo "########## B. original Java FlairSelectorResponseHandler.onSuccess ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'interface FlairSelectorResponseHandler' -A4
echo "########## C. createPostRequest call sites: what args are passed for (subreddit/title/body/flair) ##########"
grep -n 'createPostRequest(' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## D. the sendReplies/subreddit/title/body param regions (submit 191-264) ##########"
awk 'NR>=191 && NR<=264 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## E. original Java PostSubmitContentFragment onSuccess (flair) ##########"
git show $B:src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.java 2>/dev/null | grep -n 'onSuccess' | head
echo "########## F. how many callers pass postFields to createPostRequest that have nullable 2nd-arg PostFields? (check title/body/flairId nullability at call sites) ##########"
grep -n 'createPostRequest(' -A12 src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt | grep -E 'createPostRequest\(|subreddit,|title,|body,|flairId' | head -40
