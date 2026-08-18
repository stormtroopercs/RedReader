import re, os, json, collections
base = str(Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader')
log = open('/tmp/compile.log').read()
lines = log.splitlines()

# ---------- class index: file -> [(classname, startline, supertypes)] ----------
def parse_supertypes(txt, after_pos):
    """Given position right after the class name, capture ': A<..>, B' up to '{'."""
    i = after_pos
    depth = 0; in_str=None
    seg = ''
    while i < len(txt) and len(seg) < 400:
        ch = txt[i]
        if in_str:
            seg+=ch
            if ch==in_str: in_str=None
            i+=1; continue
        if ch in '"\'': in_str=ch; seg+=ch; i+=1; continue
        if ch=='{': break
        if ch=='(': depth+=1
        if ch==')' and depth>0: depth-=1
        seg+=ch; i+=1
    if ':' not in seg: return []
    seg = seg.split(':',1)[1]
    parts=[]; d=0; cur=''
    for ch in seg:
        if ch in '<([': d+=1
        elif ch in '>)]': d-=1
        if ch==',' and d==0: parts.append(cur.strip()); cur=''
        else: cur+=ch
    if cur.strip(): parts.append(cur.strip())
    out=[]
    for p in parts:
        m = re.match(r'([A-Za-z_][\w.]*(?:<[^>]*>)?(?:\?\)?)?)', p)
        out.append(p.strip())
    return out

classes = {}  # file -> list of (name, startline, supertypes, endline)
files = []
for dp,_,fs in os.walk(base):
    for f in fs:
        if f.endswith('.kt'): files.append(os.path.join(dp,f))

for p in files:
    txt = open(p, encoding='utf-8').read()
    ls = txt.splitlines()
    found = []
    for m in re.finditer(r'^(?:\s*)(?:abstract\s+|open\s+|sealed\s+|data\s+|inner\s+)*(class|interface|object)\s+([A-Za-z_]\w*)', txt, re.M):
        kw, name = m.group(1), m.group(2)
        # find the opening '{' (skip constructor parens)
        j = m.end()
        d = 0; in_str=None
        while j < len(txt) and len(txt[m.end():j]) < 1500:
            ch = txt[j]
            if in_str:
                if ch==in_str: in_str=None
            elif ch in '"\'': in_str=ch
            elif ch=='(': d+=1
            elif ch==')' and d>0: d-=1
            elif ch=='{' and d==0: break
            j+=1
        if j >= len(txt): continue
        startline = txt.count('\n',0,m.start())+1
        sups = parse_supertypes(txt, m.end())
        # endline: track brace depth from j
        depth=0; endline=startline; in_str=None
        for k in range(j, len(txt)):
            ch = txt[k]
            if in_str:
                if ch==in_str: in_str=None
            elif ch in '"\'': in_str=ch
            elif ch=='{': depth+=1
            elif ch=='}':
                depth-=1
                if depth==0:
                    endline = txt.count('\n',0,k)+1
                    break
        found.append((name, startline, sups, endline))
    classes[p] = found

print(f'class index: {sum(len(v) for v in classes.values())} classes in {len(classes)} files')

# ---------- member index per class: (file, class) -> {propname: (type, line), funname: [(params, line)]} ----------
def class_members(p, cinfo):
    name, sl, sups, el = cinfo
    ls = open(p, encoding='utf-8').read().splitlines()
    members = {'props': {}, 'funs': collections.defaultdict(list)}
    for i in range(sl-1, min(el, len(ls))):
        dl = ls[i]
        ind = len(dl) - len(dl.lstrip())
        if ind <= 0: continue
        pm = re.match(r'^\s*(abstract\s+|open\s+|override\s+|final\s+|protected\s+|private\s+|internal\s+|lateinit\s+)*(val|var)\s+(\w+)\s*:\s*([^\n{=]+)', dl)
        if pm:
            members['props'][pm.group(3)] = (pm.group(4).strip(), i+1)
            continue
        fm = re.match(r'^\s*(abstract\s+|open\s+|override\s+|final\s+|protected\s+|private\s+|internal\s+)*fun\s+(\w+)\s*\(', dl)
        if fm:
            # gather multi-line params
            buf = dl
            for k in range(i+1, min(i+10, len(ls))):
                buf += '\n'+ls[k]
                if re.search(r'\)\s*(?::[^{;]*)?\{', buf) or buf.rstrip().endswith('{'): break
            mm = re.search(r'\bfun\s+'+re.escape(fm.group(2))+r'\s*\(', buf)
            si = mm.end()-1; depth=0; in_str=None; ei=None
            for idx in range(si, len(buf)):
                ch=buf[idx]
                if in_str:
                    if ch==in_str: in_str=None
                elif ch in '"\'': in_str=ch
                elif ch=='(': depth+=1
                elif ch==')':
                    depth-=1
                    if depth==0: ei=idx; break
            if ei is None: continue
            members['funs'][fm.group(2)].append((buf[si+1:ei], i+1))
    return members

# resolve supertype name -> class info (by simple name, prefer same package)
def resolve_sup(sup):
    simple = re.sub(r'<.*', '', sup).split('.')[-1]
    # exact simple-name match
    cands = []
    for p, clist in classes.items():
        for c in clist:
            if c[0] == simple:
                cands.append((p, c))
    return cands

# ---------- collect errors ----------
errors = []
for i, l in enumerate(lines):
    if not (l.startswith('e:') and 'overrides nothing' in l): continue
    m = re.match(r"e: (file:///\S+?):(\d+):(\d+) '?(\w+)'? overrides nothing", l)
    if not m: continue
    fp = m.group(1).replace('file://','',1)
    if not os.path.exists(fp): continue
    hint = None
    if i+1 < len(lines) and 'Potential signatures' in lines[i+1]:
        hm = re.search(r'Potential signatures:\s*(.*)', lines[i+1].strip())
        if hm: hint = hm.group(1)
    errors.append({'file': fp, 'line': int(m.group(2)), 'name': m.group(4), 'hint': hint})
print(f'overrides-nothing errors: {len(errors)}')

def enclosing_class(fp, line):
    for c in classes.get(fp, []):
        if c[1] <= line <= c[3]:
            return c
    return None

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

def child_decl(fp, lineidx, name):
    ls = open(fp, encoding='utf-8').read().splitlines()
    buf = ''
    for j in range(lineidx, min(lineidx+12, len(ls))):
        buf += (ls[j] if j==lineidx else '\n'+ls[j])
        if re.search(r'\)\s*(?::[^{;]*)?\{', buf) or buf.rstrip().endswith('{'): break
    m = re.search(r'\bfun\s+'+re.escape(name)+r'\s*\(', buf)
    if not m: return None
    si = m.end()-1; depth=0; in_str=None; ei=None
    for idx in range(si, len(buf)):
        ch=buf[idx]
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

# ---------- classify ----------
prop_converts = []
null_groups = collections.defaultdict(set)
framework_align = []
manual = []

member_cache = {}
def get_members(fp, c):
    key = (fp, c[0], c[1])
    if key not in member_cache:
        member_cache[key] = class_members(fp, c)
    return member_cache[key]

for e in errors:
    nm = e['name']
    cls = enclosing_class(e['file'], e['line'])
    cd = child_decl(e['file'], e['line']-1, nm)
    if cd is None:
        manual.append((e['file'], e['line'], nm, 'CHILD DECL NOT FOUND')); continue
    cps = cd['params']

    # 1. find base member via hierarchy (depth-limited BFS over supertypes)
    base_found = None  # ('prop', name, type, file, line) or ('fun', params, file, line, is_base_non_override)
    seen_classes = set()
    queue = [(e['file'], cls[0], cls) for cls in ([cls] if cls else [])]
    depth = 0
    while queue and depth < 4:
        nextq = []
        for (cf, cname, cinfo) in queue:
            if cname in seen_classes: continue
            seen_classes.add(cname)
            mem = get_members(cf, cinfo)
            # property candidates
            for cand in [nm, nm[2:] if nm.startswith('is') and len(nm)>2 and nm[2].isupper() else None,
                          nm[3:] if nm.startswith('get') and len(nm)>3 and nm[3].isupper() else None]:
                if cand and cand in mem['props']:
                    base_found = ('prop', cand, mem['props'][cand][0], None, None)
                    break
            if base_found: break
            # fun candidate
            if nm in mem['funs']:
                for (ps, pl) in mem['funs'][nm]:
                    pps = parse_params(ps)
                    base_found = ('fun', pps, None, None, None)
                    break
            if base_found: break
            # queue supertypes
            for s in cinfo[2]:
                for (p2, c2) in resolve_sup(s):
                    if c2[0] not in seen_classes:
                        nextq.append((c2[0], c2))
        queue = nextq
        depth += 1

    if base_found and base_found[0] == 'prop':
        _, pname, ptype_, _, _ = base_found
        # collision check: does child already have a member with target name?
        clmem = get_members(e['file'], cls) if cls else {'props': {}, 'funs': {}}
        if pname in clmem['props']:
            manual.append((e['file'], e['line'], nm, f'COLLISION: child already has {pname}')); continue
        prop_converts.append({'file': e['file'], 'line': e['line'], 'fun': nm, 'prop': pname,
                              'base_type': ptype_, 'child_rtype': cd['rtype'], 'hint': e['hint']})
        continue

    if base_found and base_found[0] == 'fun':
        bps = base_found[1]
        if len(bps) == len(cps):
            pos = [k for k in range(len(cps)) if is_null(ptype(bps[k])) and not is_null(ptype(cps[k]))]
            if pos:
                null_groups[(nm, tuple(skel(ptype(x)) for x in cps))].update(pos)
                continue
            # non-null base, nullable child? (shouldn't error) or exact match but other diff
        manual.append((e['file'], e['line'], nm, f'IN-REPO FUN BASE, params base={[ptype(x) for x in bps]} child={[ptype(x) for x in cps]}'))
        continue

    # 2. no in-repo base: use framework hint
    if e['hint']:
        hm = re.search(r'fun\s+'+re.escape(nm)+r'\s*\(([^)]*)\)\s*(?::\s*([^{]+))?\{', e['hint'])
        if hm:
            hparams = parse_params(hm.group(1))
            hrtype = (hm.group(2) or '').strip()
            if len(hparams) == len(cps):
                framework_align.append({'file': e['file'], 'line': e['line'], 'name': nm,
                    'target_params': hparams, 'target_rtype': hrtype or cd['rtype'],
                    'child_params': cps, 'hint': e['hint']})
                continue
    manual.append((e['file'], e['line'], nm, 'NO BASE NO HINT: ' + (e['hint'] or '')[:80]))

print(f'\nprop_converts: {len(prop_converts)}')
for pc in prop_converts:
    print(f"  {pc['file'].replace(base+'/','')}:{pc['line']} {pc['fun']} -> val {pc['prop']}:{pc['base_type']} (child rtype={pc['child_rtype']})")
print(f'\nnull groups: {len(null_groups)}')
for (nm,sk),pos in sorted(null_groups.items()):
    print(f'  {nm}{sk} pos={sorted(pos)}')
print(f'\nframework_align: {len(framework_align)}')
for fa in framework_align:
    print(f"  {fa['file'].replace(base+'/','')}:{fa['line']} {fa['name']}")
    print(f"     child: ({', '.join(fa['child_params'])}) : {fa['child_rtype']}")
    print(f"     hint:  ({', '.join(fa['target_params'])}) : {fa['target_rtype']}")
print(f'\nmanual: {len(manual)}')
for m2 in manual:
    print(f'  {m2[0].replace(base+"/","")}:{m2[1]} {m2[2]} :: {m2[3][:110]}')

json.dump({'prop_converts': prop_converts,
           'null_groups': {f'{n}|{list(s)}': sorted(p) for (n,s),p in null_groups.items()},
           'framework_align': framework_align, 'manual': manual},
          open('/tmp/override_plan4.json','w'), indent=1, default=str)
print('\nsaved /tmp/override_plan4.json')
