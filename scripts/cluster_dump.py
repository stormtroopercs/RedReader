import re, os, collections

ROOT = str(Path(__file__).resolve().parent.parent / 'src/main/java')
errs = [l.rstrip('\n') for l in open('/tmp/errs.txt') if l.startswith('e: ')]

out = []
def p(s=''):
    out.append(s)

# 1. full list of mangled "X > y" lines
p('##### MANGLED ">=" LINES (31)')
pat = re.compile(r'^\s*[A-Za-z][A-Za-z0-9_]*(\s*<[^>]*>)?\s*>\s*[a-zA-Z(]')
for dirpath, _, files in os.walk(ROOT):
    for f in files:
        if not f.endswith('.kt'):
            continue
        path = os.path.join(dirpath, f)
        for i, line in enumerate(open(path), 1):
            if pat.match(line) and '->' not in line and 'when' not in line:
                p(f'{os.path.relpath(path, ROOT)}:{i}: {line.rstrip()}')

# 2. all errors of specific kinds, full file:line
def collect(substr, prefix):
    p(f'##### {prefix}')
    seen = set()
    for l in errs:
        if substr in l:
            m = re.search(r'redreader/(.+?\.kt):(\d+):(\d+)\s+(.*)', l)
            if m:
                key = (m.group(1), int(m.group(2)))
                if key in seen:
                    continue
                seen.add(key)
                p(f'{m.group(1)}:{m.group(2)} | {m.group(4)[:90]}')

collect("'val' cannot be reassigned", "VAL REASSIGN (26)")
collect('Property must be initialized', "PROP UNINIT (35)")
collect('Cannot weaken access privilege', "WEAKEN ACCESS (37)")
collect('Type argument is not within its bounds', "TYPE BOUNDS (31)")
collect('Optional<', "OPTIONAL MISMATCH (44)")
collect('Unresolved reference \'AndroidCommon\'', "ANDROIDCOMMON (9)")
collect("Unresolved reference 'Log'", "LOG (13)")
collect("Unresolved reference 'View'", "VIEW (24)")
collect('Unresolved label', "LABELS (42)")
collect("Unresolved reference 'not'", "NOT (23)")
collect('Incompatible types \'Int\' and \'Char\'', "INT/CHAR (26)")

open('/opt/data/cluster_dump.txt', 'w').write('\n'.join(out))
print(f'{len(out)} lines written')
