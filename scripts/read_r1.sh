#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## 130-185 (flair array/choices) ##########"
sed -n '130,185p' $f
echo "########## 460-500 (getSubreddit handler) ##########"
sed -n '460,500p' $f
echo "########## 755-800 (requestSubredditList + ValueResponseHandler) ##########"
sed -n '755,800p' $f
