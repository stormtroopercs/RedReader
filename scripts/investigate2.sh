#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## 1. Optional.get() signature ##########"
grep -n 'fun get\|fun isPresent\|fun isEmpty' src/main/java/org/quantumbadger/redreader/common/Optional.kt
echo "########## 2. fromJsonList (RedditFlairChoice) current ##########"
FF=$(find src -name 'RedditFlairChoice.kt' | head -1); echo "file: $FF"
grep -n 'fun fromJsonList' "$FF"
echo "---- original ----"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/things/RedditFlairChoice.java 2>/dev/null | grep -n 'fromJsonList' -A3 | head
echo "########## 3. PostFields class (search all) ##########"
grep -rn 'class PostFields\|PostFields(' src/main/java --include='*.kt' | grep -i 'class\|data class' | head
FF=$(grep -rln 'class PostFields' src/main/java --include='*.kt' | head -1); echo "file: $FF"
[ -n "$FF" ] && grep -n 'class PostFields\|constructor\|fun \|MutableList\|ArrayList\|Collection\|PostField\|val ' "$FF" | head -20
echo "#### original PostFields ####"
git show $B:src/main/java/org/quantumbadger/redreader/http/PostFields.java 2>/dev/null | grep -n 'class PostFields\|PostFields(\|MutableList\|ArrayList\|Collection\|PostField\|private\|public' | head -20
echo "########## 4. RedditSubreddit WritableObject bound ##########"
grep -n 'class RedditSubreddit\|: WritableObject\|WritableObject' src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.kt | head
echo "#### original RedditSubreddit WritableObject ####"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.java 2>/dev/null | grep -n 'class RedditSubreddit\|WritableObject\|getUniqueId' | head
echo "########## 5. RawObjectDB / ThreadedRawObjectDB class decls ##########"
for n in RawObjectDB ThreadedRawObjectDB; do FF=$(find src -name "$n.kt" | head -1); echo "file: $FF"; [ -n "$FF" ] && grep -n "class $n" "$FF" | head -2; done
echo "########## 6. asSubreddit definition (all) ##########"
grep -rn 'fun asSubreddit' src/main/java --include='*.kt' | head
echo "#### original asSubreddit ####"
git show $B:src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.java 2>/dev/null | grep -n 'asSubreddit' -A4 | head
echo "########## 7. SubredditListResponse original ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'class SubredditListResponse' -A6 | head
echo "########## 8. createPostRequest callers (postFields cascade) ##########"
grep -rn 'createPostRequest(' src/main/java --include='*.kt' | grep -v 'fun createPostRequest' | head
echo "########## 9. original PostField.value + getName nullability ##########"
git show $B:src/main/java/org/quantumbadger/redreader/http/PostField.java 2>/dev/null | grep -n 'PostField(\|String name\|String value\|private\|public' | head
git show $B:src/main/java/org/quantumbadger/redreader/reddit/things/RedditSubreddit.java 2>/dev/null | grep -n 'private String name\|String getName' | head
echo "########## 10. L100-135 (flair choices + the two requireNonNull) ##########"
awk 'NR>=100 && NR<=135 {printf "%d| %s\n", NR, $0}' $f
