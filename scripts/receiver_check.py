import re, os

ROOT = str(Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader')
errs = [l.rstrip('\n') for l in open('/tmp/errs.txt') if l.startswith('e: ')]

def line_for(l):
    m = re.search(r'org/quantumbadger/redreader/(.+?\.kt):(\d+):(\d+)', l)
    if not m:
        return None
    path = os.path.join(ROOT, m.group(1))
    if not os.path.exists(path):
        return None
    try:
        return path, open(path).readlines()[int(m.group(2)) - 1]
    except Exception:
        return None

def receivers(refname):
    counts = {}
    for l in errs:
        if f"Unresolved reference '{refname}'" in l:
            r = line_for(l)
            if not r:
                continue
            path, src = r
            idx = src.find(refname)
            if idx < 0:
                continue
            pre = src[:idx]
            mm = re.search(r'([A-Za-z_][A-Za-z0-9_.]*)\s*\.\s*$', pre)
            recv = mm.group(1) if mm else '<bare>'
            counts[recv] = counts.get(recv, 0) + 1
    print(f'##### .{refname}() receivers')
    for k, v in sorted(counts.items(), key=lambda x: -x[1])[:20]:
        print(f'  {v:4d}  {k}')

for r in ['getInstance', 'getAnon', 'getSubreddit', 'getCanonicalId', 'setSession',
          'getKey', 'getTimestamp', 'getRawComment', 'getParsedComment',
          'getActivity', 'getString', 'getScheme', 'getDomain', 'add',
          'decoded', 'getHumanReadableDomain']:
    receivers(r)

print('##### .x / .y sites')
for l in errs:
    if "Unresolved reference 'x'" in l or "Unresolved reference 'y'" in l:
        r = line_for(l)
        if r:
            path, src = r
            print(f"  {os.path.relpath(path, ROOT)}: {src.strip()[:100]}")

for refname, want in [('Log', 'import android.util.Log'),
                      ('AndroidCommon', 'import org.quantumbadger.redreader.common.AndroidCommon'),
                      ('View', 'import android.view.View')]:
    print(f'##### {refname} import check')
    files = set()
    for l in errs:
        if f"Unresolved reference '{refname}'" in l:
            m = re.search(r'org/quantumbadger/redreader/(.+?\.kt)', l)
            if m:
                files.add(os.path.join(ROOT, m.group(1)))
    for f in sorted(files):
        if not os.path.exists(f):
            print(f'  NFILE    {f}')
            continue
        content = open(f).read()
        status = 'HAS' if want in content else 'MISSING'
        print(f'  {status:8s} {os.path.relpath(f, ROOT)}')
