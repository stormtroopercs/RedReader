#!/bin/bash
# Summarize /tmp/compile.log: total errors, per-file counts
LOG=/tmp/compile.log
echo "total: $(grep -c '^e:' "$LOG")"
echo "--- per-file (top 30) ---"
grep '^e:' "$LOG" | sed 's|.*src/main/java/||; s|:.*||' | sort | uniq -c | sort -rn | head -30
echo "--- files with errors ---"
grep '^e:' "$LOG" | sed 's|.*src/main/java/||; s|:.*||' | sort -u | wc -l
