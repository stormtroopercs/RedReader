import re, os, collections

errs = [l.rstrip('\n') for l in open('/tmp/errs.txt') if l.startswith('e: ')]

def show(ref, n=8):
    print(f'=== {ref} ({sum(1 for l in errs if ref in l)} hits) ===')
    cnt = 0
    for l in errs:
        m = re.search(r'file://(/.+?)\.kt:(\d+):(\d+)', l)
        if m and ref in l:
            path, line = m.group(1) + '.kt', int(m.group(2))
            try:
                src = open(path).readlines()[line-1].strip()
            except Exception as e:
                src = f'<read err {e}>'
            print(f'  {os.path.basename(path)}:{line}: {src[:100]}')
            cnt += 1
            if cnt >= n:
                break

for r in ["'getInstance'", "'getActivity'", "'getString'", "'compareTo'", "'getSubreddit'",
          "'findPreference'", "'not'", "'getAnon'", "'getTimestamp'", "'getCanonicalId'",
          "'decoded'", "'getKey'", "'x'", "'setSession'", "'Log'", "'getScheme'",
          "'getDomain'", "'View'", "'y'", "'getParsedComment'", "'AndroidCommon'",
          "'add'"]:
    show(r, 5)

print("=== non-abstract anonymous ===")
cnt = 0
for l in errs:
    if 'not abstract and does not implement' in l:
        m = re.search(r'file://(/.+?)\.kt:(\d+):(\d+)', l)
        if m:
            path, line = m.group(1) + '.kt', int(m.group(2))
            try:
                src = open(path).readlines()[line-1].strip()
            except Exception:
                src = ''
            print(f'  {os.path.basename(path)}:{line}: {src[:100]} | {l.split(chr(58),4)[-1][:80]}')
            cnt += 1
            if cnt >= 6:
                break

print("=== safe-call / nullable errors (109) ===")
cnt = 0
for l in errs:
    if 'Only safe' in l or 'non-null asserted' in l:
        m = re.search(r'file://(/.+?)\.kt:(\d+):(\d+)', l)
        if m:
            path, line = m.group(1) + '.kt', int(m.group(2))
            try:
                src = open(path).readlines()[line-1].strip()
            except Exception:
                src = ''
            print(f'  {os.path.basename(path)}:{line}: {src[:100]}')
            cnt += 1
            if cnt >= 8:
                break

print("=== property must be initialized (35) ===")
cnt = 0
for l in errs:
    if 'Property must be initialized' in l:
        m = re.search(r'file://(/.+?)\.kt:(\d+):(\d+)', l)
        if m:
            path, line = m.group(1) + '.kt', int(m.group(2))
            try:
                src = open(path).readlines()[line-1].strip()
            except Exception:
                src = ''
            print(f'  {os.path.basename(path)}:{line}: {src[:100]}')
            cnt += 1
            if cnt >= 8:
                break

print("=== unresolved label (42) ===")
cnt = 0
for l in errs:
    if 'Unresolved label' in l:
        m = re.search(r'file://(/.+?)\.kt:(\d+):(\d+)', l)
        if m:
            path, line = m.group(1) + '.kt', int(m.group(2))
            try:
                src = open(path).readlines()[line-1].strip()
            except Exception:
                src = ''
            print(f'  {os.path.basename(path)}:{line}: {src[:100]} | {l.split(chr(58),4)[-1][:70]}')
            cnt += 1
            if cnt >= 6:
                break

print("=== Optional type mismatch (44) ===")
cnt = 0
for l in errs:
    if 'Optional' in l:
        m = re.search(r'file://(/.+?)\.kt:(\d+):(\d+)', l)
        if m:
            path, line = m.group(1) + '.kt', int(m.group(2))
            try:
                src = open(path).readlines()[line-1].strip()
            except Exception:
                src = ''
            print(f'  {os.path.basename(path)}:{line}: {src[:100]} | {l.split(chr(58),4)[-1][:90]}')
            cnt += 1
            if cnt >= 6:
                break
