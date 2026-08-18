#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "########## A. L100-122 (L119 JsonObject?) ##########"
awk 'NR>=100 && NR<=122 {printf "%d| %s\n", NR, $0}' $f
echo "########## B. L150-160 (L156 fromJsonList) ##########"
awk 'NR>=150 && NR<=160 {printf "%d| %s\n", NR, $0}' $f
echo "########## C. L155-177 (choices Optional) ##########"
grep -n 'choices' $f | head
echo "########## D. L845-860 (L854) ##########"
awk 'NR>=845 && NR<=860 {printf "%d| %s\n", NR, $0}' $f
echo "########## E. L890-920 (L899-916) ##########"
awk 'NR>=890 && NR<=920 {printf "%d| %s\n", NR, $0}' $f
echo "########## F. L1105-1125 (L1118) ##########"
awk 'NR>=1105 && NR<=1125 {printf "%d| %s\n", NR, $0}' $f
