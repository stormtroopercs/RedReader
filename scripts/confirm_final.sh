#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. JsonObject.getString return type ====="
grep -n 'fun getString' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonObject.kt
echo "===== 2. compose() params (L240-249) ====="
awk 'NR>=240 && NR<=249 {printf "%d| %s\n", NR, $0}' $f
echo "===== 3. comment() params (L266-276) ====="
awk 'NR>=266 && NR<=276 {printf "%d| %s\n", NR, $0}' $f
echo "===== 4. editComment() params (L356-364) ====="
awk 'NR>=356 && NR<=364 {printf "%d| %s\n", NR, $0}' $f
echo "===== 5. action() params (L379-387) ====="
awk 'NR>=379 && NR<=387 {printf "%d| %s\n", NR, $0}' $f
echo "===== 6. if-guards submit L216-227 (sr/title/flair_id/text/url) ====="
awk 'NR>=216 && NR<=227 {printf "%d| %s\n", NR, $0}' $f
echo "===== 7. unblockUser params (L582-593) ====="
awk 'NR>=582 && NR<=593 {printf "%d| %s\n", NR, $0}' $f
echo "===== 8. blockUser params (L606-615) ====="
awk 'NR>=606 && NR<=615 {printf "%d| %s\n", NR, $0}' $f
echo "===== 9. sendReplies params (L647-657) ====="
awk 'NR>=647 && NR<=657 {printf "%d| %s\n", NR, $0}' $f
echo "===== 10. ReportDialog submit: reasonFields arg (L120-145) ====="
awk 'NR>=120 && NR<=145 {printf "%d| %s\n", NR, $0}' src/main/java/org/quantumbadger/redreader/fragments/ReportDialog.kt
echo "===== 11. ReportReason.toPostFields / reasonFields source ====="
grep -rn 'toPostFields\|reasonFields\|report(' src/main/java/org/quantumbadger/redreader/fragments/ReportDialog.kt
echo "===== 12. sendReplies full (L647-667) ====="
awk 'NR>=647 && NR<=667 {printf "%d| %s\n", NR, $0}' $f
echo "===== 13. submit() L191-205 (flairId null guard) ====="
awk 'NR>=191 && NR<=205 {printf "%d| %s\n", NR, $0}' $f
echo "===== 14. L287-323 submit anon full ====="
awk 'NR>=287 && NR<=323 {printf "%d| %s\n", NR, $0}' $f
