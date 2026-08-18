#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
p() { echo "----- lines $1-$2 -----"; awk -v a=$1 -v b=$2 'NR>=a && NR<=b {printf "%d| %s\n", NR, $0}' $f; }
p 100 122
p 160 185
p 470 495
p 780 800
p 850 870
p 995 1012
p 1125 1150
