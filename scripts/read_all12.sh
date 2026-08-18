#!/bin/bash
cd /opt/data/RedReader
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
p() { awk -v a=$1 -v b=$2 'NR>=a && NR<=b {printf "%d| %s\n", NR, $0}' $f; }
echo "===== L110-125 (118) ====="; p 110 125
echo "===== L166-182 (175) ====="; p 166 182
echo "===== L470-492 (478-489) ====="; p 470 492
echo "===== L785-795 (789-790) ====="; p 785 795
echo "===== L855-863 (860) ====="; p 855 863
echo "===== L1000-1010 (1006) ====="; p 1000 1010
echo "===== L1134-1144 (1139-1141) ====="; p 1134 1144
