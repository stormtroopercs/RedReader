import re, os, json, collections
base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
log = open('/tmp/compile.log').read()
lines = log.splitlines()

def parse_params(text):
    if text is None or text.strip()=='': return []
    parts=[]; depth=0; cur=''; in_str=None
    for ch in text:
        if in_str:
            cur+=ch
            if ch==in_str: in_str=None
            continue
        if ch in '"\'': in_str=ch; cur+=ch; continue
        if ch in '<([': depth+=1
        elif ch in '>)]': depth-=1
        if ch==',' and depth==0: parts.append(cur.strip()); cur=''
        else: cur+=ch
    if cur.strip(): parts.append(cur.strip())
    return parts
def ptype(p): return p.split(':',1)[1].strip() if ':' in p else p.strip()
def is_null(t): return t.strip().endswith('?')
def skel(t): return t.strip().rstrip('?')

# full-repo multi-line fun index
groups = collections.defaultdict(list)
files = []
for dp,_,fs in os.walk(base):
    for f in fs:
        if f.endswith('.kt'): files.append(os.path.join(dp,f))

def index_file(p):
    txt = open(p, encoding='utf-8').read()
    ls = txt.splitlines()
    for j, dl in enumerate(ls):
        m = re.search(r'\bfun\s+(\w+)\s*\(', dl)
        if not m: continue
        nm = m.group(1)
        buf = dl
        for k in range(j+1, min(j+14, len(ls))):
            buf += '\n'+ls[k]
            if re.search(r'\)\s*(?::[^{;]*)?\{', buf) or buf.rstrip().endswith('{'): break
        mm = re.search(r'\bfun\s+'+re.escape(nm)+r'\s*\(', buf)
        if not mm: continue
        si = mm.end()-1; depth=0; in_str=None; ei=None
        for idx in range(si, len(buf)):
            ch = buf[idx]
            if in_str:
                if ch==in_str: in_str=None
            elif ch in '"\'': in_str=ch
            elif ch=='(': depth+=1
            elif ch==')':
                depth-=1
                if depth==0: ei=idx; break
        if ei is None: continue
        ps = parse_params(buf[si+1:ei])
        if not ps and not re.match(r'^\s*(abstract|open|override|final|private|protected|internal|synchronized|@)', dl.strip()):
            # zero-arg fun: still index
            pass
        groups[(nm, tuple(skel(ptype(x)) for x in ps))].append((p, j+1, ps))

for p in files: index_file(p)
# also index zero-arg
for p in files:
    txt = open(p, encoding='utf-8').read()
    ls = txt.splitlines()
    for j, dl in enumerate(ls):
        m = re.search(r'\bfun\s+(\w+)\s*\(\s*\)\s*(?::\s*([^{;]+))?', dl)
        if m:
            groups[(m.group(1), ())].append((p, j+1, []))
print(f'indexed {sum(len(v) for v in groups.values())} fun decls')

errors = []
for i, l in enumerate(lines):
    if not (l.startswith('e:') and 'overrides nothing' in l): continue
    m = re.match(r"e: (file:///\S+?):(\d+):(\d+) '?(\w+)'? overrides nothing", l)
    if not m: continue
    fp = m.group(1).replace('file://','',1)
    hint = None
    if i+1 < len(lines) and lines[i+1].strip().startswith('fun '):
        hint = lines[i+1].strip()
    errors.append({'file': fp, 'line': int(m.group(2)), 'name': m.group(4), 'hint': hint})
print(f'errors: {len(errors)}   with hint: {sum(1 for e in errors if e["hint"])}')

def child_decl(fp, lineidx, name):
    ls = open(fp, encoding='utf-8').read().splitlines()
    buf = ''
    for j in range(lineidx, min(lineidx+14, len(ls))):
        buf += (ls[j] if j==lineidx else '\n'+ls[j])
        if re.search(r'\)\s*(?::[^{;]*)?\{', buf) or buf.rstrip().endswith('{'): break
    m = re.search(r'\bfun\s+'+re.escape(name)+r'\s*\(', buf)
    if not m: return None
    si = m.end()-1; depth=0; in_str=None; ei=None
    for idx in range(si, len(buf)):
        ch = buf[idx]
        if in_str:
            if ch==in_str: in_str=None
        elif ch in '"\'': in_str=ch
        elif ch=='(': depth+=1
        elif ch==')':
            depth-=1
            if depth==0: ei=idx; break
    if ei is None: return None
    ps = parse_params(buf[si+1:ei])
    rest = buf[ei+1:].strip()
    rtype = None
    rm = re.match(r':\s*(.+?)\s*\{', rest, re.S)
    if rm: rtype = rm.group(1).strip()
    return {'params': ps, 'rtype': rtype}

