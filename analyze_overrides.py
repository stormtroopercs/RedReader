import re, os, sys, json, collections
base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
log = open('/tmp/compile.log').read()
lines = log.splitlines()

def norm(t):
    """Normalize a type for comparison: strip whitespace, keep ? marker separately."""
    t = t.strip()
    nullable = t.endswith('?')
    t = t.rstrip('?')
    return t, nullable

def parse_params(text):
    """Split top-level comma-separated params, respecting generics/parens/brackets/quotes."""
    if text is None: return None
    parts = []; depth = 0; cur = ''; in_str = None
    for ch in text:
        if in_str:
            cur += ch
            if ch == in_str: in_str = None
            continue
        if ch in '"\'': in_str = ch; cur += ch; continue
        if ch in '<([': depth += 1
        elif ch in '>)]': depth -= 1
        if ch == ',' and depth == 0:
            parts.append(cur); cur = ''
        else:
            cur += ch
    if cur.strip(): parts.append(cur)
    return parts

# Collect overrides-nothing errors with their potential-signature hints
# The hint is on the following lines until the next 'e:' line.
errors = []
for i, l in enumerate(lines):
    if l.startswith('e:') and 'overrides nothing' in l:
        m = re.match(r'e: file:///(.*?):(\d+):(\d+) (\S+) overrides nothing', l)
        if not m: continue
        fpath, ln, col, name = m.groups()
        # gather hint lines
        hint = ''
        for j in range(i+1, min(i+12, len(lines))):
            if lines[j].startswith('e:'): break
            hint += ' ' + lines[j]
        errors.append((fpath, int(ln), int(col), name, hint))

print(f'overrides-nothing errors: {len(errors)}')

# For each, read the child's full declaration from source (multi-line aware)
def read_child_params(fpath, ln):
    txt = open(fpath, encoding='utf-8').read()
    ls = txt.splitlines()
    # start from the error line (1-indexed) — back up to find 'fun name('
    start = ln - 1
    # the declaration line contains 'fun <name>(' or 'override fun <name>('
    decl_line = None
    for k in range(start, max(start-3, -1), -1):
        if re.search(r'\bfun\s+' + re.escape(name) + r'\b', ls[k]):
            decl_line = k; break
    if decl_line is None: return None, None
    dline = ls[decl_line]
    idx = dline.find('(')
    if idx < 0: return None, None
    # balanced parens from idx
    depth = 0; j = idx; in_str = None
    while j < len(dline):
        ch = dline[j]
        if in_str:
            if ch == in_str: in_str = None
        elif ch in '"\'': in_str = ch
        elif ch == '(': depth += 1
        elif ch == ')':
            depth -= 1
            if depth == 0: break
        j += 1
    if depth != 0:
        # multi-line: gather until balanced
        buf = dline[idx:]
        k = decl_line + 1
        while k < len(ls):
            buf += '\n' + ls[k]
            depth = 0; in_str = None
            for ch in buf:
                if in_str:
                    if ch == in_str: in_str = None
                elif ch in '"\'': in_str = ch
                elif ch == '(': depth += 1
                elif ch == ')': depth -= 1
            if depth <= 0 and buf.rstrip().endswith(')'):
                break
            k += 1
        inner = buf[buf.find('(')+1:buf.rfind(')')]
    else:
        inner = dline[idx+1:j]
    return parse_params(inner), (decl_line, dline)

# For the potential signature: parse 'open fun name(params): ret' from hint
results = []
for fpath, ln, col, name, hint in errors:
    m = re.search(r'fun\s+' + re.escape(name) + r'\s*\((.*?)\)\s*(:|\b)', hint)
    if not m:
        results.append((name, fpath, ln, 'NO_HINT', None)); continue
    hint_inner = m.group(1)
    hint_params = parse_params(hint_inner) if hint_inner.strip() else []
    child_params, decl = read_child_params(fpath, ln, ) if False else (None, None)
    # redo properly
    child_params = None
    txt = open(fpath, encoding='utf-8').read()
    ls = txt.splitlines()
    start = ln - 1
    decl_line = None
    for k in range(start, max(start-3, -1), -1):
        if re.search(r'\bfun\s+' + re.escape(name) + r'\b', ls[k]):
            decl_line = k; break
    if decl_line is not None:
        dline = ls[decl_line]
        idx = dline.find('(')
        if idx >= 0:
            depth = 0; j = idx; in_str = None; end = None
            full = dline
            k = decl_line + 1
            buf = dline
            while True:
                depth = 0; in_str = None
                for ch in buf:
                    if in_str:
                        if ch == in_str: in_str = None
                    elif ch in '"\'': in_str = ch
                    elif ch == '(': depth += 1
                    elif ch == ')': depth -= 1
                if depth <= 0 and '(' in buf and buf.strip().endswith(')'):
                    end = len(buf) - 1
                    break
                if k >= len(ls): break
                buf += '\n' + ls[k]; k += 1
            if end is not None:
                pstart = buf.find('(')
                inner = buf[pstart+1:end]
                child_params = parse_params(inner)
    results.append((name, fpath, ln, hint_inner, child_params))

# Classify
null_only = 0; other = 0; nomatch = 0
samples_other = []
for name, fpath, ln, hint_inner, child_params in results:
    if child_params is None or hint_inner is None and not hint_inner:
        nomatch += 1; continue
    hint_params = parse_params(hint_inner) if hint_inner.strip() else []
    child_params = child_params if child_params else []
    if len(hint_params) != len(child_params):
        other += 1
        samples_other.append((name, fpath, ln, f'COUNT {len(child_params)} vs {len(hint_params)}', child_params, hint_inner))
        continue
    diffs = []
    only_q = True
    for cp, hp in zip(child_params, hint_params):
        # extract type part (after last ':' if present)
        def ctype(p):
            if ':' in p:
                t = p.split(':', 1)[1]
            else:
                t = p
            return t.strip()
        ct, cn = norm(ctype(cp))
        ht, hn = norm(ctype(hp))
        if ct != ht:
            only_q = False
            diffs.append(f'type {ct} vs {ht}')
        elif cn != hn:
            diffs.append(f'null {cn}->{hn}')
    if only_q:
        null_only += 1
    else:
        other += 1
        samples_other.append((name, fpath, ln, '; '.join(diffs[:3]), child_params, hint_inner))

print(f'nullability-only: {null_only}   type-diff: {other}   unparseable: {nomatch}')
print('\n=== type-diff samples ===')
for s in samples_other[:20]:
    print(f'{s[0]} @ {os.path.basename(s[1])}:{s[2]}  {s[3]}')
    print(f'   child: {s[4]}')
    print(f'   base : {s[5]}')

json.dump([{'name':a,'f':b,'l':c,'hint':d,'child':e} for a,b,c,d,e in results], open('/tmp/override_pairs.json','w'), indent=1)
