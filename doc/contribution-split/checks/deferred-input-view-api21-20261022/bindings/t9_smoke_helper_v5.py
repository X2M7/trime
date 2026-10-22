#!/usr/bin/env python3
"""Small interactive adapter around the already frozen Observer9 and ADB journal.

Preparation/import/--help performs no ADB operations. After Root hands off ADB:
  PYTHON_BASIC_REPL=1 python3 -i t9_smoke_helper_v5.py --binding FROZEN_BINDING_JSON --serial emulator-5554 --api 21 --output NEW_DIR
  S.install_fixture()  # only if that separate fixture is not already installed
  S.start()
  S.launch('multiline'); S.capture('trime-C1-before')
  S.tap(x, y); S.label('ni'); S.capture('trime-C1-locked')
  S.capture('trime-C1-after', expected='')
  S.finish()

All touches are explicit and journaled. Images require actual visual review;
this helper does not automatically mark business acceptance as passed.
"""
import argparse
import atexit
import base64
import hashlib
import json
from pathlib import Path
import re
import secrets
import sys
import xml.etree.ElementTree as ET

TOOLS = Path('/home/xumin/Public/Trime/build/t12/tools')
sys.path.insert(0, str(TOOLS))
from t12_release_setup_gui_v31 import Gui, clipped_box, now, require, save, sha
from observer9_session_client_v1 import Observer8SessionClient

# Root supplies a new exact source/artifact binding only after cloud audit.
# The preparation template is UNFROZEN and is deliberately not executable.
SOURCE_HEAD = None
ORIGINAL_APK = None
APK_SHA = None
# Retain this before Python -i removes its interactive __file__ binding.
PRODUCER = Path(__file__).resolve()
FIXTURE = Path('/home/xumin/Public/Trime/build/t10/editor-oracle-fixture-build2/editor-fixture.apk')
FIXTURE_SHA = 'e094d767c820b77ec93d9b3f461b2208b4d7dcbec4f37ea869926b7987dfea59'
FIXTURE_PACKAGE = 'org.x2m7.trime.editorfixture'
OBSERVER = Path('/home/xumin/Public/Trime/build/t12/observer9-build1/editor-fixture.apk')
OBSERVER_SHA = '9edd1ffeade1621faa5f6c51bb1699032bfcd827f3cd28a0143b37099ad812aa'
PACKAGE = 'com.osfans.trime.debug'