# classify
prop_convert = []   # child get/isX() -> override val
null_std = collections.defaultdict(set)  # (name, skeleton) -> positions
type_align = []     # align child params to hint exactly
manual = []

for e in errors:
    nm = e['name']
    cd = child_decl(e['file'], e['line']-1, nm)
    if cd is None:
        manual.append({'file': e['file'], 'line': e['line'], 'name': nm, 'why': 'CHILD DECL NOT FOUND'}); continue
    cps = cd['params']

    if e['hint']:
        hm = re.match(r'fun\s+'+re.escape(nm)+r'\s*\((.*)\)(?:\s*:\s*(.+?))?\s*$', e['hint'])
        if not hm:
            manual.append({'file': e['file'], 'line': e['line'], 'name': nm, 'why': 'HINT PARSE FAIL', 'hint': e['hint']}); continue
        hparams = parse_params(hm.group(1))
        hrtype = (hm.group(2) or '').strip()
        if len(hparams) != len(cps):
            manual.append({'file': e['file'], 'line': e['line'], 'name': nm, 'why': f'PARAM COUNT child={len(cps)} hint={len(hparams)}', 'hint': e['hint'], 'child': cps}); continue
        # compare
        tdiff = [k for k in range(len(cps)) if skel(ptype(cps[k])) != skel(ptype(hparams[k]))]
        if tdiff:
            type_align.append({'file': e['file'], 'line': e['line'], 'name': nm,
                               'child': cps, 'hint_params': hparams, 'child_rtype': cd['rtype'],
                               'hint_rtype': hrtype, 'tdiff': tdiff, 'hint': e['hint']})
            continue
        # same skeleton: nullability diff?
        pos = [k for k in range(len(cps)) if ptype(cps[k]) != ptype(hparams[k])]
        if pos:
            # target = hint types (the base's actual signature)
            null_std[(nm, tuple(skel(ptype(x)) for x in cps))].update(pos)
            continue
        manual.append({'file': e['file'], 'line': e['line'], 'name': nm, 'why': 'IDENTICAL to hint?!', 'hint': e['hint'], 'child': cps, 'child_rtype': cd['rtype']})
    else:
        # no hint: method->property candidate?
        pm2 = re.match(r'^(get|is)([A-Z].*)$', nm)
        if pm2 and len(cps) == 0:
            if pm2.group(1) == 'get':
                prop = pm2.group(2)[0].lower() + pm2.group(2)[1:]
            else:
                prop = nm  # is* -> keep full name (isHidden, isAnimating, isAdded)
            prop_convert.append({'file': e['file'], 'line': e['line'], 'fun': nm,
                                  'prop': prop,
                                  'child_rtype': cd['rtype']})
            continue
        manual.append({'file': e['file'], 'line': e['line'], 'name': nm, 'why': 'NO HINT, NOT GET/IS'})

print(f'\nprop_convert: {len(prop_convert)}')
for pc in prop_convert:
    print(f"  {pc['file'].replace(base+'/','')}:{pc['line']} {pc['fun']} -> val {pc['prop']} (child rtype={pc['child_rtype']})")
print(f'\nnull_std groups: {len(null_std)}')
for (nm,sk),pos in sorted(null_std.items()):
    print(f'  {nm}{list(sk)} pos={sorted(pos)}')
print(f'\ntype_align: {len(type_align)}')
for ta in type_align:
    print(f"  {ta['file'].replace(base+'/','')}:{ta['line']} {ta['name']} tdiff={ta['tdiff']}")
    print(f"     child: ({', '.join(ta['child'])}) : {ta['child_rtype']}")
    print(f"     hint:  ({', '.join(ta['hint_params'])}) : {ta['hint_rtype']}")
print(f'\nmanual: {len(manual)}')
for mm in manual:
    print(f"  {mm['file'].replace(base+'/','')}:{mm['line']} {mm['name']} :: {mm['why']}")

json.dump({'prop_convert': prop_convert,
           'null_std': {f'{n}|{list(s)}': sorted(p) for (n,s),p in null_std.items()},
           'type_align': type_align, 'manual': manual},
          open('/tmp/ovplan5.json','w'), indent=1, default=str)
print('\nsaved /tmp/ovplan5.json')
