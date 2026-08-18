#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. Objects.requireNonNull: import + definition ====="
grep -n 'import.*Objects' $f
grep -rn 'requireNonNull' src/main/java/org/quantumbadger/redreader/common/*.kt | head -5
FF=$(grep -rln 'fun <T' src/main/java/org/quantumbadger/redreader/common/General.kt 2>/dev/null)
grep -rn 'requireNonNull' src/main/java --include='*.kt' -l | head
echo "===== 1b. its definition ====="
for ff in $(grep -rln 'requireNonNull' src/main/java --include='*.kt'); do echo "FILE: $ff"; grep -n -B2 -A6 'fun.*requireNonNull' "$ff" | head -30; done
echo "===== 2. PostListingFragment L290-330 (subredditHandler body) ====="
awk 'NR>=290 && NR<=330 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/PostListingFragment.kt
echo "===== 3. subscriptionAction CALLERS ====="
grep -rn 'subscriptionAction' src/main/java --include='*.kt' | grep -v 'fun subscriptionAction'
echo "===== 4. EXTERNAL callers of popularSubreddits/searchSubreddits/subscribedSubreddits/requestSubredditList (RedditAPI) ====="
grep -rn 'popularSubreddits\|RedditAPI.searchSubreddits\|Companion.searchSubreddits\|subscribedSubreddits\|requestSubredditList' src/main/java --include='*.kt' | grep -v 'RedditAPI.kt' | grep -v 'fun '
echo "===== 5. RedditIdAndType.value ====="
grep -rn 'class RedditIdAndType' src/main/java --include='*.kt'
FF=$(grep -rln 'class RedditIdAndType' src/main/java --include='*.kt' | head -1); echo "FILE: $FF"; grep -n 'value' "$FF" | head -8
echo "===== 6. ReportDialog reasonFields + ReportReason.toPostFields ====="
grep -n 'reasonFields\|toPostFields' src/main/java/org/quantumbadger/redreader/fragments/ReportDialog.kt
grep -rn 'fun toPostFields\|PostField' src/main/java/org/quantumbadger/redreader/fragments/ReportDialog.kt | head
grep -rn 'class ReportReason\|fun toPostFields' src/main/java --include='*.kt' | head -5
echo "===== 7. RedditAPI L287-332 (anon SubmitResponseHandler body) ====="
awk 'NR>=287 && NR<=332 {printf "%d| %s\n", NR, $0}' $f
echo "===== 8. original Java: SubmitResponseHandler.onSubmitErrors + submit JSON listener ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/APIResponseHandler.java 2>/dev/null | grep -n 'onSubmitErrors' -B2 -A3
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'onSubmitErrors\|errors.add\|ArrayList<String> errors\|getArrayAtPath("json", "errors")' | head
echo "===== 9. ALL PostField? / PostField> sites repo-wide ====="
grep -rn 'PostField?\|LinkedList<PostField>\|MutableList<PostField>\|Collection<PostField>\|ArrayList<PostField>' src/main/java --include='*.kt' | grep -v 'http/PostField.kt\|http/body/HTTPRequestBody.kt'
echo "===== 10. RedditAPIIndividualSubredditListRequester L205-225 (offerRawSubredditData call) ====="
awk 'NR>=205 && NR<=225 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/reddit/api/RedditAPIIndividualSubredditListRequester.kt
echo "===== 11. current getStringAtPath/getArrayAtPath signatures (post base-de-null) ====="
grep -n 'fun getStringAtPath\|fun getArrayAtPath\|fun getObjectAtPath' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
echo "===== 12. PostSubmitContentFragment L368-380 (flair handler override) ====="
awk 'NR>=368 && NR<=380 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/postsubmit/PostSubmitContentFragment.kt
echo "===== 13. original Java: report(reasonFields) + PostField import in report ====="
git show $B:src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.java 2>/dev/null | grep -n 'reasonFields\|toPostFields' | head
git show $B:src/main/java/org/quantumbadger/redreader/fragments/ReportDialog.java 2>/dev/null | grep -n 'toPostFields\|PostField\|reasonFields' | head
echo "===== 14. current report() sig (RedditAPI L430-445) ====="
awk 'NR>=430 && NR<=445 {printf "%d| %s\n", NR, $0}' $f
echo "===== 15. prepareActionUri + subscriptionPrepareActionUri (L402-425, L508-530) ====="
awk 'NR>=402 && NR<=425 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=508 && NR<=535 {printf "%d| %s\n", NR, $0}' $f
