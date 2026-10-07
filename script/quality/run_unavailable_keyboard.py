#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Disposable-emulator fault injection through a real external editor and Retry button."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import time
import uuid
import zipfile

from run_navigation import select_test_ime
from editor_windows import capture_editor_window
from runtime_audit import (
    LOG_LINE,
    audit_log,
    device_audit_lock,
    log_after_marker,
    record_logcat,
)

PACKAGE = 'com.osfans.trime.debug'
FIXTURE = 'org.x2m7.trime.editorfixture'
IME = PACKAGE + '/com.osfans.trime.ime.core.TrimeInputMethodService'


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    with device_audit_lock(args.serial):
        run(args)


def run(args):
    out = args.output
    out.mkdir(parents=True, exist_ok=False)
    adb = [args.adb, '-s', args.serial]

    def shell(*command):
        return subprocess.check_output(adb + ['shell', *map(str, command)], text=True, stderr=subprocess.STDOUT, timeout=120).strip()

    require(shell('getprop', 'ro.kernel.qemu') == '1', 'Disposable emulator only')
    data = '/sdcard/Android/data/' + PACKAGE + '/files'
    require(
        shell('cat', data + '/runtime-audit-dedicated') == '',
        'Requires a marked disposable installation',
    )
    api = int(shell('getprop', 'ro.build.version.sdk'))
    checksum = ('/data/user_de/0/' if api >= 24 else '/data/data/') + PACKAGE + '/checksums.json'
    shared = data + '/shared/default.yaml'
    def read_file(path, private=False):
        command = ['run-as', PACKAGE, 'cat', path] if private else ['cat', path]
        return subprocess.check_output(adb + ['exec-out', *command], timeout=120)

    original_shared = read_file(shared)
    original_checksum = read_file(checksum, private=True)
    require(
        isinstance(json.loads(original_checksum), dict),
        'Readable original resource manifest required',
    )
    suffix = '.unavailable-audit-' + uuid.uuid4().hex
    saved_shared, saved_checksum = shared + suffix, checksum + suffix
    identity = {
        'schema': 1,
        'probe': 'unavailable-keyboard',
        'api': api,
        'fingerprint': shell('getprop', 'ro.build.fingerprint'),
        'source_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        'packages': {},
    }
    for package in (PACKAGE, FIXTURE, 'org.x2m7.trime.editorobserver'):
        path = shell('pm', 'path', package).removeprefix('package:')
        content = subprocess.check_output(adb + ['exec-out', 'cat', path], timeout=120)
        identity['packages'][package] = hashlib.sha256(content).hexdigest()
        if package == PACKAGE:
            with zipfile.ZipFile(io.BytesIO(content)) as apk:
                require(
                    original_shared == apk.read('assets/shared/default.yaml'),
                    'Fault injection requires the unmodified packaged default config',
                )
    identity['original_resource_hashes'] = {name: hashlib.sha256(value).hexdigest() for name, value in
                                            (('shared/default.yaml', original_shared), ('checksums.json', original_checksum))}
    (out / 'identity.json').write_text(json.dumps(identity, indent=2) + '\n')
    previous_ime = shell('settings', 'get', 'secure', 'default_input_method')
    moved_shared = moved_checksum = False
    passed = False
    observed_pids = set()
    activation_marker = 'TRIME-UNAVAILABLE-' + uuid.uuid4().hex

    def record_target_pids():
        result = subprocess.run(
            adb + ['shell', 'pidof', PACKAGE],
            capture_output=True,
            text=True,
            timeout=30,
            check=False,
        )
        if result.returncode == 0:
            for value in result.stdout.split():
                if value.isdigit() and int(value) > 0:
                    observed_pids.add(int(value))

    def capture(label, expected):
        root, xml, attempts = capture_editor_window(shell, expected, attempts=12)
        (out / (label + '.xml')).write_text(xml or '')
        (out / (label + '-capture.json')).write_text(json.dumps(attempts, indent=2) + '\n')
        require(root is not None, f'UI capture failed for {label}: {expected!r}')
        with (out / (label + '.png')).open('wb') as image:
            subprocess.run(adb + ['exec-out', 'screencap', '-p'], stdout=image, check=True, timeout=60)
        return root

    def tap(root, field, value):
        node = next(n for n in root.iter('node') if n.get(field) == value and n.get('enabled') == 'true')
        box = list(map(int, re.findall(r'\d+', node.get('bounds'))))
        shell('input', 'tap', (box[0] + box[2]) // 2, (box[1] + box[3]) // 2)

    try:
        shell('input', 'keyevent', 'KEYCODE_HOME')
        shell('ime', 'set', 'com.android.inputmethod.latin/.LatinIME')
        shell('am', 'force-stop', PACKAGE)
        shell('mv', shared, saved_shared)
        moved_shared = True
        shell('run-as', PACKAGE, 'mv', checksum, saved_checksum)
        moved_checksum = True
        shell('mkdir', shared)
        with record_logcat(adb, out / 'logcat-full.txt'):
            time.sleep(.2)
            shell('log', '-t', 'TrimeRuntimeAudit', activation_marker)
            select_test_ime(shell, IME)
            shell('am', 'start', '-S', '-W', '-n', FIXTURE + '/.EditorActivity', '--es', 'text', 'STABLE')
            root = capture('unavailable', [('text', 'Chinese input unavailable. Numbers only.'), ('content-desc', 'Retry deployment')])
            record_target_pids()
            buttons = [n for n in root.iter('node') if n.get('package') == PACKAGE and n.get('class') in ('android.widget.Button', 'android.widget.ImageButton')]
            require(len(buttons) == 18, 'Not every fallback key/tool is visible')
            navigation = [n for n in root.iter('node') if n.get('package') != PACKAGE and n.get('content-desc') in ('Back', 'Home', 'Recents', 'Overview')]
            require(navigation, 'System navigation controls were not captured')
            def box(node):
                return tuple(map(int, re.findall(r'-?\d+', node.get('bounds'))))
            for button in buttons:
                left, top, right, bottom = box(button)
                require(right > left and bottom > top, 'Fallback button has empty bounds')
                for nav in navigation:
                    nl, nt, nr, nb = box(nav)
                    require(
                        min(right, nr) <= max(left, nl) or min(bottom, nb) <= max(top, nt),
                        f'Fallback button overlaps navigation: {button.attrib!r}, {nav.attrib!r}',
                    )
            (out / 'navigation-bounds.json').write_text(json.dumps({'buttons': [n.attrib for n in buttons], 'navigation': [n.attrib for n in navigation]}, indent=2))
            tap(root, 'text', '2')
            root = capture('limited-input', [('text', 'STABLE2')])
            tap(root, 'content-desc', 'Delete')
            capture('limited-delete', [('text', 'STABLE')])
            shell('rmdir', shared)
            shell('mv', saved_shared, shared)
            moved_shared = False
            shell('run-as', PACKAGE, 'mv', saved_checksum, checksum)
            moved_checksum = False
            root = capture('retry-ready', [('content-desc', 'Retry deployment')])
            tap(root, 'content-desc', 'Retry deployment')
            deadline = time.monotonic() + 180
            while True:
                root, xml, _ = capture_editor_window(shell)
                descriptions = {n.get('content-desc') for n in root.iter('node')} if root is not None else set()
                if '2 ABC' in descriptions and '9 WXYZ' in descriptions:
                    break
                require(
                    time.monotonic() < deadline,
                    'Retry never restored a nine-key layout',
                )
                time.sleep(.5)
            root = capture('recovered', [('content-desc', '2 ABC'), ('text', 'STABLE')])
            record_target_pids()
            tap(root, 'content-desc', '6 MNO')
            tap(root, 'content-desc', '4 GHI')
            capture('recovered-candidates', [('text', 'ni')])
            passed = True
    finally:
        record_target_pids()
        if moved_shared or moved_checksum:
            shell('input', 'keyevent', 'KEYCODE_HOME')
            shell('ime', 'set', 'com.android.inputmethod.latin/.LatinIME')
            shell('am', 'force-stop', PACKAGE)
        if moved_shared:
            if subprocess.run(adb + ['shell', 'test', '-d', shared]).returncode == 0:
                shell('rmdir', shared)
            shell('mv', saved_shared, shared)
        if moved_checksum:
            shell('run-as', PACKAGE, 'mv', saved_checksum, checksum)
        select_test_ime(shell, previous_ime)
        restored = read_file(shared) == original_shared and read_file(checksum, private=True) == original_checksum
        logcat_path = out / 'logcat-full.txt'
        logcat = (logcat_path.read_text(encoding='utf-8', errors='replace')
                  if logcat_path.is_file() else '')
        try:
            scoped_log = log_after_marker(logcat, activation_marker)
            marker_valid = True
        except ValueError:
            scoped_log = ''
            marker_valid = False
        app_lines = [
            line
            for line in scoped_log.splitlines()
            if (match := LOG_LINE.match(line)) and int(match[1]) in observed_pids
        ]
        app_log = '\n'.join(app_lines) + ('\n' if app_lines else '')
        (out / 'app-logcat.txt').write_text(app_log, encoding='utf-8')
        audit = audit_log(app_log)
        (out / 'runtime-audit.json').write_text(json.dumps(audit, indent=2) + '\n')
        evidence_complete = marker_valid and bool(observed_pids) and audit['capture_has_records']
        functional_passed = passed and restored and evidence_complete
        identity['process_pids'] = sorted(observed_pids)
        identity['activation_marker'] = activation_marker
        identity['logcat_sha256'] = hashlib.sha256(
            logcat_path.read_bytes() if logcat_path.is_file() else b'',
        ).hexdigest()
        identity['app_logcat_sha256'] = hashlib.sha256(
            (out / 'app-logcat.txt').read_bytes(),
        ).hexdigest()
        (out / 'identity.json').write_text(json.dumps(identity, indent=2) + '\n')
        report = {
            'schema': 1,
            'probe': 'unavailable-keyboard',
            'passed': functional_passed,
            'functional_passed': functional_passed,
            'assertions_passed': passed,
            'injects_faults': True,
            'requires_log_review': True,
            'resources_restored': restored,
            'evidence_complete': evidence_complete,
            'process_pids': sorted(observed_pids),
            'activation_marker': activation_marker,
            'diagnostic_scope': 'after_activation_marker',
            'warning_free': audit['warning_free'],
            'warning_count': len(audit['warnings']),
            'full_probe_diagnostic_count': len(audit['known_project_diagnostics']),
            'runtime_audit_sha256': hashlib.sha256((out / 'runtime-audit.json').read_bytes()).hexdigest(),
        }
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
        require(restored, 'Fault-injected resource bytes were not restored')
        require(evidence_complete, 'Target-process runtime audit is incomplete')
    print(json.dumps(report))


if __name__ == '__main__':
    main()
