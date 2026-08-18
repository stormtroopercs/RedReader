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

def fpath(fp):
    p = fp.replace('file://','',1)
    return p if os.path.exists(p) else None

# ---- multi-line capable: extract fun decl starting at (line) -> (params, rtype, end_line, decl_text)
def extract_fun_decl(fpath, lineidx, name, maxlines=12):
    ls = open(fpath, encoding='utf-8').read().splitlines()
    start = lineidx
    # find '(' on/after start
    buf = ''
    pstart = None
    for j in range(start, min(start+maxlines, len(ls))):
        buf += (ls[j] + '\n')
        if pstart is None and re.search(r'\bfun\s+'+re.escape(name)+r'\b', ls[j]):
            pstart = j
        if pstart is not None and '(' in buf and buf.rstrip().endswith(')'):
            break
        if pstart is not None and re.search(r'\)\s*(?::[^{]*)?\{', buf):
            break
    if pstart is None: return None
    m = re.search(r'\bfun\s+'+re.escape(name)+r'\s*\(', buf)
    if not m: return None
    si = m.end()-1
    depth=0; in_str=None; ei=None
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
    params = parse_params(buf[si+1:ei])
    rest = buf[ei+1:].strip()
    rtype=None
    rm = re.match(r':\s*(.+?)\s*\{', rest, re.S)
    if rm: rtype = rm.group(1).strip()
    return {'params': params, 'rtype': rtype, 'start_line': pstart+1, 'text': buf.rstrip()}

# ---- index in-repo fun decls (single line is fine for bases; multi-line via extract later)
groups = collections.defaultdict(list)
valprops = collections.defaultdict(list)
for dp,_,fs in os.walk(base):
    for f in fs:
        if not f.endswith('.kt'): continue
        p=os.path.join(dp,f)
        try: txt=open(p,encoding='utf-8').read()
        except: continue
        ls = txt.splitlines()
        for j,dline in enumerate(ls):
            for m in re.finditer(r'\bfun\s+(\w+)\s*\(', dline):
                nm=m.group(1)
                si=dline.find('(', m.start())
                ei=None; depth=0; in_str=None
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
                        groups[(nm, tuple(skel(ptype(x)) for x in ps))].append((p,j,ps,dline))
        for j,dline in enumerate(ls):
            for m in re.finditer(r'\bval\s+(\w+)\s*:\s*([^\n{]+)', dline):
                valprops[m.group(1)].append((p,j,dline.strip()))

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
    errors.append({'file':fp,'line':int(m.group(2)),'name':m.group(4),'hint':hint})
print(f'overrides-nothing: {len(errors)}')

null_groups = collections.defaultdict(set)
prop_converts = []
framework_align = []  # (file,line,name, target_params_string, rtype)
manual = []

