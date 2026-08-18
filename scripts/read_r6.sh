#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. L490 function: subscriptionPrepareActionUri + prepareActionUri sig ##########"
grep -n 'fun subscriptionPrepareActionUri\|fun prepareActionUri' $f
sed -n '/fun subscriptionPrepareActionUri/,/^    }/p' $f | head -20
echo "########## B. L1000-1008 makeRequest (createPostRequestUnprocessedResponse) - the 'None of the following' ##########"
echo "   (CacheRequest ctors: 117=10arg, 142=9arg, 165=10arg-with-reqBody; call has 10 args: url,user,null,Pri,Strat,FileType,QueueType,PostFields,context,callbacks)"
echo "   queueType type in ctor = DownloadQueueType? ; call passes DownloadQueueType.REDDIT_API (non-null) - OK"
echo "   6th ctor arg 'cache: Boolean' vs call 'PostFields(postFields)'? MISMATCH - call has NO bool!"
echo "########## C. BezelSwipeOverlay current error count (does @IntDef+java Retention work?) ##########"
grep -c '^e:.*BezelSwipeOverlay' /tmp/compile.log
echo "########## D. JsonValue base: asArray/asObject/asString return types ##########"
sed -n '13,58p' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt
echo "########## E. ORIGINAL JsonValue asArray/asObject/asString ##########"
grep -n 'asArray\|asObject\|asString' /tmp/JsonValue.java | head
echo "########## F. JsonArray size() ##########"
grep -n 'fun size' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonArray.kt
echo "########## G. how many getArrayAtPath/getStringAtPath/getObjectAtPath callers across codebase (impact of de-null) ##########"
grep -rn 'getArrayAtPath\|getStringAtPath\|getObjectAtPath' src/main/java --include='*.kt' | wc -l
