#!/usr/bin/env python3
import re, collections, json
from pathlib import Path

log = Path("/tmp/compile.log").read_text(errors="replace")
pat = re.compile(r"^e: file:///(.*?):(\d+):(\d+) (.*)$")
per_file = collections.Counter()
file_errors = collections.defaultdict(list)
for line in log.splitlines():
    m = pat.match(line)
    if not m:
        continue
    f, ln, col, msg = m.groups()
    short = f.split("org/quantumbadger/redreader/")[-1] if "org/quantumbadger/redreader/" in f else f
    per_file[short] += 1
    file_errors[short].append((int(ln), msg))

total = sum(per_file.values())
print(f"TOTAL PARSED: {total}  (raw grep count {len([l for l in log.splitlines() if l.startswith('e:')])})")
print(f"FILES WITH ERRORS: {len(per_file)}")
print("\n=== TOP 25 FILES ===")
for f, c in per_file.most_common(25):
    print(f"{c:4d}  {f}")

# Save full per-file detail to workspace for reading
out = Path("/opt/data/err_analysis.json")
data = {f: [{"line": ln, "msg": m} for ln, m in file_errors[f]] for f in per_file}
out.write_text(json.dumps(data, indent=1))
print(f"\nSaved per-file detail -> {out}")
