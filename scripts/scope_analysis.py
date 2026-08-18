import re, json, collections, sys

pat2 = re.compile(r"^e: file:///(\S+?)\.kt:(\d+):(\d+) (.*)$")
errs2 = []
with open("/tmp/compile2.log") as f:
    for l in f:
        m = pat2.match(l)
        if m:
            errs2.append((m.group(1), int(m.group(2)), m.group(4)))

print("TOTAL PARSED ERRORS:", len(errs2))

cats = collections.Counter()
for _, _, msg in errs2:
    if msg.startswith("Unresolved reference '"):
        cats["A. Unresolved reference"] += 1
    elif "type mismatch" in msg.lower():
        cats["B. Type mismatch"] += 1
    elif msg.startswith("Syntax error"):
        cats["C. Syntax error"] += 1
    elif "overrides nothing" in msg:
        cats["D. 'overrides nothing' (mechanical rename of override)"] += 1
    elif "cannot be invoked as a function" in msg:
        cats["E. Java-style getter called as function (x.getFoo())"] += 1
    elif "Property must be initialized or be abstract" in msg:
        cats["F. Property must be initialized/abstract (dropped field init?)"] += 1
    elif "cannot be reassigned" in msg:
        cats["G. 'val' cannot be reassigned (field->val conversion of mutable state)"] += 1
    elif msg.startswith("Unresolved label"):
        cats["H. Unresolved label (converter artifact?)"] += 1
    elif "exhaustive" in msg:
        cats["I. 'when' not exhaustive"] += 1
    elif "not abstract and does not implement" in msg:
        cats["J. Class not abstract / unimplemented members"] += 1
    elif "Cannot weaken access" in msg:
        cats["K. Access privilege mismatch on overrides"] += 1
    elif "within its bounds" in msg:
        cats["L. Type argument out of bounds (mangled generics)"] += 1
    elif "prohibited" in msg:
        cats["M. Varargs-in-named-form prohibited"] += 1
    elif "No value passed for parameter" in msg:
        cats["N. Missing parameter"] += 1
    else:
        key = "Z. Other: " + re.sub(r"\(.*", "", msg)[:70]
        cats[key] += 1

print("\n=== CATEGORY COUNTS ===")
total = 0
for k, v in cats.most_common():
    print(f"{v:5d}  {k}")
    total += v
print("sum:", total)

# Unresolved reference histogram
ur = collections.Counter()
for _, _, msg in errs2:
    m = re.match(r"Unresolved reference '(\w+)'", msg)
    if m: ur[m.group(1)] += 1
print("\n=== UNRESOLVED REF NAMES (all) ===")
for k, v in ur.most_common(100):
    print(f"{v:5d}  {k}")

print("\n=== TOP FILES BY ERROR COUNT ===")
for k, v in collections.Counter(p for p, _, _ in errs2).most_common(30):
    short = k.split("/org/quantumbadger/redreader/")[-1]
    print(f"{v:5d}  {short}")

# Per-category file spread
print("\n=== PER-CATEGORY FILE SPREAD ===")
def catname(msg):
    if msg.startswith("Unresolved reference '"): return "A"
    if "type mismatch" in msg.lower(): return "B"
    if msg.startswith("Syntax error"): return "C"
    if "overrides nothing" in msg: return "D"
    if "cannot be invoked as a function" in msg: return "E"
    if "Property must be initialized or be abstract" in msg: return "F"
    if "cannot be reassigned" in msg: return "G"
    if msg.startswith("Unresolved label"): return "H"
    if "exhaustive" in msg: return "I"
    if "not abstract and does not implement" in msg: return "J"
    if "Cannot weaken access" in msg: return "K"
    if "within its bounds" in msg: return "L"
    if "prohibited" in msg: return "M"
    if "No value passed for parameter" in msg: return "N"
    return "Z"

spread = collections.defaultdict(collections.Counter)
for p, ln, m in errs2:
    short = p.split("/org/quantumbadger/redreader/")[-1]
    spread[catname(m)][short] += 1

for c in sorted(spread):
    files = spread[c]
    top = ", ".join(f"{k}({v})" for k, v in files.most_common(5))
    print(f"\n[{c}] {sum(files.values())} errs across {len(files)} files: {top}")

# Samples
print("\n=== SAMPLES ===")
def sample(label, pred, n=5):
    print(f"\n--- {label} ---")
    cnt = 0
    for p, ln, m in errs2:
        if pred(m):
            print(f"  {p.split('/')[-1]}:{ln}  {m[:150]}")
            cnt += 1
            if cnt >= n: break

sample("Syntax errors", lambda m: m.startswith("Syntax error"), 8)
sample("Unresolved label", lambda m: m.startswith("Unresolved label"), 8)
sample("Type mismatch", lambda m: "type mismatch" in m.lower(), 8)
sample("Type bounds", lambda m: "within its bounds" in m, 5)
sample("Property init", lambda m: "Property must be initialized" in m, 6)
sample("val reassigned", lambda m: "cannot be reassigned" in m, 6)
sample("overrides nothing", lambda m: "overrides nothing" in m, 8)
sample("getter-as-function", lambda m: "cannot be invoked as a function" in m, 8)
sample("Other Z", lambda m: catname(m)=="Z", 10)

# Save structured
with open("/opt/data/errs_full.json", "w") as f:
    json.dump([{"file": p, "line": ln, "msg": m} for p, ln, m in errs2], f)
print("\nsaved /opt/data/errs_full.json")
