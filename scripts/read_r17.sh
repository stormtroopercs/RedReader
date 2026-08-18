#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. RedditAPI 460-500 (subscriptionAction anon handler) ##########"
awk 'NR>=460 && NR<=500 {printf "%d| %s\n", NR, $0}' $f
echo "########## B. RedditAPI 660-800 (3 wrappers + subscribedSubredditsInternal) ##########"
awk 'NR>=660 && NR<=800 {printf "%d| %s\n", NR, $0}' $f
