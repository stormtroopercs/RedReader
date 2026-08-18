import re, os, json, sys, collections
base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
APPLY = '--apply' in sys.argv
log = open('/tmp/compile.log').read()
lines = log.splitlines()

def fpath_fix(fp):
    p = '/'+fp
    if os.path.exists(p): return p
    return None

# 1. Collect all overrides-nothing WITHOUT a potential-signatures hint
nohint = []
for i, l in enumerate(lines):
    if l.startswith('e:') and 'overrides nothing' in l:
        m = re.match(r"e: (file:///\S+?):(\d+):(\d+) \w+'?(\w+)'? overrides nothing", l) or \
            re.match(r"e: (file:///\S+?):(\d+):(\d+) .*?(\w+) overrides nothing", l)
        if not m: continue
        fp = fpath_fix(m.group(1))
        # check next line for hint
        has_hint = i+1 < len(lines) and 'Potential signatures' in lines[i+1]
        if not has_hint:
            nohint.append({'file': fp, 'line': int(m.group(2)), 'name': m.group(4), 'raw': l})

print(f'nohint overrides-nothing: {len(nohint)}')

# 2. For each, extract the full fun declaration + body
def extract_fun_body(fpath, lineidx, name):
    ls = open(fpath, encoding='utf-8').read().splitlines()
    dline = ls[lineidx]
    if not re.search(r'\bfun\s+'+re.escape(name)+r'\b', dline):
        return None
    retm = re.search(r'\)\s*(?::\s*([^{\s=][^=]*?))?\s*\{', dline)
    # find return type
    rtype = None
    pm = re.search(r'\)\s*:\s*(.+?)\s*\{', dline)
    if pm: rtype = pm.group(1)
    # find body braces
    start = dline.find('{')
    depth = 0; in_str = None; in_chr = None
    body_lines = []
    for j in range(lineidx, min(lineidx+40, len(ls))):
        line = ls[j]
        s = j == lineidx and start or 0
        for idx in range(s if j == lineidx else 0, len(line)):
            ch = line[idx]
            if in_str:
                if ch == in_str: in_str = None
                continue
            if in_chr:
                if ch == in_chr: in_chr = None
                continue
            if ch == '"': in_str = ch
            elif ch == "'": in_chr = ch
            elif ch == '{':
                depth += 1
                if depth == 1:
                    rest = line[idx+1:].strip()
                    if rest: body_lines.append(rest)
                continue
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    return {'decl_line': lineidx, 'decl': dline, 'rtype': rtype, 'body': body_lines, 'end_line': j}
    return None

results = []
for d in nohint:
    if not d['file']: continue
    fb = extract_fun_body(d['file'], d['line']-1, d['name'])
    if fb:
        fb['name'] = d['name']
        fb['rel'] = d['file'].replace('/opt/data/RedReader/src/main/java/org/quantumbadger/redreader/','')
        results.append(fb)
    else:
        print(f"  EXTRACT-FAIL {d['file']}:{d['line']} {d['name']}")

print(f'extracted: {len(results)}')
# summarize body shapes
shapes = collections.Counter()
for fb in results:
    b = ' '.join(fb['body']).strip()
    if re.fullmatch(r'return .+', b): shapes['single-return'] += 1
    elif 'return' in b: shapes['multi-stmt'] += 1
    else: shapes['other'] += 1
print('shapes:', dict(shapes))
# show multi-stmt and other
for fb in results:
    b = ' '.join(fb['body']).strip()
    if not re.fullmatch(r'return .+', b):
        print(f"\n  {fb['rel']}:{fb['decl_line']+1} fun {fb['name']}(): {fb['rtype']}")
        for bl in fb['body'][:4]:
            print(f"    | {bl[:100]}")

json.dump([{k:v for k,v in fb.items() if k!='body'} | {'body': fb['body']} for fb in results], open('/tmp/nohint2.json','w'))
print('saved /tmp/nohint2.json')
