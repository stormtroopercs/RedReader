#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## 1. L110-125 (L118 JsonObject? receiver) ##########"
awk 'NR>=110 && NR<=125 {printf "%d| %s\n", NR, $0}' $f
echo "########## 2. L160-180 (L175 MutableList<RedditFlairChoice?>?) ##########"
awk 'NR>=160 && NR<=180 {printf "%d| %s\n", NR, $0}' $f
echo "########## 3. L470-500 (anon RequestResponseHandler + L489) ##########"
awk 'NR>=470 && NR<=500 {printf "%d| %s\n", NR, $0}' $f
echo "########## 4. L780-800 (anon ValueResponseHandler) ##########"
awk 'NR>=780 && NR<=800 {printf "%d| %s\n", NR, $0}' $f
echo "########## 5. L850-865 (L860 Optional<String> vs ?) ##########"
awk 'NR>=850 && NR<=865 {printf "%d| %s\n", NR, $0}' $f
echo "########## 6. L995-1015 (L1006 no candidates) ##########"
awk 'NR>=995 && NR<=1015 {printf "%d| %s\n", NR, $0}' $f
echo "########## 7. L1125-1145 (L1139/1141) ##########"
awk 'NR>=1125 && NR<=1145 {printf "%d| %s\n", NR, $0}' $f
