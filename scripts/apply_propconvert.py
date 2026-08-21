import json, re, os, sys
base = '/opt/data/RedReader/src/main/java/org/quantumbadger/redreader'
APPLY = '--apply' in sys.argv
plan = json.load(open('/tmp/ovplan5.json'))

# Group by file
from collections import defaultdict
byf = defaultdict(list)
for e in plan['prop_convert']:
    byf[e['file']].append(e)

def extract(lines, found):
    """Return (kind, newlines) where kind in {'expr','single','block'}.
    newlines = list of lines to replace lines[found:end+1]."""
    decl = lines[found]
    # expression body: `override fun X() ... = expr`
    m = re.match(r'(\s*)override\s+fun\s+(\w+)\s*\(\s*\)\s*=\s*(.+)$', decl)
    if m:
        return 'expr', m.group(3).strip(), None
    # block body
    depth = 0; started = False; end = found
    for j in range(found, min(found+20, len(lines))):
        for ch in lines[j]:
            if ch == '{': depth += 1; started = True
            elif ch == '}': depth -= 1
        if started and depth == 0:
            end = j; break
    block = lines[found+1:end]
    nonempty = [b for b in block if b.strip()]
    returns = [b.strip() for b in nonempty if b.strip().startswith('return')]
    return 'block', end, (nonempty, returns, block)

stats = {'expr':0,'single':0,'block':0,'fail':0}
report = []
for fpath, entries in byf.items():
    if not os.path.exists(fpath):
        stats['fail'] += len(entries)
        for e in entries: report.append(('MISSING', e['file'].split('redreader/')[-1], e['fun']))
        continue
    lines = open(fpath, encoding='utf-8').read().splitlines()
    # process bottom-to-top
    for e in sorted(entries, key=lambda x: -x['line']):
        name = e['fun']; prop = e['prop']; ctype = e.get('child_rtype')
        target = e['line'] - 1
        found = None
        for off in range(-5, 6):
            jj = target + off
            if 0 <= jj < len(lines) and re.search(r'\boverride\s+fun\s+'+re.escape(name)+r'\s*\(', lines[jj]):
                found = jj; break
        if found is None:
            stats['fail'] += 1
            report.append(('NOTFOUND', fpath.split('redreader/')[-1], name)); continue
        decl = lines[found]
        indent = re.match(r'(\s*)', decl).group(1)
        typ = f': {ctype}' if ctype else ''
        kind, a, b = extract(lines, found)
        if kind == 'expr':
            lines[found] = f'{indent}override val {prop}{typ} get() = {a}'
            stats['expr'] += 1
            report.append(('expr', fpath.split('redreader/')[-1], name))
        else:
            end = a
            nonempty, returns, block = b
            if len(nonempty) == 1 and len(returns) == 1:
                expr = returns[0][6:].strip()
                if expr.endswith(';'): expr = expr[:-1].strip()
                lines[found:end+1] = [f'{indent}override val {prop}{typ} get() = {expr}']
                stats['single'] += 1
                report.append(('single', fpath.split('redreader/')[-1], name))
            else:
                new = [f'{indent}override val {prop}{typ}', f'{indent}    get() {{']
                new.extend(block)
                new.append(f'{indent}    }}')
                lines[found:end+1] = new
                stats['block'] += 1
                report.append(('block', fpath.split('redreader/')[-1], name))
    if APPLY:
        open(fpath, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')

print('stats:', stats)
print('--- failures ---')
for r in report:
    if r[0] in ('fail','NOTFOUND','MISSING'): print('  ', r)
print('--- sample applied ---')
for r in report[:8]: print('  ', r)
