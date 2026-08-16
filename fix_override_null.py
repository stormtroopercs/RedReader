import re, os, json, sys
base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
log = open('/tmp/compile.log').read()
lines = log.splitlines()
APPLY = '--apply' in sys.argv

def fpath_fix(fp): return '/'+fp if not fp.startswith('/') else fp
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
def skeleton(t):
    t = t.strip()
    return t.rstrip('?')
def is_null(t): return t.strip().endswith('?')

# index all fun decls: name -> list of (file, line, raw params)
def get_params_from_source(fpath, lineno, name):
    ls = open(fpath, encoding='utf-8').read().splitlines()
    for k in range(lineno-1, max(lineno-4,-1), -1):
        if re.search(r'\bfun\s+'+re.escape(name)+r'\b', ls[k]):
            buf=ls[k]; j=k+1
            while True:
                depth=0; in_str=None
                for ch in buf:
                    if in_str:
                        if ch==in_str: in_str=None
                    elif ch in '"\'': in_str=ch
                    elif ch=='(': depth+=1
                    elif ch==')': depth-=1
                if depth<=0 and '(' in buf and buf.rstrip().endswith(')'): break
                if j>=len(ls): break
                buf+='\n'+ls[j]; j+=1
            si=buf.find('(')
            if si<0: return None
            depth=0; in_str=None; ei=None
            for idx in range(si,len(buf)):
                ch=buf[idx]
                if in_str:
                    if ch==in_str: in_str=None
                elif ch in '"\'': in_str=ch
                elif ch=='(': depth+=1
                elif ch==')':
                    depth-=1
                    if depth==0: ei=idx; break
            return parse_params(buf[si+1:ei])
    return None

fun_index = {}
for dp,_,fs in os.walk(base):
    for f in fs:
        if not f.endswith('.kt'): continue
        p=os.path.join(dp,f)
        try: txt=open(p,encoding='utf-8').read()
        except: continue
        for m in re.finditer(r'\bfun\s+(\w+)\s*\(', txt):
            nm=m.group(1); ln=txt.count('\n',0,m.start())+1
            ps = get_params_from_source(p, ln, nm)
            if ps: fun_index.setdefault(nm, []).append((p, ln, ps))

# collect null-only overrides
nullonly = []
for i,l in enumerate(lines):
    if l.startswith('e:') and 'overrides nothing' in l:
        m = re.match(r"e: file:///(.*?):(\d+):(\d+) '(\w+)' overrides nothing", l)
        if not m: continue
        child_f, child_ln, name = fpath_fix(m.group(1)), int(m.group(2)), m.group(4)
        hints=[]
        for j in range(i+1, len(lines)):
            if lines[j].startswith('e:'): break
            hints.append(lines[j].strip())
        hp=None
        for h in hints:
            hm = re.search(r'fun\s+'+re.escape(name)+r'\s*\((.*?)\)\s*(:.*)?$', h)
            if hm: hp=parse_params(hm.group(1)); break
        cp = get_params_from_source(child_f, child_ln, name)
        if cp is None or hp is None or len(cp)!=len(hp): continue
        only_q=True; diffs=[]
        for a,b in zip(cp,hp):
            if skeleton(ptype(a))!=skeleton(ptype(b)): only_q=False; break
        if not only_q: continue
        # which params differ by nullability (child non-null, base nullable)
        fix_indices=[k for k in range(len(cp)) if is_null(ptype(hp[k])) and not is_null(ptype(cp[k]))]
        if not fix_indices: continue
        nullonly.append((name, child_f, child_ln, cp, hp, fix_indices))

print(f'null-only candidates: {len(nullonly)}')

# For each, find the base declaration (a fun_index entry with same skeleton, different file)
edits = {}  # base file -> list of (line, name, fix_indices)
for name, child_f, child_ln, cp, hp, fix_indices in nullonly:
    target_skel = [skeleton(ptype(x)) for x in cp]
    best = None
    for (p, ln, ps) in fun_index.get(name, []):
        if p == child_f: continue
        ps_skel = [skeleton(ptype(x)) for x in ps]
        if ps_skel == target_skel:
            best = (p, ln, ps)
            break
    if best is None:
        print(f'  NO BASE FOUND: {name} @ {os.path.basename(child_f)}:{child_ln}')
        continue
    bp, bln, bps = best
    edits.setdefault(bp, []).append((bln, name, fix_indices))

total = sum(len(v) for v in edits.values())
print(f'base edits to apply: {total} across {len(edits)} files')

def do_edit(fpath, line, name, fix_indices):
    ls = open(fpath, encoding='utf-8').read().splitlines()
    # find the fun decl line (may be the line or a bit above)
    dln = None
    for k in range(line-1, max(line-4,-1), -1):
        if re.search(r'\bfun\s+'+re.escape(name)+r'\b', ls[k]):
            dln=k; break
    if dln is None: return False
    buf='\n'.join(ls[dln:])
    si=buf.find('(')
    if si<0: return False
    depth=0; in_str=None; ei=None
    for idx in range(si,len(buf)):
        ch=buf[idx]
        if in_str:
            if ch==in_str: in_str=None
        elif ch in '"\'': in_str=ch
        elif ch=='(': depth+=1
        elif ch==')':
            depth-=1
            if depth==0: ei=idx; break
    inner = buf[si+1:ei]
    params = parse_params(inner)
    for k in fix_indices:
        if k < len(params):
            params[k] = params[k].replace('?', '', 1) if '?:' not in params[k] else params[k]
            # remove trailing ? from the type token
    # rebuild param types: just strip a single trailing ? from type part
    new_params=[]
    for k,p in enumerate(params):
        if k in fix_indices and ':' in p:
            nm2, ty = p.split(':',1)
            ty = ty.strip()
            if ty.endswith('?'): ty = ty[:-1].rstrip()
            new_params.append(f'{nm2.strip()} : {ty}' if nm2.strip() else ty)
        else:
            new_params.append(p)
    new_inner = ', '.join(new_params)
    new_buf = buf[:si+1] + new_inner + buf[ei:]
    # write back into ls (buf started at dln, single-line params assumed)
    if '\n' in new_buf:
        newls = ls[:dln] + new_buf.split('\n')
    else:
        newls = ls[:dln] + [new_buf] + ls[dln+1:]
    if APPLY:
        open(fpath,'w',encoding='utf-8').write('\n'.join(newls))
    return True

for fpath, lst in edits.items():
    for (bln, name, fix_indices) in lst:
        ok = do_edit(fpath, bln, name, fix_indices)
        print(f'  {"APPLY" if APPLY else "dry"} {os.path.basename(fpath)}:{bln} {name} fix={fix_indices} ok={ok}')
