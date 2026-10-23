from pathlib import Path
import hashlib, json, subprocess, time

root = Path(__file__).resolve().parent
out = root / 'cancel-diagnostic1'
out.mkdir(exist_ok=False)
work = Path('/home/xumin/Public/Trime-pr-logcat-reliability')
commands = json.loads((root / 'host3/commands.json').read_text())
compile_base = commands[0]['argv']
java = compile_base[0]
compiler_cp = compile_base[3]
base_cp = commands[2]['argv'][-2]
base_classes = str(root / 'host3/classes')
deps = base_cp[len(base_classes) + 1:]
cmds = []
def run(name, args):
    started = time.time()
    p = subprocess.run(args, capture_output=True, timeout=80)
    (out / (name + '.stdout')).write_bytes(p.stdout)
    (out / (name + '.stderr')).write_bytes(p.stderr)
    cmds.append({'name': name, 'argv': args, 'exit': p.returncode, 'seconds': time.time()-started})
    (out / 'commands.json').write_text(json.dumps(cmds, indent=2)+'\n')
    print(name, p.returncode, p.stdout.decode(errors='replace')[-7000:], p.stderr.decode(errors='replace')[-3000:], flush=True)
    return p.returncode

fixture = out / 'CancellationCleanupDiagnostic.kt'
fixture.write_text('''package com.osfans.trime.util
import io.kotest.core.spec.style.StringSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import java.io.IOException
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.TimeUnit
class CancellationCleanupDiagnostic : StringSpec({
  "actual cancelled await retains the original cleanup failure" {
    runBlocking {
      val pipe = object : BlockingPipe() { override fun close() = release() }
      val destroyFailure = IOException("Synthetic destruction failure")
      val process = PipeProcess(pipe, { throw destroyFailure })
      val reading = async(Dispatchers.Default) { process.consumeOutput { it.readLines() } }
      check(pipe.entered.await(5, TimeUnit.SECONDS))
      withTimeout(3_000L) { reading.cancelAndJoin() }
      val failure = shouldThrow<CancellationException> { reading.await() }
      val pending = ArrayDeque<Throwable>()
      val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
      pending.add(failure)
      while (pending.isNotEmpty()) {
        val next = pending.removeFirst()
        if (!seen.add(next)) continue
        println("NODE identity=${System.identityHashCode(next)} class=${next.javaClass.name} message=${next.message} cause=${next.cause?.let { System.identityHashCode(it) }} suppressed=${next.suppressed.map { System.identityHashCode(it) }} originalDestroy=${next === destroyFailure}")
        next.cause?.let(pending::add)
        pending.addAll(next.suppressed)
      }
      println("DESTROY originalIdentity=${System.identityHashCode(destroyFailure)} actualDestructions=${process.destructions.get()} reachable=${destroyFailure in seen}")
      failure.printStackTrace(System.out)
      (destroyFailure in seen) shouldBe true
      process.assertReleased()
    }
  }
})
''')
launcher = out / 'DiagnosticJUnit.java'
text = (root / 'host3/fixtures/HostJUnit.java').read_text().replace('public class HostJUnit', 'public class DiagnosticJUnit')
start = text.index('      .selectors(')
end = text.index('      .configurationParameter(', start)
text = text[:start] + '      .selectors(selectClass("com.osfans.trime.util.CancellationCleanupDiagnostic"))\n' + text[end:]
launcher.write_text(text)
common = out / 'common'; common.mkdir()
assert run('launcher-compile', [java.replace('/java', '/javac'), '-J-Xmx128m', '--release', '11', '-Xlint:all', '-Werror', '-cp', deps, '-d', str(common), str(launcher)]) == 0
final = (work / 'app/src/main/java/com/osfans/trime/util/Process.kt').read_text()
loop = '''            while (primary is CancellationException && primary.cause is CancellationException) {
                primary = primary.cause
            }
'''
assert loop in final
variants = {
    'host1-original': (root / 'host1/source/app/src/main/java/com/osfans/trime/util/Process.kt').read_text(),
    'final-no-cause-restore': final.replace(loop, ''),
    'final-with-cause-restore': final,
}
results = {}
for name, source in variants.items():
    dest = out / name; dest.mkdir(); classes = dest / 'classes'; classes.mkdir()
    file = dest / 'Process.kt'; file.write_text(source)
    result = run(name+'-compile', [java, '-Xmx384m', '-cp', compiler_cp, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-Werror', '-jvm-target', '11', '-Xfriend-paths='+base_classes, '-classpath', base_cp, '-d', str(classes), str(file), str(fixture)])
    assert result == 0
    exit_code = run(name+'-test', [java, '-Xmx256m', '-Dkotest.framework.classpath.scanning.autoscan.disable=true', '-Dkotest.framework.classpath.scanning.config.disable=true', '-cp', str(classes)+':'+str(common)+':'+base_cp, 'DiagnosticJUnit'])
    results[name] = {'source_sha256': hashlib.sha256(file.read_bytes()).hexdigest(), 'compile_exit': result, 'test_exit': exit_code}
(out/'result.json').write_text(json.dumps(results, indent=2)+'\n')
