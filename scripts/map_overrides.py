#!/usr/bin/env python3
"""For each 'overrides nothing' / 'not abstract' error, pull the implementer
override signature from the source and print it alongside, so we can see the
mismatch vs the base interface."""
import json, re
from pathlib import Path

root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'
log = Path("/tmp/compile.log").read_text(errors="replace")
pat = re.compile(r"^e: file:///(.*?):(\d+):(\d+) (.*)$")

errs = []
for line in log.splitlines():
    m = pat.match(line)
    if m:
        f, ln, col, msg = m.groups()
        if "overrides nothing" in msg or "not abstract and does not" in msg:
            short = f.split("org/quantumbadger/redreader/")[-1]
            errs.append((short, int(ln), msg))

# Group by file
byfile = {}
for f, ln, msg in errs:
    byfile.setdefault(f, []).append((ln, msg))

def read_around(f, ln, before=0, after=6):
    p = root / f
    if not p.exists():
        return "[no file]"
    lines = p.read_text(errors="replace").splitlines()
    start = max(0, ln - 1 - before)
    out = []
    for i in range(start, min(len(lines), ln - 1 + after)):
        out.append(f"   {i+1:4d}| {lines[i]}")
    return "\n".join(out)

for f in sorted(byfile):
    print(f"\n{'='*72}\nFILE {f}")
    for ln, msg in byfile[f]:
        print(f"  {ln:4d}  {msg[:80]}")
        print(read_around(f, ln, after=5))
