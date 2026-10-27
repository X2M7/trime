#!/usr/bin/env python3
import datetime
import hashlib
import json
import os
import signal
from pathlib import Path
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parent
ASSET_ROOT = ROOT / 'vm-assets-api35-r2'
OUT = ROOT / 'api35-8a1e-v11-run2'
OUT.mkdir(exist_ok=True)
SDK = Path('/home/xumin/Public/android-toolchain/android-sdk')
ADB = SDK / 'platform-tools/adb'
SERIAL = 'emulator-5584'
BINDING = ROOT / 'original-apk-binding-8a1e-v10.json'
if not BINDING.is_file():
    raise SystemExit('New source APK is UNFROZEN; do not start a VM')
binding = json.loads(BINDING.read_bytes())
if binding.get('state') != 'FROZEN_ORIGINAL_CLOUD_APK' or binding.get('source_head') != '8a1ee0ea6538e220fcddf31ed6fd491f5bc3cca7':
    raise SystemExit('New source APK binding is not frozen for this round')
helper_path = ROOT / 't9_smoke_helper_v7.py'
if (not helper_path.is_file() or
        hashlib.sha256(helper_path.read_bytes()).hexdigest() != binding.get('helper_sha256')):
    raise SystemExit('Reviewed producer validator/helper bytes differ before VM launch')
from t9_smoke_helper_v7 import validate_producer_binding
producer_verification = validate_producer_binding(binding)
apk = Path(binding['original_apk_path'])
if hashlib.sha256(apk.read_bytes()).hexdigest() != binding['original_apk_sha256']:
    raise SystemExit('Original cloud APK changed')
STOP = OUT / 'stop-request'
assert not STOP.exists()
for output_name in ('emulator.log', 'memory.jsonl', 'launch.json', 'booted.json',
                    'device-logcat.txt', 'completion.json'):
    if (OUT / output_name).exists():
        raise SystemExit('Existing round output; use a new output directory')
# Refuse a second VM; the operator normally stops and reaps one before the other.
for entry in Path('/proc').iterdir():
    if not entry.name.isdigit():
        continue
    try:
        binary = (entry / 'cmdline').read_bytes().split(b'\0', 1)[0]
    except (FileNotFoundError, PermissionError, ProcessLookupError):
        continue
    if b'qemu-system' in binary or binary.endswith(b'/emulator'):
        raise SystemExit('Another emulator is active; refusing concurrent VM startup')
env = dict(os.environ)
env.update(ANDROID_HOME=str(SDK), ANDROID_SDK_ROOT=str(SDK),
           ANDROID_USER_HOME=str(ASSET_ROOT / 'android-user'),
           ANDROID_AVD_HOME=str(ASSET_ROOT / 'avd'))
argv = [str(SDK / 'emulator/emulator'), '-avd', 'trime_pr2183_api35_20261027_r2',
        '-sysdir', str(ASSET_ROOT / 'api35' / 'system-images'), '-no-snapshot', '-no-window',
        '-no-boot-anim', '-gpu', 'swangle', '-cores', '4', '-memory', '3072',
        '-port', '5584', '-accel', 'on', '-feature', '-Vulkan']
record = {'argv': argv, 'serial': SERIAL, 'started_utc': datetime.datetime.now(
    datetime.timezone.utc).isoformat(), 'memory_floor_kib': 1048576,
    'source_head': binding['source_head'], 'original_apk_sha256': binding['original_apk_sha256'],
    'source_producer_verification': producer_verification,
    'controller_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
    'session_lifetime_guard': None, 'operator_normal_stop_required': True}

def adb(*args, timeout=12):
    return subprocess.run([str(ADB), '-s', SERIAL, *args], capture_output=True,
                          text=True, timeout=timeout)


