import re, os, collections, json, sys

base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
logpath = sys.argv[1] if len(sys.argv) > 1 else '/tmp/compile.log'
apply = '--apply' in sys.argv

def to_prop(name):
    if name.startswith('get'): s = name[3:]
    elif name.startswith('is'): return None  # handled manually: receiver-dependent
    else: return None
    return s[0].lower()+s[1:] if s else name

idx = json.load(open('/tmp/class_index.json'))
all_props = set()
for ps in idx['class_props'].values():
    all_props.update(ps)

log = open(logpath, encoding='utf-8').read()
pat = re.compile(r"Unresolved reference '(\w+)'")
mpat = re.compile(r"e: file:///(.*?):(\d+):(\d+)")
sites = collections.defaultdict(list)
for e in log.splitlines():
    if not e.startswith('e:') or 'Unresolved reference' not in e: continue
    m = pat.search(e)
    if not m: continue
    name = m.group(1)
    prop = to_prop(name)
    if prop is None or prop not in all_props: continue
    mm = mpat.match(e)
    if not mm: continue
    f, ln, col = mm.group(1), int(mm.group(2)), int(mm.group(3))
    if not f.startswith('/'): f = '/'+f
    sites[f].append((ln, col, name, prop))

applied = 0
fail = []
files_touched = 0
for f in sorted(sites):
    lst = sites[f]
    lines = open(f, encoding='utf-8').read().splitlines()
    for ln, col, name, prop in sorted(lst, key=lambda x: (-x[0], -x[1])):
        i = ln-1
        if i < 0 or i >= len(lines):
            fail.append((f, ln, name, 'badidx')); continue
        line = lines[i]
        pos = col-1
        if pos >= len(line) or not line[pos:].startswith(name):
            fail.append((f, ln, name, line.strip()[:70])); continue
        after = line[pos+len(name):]
        if after.startswith('()'):
            lines[i] = line[:pos] + prop + after[2:]
            applied += 1
        else:
            fail.append((f, ln, name, 'after=%r' % after[:25]))
    if apply:
        open(f, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
        files_touched += 1

print(f'sites planned: {sum(len(v) for v in sites.values())}  files: {len(sites)}')
print(f'dry-{"RUN" if apply else "DRY"} applied: {applied}')
print(f'fail: {len(fail)}')
for x in fail[:20]: print('  ', x)
