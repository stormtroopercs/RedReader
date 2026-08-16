import re, os, collections, json
base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
am = json.load(open('/tmp/argmismatch.json'))

def extract_expr(fpath, line, col):
    """col = 1-based position of the FIRST char of the argument. Walk RIGHT."""
    ls = open(fpath, encoding='utf-8').read().splitlines()
    if line-1 >= len(ls): return None
    ln = ls[line-1]
    s = col-1
    if s >= len(ln): return None
    i = s
    depth = 0
    in_str = None
    while i < len(ln):
        ch = ln[i]
        if in_str:
            if ch == in_str: in_str = None
            i += 1
            continue
        if ch in '"\'': in_str = ch; i += 1; continue
        if ch in '([{<': depth += 1
        elif ch in ')]}>':
            if depth == 0: break
            depth -= 1
        elif ch == ',' and depth == 0: break
        elif ch == ';' : break
        elif depth == 0 and (not (ch.isalnum() or ch in '._$')) and not (ch.isspace() and i+1 < len(ln) and ln[i+1].isalpha()):
            # allow a single space inside a multi-part? no: stop at whitespace
            break
        i += 1
    expr = ln[s:i].strip()
    return expr or None

expr_count = collections.Counter()
expr_files = collections.defaultdict(set)
ok = 0
for a in am:
    if not os.path.exists(a['file']): continue
    e = extract_expr(a['file'], a['line'], a['col'])
    if e:
        expr_count[e] += 1
        expr_files[e].add(a['file'].replace(base+'/',''))
        ok += 1
print(f'extracted {ok}/{len(am)}')
print('\n=== top 45 actual-side expressions ===')
for e, c in expr_count.most_common(45):
    print(f'  {c:4d}  {e:55s} {sorted(expr_files[e])[:2]}')
