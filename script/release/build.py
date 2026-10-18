#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""One bounded build phase. No emulator may run concurrently; no daemon remains."""
import argparse
import json
import os
from pathlib import Path
import signal
import shutil
import subprocess
import time

from release import ROOT, binding, metadata, repositories, require, write_json


CODE_CACHE_MIB = 384
NATURAL_EXIT_WAIT_SECONDS = 60


def available():
    return int(next(x.split()[1] for x in Path('/proc/meminfo').read_text().splitlines() if x.startswith('MemAvailable:')))


def identity(pid):
    try:
        fields = Path(f'/proc/{pid}/stat').read_text().rsplit(') ', 1)[1].split()
        return int(fields[1]), fields[19]
    except (OSError, IndexError, ValueError):
        return None


def processor_count(value):
    try:
        count = int(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError('Processors must be an integer') from error
    maximum = min(16, os.cpu_count() or 1)
    if not 1 <= count <= maximum:
        raise argparse.ArgumentTypeError(f'Processors must be between 1 and {maximum}')
    return count


def arguments(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('phase', choices=('debug', 'release', 'lint', 'format'))
    parser.add_argument('--sdk', type=Path, required=True)
    parser.add_argument('--jdk', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--build-timestamp', type=int,
                        help='Shared candidate build time in milliseconds for all three phases')
    parser.add_argument('--compact-memory', action='store_true',
                        help='Use a 768 MiB heap and 3.75 GiB start guard; retain 640 MiB Metaspace and 1 GiB runtime floor')
    parser.add_argument('--heap-mib', type=int,
                        help='Explicit heap size; retain one worker, 640 MiB Metaspace and the 1 GiB runtime floor')
    parser.add_argument('--minimum-start-available-kib', type=int,
                        help='Explicit startup headroom override; runtime 1 GiB floor and JVM limits remain unchanged')
    parser.add_argument('--processors', type=processor_count, default=1,
                        help='Active JVM processors, from 1 through min(16, host CPUs); Gradle and CMake remain single-worker')
    return parser.parse_args(argv)


def build_command(phase, heap_mib, processors):
    phases = {
        'debug': [':spotlessCheck', ':build-logic:convention:test', ':build-logic:compatibility:test',
                  ':app:compileDebugKotlin', ':app:compileDebugAndroidTestKotlin',
                  ':app:testDebugUnitTest', ':app:assembleDebug', ':app:assembleDebugAndroidTest'],
        'release': [':app:assembleRelease', '-x', ':app:lintVitalAnalyzeRelease',
                    '-x', ':app:lintVitalReportRelease', '-x', ':app:lintVitalRelease'],
        'lint': [':app:lintDebug', ':app:lintRelease'],
        'format': [':spotlessApply'],
    }
    return ['./gradlew', '--no-daemon', '--console=plain', '--max-workers=1', '--warning-mode=fail',
            f'-Dorg.gradle.jvmargs=-Xmx{heap_mib}m -XX:MaxMetaspaceSize=640m '
            f'-XX:ActiveProcessorCount={processors} -XX:ReservedCodeCacheSize={CODE_CACHE_MIB}m',
            '-Pkotlin.compiler.execution.strategy=in-process', '-Ptrime.performanceTrace=false',
            '-Ptrime.forkReleaseIdentity=true', *phases[phase]]


def wait_for_children(live_children):
    deadline = time.monotonic() + NATURAL_EXIT_WAIT_SECONDS
    pending = live_children()
    while pending:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            break
        time.sleep(min(.2, remaining))
        pending = live_children()
    return pending


def main():
    args = arguments()
    build_timestamp = args.build_timestamp if args.build_timestamp is not None else int(time.time() * 1000)
    require(10**12 <= build_timestamp <= int(time.time() * 1000),
            'Build timestamp must be a past or current millisecond epoch, not seconds')
    meta = metadata()
    heap_mib = 768 if args.compact_memory else 896
    if args.heap_mib is not None:
        require(not args.compact_memory and args.heap_mib >= 512,
                'Explicit heap must be at least 512 MiB and cannot be combined with compact-memory')
        heap_mib = args.heap_mib
    start_kib = 3840 * 1024 if args.compact_memory else 4 * 1024 * 1024
    if args.minimum_start_available_kib is not None:
        require(args.minimum_start_available_kib >= 1024 * 1024,
                'Startup headroom cannot be below the unchanged 1 GiB runtime floor')
        start_kib = args.minimum_start_available_kib
    require(start_kib >= (heap_mib + 640 + 1024) * 1024,
            'Startup headroom must cover heap, Metaspace and the unchanged 1 GiB runtime floor')
    require(available() >= start_kib, f'Wait for at least {start_kib / 1024 / 1024:g} GiB available')
    for path in Path('/proc').iterdir():
        if not path.name.isdecimal():
            continue
        try:
            argv = (path / 'cmdline').read_bytes().split(b'\0')
            name = os.path.basename(argv[0]).decode(errors='replace')
            busy = name.startswith('qemu-system-') or name == 'emulator' or (
                name == 'java' and any(s in b' '.join(argv) for s in (b'org.gradle.', b'GradleDaemon', b'GradleWrapperMain', b'kotlin.daemon')))
            require(not busy, 'Another emulator or build is running')
        except (OSError, IndexError):
            continue
    args.output.mkdir(parents=True, exist_ok=False)
    sources = repositories()
    env = dict(JAVA_HOME=str(args.jdk.resolve()), ANDROID_HOME=str(args.sdk.resolve()),
               ANDROID_SDK_ROOT=str(args.sdk.resolve()), BUILD_ABI=','.join(meta['abis']),
               CMAKE_BUILD_PARALLEL_LEVEL='1', BUILD_GIT_REPO='https://github.com/' + meta['repository'],
               BUILD_VERSION_NAME=meta['versionName'], BUILD_COMMIT_HASH=sources[''],
               BUILD_TIMESTAMP=str(build_timestamp), CI_NAME='X2M7')
    argv = build_command(args.phase, heap_mib, args.processors)
    write_json(args.output / 'environment.json', env)
    write_json(args.output / 'source.json', sources)
    write_json(args.output / 'command.json', argv)
    runenv = dict(os.environ, **env)
    runenv['PATH'] = env['JAVA_HOME'] + '/bin:' + runenv['PATH']
    owned, minimum, reason, started = {}, available(), None, time.time()
    signals, residual = [], []
    with (args.output / 'command.log').open('x') as log, (args.output / 'memory.jsonl').open('x') as samples:
        process = subprocess.Popen(argv, cwd=ROOT, env=runenv, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
        owned[process.pid] = identity(process.pid)
        try:
            while process.poll() is None:
                rows = {int(p.name): identity(int(p.name)) for p in Path('/proc').iterdir() if p.name.isdecimal()}
                while True:
                    new = {pid: row for pid, row in rows.items() if row and row[0] in owned and owned[row[0]] and
                           rows.get(row[0]) and rows[row[0]][1] == owned[row[0]][1] and pid not in owned}
                    if not new:
                        break
                    owned.update(new)
                current = available()
                minimum = min(minimum, current)
                samples.write(json.dumps(dict(epoch=time.time(), available_kib=current)) + '\n')
                samples.flush()
                if current < 1024 * 1024 or time.time() - started > 5400:
                    reason = 'Memory below 1 GiB' if current < 1024 * 1024 else '90 minute build deadline'
                    break
                time.sleep(2)
        except BaseException:
            reason = 'Build controller interrupted'
        finally:
            def live_children():
                result = []
                for pid, row in owned.items():
                    current = identity(pid)
                    if pid != process.pid and row and current and current[1] == row[1]:
                        result.append(pid)
                return result
            if reason is None:
                wait_for_children(live_children)
            # Only descendants identified during this owned build are terminated.
            for sig in (signal.SIGTERM, signal.SIGKILL):
                for pid, row in owned.items():
                    now = identity(pid)
                    if row and now and row[1] == now[1] and (reason or pid != process.pid):
                        try:
                            os.kill(pid, sig)
                            signals.append(dict(pid=pid, starttime=row[1], signal=sig.name))
                        except ProcessLookupError:
                            pass
                if sig == signal.SIGTERM and any(identity(pid) == row for pid, row in owned.items()):
                    time.sleep(2)
            code = process.wait()
            residual = wait_for_children(live_children)
            if signals or residual:
                reason = reason or 'Build descendants required termination or remain alive'
    try:
        unchanged = args.phase == 'format' or sources == repositories()
    except ValueError:
        unchanged = False
    result = dict(phase=args.phase, started=started, finished=time.time(), exit_code=code,
                  memory_profile='custom' if args.heap_mib is not None else ('compact' if args.compact_memory else 'standard'),
                  heap_mib=heap_mib, metaspace_mib=640, code_cache_mib=CODE_CACHE_MIB,
                  active_processor_count=args.processors, natural_exit_wait_seconds=NATURAL_EXIT_WAIT_SECONDS,
                  minimum_start_available_kib=start_kib,
                  startup_guard_override=args.minimum_start_available_kib is not None,
                  stopped_reason=reason, minimum_available_kib=minimum, source_unchanged=unchanged,
                  build_git_sha=sources[''], environment=binding(args.output / 'environment.json'),
                  command=binding(args.output / 'command.json'), log=binding(args.output / 'command.log'),
                  source=binding(args.output / 'source.json'), metadata=binding(ROOT / 'release.properties'),
                  cleanup_signals=signals, residual_owned_pids=residual)
    try:
        if code == 0 and reason is None and unchanged and args.phase in ('debug', 'release'):
            (args.output / 'apks').mkdir()
            folders = [ROOT / 'app/build/outputs/apk' / args.phase]
            if args.phase == 'debug':
                folders.append(ROOT / 'app/build/outputs/apk/androidTest/debug')
            for folder in folders:
                for apk in folder.glob('*.apk'):
                    shutil.copyfile(apk, args.output / 'apks' / apk.name)
            result['apks'] = [binding(p) for p in sorted((args.output / 'apks').glob('*.apk'))]
            config = ROOT / 'app/build/generated/source/buildConfig' / args.phase / 'com/osfans/trime/BuildConfig.java'
            shutil.copyfile(config, args.output / 'BuildConfig.java')
            result['build_config'] = binding(args.output / 'BuildConfig.java')
        if code == 0 and args.phase in ('debug', 'lint'):
            if args.phase == 'debug':
                src = ROOT / 'app/build/test-results/testDebugUnitTest'
                if src.exists():
                    shutil.copytree(src, args.output / 'unit-tests', ignore=shutil.ignore_patterns('binary'))
                    result['unit_tests'] = [binding(p) for p in sorted((args.output / 'unit-tests').glob('*.xml'))]
                result['build_logic_tests'] = []
                for module in ('convention', 'compatibility'):
                    source = ROOT / 'build-logic' / module / 'build/test-results/test'
                    destination = args.output / (module + '-tests')
                    shutil.copytree(source, destination, ignore=shutil.ignore_patterns('binary'))
                    result['build_logic_tests'].extend(binding(p) for p in sorted(destination.glob('*.xml')))
            else:
                for name in ('lint-results-debug.xml', 'lint-results-release.xml'):
                    shutil.copyfile(ROOT / 'app/build/reports' / name, args.output / name)
                result['lint'] = [binding(args.output / name) for name in ('lint-results-debug.xml', 'lint-results-release.xml')]
    except Exception as error:
        result['freeze_error'] = str(error)
    write_json(args.output / 'result.json', result)
    print(json.dumps(result), flush=True)
    raise SystemExit(0 if code == 0 and reason is None and unchanged and 'freeze_error' not in result else 1)


if __name__ == '__main__':
    main()