def memory_sample():
    data = {}
    for line in Path('/proc/meminfo').read_text().splitlines():
        key, value = line.split(':', 1)
        if key in ('MemAvailable', 'MemTotal', 'SwapFree'):
            data[key] = int(value.strip().split()[0])
    data['utc'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    return data


cleanup_errors = []


def cleanup_error(stage, error):
    cleanup_errors.append({'stage': stage, 'type': type(error).__name__,
                           'message': str(error)})


def terminate_owned(child, name):
    if child.poll() is not None:
        return
    record[name + '_term_requested'] = True
    try:
        child.terminate()
    except Exception as error:
        cleanup_error(name + '-term', error)
    try:
        child.wait(timeout=15)
        return
    except subprocess.TimeoutExpired as error:
        cleanup_error(name + '-term-wait', error)
    except Exception as error:
        cleanup_error(name + '-wait', error)
    if child.poll() is None:
        record[name + '_kill_requested'] = True
        try:
            child.kill()
        except Exception as error:
            cleanup_error(name + '-kill', error)
        try:
            child.wait(timeout=15)
        except Exception as error:
            cleanup_error(name + '-kill-wait', error)


proc = None
collector = None
logs = None
reason = 'unspecified'


def on_termination(signum, _frame):
    global reason
    reason = 'controller-signal-' + str(signum)
    # Preserve the first termination as a failure while allowing bounded finally
    # cleanup to finish if the host sends the same signal again.
    signal.signal(signal.SIGTERM, signal.SIG_IGN)
    raise SystemExit(128 + signum)


signal.signal(signal.SIGTERM, on_termination)
try:
    with (OUT / 'emulator.log').open('wb') as log, (OUT / 'memory.jsonl').open('w') as mem:
        initial_memory = memory_sample()
        record['prelaunch_memory'] = initial_memory
        mem.write(json.dumps(initial_memory) + '\n')
        mem.flush()
        if initial_memory['MemAvailable'] < record['memory_floor_kib']:
            reason = 'prelaunch-host-memory-guard'
            raise SystemExit('Insufficient MemAvailable before emulator startup')
        proc = subprocess.Popen(argv, env=env, stdout=log, stderr=subprocess.STDOUT,
                                start_new_session=True)
        record['pid'] = proc.pid
        record['proc_stat_at_launch'] = Path('/proc', str(proc.pid), 'stat').read_text()
        (OUT / 'launch.json').write_text(json.dumps(record, indent=2) + '\n')
        print('API35-8a1e emulator PID', proc.pid, 'serial', SERIAL, flush=True)
        booted = False
        launched = time.monotonic()
        minimum = initial_memory['MemAvailable']
        while proc.poll() is None:
            data = memory_sample()
            minimum = min(minimum or data['MemAvailable'], data['MemAvailable'])
            mem.write(json.dumps(data) + '\n')
            mem.flush()
            if data['MemAvailable'] < record['memory_floor_kib']:
                reason = 'host-memory-guard'
                break
            if STOP.exists():
                reason = 'requested-normal-stop'
                break
            if not booted:
                try:
                    result = adb('shell', 'getprop', 'sys.boot_completed')
                    booted = result.returncode == 0 and result.stdout.strip() == '1'
                except subprocess.TimeoutExpired:
                    pass
                if booted:
                    (OUT / 'booted.json').write_text(json.dumps({
                        'utc': data['utc'], 'boot_seconds': time.monotonic() - launched,
                        'serial': SERIAL, 'emulator_pid': proc.pid}, indent=2) + '\n')
                    logs = (OUT / 'device-logcat.txt').open('wb')
                    collector = subprocess.Popen([str(ADB), '-s', SERIAL, 'logcat',
                        '-b', 'all', '-v', 'threadtime'], stdout=logs, stderr=logs)
                    print('API35-8a1e boot completed', flush=True)
                elif time.monotonic() - launched > 240:
                    reason = 'boot-timeout'
                    break
            time.sleep(2)
        else:
            reason = 'emulator-exited'
        record['minimum_mem_available_kib'] = minimum
        record['booted'] = booted
finally:
    original_error = sys.exc_info()[1]
    if original_error is not None:
        record['controller_error'] = {'type': type(original_error).__name__,
                                      'message': str(original_error)}
    try:
        if proc is not None:
            try:
                if proc.poll() is None:
                    try:
                        result = adb('emu', 'kill', timeout=15)
                        record['normal_stop'] = {'exit': result.returncode,
                            'stdout': result.stdout, 'stderr': result.stderr}
                        if result.returncode != 0:
                            cleanup_error('emulator-normal-stop', RuntimeError(
                                'adb emu kill exit ' + str(result.returncode)))
                    except Exception as error:
                        cleanup_error('emulator-normal-stop', error)
                    try:
                        proc.wait(timeout=35)
                    except Exception as error:
                        cleanup_error('emulator-normal-stop-wait', error)
                    if proc.poll() is None:
                        record['stop_escalated_to_term'] = True
                        terminate_owned(proc, 'emulator')
            except Exception as error:
                cleanup_error('emulator-cleanup', error)
            finally:
                record['emulator_exit'] = proc.returncode
    finally:
        try:
            if collector is not None:
                try:
                    terminate_owned(collector, 'collector')
                except Exception as error:
                    cleanup_error('collector-cleanup', error)
                finally:
                    record['collector_exit'] = collector.returncode
        finally:
            try:
                if logs is not None:
                    logs.close()
            except Exception as error:
                cleanup_error('collector-log-close', error)
            finally:
                pending_error = sys.exc_info()[1]
                if pending_error is not None and pending_error is not original_error:
                    cleanup_error('cleanup-interrupted', pending_error)
                record['cleanup_errors'] = cleanup_errors
                record['reason'] = reason
                record['ended_utc'] = datetime.datetime.now(
                    datetime.timezone.utc).isoformat()
                (OUT / 'completion.json').write_text(json.dumps(record, indent=2) + '\n')
                print('API35-8a1e stopped:', reason, record.get('emulator_exit'), flush=True)
    if cleanup_errors:
        raise RuntimeError('API35-8a1e cleanup failed; see completion.json') from original_error
