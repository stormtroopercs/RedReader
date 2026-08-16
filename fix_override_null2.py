import re, os, json, sys, collections
base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
APPLY = '--apply' in sys.argv

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

def extract_params(fpath, lineidx, name):
    ls = open(fpath, encoding='utf-8').read().splitlines()
    if lineidx is None or lineidx < 0 or lineidx >= len(ls): return None
    dline = ls[lineidx]
    if not re.search(r'\bfun\s+'+re.escape(name)+r'\b', dline): return None
    si = dline.find('(')
    if si < 0: return None
    depth=0; in_str=None; ei=None
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

# Index all fun decls: (name, skeleton) -> list of (file, line, params)
groups = collections.defaultdict(list)
for dp,_,fs in os.walk(base):
    for f in fs:
        if not f.endswith('.kt'): continue
        p=os.path.join(dp,f)
        try: txt=open(p,encoding='utf-8').read()
        except: continue
        ls=txt.splitlines()
        for m in re.finditer(r'\bfun\s+(\w+)\s*\(', txt):
            nm=m.group(1); ln=txt.count('\n',0,m.start())
            ps = extract_params(p, ln, nm)
            if ps:
                key=(nm, tuple(skel(ptype(x)) for x in ps))
                groups[key].append((p, ln, ps))

# Load null-only errors
no = json.load(open('/tmp/nullonly.json'))
# Determine target groups + positions
target = {}  # (name, skeleton) -> set of positions to make non-null
for d in no:
    nm = d['name']
    child = d['child']; hint = d['hint']
    if not child or not hint or len(child)!=len(hint): continue
    skeleton = tuple(skel(ptype(x)) for x in child)
    pos = set()
    for k in range(len(child)):
        if is_null(ptype(hint[k])) and not is_null(ptype(child[k])):
            pos.add(k)
    if pos:
        target.setdefault((nm, skeleton), set()).update(pos)

print(f'target groups: {len(target)}')

# For each target group, fix ALL declarations to non-null at those positions
edits = []  # (file, line, name, positions)
for (nm, skeleton), positions in target.items():
    for (p, ln, ps) in groups.get((nm, skeleton), []):
        # only fix if it currently has a nullable param at any target position
        need = [k for k in positions if k < len(ps) and is_null(ptype(ps[k]))]
        if need:
            edits.append((p, ln, nm, need))

# Dedup by (file,line)
seen = set(); uniq=[]
for e in edits:
    key=(e[0], e[1])
    if key in seen: continue
    seen.add(key); uniq.append(e)
edits = uniq
print(f'declarations to edit: {len(edits)}')

def do_edit(fpath, lineidx, name, positions):
    ls = open(fpath, encoding='utf-8').read().splitlines()
    dline = ls[lineidx]
    si = dline.find('(')
    if si < 0: return False
    depth=0; in_str=None; ei=None
    for idx in range(si,len(dline)):
        ch=dline[idx]
        if in_str:
            if ch==in_str: in_str=None
        elif ch in '"\'': in_str=ch
        elif ch=='(': depth+=1
        elif ch==')':
            depth-=1
            if depth==0: ei=idx; break
    if ei is None: return False
    inner = dline[si+1:ei]
    params = parse_params(inner)
    for k in positions:
        if k < len(params) and ':' in params[k]:
            nm2, ty = params[k].split(':',1)
            ty = ty.strip()
            if ty.endswith('?'):
                ty = ty[:-1].rstrip()
                params[k] = f'{nm2} : {ty}' if nm2.strip() else ty
    new_inner = ', '.join(params)
    new_dline = dline[:si+1] + new_inner + dline[ei:]
    if APPLY:
        ls[lineidx] = new_dline
        open(fpath,'w',encoding='utf-8').write('\n'.join(ls))
    return True

for (p, ln, nm, positions) in edits:
    ok = do_edit(p, ln, nm, positions)
    if not APPLY:
        rel = p.replace(base+'/','')
        print(f'  dry {rel}:{ln+1} {nm} pos={positions}')

print(f'\n{"APPLIED" if APPLY else "dry-run"} {len(edits)} declarations')
