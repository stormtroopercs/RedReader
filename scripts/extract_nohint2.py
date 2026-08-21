import re, os, json, sys, collections
base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
nohint = json.load(open('/tmp/nohint3.json'))

def read_lines(fpath):
    return open(fpath, encoding='utf-8').read().splitlines()

def extract_decl_and_body(fpath, lineidx, name):
    ls = read_lines(fpath)
    dline = ls[lineidx]
    # param list
    si = dline.find('(')
    ei = None
    if si >= 0:
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
            params = [p.strip() for p in re.split(r',', dline[si+1:ei]) if p.strip()]
        else: params=[]
    else:
        params=[]
    # return type
    rtype = None
    if ei is not None:
        between = dline[ei+1:].strip()
        m = re.match(r':\s*(.+?)\s*\{', between)
        if m: rtype = m.group(1)
    return dline, params, rtype

# Index base candidate decls: for a method name, find interface/abstract decls
# (fun name in an interface or abstract). We'll search all files.
def find_base_decls(name):
    """Return list of (file, line, decl) where `name` is a fun/val in an interface or abstract class,
    OR any abstract fun decl. We'll just collect all `fun name` and `val name` decls not in a known impl."""
    res = []
    for dp,_,fs in os.walk(base):
        for f in fs:
            if not f.endswith('.kt'): continue
            p = os.path.join(dp,f)
            try: txt = open(p, encoding='utf-8').read()
            except: continue
            for m in re.finditer(r'(^|\n)(\s*)((?:abstract\s+|open\s+|override\s+)*)(fun|val|var)\s+'+re.escape(name)+r'\b', txt):
                line = txt.count('\n', 0, m.start())
                res.append((p, line, txt.splitlines()[line]))
    return res

out = []
for d in nohint:
    fp = d['file']; nm = d['name']
    dline, params, rtype = extract_decl_and_body(fp, d['line']-1, nm)
    entry = {'file': fp, 'line': d['line'], 'name': nm,
             'rel': fp.replace('/opt/data/RedReader/src/main/java/org/quantumbadger/redreader/',''),
             'child_decl': dline.strip(), 'child_params': params, 'child_rtype': rtype}
    out.append(entry)

json.dump(out, open('/tmp/nohint4.json','w'), indent=1)
print(f'saved {len(out)} entries')

# classify
getprop = [e for e in out if re.match(r'^(get|is)[A-Z]', e['name']) and len(e['child_params'])==0]
method  = [e for e in out if e not in getprop]
print(f'get/is no-arg (property candidates): {len(getprop)}')
print(f'method-signature (alignment): {len(method)}')
print('\n=== method-signature names ===')
for nm,c in collections.Counter(e['name'] for e in method).most_common():
    print(f'  {nm}: {c}')
