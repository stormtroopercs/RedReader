#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/api/RedditOAuth.kt
echo "count 'failureType: RequestFailureType': $(grep -c 'failureType: RequestFailureType' $f)"
echo "count 'body: InputStream?': $(grep -c 'body: InputStream?' $f)"
echo "count 'body : InputStream?': $(grep -c 'body : InputStream?' $f)"
echo "=== the onSuccess body lines (exact) ==="
grep -n 'InputStream?' $f
echo "=== import RequestFailureType? ==="
grep -n 'import.*RequestFailureType' $f || echo "NO import -> needs adding"
echo "=== JsonValue.parse sig ==="
grep -rn 'fun parse' src/main/java/org/quantumbadger/redreader/jsonwrap/JsonValue.kt | head