for e in errors:
    nm=e['name']
    cd = extract_fun_decl(e['file'], e['line']-1, nm)
    if cd is None:
        manual.append((e['file'],e['line'],nm,'DECL NOT FOUND')); continue
    cps=cd['params']
    child_sk=tuple(skel(ptype(x)) for x in cps)

    if e['hint']:
        hm=re.search(r'fun\s+'+re.escape(nm)+r'\s*\(([^)]*)\)\s*(?::\s*([^{]+))?\{', e['hint'])
        if not hm:
            manual.append((e['file'],e['line'],nm,'HINT PARSE: '+e['hint'][:90])); continue
        hparams=parse_params(hm.group(1))
        hrtype=(hm.group(2) or '').strip()
        if len(hparams)!=len(cps):
            manual.append((e['file'],e['line'],nm,f'COUNT child={len(cps)} hint={len(hparams)}')); continue
        type_diffs=[k for k in range(len(cps)) if skel(ptype(cps[k]))!=skel(ptype(hparams[k]))]
        if type_diffs:
            # genuine type difference: align child to hint exactly
            framework_align.append({'file':e['file'],'line':cd['start_line'],'name':nm,
                'target_params':[h for h in hparams], 'target_rtype': hrtype or cd['rtype'],
                'diff': str([(skel(ptype(cps[k])),skel(ptype(hparams[k]))) for k in type_diffs])})
            continue
        pos=[k for k in range(len(cps)) if is_null(ptype(hparams[k])) and not is_null(ptype(cps[k]))]
        if pos:
            # in-repo base? standardize all decls
            cands=[c for c in groups.get((nm,child_sk),[]) if c[0]!=e['file']]
            base_cands=[c for c in cands if 'override' not in c[3]] or cands
            if base_cands:
                null_groups[(nm,child_sk)].update(pos)
            else:
                # framework base with nullability-only diff: fix child params to hint
                framework_align.append({'file':e['file'],'line':cd['start_line'],'name':nm,
                    'target_params':list(hparams),'target_rtype':hrtype or cd['rtype'],'diff':'null-only vs framework'})
        else:
            manual.append((e['file'],e['line'],nm,'hint matches child already?'))
    else:
        pm2=re.match(r'^(get|is)([A-Z].*)$', nm)
        if pm2 and len(cps)==0:
            prop=pm2.group(2)[0].lower()+pm2.group(2)[1:]
            cands=valprops.get(prop,[])
            good=[c for c in cands if not c[2].startswith('override')]
            pool = good if len(good)<=1 else cands
            if len(pool)==1:
                bfile,bln,bdecl=pool[0]
                btm=re.search(r'val\s+'+re.escape(prop)+r'\s*:\s*([^\n{]+)', bdecl)
                btype=btm.group(1).strip() if btm else None
                prop_converts.append({'child':e['file'],'line':cd['start_line'],'fun':nm,'prop':prop,
                    'base_type':btype,'base_decl_file':bfile.replace(base+"/",""),'body_rtype':cd['rtype']})
            else:
                manual.append((e['file'],e['line'],nm,f'AMBIGUOUS val {prop} ({len(cands)}): '+
                    '; '.join(c[0].replace(base+"/","")+':'+str(c[1]+1) for c in cands)))
            continue
        cands=[c for c in groups.get((nm,child_sk),[]) if c[0]!=e['file']]
        base_cands=[c for c in cands if 'override' not in c[3]] or cands
        matched=False
        for (bfile,bln,bps,bdecl) in base_cands[:4]:
            if len(bps)!=len(cps): continue
            pos=[k for k in range(len(cps)) if is_null(ptype(bps[k])) and not is_null(ptype(cps[k]))]
            if pos:
                null_groups[(nm,child_sk)].update(pos)
                matched=True
                break
        if not matched:
            manual.append((e['file'],e['line'],nm,'NO MATCH; child='+str([ptype(x) for x in cps])+
                ' bases='+str([(c[0].rsplit("/",1)[-1], [ptype(x) for x in c[2]]) for c in base_cands[:2]])[:150]))

print(f'\nnull groups: {len(null_groups)}')
for (nm,sk),pos in sorted(null_groups.items()):
    allc=groups[(nm,sk)]
    print(f'  {nm}{sk} pos={sorted(pos)} decls={len(allc)}')
    for c in allc: print(f'      {c[0].replace(base+"/","")}:{c[1]+1} {c[3].strip()[:90]}')
print(f'\nproperty conversions: {len(prop_converts)}')
for pc in prop_converts:
    print(f"  {pc['child'].replace(base+'/','')}:{pc['line']} {pc['fun']} -> val {pc['prop']} base={pc['base_decl_file']} type={pc['base_type']}")
print(f'\nframework/signature align: {len(framework_align)}')
for fa in framework_align:
    print(f"  {fa['file'].replace(base+'/','')}:{fa['line']} {fa['name']} {fa['diff'][:80]}")
    print(f"      target: ({', '.join(fa['target_params'])}) : {fa['target_rtype']}")
print(f'\nmanual: {len(manual)}')
for m in manual:
    print(f'  {m[0].replace(base+"/","")}:{m[1]} {m[2]} :: {m[3][:130]}')

json.dump({'null_groups':{f'{n}|{s}':sorted(p) for (n,s),p in null_groups.items()},
           'prop_converts':prop_converts,'framework_align':framework_align,'manual':manual},
          open('/tmp/override_plan2.json','w'), indent=1, default=str)
print('\nsaved /tmp/override_plan2.json')
