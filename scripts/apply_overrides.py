import re, os, json, collections, sys
base = str(Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader')
APPLY = '--apply' in sys.argv

# ---- file/class index: file -> {classname: (line, supertypes[])}
def class_index():
    idx = {}
    cls_file = {}
    for dp,_,fs in os.walk(base):
        for f in fs:
            if not f.endswith('.kt'): continue
            p = os.path.join(dp,f)
            try: txt=open(p,encoding='utf-8').read()
            except: continue
            fi = idx.setdefault(p, {})
            for m in re.finditer(r'^\s*(?:abstract\s+|open\s+|sealed\s+|data\s+|enum\s+)*(class|object|interface|fun interface)\s+(\w+)([^{{]*)\{?', txt, re.M):
                kind, nm, header = m.group(1), m.group(2), m.group(3)
                sups = re.findall(r'[:]\s*([A-Z]\w*(?:<[^{}]*>)?(?:\.\w+)?)', header)
                # crude: capture identifiers after ':' up to ( or {
                after = header.split(':',1)
                stypes = []
                if len(after)>1:
                    for part in after[1].split(','):
                        part = part.strip()
                        tm = re.match(r'([A-Z][\w.]*)(<[^>]*>)?', part)
                        if tm: stypes.append(tm.group(1).split('.')[-1])
                        elif re.match(r'\(', part):  # constructor args, skip
                            pass
                fi[nm] = {'kind':kind, 'line': txt.count('\n',0,m.start())+1, 'suptypes':stypes, 'text':txt, 'lines':txt.splitlines()}
                cls_file.setdefault(nm, []).append(p)
    return idx, cls_file

IDX, CLSFILE = class_index()

def file_classes(p): return IDX.get(p, {})

def resolve_supertypes(p, cls, depth=0):
    """BFS up the hierarchy (in-repo). Returns list of (class, file) pairs."""
    seen=set(); out=[]; queue=[(cls,p)]
    while queue and depth<6:
        c, f = queue.pop(0)
        if c in seen: continue
        seen.add(c)
        info = file_classes(f).get(c)
        if not info: continue
        out.append((c,f))
        for st in info['suptypes']:
            cands = CLSFILE.get(st, [])
            for cf in cands:
                queue.append((st, cf))
        depth+=1
    return out

def find_base_member(p, cls, prop_names, getter_names):
    """Walk the class hierarchy of `cls` in file p. Find first base declaration matching
    a property (val/var <name>) or fun <getter>. Returns (kind, name, type, file, line, text)."""
    chain = resolve_supertypes(p, cls)
    for (c, f) in chain:
        if c == cls: continue
        lines = file_classes(f)[c]['lines']
        text = file_classes(f)[c]['text']
        # property: val <name> : Type   (class-level; exclude override & local: check indentation<=4 and not inside fun)
        for prop in prop_names:
            for m in re.finditer(r'^\s{0,4}(val|var)\s+(' + '|'.join(re.escape(x) for x in prop_names) + r')\s*:\s*([^\n{=]+)', text, re.M):
                ln = text.count('\n', 0, m.start())
                dl = lines[ln]
                if dl.startswith('        '):  # local, skip
                    continue
                return ('prop', m.group(2), m.group(3).strip(), f, ln+1, dl.strip())
        for g in getter_names:
            for m in re.finditer(r'^\s{0,4}(abstract\s+|open\s+)?fun\s+(' + '|'.join(re.escape(x) for x in getter_names) + r')\s*\(', text, re.M):
                ln = text.count('\n', 0, m.start())
                return ('fun', m.group(2), None, f, ln+1, lines[ln].strip())
    return None

plan = json.load(open('/tmp/override_plan2.json'))
results = []
unresolved = []

for pc in plan['prop_converts'] + [{'child': m[0], 'line': m[1], 'fun': m[2]} for m in plan['manual'] if 'AMBIGUOUS' in m[3]]:
    childf = pc['child']; line = pc['line']; fun = pc['fun']
    pm = re.match(r'^(get|is)([A-Z].*)$', fun)
    if not pm: continue
    prop = pm.group(2)[0].lower()+pm.group(2)[1:]
    prop_names = [prop]
    if pm.group(1)=='is': prop_names = [pm.group(2), prop]  # try isX then x? Actually base could be `isHidden` (full) or `hidden`
    # order: if getter is isX, base prop is most likely `isX`-stripped? In this codebase, booleans kept `is` (isComment).
    # try full first for is*: names = [isX-stripped-lower... ] let's just try [prop_full, prop]
    prop_full = ('is' if pm.group(1)=='is' else 'get') + pm.group(2)
    # candidate prop names: for is* -> [prop, prop_full]; for get* -> [prop]
    cands = [prop, prop_full] if pm.group(1)=='is' else [prop]

    # find the child's class
    ctext_lines = open(childf, encoding='utf-8').read().splitlines()
    # walk up to find enclosing class
    cls = None
    for i in range(line-1, -1, -1):
        m = re.match(r'^\s*(?:abstract\s+|open\s+|sealed\s+|data\s+|enum\s+)*(class|object|interface)\s+(\w+)', ctext_lines[i])
        if m:
            cls = m.group(2); break
    if not cls:
        unresolved.append((childf, line, fun, 'no enclosing class')); continue

    res = find_base_member(childf, cls, cands, [fun])
    if res:
        results.append({'child':childf, 'line':line, 'fun':fun, 'cls':cls, **dict(zip(('kind','base_name','base_type','base_file','base_line','base_text'), res))})
    else:
        unresolved.append((childf, line, fun, f'class {cls}, no base member for {[prop, prop_full]}'))

print(f'resolved: {len(results)}   unresolved: {len(unresolved)}')
print('\n=== RESOLVED ===')
for r in results:
    bf = r['base_file'].replace(base+'/','') if r['base_file'] else '?'
    print(f"  {r['child'].replace(base+'/','')}:{r['line']} [{r['cls']}] {r['fun']} -> base {r['kind']} {r['base_name']}:{r['base_type']} @ {bf}:{r['base_line']}")
print('\n=== UNRESOLVED ===')
for u in unresolved:
    print(f'  {u[0].replace(base+"/","")}:{u[1]} {u[2]} :: {u[3][:100]}')

json.dump(results, open('/tmp/prop_convert_resolved.json','w'), indent=1)

# ---- APPLY: 4 null groups + resolved property conversions
if APPLY:
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

    # 1. null groups: for each (name|skeleton), make all decls non-null at positions
    ng = plan['null_groups']
    for key, positions in ng.items():
        nm, sk = key.rsplit('|',1)
        sk2 = tuple(json.loads(sk)) if sk.startswith('[') else tuple(sk)
        # find all decls of this name+skel in repo (multi-line aware: use extract)
        for dp,_,fs in os.walk(base):
            for f in fs:
                if not f.endswith('.kt'): continue
                p=os.path.join(dp,f)
                txt=open(p,encoding='utf-8').read()
                lines=txt.splitlines()
                changed=False
                for j,dl in enumerate(lines):
                    if not re.search(r'\bfun\s+'+re.escape(nm)+r'\s*\(', dl): continue
                    # multi-line param extraction
                    buf=dl; end=j
                    for k in range(j+1, min(j+10,len(lines))):
                        buf += '\n'+lines[k]; end=k
                        if re.search(r'\)\s*(?::[^{]*)?\{', buf): break
                    m=re.search(r'\bfun\s+'+nm+r'\s*\(', buf)
                    if not m: continue
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
                    if ei is None: continue
                    ps=parse_params(buf[si+1:ei])
                    if tuple(x.rstrip('?') for x in (ptype(x) for x in ps)) != sk2: continue
                    newps=list(ps); hit=False
                    for k in positions:
                        if k<len(newps) and ':' in newps[k] and ptype(newps[k]).endswith('?'):
                            n,t=newps[k].split(':',1)
                            newps[k]=f'{n} : {t.strip()[:-1].rstrip()}'
                            hit=True
                    if hit:
                        # rebuild only if single line
                        if end==j:
                            lines[j]=dl[:si+1]+', '.join(newps)+dl[ei:]
                            changed=True
                        else:
                            print(f'  SKIP multi-line: {p.replace(base+"/","")}:{j+1} {nm}')
                if changed:
                    open(p,'w',encoding='utf-8').write('\n'.join(lines))
    print('null groups applied')

    # 2. property conversions
    for r in results:
        childf=r['child']; fun=r['fun']
        txt=open(childf,encoding='utf-8').read()
        lines=txt.splitlines()
        # find the fun at/after r['line']
        start=r['line']-1
        # extract decl + body (single-line assumed; verify)
        for j in range(start, min(start+3,len(lines))):
            if not re.search(r'\bfun\s+'+re.escape(fun)+r'\b', lines[j]): continue
            dline=lines[j]
            # body
            bi=dline.find('{')
            if bi<0:
                print(f'  SKIP no brace: {childf}:{j+1}'); break
            # find matching close
            depth=0; in_str=None; in_chr=None; endj=None
            for k in range(j, min(j+15,len(lines))):
                s = k==j and bi or 0
                for idx in range(s, len(lines[k])):
                    ch=lines[k][idx]
                    if in_str:
                        if ch==in_str: in_str=None
                        continue
                    if in_chr:
                        if ch==in_chr: in_chr=None
                        continue
                    if ch=='"': in_str=ch
                    elif ch=="'": in_chr=ch
                    elif ch=='{': depth+=1
                    elif ch=='}':
                        depth-=1
                        if depth==0: endj=k
            if endj is None:
                print(f'  SKIP no body end: {childf}:{j+1}'); break
            # body inner text
            inner=[]
            for k in range(j, endj+1):
                s = k==j and bi+1 or 0
                e = k==endj and lines[k].rfind('}') or len(lines[k])
                t=lines[k][s:e].strip()
                if t: inner.append(t)
            body=' '.join(inner)
            retm=re.search(r'\)\s*:\s*(.+?)\s*\{', dline)
            rtype=retm.group(1).strip() if retm else (r['base_type'] or 'Any')
            # base property name: use base_name from resolution
            bname=r['base_name']
            if r['kind']=='prop':
                newdecl=f'    override val {bname}: {rtype}'
                if body.startswith('return ') and body.count(';')==0 and ' ' not in body[len('return '):].split('\n')[0][:0]:
                    expr=body[len('return '):].strip()
                    # if expr has no control flow
                    if re.fullmatch(r'[^{};]+', expr):
                        open(childf,'w',encoding='utf-8').write('\n'.join(lines[:j])+'\n'+newdecl+f' get() = {expr}\n'+'\n'.join(lines[endj+1:]))
                        print(f'  OK {childf.replace(base+"/","")}:{j+1} {fun} -> override val {bname}: {rtype} get() = {expr[:60]}')
                        break
                    else:
                        open(childf,'w',encoding='utf-8').write('\n'.join(lines[:j])+'\n'+newdecl+' get() {\n        return ' + body + '\n    }\n'+'\n'.join(lines[endj+1:]))
                        print(f'  OK(multi) {childf.replace(base+"/","")}:{j+1} {fun} -> override val {bname}: {rtype} get() {{...}}')
                        break
                else:
                    open(childf,'w',encoding='utf-8').write('\n'.join(lines[:j])+'\n'+newdecl+' get() {\n        ' + '\n        '.join(inner) + '\n    }\n'+'\n'.join(lines[endj+1:]))
                    print(f'  OK(multi) {childf.replace(base+"/","")}:{j+1} {fun} -> override val {bname}: {rtype}')
                    break
            else:
                # base is a fun -> keep as fun but it should already override... (shouldn't happen for nohint)
                print(f'  SKIP base-is-fun: {childf}:{j+1} {fun}')
                break
    print('property conversions applied')
