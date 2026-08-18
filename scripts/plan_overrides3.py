import re, os, json, sys, collections
base = str(Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader')
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

# ---- MULTI-LINE-AWARE full-repo fun decl index
groups = collections.defaultdict(list)  # (name, skeleton) -> [(file, line, params, has_override)]
files = []
for dp,_,fs in os.walk(base):
    for f in fs:
        if f.endswith('.kt'): files.append(os.path.join(dp,f))

def index_file(p):
    txt=open(p,encoding='utf-8').read()
    ls=txt.splitlines()
    for j,dl in enumerate(ls):
        m=re.search(r'\bfun\s+(\w+)\s*\(', dl)
        if not m: continue
        nm=m.group(1)
        # gather up to 12 lines until balanced ')' + optional ':type' + '{'
        buf=dl
        for k in range(j+1, min(j+12,len(ls))):
            buf+='\n'+ls[k]
            if re.search(r'\)\s*(?::[^{;]*)?\{', buf): break
            if buf.rstrip().endswith('{'): break
        mm=re.search(r'\bfun\s+'+re.escape(nm)+r'\s*\(', buf)
        if not mm: continue
        si=mm.end()-1; depth=0; in_str=None; ei=None
        for idx in range(si,len(buf)):
            ch=buf[idx]
            if in_str:
                if ch==in_str: in_str=None
            elif ch in '"\'': in_str=ch
            elif ch=='(': depth+=1
            elif ch==')':
                depth-=1
                if depth==0: ei=idx; break
        if ei is None: continue
        ps=parse_params(buf[si+1:ei])
        if not ps: continue
        if re.search(r'\bfun\s+'+re.escape(nm)+r'\b[^{]*\{', buf[:ei+1]) and False: pass
        groups[(nm, tuple(skel(ptype(x)) for x in ps))].append((p, j+1, ps, 'override' in dl))

for p in files: index_file(p)
print(f'indexed fun decls: {sum(len(v) for v in groups.values())} in {len(groups)} groups')

# property index: class-level val/var only (indent<=4, not override, with ':')
valprops = collections.defaultdict(list)
for p in files:
    txt=open(p,encoding='utf-8').read()
    ls=txt.splitlines()
    for j,dl in enumerate(ls):
        if re.match(r'^\s{0,4}(val|var)\s+\w+\s*:', dl) and 'override' not in dl:
            m=re.match(r'^\s{0,4}(val|var)\s+(\w+)\s*:\s*([^\n{=]+)', dl)
            if m: valprops[m.group(2)].append((p, j+1, dl.strip()))

errors=[]
for i,l in enumerate(lines):
    if not (l.startswith('e:') and 'overrides nothing' in l): continue
    m=re.match(r"e: (file:///\S+?):(\d+):(\d+) '?(\w+)'? overrides nothing", l)
    if not m: continue
    fp=m.group(1).replace('file://','',1)
    if not os.path.exists(fp): continue
    hint=None
    if i+1<len(lines) and 'Potential signatures' in lines[i+1]:
        hm=re.search(r'Potential signatures:\s*(.*)', lines[i+1].strip())
        if hm: hint=hm.group(1)
    errors.append({'file':fp,'line':int(m.group(2)),'name':m.group(4),'hint':hint})
print(f'overrides-nothing: {len(errors)}')

def extract_child(fpath, lineidx, name):
    ls=open(fpath,encoding='utf-8').read().splitlines()
    buf=''
    for j in range(lineidx, min(lineidx+12,len(ls))):
        buf += (ls[j] if j==lineidx else '\n'+ls[j])
        if re.search(r'\)\s*(?::[^{;]*)?\{', buf) or buf.rstrip().endswith('{'): break
    m=re.search(r'\bfun\s+'+re.escape(name)+r'\s*\(', buf)
    if not m: return None
    si=m.end()-1; depth=0; in_str=None; ei=None
    for idx in range(si,len(buf)):
        ch=buf[idx]
        if in_str:
            if ch==in_str: in_str=None
        elif ch in '"\'': in_str=ch
        elif ch=='(': depth+=1
        elif ch==')':
            depth-=1
            if depth==0: ei=idx; break
    if ei is None: return None
    ps=parse_params(buf[si+1:ei])
    rest=buf[ei+1:].strip()
    rtype=None
    rm=re.match(r':\s*(.+?)\s*\{', rest, re.S)
    if rm: rtype=rm.group(1).strip()
    return {'params':ps,'rtype':rtype}

null_groups=collections.defaultdict(set)
prop_converts=[]
method_align=[]
manual=[]

for e in errors:
    nm=e['name']
    cd=extract_child(e['file'], e['line']-1, nm)
    if cd is None:
        manual.append((e['file'],e['line'],nm,'CHILD DECL NOT FOUND')); continue
    cps=cd['params']
    child_sk=tuple(skel(ptype(x)) for x in cps)

    def try_null_group(pos):
        if pos:
            cands=[c for c in groups.get((nm,child_sk),[]) if c[0]!=e['file']]
            base_c=[c for c in cands if not c[3]] or cands
            if base_c:
                null_groups[(nm,child_sk)].update(pos)
                return True
            return False
        return False

    if e['hint']:
        hm=re.search(r'fun\s+'+re.escape(nm)+r'\s*\(([^)]*)\)\s*(?::\s*([^{]+))?\{', e['hint'])
        if not hm:
            manual.append((e['file'],e['line'],nm,'HINT PARSE: '+e['hint'][:90])); continue
        hparams=parse_params(hm.group(1)); hrtype=(hm.group(2) or '').strip()
        if len(hparams)!=len(cps):
            manual.append((e['file'],e['line'],nm,f'COUNT child={len(cps)} hint={len(hparams)}')); continue
        tdiff=[k for k in range(len(cps)) if skel(ptype(cps[k]))!=skel(ptype(hparams[k]))]
        if tdiff:
            method_align.append({'file':e['file'],'line':e['line'],'name':nm,'target_params':hparams,
                'target_rtype':hrtype or cd['rtype'],'note':'type-diff vs hint'})
            continue
        if not try_null_group([k for k in range(len(cps)) if is_null(ptype(hparams[k])) and not is_null(ptype(cps[k]))]):
            method_align.append({'file':e['file'],'line':e['line'],'name':nm,'target_params':hparams,
                'target_rtype':hrtype or cd['rtype'],'note':'null-only vs framework hint'})
    else:
        pm2=re.match(r'^(get|is)([A-Z].*)$', nm)
        if pm2 and len(cps)==0:
            prop=pm2.group(2)[0].lower()+pm2.group(2)[1:]
            cands=valprops.get(prop,[])
            if len(cands)==1:
                bfile,bln,bdecl=cands[0]
                btm=re.search(r'(?:val|var)\s+'+re.escape(prop)+r'\s*:\s*([^\n{=]+)', bdecl)
                prop_converts.append({'child':e['file'],'line':e['line'],'fun':nm,'prop':prop,
                    'base_type':btm.group(1).strip() if btm else None,
                    'base_file':bfile.replace(base+"/",""),'base_line':bln,'child_rtype':cd['rtype']})
                continue
            manual.append((e['file'],e['line'],nm,f'PROP AMBIGUOUS {len(cands)}: '+
                '; '.join(c[0].replace(base+"/","")+':'+str(c[1]) for c in cands[:6])))
            continue
        cands=[c for c in groups.get((nm,child_sk),[]) if c[0]!=e['file']]
        base_c=[c for c in cands if not c[3]] or cands
        matched=False
        for (bf,bl,bps,bov) in base_c[:4]:
            if len(bps)!=len(cps): continue
            pos=[k for k in range(len(cps)) if is_null(ptype(bps[k])) and not is_null(ptype(cps[k]))]
            if pos and try_null_group(pos):
                matched=True; break
        if not matched:
            method_align.append({'file':e['file'],'line':e['line'],'name':nm,
                'child_params':cps,'child_rtype':cd['rtype'],
                'note':'NO NULL-ONLY IN-REPO BASE','base_cands':[(c[0].replace(base+"/",""), c[1], [ptype(x) for x in c[2]]) for c in base_c[:3]]})

print(f'\nnull groups: {len(null_groups)}')
for (nm,sk),pos in sorted(null_groups.items()):
    print(f'  {nm}{sk} pos={sorted(pos)}')
print(f'\nproperty conversions: {len(prop_converts)}')
for pc in prop_converts:
    print(f"  {pc['child'].replace(base+'/','')}:{pc['line']} {pc['fun']} -> val {pc['prop']}:{pc['base_type']} (base {pc['base_file']}:{pc['base_line']})")
print(f'\nmethod align: {len(method_align)}')
for ma in method_align:
    print(f"  {ma['file'].replace(base+'/','')}:{ma['line']} {ma['name']} :: {ma['note'][:70]}")
    if 'target_params' in ma:
        print(f"      target: ({', '.join(ma['target_params'])}) : {ma['target_rtype']}")
    elif 'base_cands' in ma:
        for bc in ma['base_cands']: print(f"      base cand: {bc[0]}:{bc[1]} {bc[2]}")
print(f'\nmanual: {len(manual)}')
for m in manual:
    print(f'  {m[0].replace(base+"/","")}:{m[1]} {m[2]} :: {m[3][:120]}')

json.dump({'null_groups':{f'{n}|{list(s)}':sorted(p) for (n,s),p in null_groups.items()},
           'prop_converts':prop_converts,'method_align':method_align,'manual':manual},
          open('/tmp/override_plan3.json','w'), indent=1, default=str)
print('\nsaved /tmp/override_plan3.json')
