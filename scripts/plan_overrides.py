import re, os, json, sys, collections
base = str(Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader')
APPLY = '--apply' in sys.argv
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
def pname(p): return (p.split(':',1)[0].strip() if ':' in p else '')
def is_null(t): return t.strip().endswith('?')
def skel(t): return t.strip().rstrip('?')

def fpath(fp):
    p = fp.replace('file://','',1)
    return p if os.path.exists(p) else None

# ---- index all in-repo fun decls: (name, skeleton) -> [(file,line,params,decl)]
groups = collections.defaultdict(list)
valprops = collections.defaultdict(list)  # propname -> [(file,line,decl)]
for dp,_,fs in os.walk(base):
    for f in fs:
        if not f.endswith('.kt'): continue
        p=os.path.join(dp,f)
        try: txt=open(p,encoding='utf-8').read()
        except: continue
        ls = txt.splitlines()
        for m in re.finditer(r'\bfun\s+(\w+)\s*\(', txt):
            nm=m.group(1); ln=txt.count('\n',0,m.start())
            dline=ls[ln]
            si=dline.find('('); ei=None
            if si>=0:
                depth=0; in_str=None
                for idx in range(si,len(dline)):
                    ch=dline[idx]
                    if in_str:
                        if ch==in_str: in_str=None
                    elif ch in '"\'': in_str=ch
                    elif ch=='(': depth+=1
                    elif ch==')':
                        depth-=1
                        if depth==0: ei=idx; break
                if ei is not None:
                    ps=parse_params(dline[si+1:ei])
                    if ps:
                        groups[(nm, tuple(skel(ptype(x)) for x in ps))].append((p,ln,ps,dline))
        for m in re.finditer(r'\bval\s+(\w+)\s*:\s*([^\n{]+)\s*(get\(\)|=|\{)?', txt):
            nm=m.group(1); ln=txt.count('\n',0,m.start())
            valprops[nm].append((p,ln,txt.splitlines()[ln].strip()))

# ---- parse all overrides-nothing from current log
errors=[]
for i,l in enumerate(lines):
    if not (l.startswith('e:') and 'overrides nothing' in l): continue
    m=re.match(r"e: (file:///\S+?):(\d+):(\d+) '?(\w+)'? overrides nothing", l)
    if not m: continue
    fp=fpath(m.group(1))
    if not fp: continue
    hint=None
    if i+1<len(lines) and 'Potential signatures' in lines[i+1]:
        hm=re.search(r'Potential signatures:\s*(.*)', lines[i+1].strip())
        if hm: hint=hm.group(1)
    errors.append({'file':fp,'line':int(m.group(2)),'name':m.group(4),'hint':hint,'idx':i})
print(f'overrides-nothing in current log: {len(errors)}')

def child_params_at(fpath, lineidx, name):
    ls=open(fpath,encoding='utf-8').read().splitlines()
    if lineidx<0 or lineidx>=len(ls): return None
    dline=ls[lineidx]
    if not re.search(r'\bfun\s+'+re.escape(name)+r'\b', dline): return None
    si=dline.find('('); ei=None
    if si<0: return None
    depth=0; in_str=None
    for idx in range(si,len(dline)):
        ch=dline[idx]
        if in_str:
            if ch==in_str: in_str=None
        elif ch in '"\'': in_str=ch
        elif ch=='(': depth+=1
        elif ch==')':
            depth-=1
            if depth==0: ei=idx; break
    if ei is None: return None
    return parse_params(dline[si+1:ei])

# ---- classify
null_groups = collections.defaultdict(set)   # (name,skeleton) -> set of positions
prop_converts = []   # (childfile, line, name, propname, base_type)
manual = []

for e in errors:
    nm=e['name']
    cps=child_params_at(e['file'], e['line']-1, nm)
    if cps is None:
        manual.append((e['file'],e['line'],nm,'child decl not found')); continue
    child_sk=tuple(skel(ptype(x)) for x in cps)
    if e['hint']:
        # parse hint: fun name(a: T, b: U): R
        hm=re.search(r'fun\s+'+re.escape(nm)+r'\s*\(([^)]*)\)\s*(?::\s*([^{]+))?\{', e['hint'])
        if not hm:
            manual.append((e['file'],e['line'],nm,'hint unparseable: '+e['hint'][:80])); continue
        hparams=parse_params(hm.group(1))
        if len(hparams)!=len(cps):
            manual.append((e['file'],e['line'],nm,f'param count child={len(cps)} hint={len(hparams)}')); continue
        diffs=[k for k in range(len(cps)) if skel(ptype(cps[k]))!=skel(ptype(hparams[k]))]
        if diffs:
            manual.append((e['file'],e['line'],nm,'TYPE DIFF: '+str([(skel(ptype(cps[k])),skel(ptype(hparams[k]))) for k in diffs]))); continue
        # null-only: positions where hint nullable, child non-null
        pos=[k for k in range(len(cps)) if is_null(ptype(hparams[k])) and not is_null(ptype(cps[k]))]
        if pos: null_groups[(nm,child_sk)].update(pos)
        else: manual.append((e['file'],e['line'],nm,'hint fully matches child?'))
    else:
        # no hint: getter -> property?
        pm2=re.match(r'^(get|is)([A-Z].*)$', nm)
        if pm2 and len(cps)==0:
            prop=pm2.group(2)[0].lower()+pm2.group(2)[1:]
            cands=valprops.get(prop,[])
            if len(cands)==1:
                bfile,bln,bdecl=cands[0]
                btm=re.search(r'val\s+'+re.escape(prop)+r'\s*:\s*([^\n{]+)', bdecl)
                btype=btm.group(1).strip() if btm else '?'
                prop_converts.append((e['file'],e['line'],nm,prop,btype))
            elif len(cands)>1:
                # pick one that is interface/abstract-ish: prefer non-override decls
                good=[c for c in cands if not c[2].startswith('override')]
                if len(good)==1:
                    bfile,bln,bdecl=good[0]
                    btm=re.search(r'val\s+'+re.escape(prop)+r'\s*:\s*([^\n{]+)', bdecl)
                    btype=btm.group(1).strip() if btm else '?'
                    prop_converts.append((e['file'],e['line'],nm,prop,btype))
                else:
                    manual.append((e['file'],e['line'],nm,f'ambiguous base for val {prop}: {len(cands)}')); continue
            else:
                manual.append((e['file'],e['line'],nm,f'no base val {prop}')); continue
            continue
        # method: find base fun decl in-repo for same (name, skeleton)
        cands=groups.get((nm,child_sk),[])
        # exclude the child file itself
        other=[c for c in cands if c[0]!=e['file']]
        if not other:
            manual.append((e['file'],e['line'],nm,'no in-repo base fun with same skeleton')); continue
        # if multiple, prefer non-override (base) ones
        base_cands=[c for c in other if 'override' not in c[3]]
        if not base_cands: base_cands=other
        # check nullability diff against each; group by the base that differs only in nullability
        matched=False
        for (bfile,bln,bps, bdecl) in base_cands[:3]:
            if len(bps)!=len(cps): continue
            pos=[k for k in range(len(cps)) if is_null(ptype(bps[k])) and not is_null(ptype(cps[k]))]
            if pos:
                null_groups[(nm,child_sk)].update(pos)
                matched=True
                break
        if not matched:
            manual.append((e['file'],e['line'],nm,f'no null-only base match; bases: {[(c[0].rsplit("/",1)[-1], [ptype(x) for x in c[2]]) for c in base_cands[:2]]}'))

print(f'\nnull groups: {len(null_groups)}')
for (nm,sk),pos in sorted(null_groups.items()):
    impls=[(c[0].replace(base+"/",""),c[1]+1) for c in groups[(nm,sk)]]
    print(f'  {nm}{sk} pos={sorted(pos)}  decls={len(impls)}')
print(f'\nproperty conversions: {len(prop_converts)}')
for (cf,cl,nm,prop,bt) in prop_converts:
    print(f'  {cf.replace(base+"/","")}:{cl} {nm} -> val {prop} (base type {bt})')
print(f'\nmanual: {len(manual)}')
for m in manual:
    print(f'  {m[0].replace(base+"/","")}:{m[1]} {m[2]} :: {m[3][:110]}')

json.dump({'null_groups':{f'{n}|{s}':sorted(p) for (n,s),p in null_groups.items()},
           'prop_converts':prop_converts,
           'manual':manual}, open('/tmp/override_plan.json','w'), indent=1, default=str)
print('\nsaved /tmp/override_plan.json')
