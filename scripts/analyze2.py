import re, os

errs = [l.rstrip('\n') for l in open('/tmp/errs.txt') if l.startswith('e: ')]

def full(cluster_pat, n=10, filefilter=None):
    print(f'### {cluster_pat}')
    cnt = 0
    seen = set()
    for l in errs:
        if re.search(cluster_pat, l):
            m = re.search(r'file://(/.+?)\.kt:(\d+):(\d+)\s+(.*)', l)
            if not m:
                continue
            path = m.group(1) + '.kt'
            line = int(m.group(2))
            msg = m.group(4)
            key = (os.path.basename(path), msg[:60])
            if filefilter and filefilter not in path:
                continue
            if key in seen:
                continue
            seen.add(key)
            try:
                src = open(path).readlines()[line-1].strip()
            except Exception:
                src = '<no src>'
            print(f'  {os.path.basename(path)}:{line} | {msg[:95]}')
            print(f'      src: {src[:110]}')
            cnt += 1
            if cnt >= n:
                break

full("compareTo")
full("Unresolved reference 'not'", 8)
full("Unresolved label", 8)
full("Incompatible types 'Int' and 'Char'", 6)
full("'val' cannot be reassigned", 6)
full("Type argument is not within its bounds", 6)
full("None of the following candidates", 6)
full("Cannot weaken access privilege", 8)
full("Property must be initialized", 10)
full("Unresolved reference 'decoded'", 8)
full("Unresolved reference 'View'", 8)
full("Unresolved reference 'AndroidCommon'", 8)
full("Unresolved reference 'Log'", 8)
full("Unresolved reference 'getString'", 8)
full("Unresolved reference 'add'", 8)
