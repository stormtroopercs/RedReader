#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
AF=src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt
echo "===== 1. RequestResponseHandler base def (search whole io/ + reddit/) ====="
grep -rn 'class RequestResponseHandler' src/main/java --include='*.kt'
echo "----- its methods -----"
grep -rn 'onRequestSuccess\|onRequestFailed' src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.kt | head
FF=$(grep -rln 'abstract class RequestResponseHandler\|class RequestResponseHandler' src/main/java --include='*.kt' | head -1)
echo "FILE=$FF"
if [ -n "$FF" ]; then grep -n 'class RequestResponseHandler' -A20 "$FF"; fi
echo "===== 2. ORIGINAL Java RequestResponseHandler (io/) ====="
for p in io/RequestResponseHandler.java reddit/RequestResponseHandler.java io/RequestHandler.java; do
  J=$(git show $B:src/main/java/org/quantumbadger/redreader/$p 2>/dev/null)
  [ -n "$J" ] && { echo "--- $p ---"; echo "$J" | grep -n 'class RequestResponseHandler\|onRequestSuccess\|onRequestFailed' | head; }
done
echo "===== 3. Classes extending/implementing SubmitResponseHandler ====="
grep -rn 'SubmitResponseHandler' src/main/java --include='*.kt' | grep -v 'APIResponseHandler.kt\|SubmitResponseHandler(context)\|SubmitJSONListener' 
echo "===== 4. Implementers of FlairSelectorResponseHandler ====="
grep -rn 'FlairSelectorResponseHandler' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt:109'
echo "===== 5. fromJsonList callers ====="
grep -rn 'fromJsonList' src/main/java --include='*.kt'
echo "===== 6. callers of subscriptionAction ====="
grep -rn 'subscriptionAction(' src/main/java --include='*.kt'
echo "===== 7. ORIGINAL Java RedditSubreddit.name ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubreddit.java 2>/dev/null | grep -n 'String name\|public String name\|name;' | head
echo "===== 8. ORIGINAL Java getSubreddit full sig ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.java 2>/dev/null | sed -n '100,116p'
echo "===== 9. RedditSubreddit current name + usages (setters/readers) ====="
grep -n 'name' src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.kt | head
echo "===== 10. FunctionOneArgNoReturn ====="
grep -rn 'FunctionOneArgNoReturn' src/main/java --include='*.kt' | head -4
