#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. PostFields constructor + HTTPRequestBody hierarchy ##########"
grep -rn 'class PostFields\|sealed class HTTPRequestBody\|abstract class HTTPRequestBody\|class HTTPRequestBody' src/main/java --include='*.kt'
find src -name 'HTTPRequestBody.kt' | head -1 | xargs grep -n 'class PostFields\|class HTTPRequestBody\|sealed\|abstract' | head
echo "--- PostFields ctor body ---"
find src -name 'HTTPRequestBody.kt' | head -1 | xargs awk '/class PostFields/,/^    }/' | head -20
echo "########## B. the 3 requestSubredditList wrappers (660-800) - func heads + anon handlers ##########"
awk 'NR>=660 && NR<=800 && (/private fun/ || /fun / || /ValueResponseHandler</ || /object :/ || /onSuccess/ || /requestSubredditList/ || /handler: /) {printf "%d| %s\n", NR, $0}' $f
echo "########## C. getSubreddit callers in PostListingFragment (345-360) ##########"
awk 'NR>=345 && NR<=360 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt
echo "########## D. ORIGINAL Java: the subscriptionAction getSubreddit anon handler (type args) ##########"
grep -n -B3 -A12 'getSubreddit(' /tmp/RAPI.java | head -40
echo "########## E. ORIGINAL Java: subscribedSubredditsInternal / the 3 requestSubredditList wrappers (680-840) ##########"
awk 'NR>=680 && NR<=840 && (/ValueResponseHandler</ || /private static void/ || /new APIResponseHandler/) {printf "%d| %s\n", NR, $0}' /tmp/RAPI.java | head -40
echo "########## F. ORIGINAL Java: L1007 area createPostRequestUnprocessedResponse (990-1010) ##########"
awk 'NR>=985 && NR<=1010 {printf "%d| %s\n", NR, $0}' /tmp/RAPI.java