class Smoke(Gui):
    def __init__(self, args):
        global SOURCE_HEAD, ORIGINAL_APK, APK_SHA
        binding = json.loads(args.binding.read_bytes())
        require(binding.get('schema') == 'trime-targeted-debug-smoke-binding/v1'
                and binding.get('state') == 'FROZEN_ORIGINAL_CLOUD_APK',
                'New combined source/APK is UNFROZEN; no device operation is allowed')
        require(binding.get('helper_sha256') == sha(PRODUCER), 'Helper differs from frozen binding')
        SOURCE_HEAD = binding.get('source_head')
        require(isinstance(SOURCE_HEAD, str) and re.fullmatch(r'[0-9a-f]{40}', SOURCE_HEAD)
                and binding.get('actual_build_commit') == SOURCE_HEAD,
                'This round requires a direct exact cloud source build, not a virtual merge claim')
        require(binding.get('package') == PACKAGE, 'Frozen Debug package differs')
        ORIGINAL_APK = Path(binding['original_apk_path'])
        APK_SHA = binding.get('original_apk_sha256')
        require(ORIGINAL_APK.is_file() and isinstance(APK_SHA, str)
                and re.fullmatch(r'[0-9a-f]{64}', APK_SHA) and sha(ORIGINAL_APK) == APK_SHA,
                'Frozen original APK path/bytes differ before any device operation')
        audit = Path(binding['original_artifact_audit_path'])
        require(audit.is_file() and sha(audit) == binding.get('original_artifact_audit_sha256'),
                'Original cloud artifact audit differs')
        # Do not invoke T12's Release constructor or its unrelated release gates.
        self.args = args
        args.apk_sha256 = APK_SHA
        self.out = args.output.resolve()
        require(not self.out.exists(), 'Use a new evidence directory')
        self.out.mkdir(parents=True)
        for name in ['commands', 'captures']:
            (self.out / name).mkdir()
        self.command_count = self.capture_count = 0
        self.observer_session = None
        self.last_xml = None
        self.report = dict(format=1, task='T9 plus deferred InputView targeted Debug smoke', passed=False,
            started=now(), api=args.api, serial=args.serial, package=PACKAGE,
            apk_sha256=APK_SHA, helper_sha256=sha(PRODUCER), commands=[], actions=[], checks=[],
            reused_journal_source=dict(path=str(TOOLS / 't12_release_setup_gui_v31.py'),
                                       sha256=sha(TOOLS / 't12_release_setup_gui_v31.py')),
            captures=[], source_head=SOURCE_HEAD,
            source_binding=dict(path=str(args.binding.resolve()), sha256=sha(args.binding), value=binding),
            limits=['Explicit touches only; no automatic image or business acceptance.',
                    'Original cloud Debug APK; no signing, deployment, preference or YAML modification.',
                    'Oracle observes Editable text/selection; cancellation proves pending text was not committed.'])
        self.persist()
        atexit.register(self.cleanup)

    def identity(self, label):
        dump = self.shell('dumpsys', 'package', PACKAGE)
        (self.out / (label + '-package.txt')).write_text(dump + '\n')
        uid = re.search(r'(?:userId|appId)=(\d+)', dump)
        data = re.search(r'\bdataDir=(\S+)', dump)
        version = re.search(r'\bversionCode=(\d+)', dump)
        name = re.search(r'\bversionName=(\S+)', dump)
        require(uid and data and version and name and 'DEBUGGABLE' in dump,
                'Installed Debug identity missing')
        require(version[1] == '20261101' and name[1] == '3.3.13', 'APK manifest identity differs')
        paths = self.shell('pm', 'path', PACKAGE).splitlines()
        require(len(paths) == 1 and paths[0].startswith('package:'), 'One installed APK required')
        raw = self.raw(paths[0][8:], label + '-installed-apk')
        require(raw['sha256'] == APK_SHA, 'Installed product APK is not the original cloud APK')
        return dict(uid=int(uid[1]), data_dir=data[1], apk_sha256=APK_SHA,
                    version_code=int(version[1]), version_name=name[1], evidence=raw)

    def pid(self):
        rows = self.shell('ps', *([] if self.args.api == 21 else ['-A'])).splitlines()
        header = rows[0].split()
        matches = [r.split() for r in rows[1:] if r.split() and r.split()[-1] == PACKAGE]
        require(len(matches) == 1 and 'PID' in header, 'One stable Trime process required')
        pid = int(matches[0][header.index('PID')])
        stat = self.shell('cat', '/proc/' + str(pid) + '/stat')
        cmdline = self.run('exec-out', 'cat', '/proc/' + str(pid) + '/cmdline')
        require(cmdline.rstrip(b'\0') == PACKAGE.encode(), 'Trime PID was reused')
        return dict(pid=pid, starttime=stat.rsplit(') ', 1)[1].split()[19])

    def install_fixture(self):
        require(sha(FIXTURE) == FIXTURE_SHA, 'Fixture input bytes changed')
        return self.run('install', '-r', '-t', FIXTURE, timeout=90)

    def start(self):
        require(self.observer_session is None, 'Only one bounded observer session')
        self.report['boot_id'] = self.shell('cat', '/proc/sys/kernel/random/boot_id')
        self.report['fresh_identity'] = self.identity('start')
        self.report['frozen_pid'] = self.pid()
        paths = self.shell('pm', 'path', FIXTURE_PACKAGE).splitlines()
        require(len(paths) == 1 and paths[0].startswith('package:'), 'Install the separate oracle fixture first')
        raw = self.raw(paths[0][8:], 'installed-oracle-fixture')
        require(raw['sha256'] == FIXTURE_SHA, 'Existing fixture is not frozen oracle v2')
        self.observer_session = Observer8SessionClient(self, OBSERVER, OBSERVER_SHA,
                                                      OBSERVER.with_name('identity.json'), PRODUCER)
        self.observer_session.start()
        self.persist()

    def stable(self):
        require(self.pid() == self.report['frozen_pid'], 'Trime restarted during smoke')
        self.observer_session.alive()

    def launch(self, mode='multiline', text=None):
        require(mode in {'multiline', 'chat', 'search', 'email', 'address', 'phone', 'integer', 'decimal', 'password'},
                'Unsupported actual fixture mode')
        # -S affects the independent fixture only; Trime must retain its PID.
        args = ['am', 'start', '-W', '-S', '-n', FIXTURE_PACKAGE + '/.EditorActivity', '--es', 'mode', mode]
        if text is not None:
            args += ['--es', 'text', text]
        result = self.shell(*args)
        self.report['actions'].append(dict(action='launch_independent_editor', mode=mode, initial_text=text, at=now()))
        self.persist()
        return result

    def tap(self, x, y):
        self.stable()
        require(type(x) is int and type(y) is int and x >= 0 and y >= 0, 'Actual screen coordinates required')
        self.shell('input', 'tap', x, y)
        self.report['actions'].append(dict(action='tap', x=x, y=y, at=now()))
        self.persist()

    def longpress(self, x, y):
        self.stable()
        self.shell('input', 'swipe', x, y, x, y, 750)
        self.report['actions'].append(dict(action='longpress', x=x, y=y, duration_ms=750, at=now()))
        self.persist()

    def oracle(self, label):
        nonce = secrets.token_hex(16)
        raw = self.shell('content', 'query', '--uri',
            'content://org.x2m7.trime.editorfixture.t10/state?nonce=' + nonce, '--projection', 'snapshot_b64')
        (self.out / (label + '-oracle-raw.txt')).write_text(raw + '\n')
        found = re.findall(r'snapshot_b64=([A-Za-z0-9+/=]+)', raw)
        require(len(found) == 1, 'One original oracle row required')
        value = json.loads(base64.b64decode(found[0], validate=True).decode('utf-8'))
        require(value['request_nonce'] == nonce and value['schema'] == 'editorfixture-state-v1'
                and value['main_thread_read'] is True and value['read_only'] is True
                and value['editor_focused'] is True and value['has_window_focus'] is True,
                'Actual focused editor oracle identity differs')
        text_raw = base64.b64decode(value['text_utf8_base64'], validate=True)
        text = text_raw.decode('utf-8')
        require(len(text_raw) == value['text_utf8_bytes'], 'Oracle byte length differs')
        length = len(text.encode('utf-16-le')) // 2
        require(0 <= value['selection_start'] <= length and 0 <= value['selection_end'] <= length,
                'Oracle UTF16 selection outside actual text')
        save(self.out / (label + '-oracle.json'), value)
        return text, value

    def capture(self, label, *, expected=None, oracle=True):
        require(re.fullmatch(r'[a-zA-Z0-9_-]{1,90}', label), 'Fresh simple capture label required')
        self.stable()
        folder = self.out / 'captures' / label
        folder.mkdir(exist_ok=False)
        before = self.oracle(label + '-before') if oracle else None
        metadata, files, row = self.observer_session.capture(folder)
        require(metadata.get('capture_complete') is True, 'Incomplete capture retained; no acceptance')
        after = self.oracle(label + '-after') if oracle else None
        if oracle:
            require(before[0] == after[0] and before[1]['fixture_pid'] == after[1]['fixture_pid']
                    and before[1]['activity_instance'] == after[1]['activity_instance']
                    and after[1]['sequence'] > before[1]['sequence'], 'Editor changed during capture')
            if expected is not None:
                require(after[0] == expected, 'Actual editor text differs: ' + repr(after[0]))
        self.stable()
        self.last_xml = ET.fromstring((folder / 'observer-after.xml').read_bytes())
        self.report['captures'].append(dict(label=label, folder=str(folder), text=after[0] if after else None,
            xml_before_after_equal=(folder / 'observer-before.xml').read_bytes() == (folder / 'observer-after.xml').read_bytes(),
            image_reviewed=False, accepted_business_case=False))
        self.persist()
        return dict(folder=str(folder), text=after[0] if after else None)

    def label(self, text):
        require(self.last_xml is not None, 'Capture the actual UI first')
        nodes = [n for n in self.last_xml.iter('node') if n.get('text') == text
                 and n.get('clickable') == 'true' and n.get('enabled') == 'true']
        require(len(nodes) == 1, 'Exact enabled clickable label is ambiguous or missing')
        left, top, right, bottom = clipped_box(self.last_xml, nodes[0])
        require(left < right and top < bottom, 'Node rectangle empty')
        self.tap((left + right) // 2, (top + bottom) // 2)

    def finish(self):
        self.stable()
        self.observer_session.stop()
        self.report['completed'] = True
        self.report['finished'] = now()
        self.persist()
        return 'Observer stop/wait passed; business cases still require actual evidence review.'

    def cleanup(self):
        if self.observer_session is not None:
            self.observer_session.cleanup()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', type=Path, default=Path('/home/xumin/Public/android-toolchain/android-sdk/platform-tools/adb'))
    parser.add_argument('--binding', type=Path, required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--api', type=int, choices=[21, 35], required=True)
    parser.add_argument('--output', type=Path, required=True)
    options = parser.parse_args()
    S = Smoke(options)
    print('Prepared S. No ADB has run; wait for Root handoff before S.start()/other methods.', flush=True)
