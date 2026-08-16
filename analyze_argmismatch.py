import re, os, collections, json
base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
log = open('/tmp/compile.log').read()
lines = log.splitlines()

# Parse all ARG_MISMATCH errors
am = []
for i, l in enumerate(lines):
    if not (l.startswith('e:') and 'Argument type mismatch' in l): continue
    m = re.match(r"e: (file:///\S+?):(\d+):(\d+) Argument type mismatch: actual type is '([^']+)', but '([^']*)' was expected", l)
    if not m: continue
    am.append({'file': m.group(1).replace('file://','',1), 'line': int(m.group(2)), 'col': int(m.group(3)),
               'actual': m.group(4), 'expected': m.group(5)})

print(f'ARG_MISMATCH total: {len(am)}')

# 1. How many are the simple nullable form: actual = 'T?', expected = 'T'
simple = [a for a in am if re.fullmatch(r'.+?', a['actual']) and a['actual'][:-1]==a['expected']]
print(f'simple nullable (T? -> T): {len(simple)}')

# 2. extract the argument expression at the error column
def extract_expr(fpath, line, col):
    ls = open(fpath, encoding='utf-8').read().splitlines()
    if line-1 >= len(ls): return None
    ln = ls[line-1]
    # find start of expression: walk left from col-1 through identifiers, dots, parens
    s = col-1
    while s > 0 and (ln[s-1].isalnum() or ln[s-1] in '._[]'):
        s -= 1
    # skip leading receiver dots to get just last component? keep full
    return ln[s:col-1].strip() or ln[max(0,col-20):col].strip()

expr_count = collections.Counter()
expr_files = collections.defaultdict(set)
for a in am:
    if not os.path.exists(a['file']): continue
    e = extract_expr(a['file'], a['line'], a['col'])
    if e:
        expr_count[e] += 1
        expr_files[e].add(a['file'].replace(base+'/',''))

print('\n=== top 40 actual-side expressions ===')
for e, c in expr_count.most_common(40):
    print(f'  {c:4d}  {e:45s}  files={sorted(expr_files[e])[:3]}')

# 3. distribution by file
fc = collections.Counter(a['file'].replace(base+'/','') for a in am)
print('\n=== top 20 files ===')
for f, c in fc.most_common(20): print(f'  {c:4d}  {f}')

json.dump(am, open('/tmp/argmismatch.json','w'), indent=1)
print('\nsaved /tmp/argmismatch.json')
