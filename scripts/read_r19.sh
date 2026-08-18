#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. L115-121 (L118 JsonObject?) ##########"
awk 'NR>=115 && NR<=121 {printf "%d| %s\n", NR, $0}' $f
echo "########## B. L170-180 (L175 flairChoices arg) ##########"
awk 'NR>=170 && NR<=180 {printf "%d| %s\n", NR, $0}' $f
echo "########## C. L476-500 (anon handler + L489) ##########"
awk 'NR>=476 && NR<=500 {printf "%d| %s\n", NR, $0}' $f
echo "########## D. L786-800 (L790 ValueResponseHandler) ##########"
awk 'NR>=786 && NR<=800 {printf "%d| %s\n", NR, $0}' $f
echo "########## E. L855-865 (L860 Optional<String>) ##########"
awk 'NR>=855 && NR<=865 {printf "%d| %s\n", NR, $0}' $f
echo "########## F. L1000-1012 (L1006 makeRequest) ##########"
awk 'NR>=1000 && NR<=1012 {printf "%d| %s\n", NR, $0}' $f
echo "########## G. L1133-1145 (L1139/1141) ##########"
awk 'NR>=1133 && NR<=1145 {printf "%d| %s\n", NR, $0}' $f
