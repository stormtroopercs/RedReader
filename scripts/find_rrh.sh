#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== A. FIND RequestResponseHandler definition (by method names) ====="
grep -rln 'onRequestSuccess' src/main/java --include='*.kt'
echo "----- definition + full body -----"
FF=$(grep -rln 'onRequestSuccess' src/main/java --include='*.kt' | head -1); echo "FILE=$FF"
LN=$(grep -n 'onRequestSuccess' "$FF" | head -1 | cut -d: -f1)
awk -v s=$LN 'NR>=s-6 && NR<=s+14 {printf "%d| %s\n", NR, $0}' "$FF"
echo "===== B. L660-745 (searchSubreddits / requestSubredditList call sites) ====="
awk 'NR>=660 && NR<=745 {printf "%d| %s\n", NR, $0}' $f
echo "===== C. PostSubmitContentFragment SubmitResponseHandler impl (L475-525) ====="
awk 'NR>=475 && NR<=525 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "===== D. ALL PostField( call sites in RedditAPI.kt (check arg nullability) ====="
grep -n 'PostField(' $f
echo "===== E. RedditSubredditManager: subredditDbWrapper type + imports ====="
grep -n 'subredditDbWrapper\|RawObjectDB\|ThreadedRawObjectDB\|PermanentCache\|import.*ObjectDB\|import.*Cache' src/main/java/org/quantumbadger/redreader/reddit/RedditSubredditManager.kt | head
