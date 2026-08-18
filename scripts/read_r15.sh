#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. ALL java.lang.annotation + androidx.annotation imports in RedditAPI.kt ##########"
grep -n 'import.*annotation\|import androidx.annotation' $f
echo ""
echo "########## B. A WORKING @IntDef example elsewhere in the codebase (annotation class + usage) ##########"
grep -rn 'annotation class' src/main/java --include='*.kt' | grep -i 'intdef\|@IntDef' | head
echo "--- find a file that uses @IntDef( and compiles (has annotation class) ---"
grep -rln '@IntDef(' src/main/java --include='*.kt' | head -5
echo ""
echo "########## C. ORIGINAL Java: the @IntDef annotation + imports ##########"
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'IntDef\|Retention\|annotation RedditAction\|annotation RedditSubredditAction\|import androidx.annotation\|import java.lang.annotation' | head -20
echo ""
echo "########## D. fromJsonList signature ##########"
grep -rn 'fun fromJsonList' src/main/java --include='*.kt' | head -5
