from pathlib import Path
import hashlib
import json
import subprocess
import sys
import time
import zipfile

root = Path(__file__).resolve().parent
work = Path('/home/xumin/Public/Trime-pr-logcat-reliability')
output = root / sys.argv[1]
output.mkdir(exist_ok=False)
cache = Path('/home/xumin/.gradle/caches/modules-2/files-2.1')
java = '/home/xumin/Public/android-toolchain/jdk-17.0.19+10/bin/java'
javac = '/home/xumin/Public/android-toolchain/jdk-17.0.19+10/bin/javac'
compiler_cp = json.loads(Path('/home/xumin/Public/Trime/build/pr2187-settext-order-20261023/host-compile1/command.json').read_text())['argv'][3]

def jar(group, artifact, version):
    matches = list((cache / group / artifact / version).glob('*/*.jar'))
    assert len(matches) == 1, (group, artifact, version, matches)
    return str(matches[0])

deps = [jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.3.21'),
        jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.3.20'),
        jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.10.2'),
        jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-test-jvm', '1.10.2'),
        jar('io.github.classgraph', 'classgraph', '4.8.186'),
        jar('org.opentest4j', 'opentest4j', '1.3.0'),
        jar('org.apiguardian', 'apiguardian-api', '1.1.2'),
        jar('org.jetbrains', 'annotations', '13.0')]
deps += [str(p) for p in cache.glob('io.kotest/*/6.0.7/*/*.jar')]
deps += [str(p) for p in cache.glob('org.junit.platform/*/1.13.4/*/*.jar')]
android = '/home/xumin/Public/android-toolchain/android-sdk/platforms/android-35/android.jar'
assert Path(android).is_file()
timber = next(cache.glob('com.jakewharton.timber/timber/5.0.1/*/*.aar'))
with zipfile.ZipFile(timber) as archive:
    (output / 'timber-classes.jar').write_bytes(archive.read('classes.jar'))
deps += [str(output / 'timber-classes.jar'), android]

fixtures = output / 'fixtures'
fixtures.mkdir()
previous = Path('/home/xumin/Public/Trime/build/upstream-split-20261020/second-batch-host1/src/main/kotlin')
for name in ['com/osfans/trime/R.kt', 'com/osfans/trime/util/ExceptionFixture.kt']:
    (fixtures / Path(name).name).write_bytes((previous / name).read_bytes())

launcher = fixtures / 'HostJUnit.java'
launcher.write_text('''import java.io.PrintWriter;
import java.nio.file.*;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
public class HostJUnit {
  public static void main(String[] args) {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
      .selectors(selectClass("com.osfans.trime.util.ProcessLifecycleTest"),
                 selectClass("com.osfans.trime.util.LogcatLifecycleTest"),
                 selectClass("com.osfans.trime.ui.main.log.LogBufferTest"))
      .configurationParameter("kotest.framework.classpath.scanning.autoscan.disable", "true")
      .configurationParameter("kotest.framework.classpath.scanning.config.disable", "true")
      .build();
    SummaryGeneratingListener summary = new SummaryGeneratingListener();
    Launcher launcher = LauncherFactory.create();
    launcher.registerTestExecutionListeners(summary, new TestExecutionListener() {
      public void executionFinished(TestIdentifier id, TestExecutionResult result) {
        if (id.isTest()) System.out.println(result.getStatus() + " " + id.getDisplayName());
      }
    });
    launcher.execute(request);
    summary.getSummary().printTo(new PrintWriter(System.out, true));
    summary.getSummary().printFailuresTo(new PrintWriter(System.out, true));
    if (summary.getSummary().getTestsFoundCount() == 0 || summary.getSummary().getTotalFailureCount() != 0)
      System.exit(1);
  }
}
''')

sources = [work / name for name in [
    'app/src/main/java/com/osfans/trime/util/Process.kt',
    'app/src/main/java/com/osfans/trime/util/Logcat.kt',
    'app/src/main/java/com/osfans/trime/ui/main/log/LogBuffer.kt',
    'app/src/test/java/com/osfans/trime/util/ProcessLifecycleTest.kt',
    'app/src/test/java/com/osfans/trime/util/LogcatLifecycleTest.kt',
    'app/src/test/java/com/osfans/trime/ui/main/log/LogBufferTest.kt']]
binding = {str(p.relative_to(work)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
(output / 'inputs.json').write_text(json.dumps(binding, indent=2) + '\n')
for source in sources:
    saved = output / 'source' / source.relative_to(work)
    saved.parent.mkdir(parents=True, exist_ok=True)
    saved.write_bytes(source.read_bytes())
classes = output / 'classes'
classes.mkdir()
commands = []
def run(name, argv):
    started = time.time()
    result = subprocess.run(argv, capture_output=True, timeout=90)
    (output / (name + '.stdout')).write_bytes(result.stdout)
    (output / (name + '.stderr')).write_bytes(result.stderr)
    commands.append({'name': name, 'argv': argv, 'exit': result.returncode, 'seconds': time.time() - started})
    (output / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    print(name, result.returncode, 'stdout', len(result.stdout), 'stderr', len(result.stderr), flush=True)
    if result.returncode:
        print(result.stdout.decode(errors='replace')[-16000:])
        print(result.stderr.decode(errors='replace')[-16000:])
        raise SystemExit(result.returncode)

cp = ':'.join(deps)
run('compile', [java, '-Xmx384m', '-cp', compiler_cp, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
     '-no-stdlib', '-no-reflect', '-Werror', '-jvm-target', '11', '-classpath', cp,
     '-d', str(classes)] + [str(p) for p in sources] + [str(p) for p in fixtures.glob('*.kt')])
run('launcher-compile', [javac, '-J-Xmx128m', '--release', '11', '-Xlint:all', '-Werror',
    '-cp', cp, '-d', str(classes), str(launcher)])
run('tests', [java, '-ea', '-Xmx384m', '-Dkotest.framework.classpath.scanning.autoscan.disable=true',
    '-Dkotest.framework.classpath.scanning.config.disable=true', '-cp', str(classes) + ':' + cp, 'HostJUnit'])
after = {str(p.relative_to(work)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
assert after == binding
(output / 'result.json').write_text(json.dumps({'passed': True, 'input_bytes_unchanged': True,
    'head': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=work, text=True).strip(),
    'working_tree_dirty': True, 'scope': 'Host-only focused process/logcat/buffer tests, not Android app/Gradle/device acceptance',
    'commands': commands}, indent=2) + '\n')
