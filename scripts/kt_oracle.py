import subprocess, os, tempfile, sys

JAVA = '/opt/data/tools/jdk-17.0.17+10/bin/java'
CP = open('/tmp/kc_cp.txt').read() if os.path.exists('/tmp/kc_cp.txt') else None

def kc(code, name='t'):
    """Compile Kotlin snippet with the project's exact Kotlin 2.4.10.
    Returns list of REAL semantic errors (harness stdlib-noise filtered out)."""
    d = tempfile.mkdtemp(); f = os.path.join(d, name + '.kt'); open(f, 'w').write(code)
    r = subprocess.run([JAVA, '-cp', CP, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
                        '-no-stdlib', '-no-reflect', '-nowarn', '-d', os.path.join(d, 'out'), f],
                       capture_output=True, text=True, timeout=240)
    errs = [l for l in (r.stdout + r.stderr).splitlines() if 'error:' in l]
    real = [e for e in errs if 'built-in declaration' not in e]
    return real

if __name__ == '__main__':
    # self-test
    assert kc('fun main() { val x: Int = "hello" }'), "oracle broken"
    assert not kc('fun main() { val x: Int = 5; println(x) }'), "false positive"
    print("oracle OK")
