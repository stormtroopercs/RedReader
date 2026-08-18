#!/bin/bash
cd /opt/data/RedReader
B=1d35f61e
f=src/main/java/org/quantumbadger/redreader/reddit/RedditAPI.kt
echo "===== 1. function signatures enclosing the postFields blocks (L90-110, 195-235, 240-260, 270-290, 320-340, 355-375, 380-400, 440-465, 580-600, 605-625, 650-665) ====="
awk 'NR>=90 && NR<=100 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=195 && NR<=235 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=240 && NR<=260 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=270 && NR<=290 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=320 && NR<=340 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=355 && NR<=375 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=380 && NR<=400 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=440 && NR<=465 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=580 && NR<=600 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=605 && NR<=625 {printf "%d| %s\n", NR, $0}' $f
echo "---"
awk 'NR>=650 && NR<=665 {printf "%d| %s\n", NR, $0}' $f
