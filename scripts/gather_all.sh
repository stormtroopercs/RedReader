#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. Objects import in RedditAPI.kt ====="
grep -n 'import.*Objects\|import.*Objects\.' $f
echo "===== 2. Objects.requireNonNull definition (search all kt) ====="
grep -rln 'fun <T.*requireNonNull\|fun requireNonNull' src/main/java --include='*.kt' | head -3
echo "===== 3. APIResponseHandler.kt: SubmitResponseHandler + RequestResponseHandler + ValueResponseHandler defs ====="
AF=src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
grep -n 'class SubmitResponseHandler' -A20 $AF
grep -n 'open class RequestResponseHandler\|abstract class RequestResponseHandler\|class RequestResponseHandler' -A15 $AF 2>/dev/null | head -30
grep -rn 'class ValueResponseHandler' -A8 $AF
echo "===== 4. RedditFlairChoice.fromJsonList signature ====="
grep -n 'fun fromJsonList' -A6 src/main/java/org/quantumbadger/redreader/reddit/RedditFlairChoice.kt
echo "===== 5. RedditSubreddit.name ====="
grep -n 'name' src/main/java/org/quantumbadger/redreader/reddit/RedditSubreddit.kt | head -8
echo "===== 6. PostField + PostFields classes ====="
grep -rn 'class PostField\b' -A6 src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
grep -rn 'class PostFields' -A6 src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 7. WeakCache/RawObjectDB performRequest sigs ====="
grep -rn 'fun performRequest' -A4 src/main/java/org/quantumbadger/redreader/io/ src/main/java/org/quantumbadger/redreader/reddit/ 2>/dev/null | grep -v 'RedditSubredditManager\|RedditUserManager\|RedditListingManager' | head -30
echo "===== 8. RedditSubredditManager subredditCache decl ====="
grep -n 'subredditCache' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head -5
echo "===== 9. asSubreddit signature ====="
grep -rn 'fun asSubreddit' -A3 src/main/java/org/quantumbadger/redreader/reddit/RedditThing.kt 2>/dev/null || grep -rn 'fun asSubreddit' src/main/java --include='*.kt' -A3 | head -8
echo "===== 10. original Java: getSubreddit + requestSubredditList + SubredditListResponse ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'public static void getSubreddit\|requestSubredditList\|class SubredditListResponse' -A8 | head -50
