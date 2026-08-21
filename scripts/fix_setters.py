import re, os, collections, sys

base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
logpath = sys.argv[1] if len(sys.argv) > 1 else '/tmp/compile.log'
apply = '--apply' in sys.argv

# setter name -> kotlin property name (verified against class defs)
SETTER_TO_PROP = {
    'setSession': 'session',
    'setSort': 'sort',
    'setSearchString': 'searchString',
    'setDefaultAccount': 'defaultAccount',
    'setScale': 'scale',
    'setMuted': 'isMuted',   # ExoPlayerWrapperView: var isMuted
}
# skip real AndroidX/other methods
SKIP = {'setSummary', 'setAdapter', 'setOn...', }

def extract_balanced(text, open_idx):
    """given index of '(', return (inner, idx_after_close)"""
    assert text[open_idx] == '('
    depth = 0
    i = open_idx
    in_str = None
    while i < len(text):
        c = text[i]
        if in_str:
            if c == '\\': i += 2; continue
            if c == in_str: in_str = None
        else:
            if c in '"\'': in_str = c
            elif c == '(': depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0:
                    return text[open_idx+1:i], i
        i += 1
    return None, None

log = open(logpath, encoding='utf-8').read()
pat = re.compile(r"Unresolved reference '(\w+)'")
mpat = re.compile(r"e: file:///(.*?):(\d+):(\d+)")
sites = collections.defaultdict(list)
for e in log.splitlines():
    if not e.startswith('e:') or 'Unresolved reference' not in e: continue
    m = pat.search(e)
    if not m: continue
    name = m.group(1)
    if name not in SETTER_TO_PROP: continue
    mm = mpat.match(e)
    if not mm: continue
    f, ln, col = mm.group(1), int(mm.group(2)), int(mm.group(3))
    if not f.startswith('/'): f = '/'+f
    sites[f].append((ln, col, name, SETTER_TO_PROP[name]))

applied = 0
fail = []
files = 0
for f in sorted(sites):
    lines = open(f, encoding='utf-8').read().splitlines()
    for ln, col, name, prop in sorted(sites[f], key=lambda x: (-x[0], -x[1])):
        i = ln-1
        if i < 0 or i >= len(lines):
            fail.append((f, ln, name, 'badidx')); continue
        line = lines[i]
        pos = col-1
        if pos >= len(line) or not line[pos:].startswith(name):
            fail.append((f, ln, name, line.strip()[:70])); continue
        open_idx = line.find('(', pos)
        if open_idx < 0 or open_idx - pos != len(name):
            fail.append((f, ln, name, f'no ( right after: {line[pos:pos+15]!r}')); continue
        inner, close_idx = extract_balanced(line, open_idx)
        if inner is None:
            fail.append((f, ln, name, 'unbalanced')); continue
        # Only convert single-arg setters (all verified sites are single-arg)
        if inner.count(',') > 0 and re.search(r',\s*\S', inner):
            fail.append((f, ln, name, f'multi-arg: {inner[:40]!r}')); continue
        new = f'{prop} = {inner}'
        lines[i] = line[:pos] + new + line[close_idx+1:]
        applied += 1
    if apply:
        open(f, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
        files += 1

print(f'setter sites: {sum(len(v) for v in sites.values())}  files: {len(sites)}')
print(f'dry-{"RUN" if apply else "DRY"} applied: {applied}')
print(f'fail: {len(fail)}')
for x in fail[:20]: print('  ', x)
