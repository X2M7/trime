#!/usr/bin/env python3
"""Tiny client of the existing, unmodified self-targeted Observer9 APK.

It only publishes authenticated requests and retrieves original observations.
The helper itself enforces its fixed 28-minute/90-second/400-request limits.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import time
import uuid

ADB = '/home/xumin/Public/android-toolchain/android-sdk/platform-tools/adb'
HELPER = 'org.x2m7.trime.editorobserver8'
TARGET_SHA = '6dc7da9b11bad2ea9b41dc6f907bd0809484656ea31001f34c9e571343047e90'
HELPER_SHA = '9edd1ffeade1621faa5f6c51bb1699032bfcd827f3cd28a0143b37099ad812aa'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['init', 'ready', 'capture', 'stop'])
    parser.add_argument('--serial', required=True)
    parser.add_argument('--session', required=True, type=Path)
    parser.add_argument('--name')
    parser.add_argument('--expected-apk-sha', default=TARGET_SHA)
    args = parser.parse_args()
    state_path = args.session / 'client-state.json'
    if args.action == 'init':
        args.session.mkdir(parents=True, exist_ok=False)
        token = uuid.uuid4().hex
        command = [ADB, '-s', args.serial, 'shell', 'am', 'instrument', '-w', '-r',
            '-e', 'mode', 'session', '-e', 'session_token', token,
            '-e', 'expected_package', 'com.osfans.trime.debug',
            '-e', 'expected_apk_sha256', args.expected_apk_sha, HELPER + '/.Observer']
        state = {'token': token, 'sequence': 0, 'serial': args.serial,
            'remote': '/data/data/' + HELPER + '/cache/session-' + token,
            'command': command, 'expected_target_sha256': args.expected_apk_sha,
            'expected_helper_sha256': HELPER_SHA}
        state_path.write_text(json.dumps(state, indent=2) + '\n')
        print(json.dumps(state, indent=2))
        return
    state = json.loads(state_path.read_text())
    assert state['serial'] == args.serial
    remote = state['remote']

    def run(label, command, destination=None):
        result = subprocess.run([ADB, '-s', args.serial, *command], capture_output=True, timeout=40)
        log = {'command': [ADB, '-s', args.serial, *command], 'exit': result.returncode,
            'stdout': result.stdout.decode('utf-8', errors='replace'),
            'stderr': result.stderr.decode('utf-8', errors='replace')}
        if destination:
            destination.write_text(json.dumps(log, indent=2) + '\n')
        assert result.returncode == 0, label
        return result.stdout

    def original_json(filename):
        deadline = time.monotonic() + 40
        while time.monotonic() < deadline:
            data = run('read ' + filename, ['exec-out', 'cat', remote + '/' + filename])
            try:
                row = json.loads(data)
                return row, data
            except (ValueError, UnicodeDecodeError):
                time.sleep(0.1)
        raise TimeoutError('Missing original complete JSON: ' + filename)

    def identity(row):
        assert row['session_token'] == state['token']
        assert row['package'] == 'com.osfans.trime.debug'
        assert row['installed_apk_sha256'] == state['expected_target_sha256']
        assert row['observer_apk_sha256'] == HELPER_SHA
        if state.get('observer_pid'):
            assert row['observer_pid'] == state['observer_pid']
            assert row['observer_starttime_ticks'] == state['observer_starttime_ticks']
            assert row['boot_id'] == state['boot_id']

    if args.action == 'ready':
        row, data = original_json('ready.json')
        identity(row)
        assert row['ready'] and row['session_timeout_ms'] == 1680000 and row['idle_timeout_ms'] == 90000
        (args.session / 'ready.json').write_bytes(data)
        for key in ['observer_pid', 'observer_starttime_ticks', 'boot_id']:
            state[key] = row[key]
        state_path.write_text(json.dumps(state, indent=2) + '\n')
        print(json.dumps(row, indent=2))
        return

    assert args.name and args.name.replace('-', '').replace('_', '').isalnum()
    out = args.session / args.name
    out.mkdir(exist_ok=False)
    sequence = state['sequence'] + 1
    request = {'session_token': state['token'], 'sequence': sequence,
        'action': args.action, 'capture_token': uuid.uuid4().hex}
    if args.action == 'capture':
        request.update({'settle_ms': 2000, 'listen_ms': 0})
    published = out / 'request.json'
    published.write_text(json.dumps(request) + '\n')
    base = remote + '/requests/' + '%05d' % sequence
    run('push request', ['push', str(published), base + '.tmp'], out / 'push.json')
    run('publish request', ['shell', 'mv', base + '.tmp', base + '.json'], out / 'publish.json')
    # A published request is never reused even if the captured UI is invalid.
    state['sequence'] = sequence
    state_path.write_text(json.dumps(state, indent=2) + '\n')
    if args.action == 'capture':
        remote_response = 'response-' + request['capture_token']
        row, data = original_json(remote_response + '/observation.json')
        identity(row)
        assert row['sequence'] == sequence and row['capture_token'] == request['capture_token']
        (out / 'original-observation.json').write_bytes(data)
        run('pull originals', ['pull', remote + '/' + remote_response, str(out / 'original')], out / 'pull.json')
        if row.get('capture_complete'):
            for filename, key in [('before.xml', 'before_xml_sha256'), ('after.xml', 'after_xml_sha256'), ('in-connection.png', 'png_sha256')]:
                assert hashlib.sha256((out / 'original' / filename).read_bytes()).hexdigest() == row[key]
        print(json.dumps(row, indent=2))
        assert row.get('capture_complete'), 'Capture failed; original receipt retained'
    else:
        row, data = original_json('finished.json')
        identity(row)
        (out / 'original-finished.json').write_bytes(data)
        print(json.dumps(row, indent=2))
        assert row['passed'] and row['stop_received'] and row['stop_sequence'] == sequence


if __name__ == '__main__':
    main()
