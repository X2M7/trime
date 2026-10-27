#!/usr/bin/env python3
"""Small interactive adapter around the already frozen Observer9 and ADB journal.

Preparation/import/--help performs no ADB operations. After the new exact cloud APK is frozen and this operator owns ADB:
  PYTHON_BASIC_REPL=1 python3 -i t9_smoke_helper_v7.py --binding FROZEN_BINDING_JSON --serial emulator-5554 --api 21 --output NEW_DIR
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


EXPECTED_SOURCE_HEAD = '8a1ee0ea6538e220fcddf31ed6fd491f5bc3cca7'
EXPECTED_BUILD_COMMIT = '0ef0eb551a6f8d000266d4446348b2d2ab2823e9'
EXPECTED_SOURCE_TREE = 'b022a1e810e231af9f9776c2b5414faf6b5bd615'
EXPECTED_BASE = 'a047f74757fae0d7b5ef64e54f0b957dee2bf8f3'
EXPECTED_SOURCE_PARENT = 'aa33c0bdfd326fe82baf7db5c80528885c57a000'
EXPECTED_RUN = 40527129836
EXPECTED_JOB = 122272383005


def _producer_need(condition, message):
    if not condition:
        raise AssertionError(message)


def _producer_digest(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def _producer_json(path):
    path = Path(path)
    _producer_need(path.is_file() and 0 < path.stat().st_size <= 8 * 1024 * 1024,
                   'Producer JSON is missing or exceeds the original bound')
    return json.loads(path.read_bytes())


def _producer_reference(reference, label, *, json_value=True):
    _producer_need(isinstance(reference, dict) and isinstance(reference.get('path'), str)
                   and isinstance(reference.get('sha256'), str)
                   and re.fullmatch(r'[0-9a-f]{64}', reference['sha256']),
                   'Original producer reference is incomplete: ' + label)
    path = Path(reference['path'])
    _producer_need(path.is_file() and _producer_digest(path) == reference['sha256'],
                   'Original producer reference bytes changed: ' + label)
    return _producer_json(path) if json_value else path


def validate_producer_binding(binding):
    """Read-only producer proof, never an APK equality or real-device claim.

    The PR source and its synthetic merge have distinct commit IDs. Their entire
    source Git trees match, but build-time Git metadata can still differ. Raw
    commit/PR/run/job/checkout/artifact receipts must independently identify this
    exact Ubuntu APK producer. This function runs no GUI, ADB or subprocess.
    """
    _producer_need(binding.get('state') == 'FROZEN_ORIGINAL_CLOUD_APK',
                   'Source/APK binding is PENDING; no device operation is allowed')
    _producer_need(binding.get('source_head') == EXPECTED_SOURCE_HEAD
                   and binding.get('actual_build_commit') == EXPECTED_BUILD_COMMIT
                   and binding.get('source_tree') == EXPECTED_SOURCE_TREE
                   and binding.get('actual_build_tree') == EXPECTED_SOURCE_TREE
                   and binding.get('base') == EXPECTED_BASE,
                   'Exact PR source, merge producer or whole-tree binding differs')
    _producer_need(binding.get('repository') == 'osfans/trime'
                   and binding.get('source_repository') == 'X2M7/trime'
                   and type(binding.get('run')) is int and binding['run'] == EXPECTED_RUN
                   and type(binding.get('run_attempt')) is int and binding['run_attempt'] == 1
                   and type(binding.get('producer_job_id')) is int
                   and binding['producer_job_id'] == EXPECTED_JOB
                   and type(binding.get('artifact')) is int and binding['artifact'] > 0,
                   'Exact upstream run/job/artifact producer binding differs')
    _producer_need(binding.get('package') == 'com.osfans.trime.debug'
                   and type(binding.get('version_code')) is int and binding['version_code'] > 0
                   and isinstance(binding.get('version_name'), str) and binding['version_name']
                   and isinstance(binding.get('original_apk_signer_sha256'), str)
                   and re.fullmatch(r'[0-9a-f]{64}', binding['original_apk_signer_sha256']),
                   'Original Debug APK package/version/signer binding is incomplete')
    proof_reference = dict(path=binding.get('producer_tree_equivalence_proof_path'),
                           sha256=binding.get('producer_tree_equivalence_proof_sha256'))
    proof = _producer_reference(proof_reference, 'tree equivalence proof')
    _producer_need(proof.get('schema') == 'trime-pr-merge-producer-proof/v1'
                   and proof.get('state') == 'FROZEN_ORIGINAL_CLOUD_ARTIFACT'
                   and proof.get('source_head') == EXPECTED_SOURCE_HEAD
                   and proof.get('actual_build_commit') == EXPECTED_BUILD_COMMIT
                   and proof.get('source_tree') == EXPECTED_SOURCE_TREE
                   and proof.get('actual_build_tree') == EXPECTED_SOURCE_TREE
                   and proof.get('merge_parents') == [EXPECTED_BASE, EXPECTED_SOURCE_HEAD],
                   'Frozen proof does not identify the exact two distinct commits/trees/parents')
    producer = dict(repository='osfans/trime', run_id=EXPECTED_RUN, run_attempt=1,
                    job_id=EXPECTED_JOB, artifact_id=binding['artifact'])
    _producer_need(proof.get('producer') == producer, 'Proof producer differs from binding')
    refs = proof.get('references')
    required = ('pull_request', 'source_commit', 'merge_commit', 'workflow_run',
                'producer_job', 'checkout_log', 'artifact_metadata', 'artifact_audit')
    _producer_need(isinstance(refs, dict) and set(refs) == set(required),
                   'All original producer references are required')
    values = {name: _producer_reference(refs[name], name, json_value=name != 'checkout_log')
              for name in required}
    pr, source, merge = values['pull_request'], values['source_commit'], values['merge_commit']
    _producer_need(pr.get('url') == 'https://api.github.com/repos/osfans/trime/pulls/2183'
                   and pr.get('number') == 2183 and pr.get('head', {}).get('sha') == EXPECTED_SOURCE_HEAD
                   and pr.get('head', {}).get('ref') == 'contrib/pr-t9-input'
                   and pr.get('head', {}).get('repo', {}).get('full_name') == 'X2M7/trime'
                   and pr.get('base', {}).get('sha') == EXPECTED_BASE
                   and pr.get('base', {}).get('ref') == 'develop'
                   and pr.get('base', {}).get('repo', {}).get('full_name') == 'osfans/trime'
                   and pr.get('merge_commit_sha') == EXPECTED_BUILD_COMMIT,
                   'Original PR snapshot does not match this producer merge')
    for label, commit, expected in [('source_commit', source, EXPECTED_SOURCE_HEAD),
                                    ('merge_commit', merge, EXPECTED_BUILD_COMMIT)]:
        allowed_urls = {'https://api.github.com/repos/osfans/trime/git/commits/' + expected}
        if label == 'source_commit':
            allowed_urls.add('https://api.github.com/repos/X2M7/trime/git/commits/' + expected)
        _producer_need(commit.get('sha') == expected
                       and commit.get('tree', {}).get('sha') == EXPECTED_SOURCE_TREE
                       and commit.get('url') in allowed_urls,
                       'Original Git commit/tree identity differs: ' + label)
    _producer_need([p.get('sha') for p in source.get('parents', [])] == [EXPECTED_SOURCE_PARENT]
                   and [p.get('sha') for p in merge.get('parents', [])]
                   == [EXPECTED_BASE, EXPECTED_SOURCE_HEAD],
                   'Original Git commit parent identities differ')
    run, job = values['workflow_run'], values['producer_job']
    _producer_need(run.get('id') == EXPECTED_RUN and run.get('run_attempt') == 1
                   and run.get('head_sha') == EXPECTED_SOURCE_HEAD and run.get('event') == 'pull_request'
                   and run.get('repository', {}).get('full_name') == 'osfans/trime'
                   and run.get('head_repository', {}).get('full_name') == 'X2M7/trime'
                   and run.get('path') == '.github/workflows/pull-request-ci.yml'
                   and run.get('url') == 'https://api.github.com/repos/osfans/trime/actions/runs/' + str(EXPECTED_RUN),
                   'Original workflow run identity differs')
    _producer_need(job.get('id') == EXPECTED_JOB and job.get('run_id') == EXPECTED_RUN
                   and job.get('run_attempt') == 1 and job.get('head_sha') == EXPECTED_SOURCE_HEAD
                   and job.get('name') == 'build (ubuntu-26.04)'
                   and job.get('status') == 'completed' and job.get('conclusion') == 'success'
                   and job.get('url') == 'https://api.github.com/repos/osfans/trime/actions/jobs/' + str(EXPECTED_JOB)
                   and job.get('run_url') == run['url'],
                   'Original Ubuntu producer job is not this successful build')
    log_path = values['checkout_log']
    _producer_need(log_path.stat().st_size <= 256 * 1024 * 1024,
                   'Original checkout log exceeds its bound')
    log_lines = log_path.read_text(encoding='utf-8').splitlines()
    checkouts = []
    for index, line in enumerate(log_lines[:-1]):
        if re.search(r'\[command\].*\bgit log -1 --format=%H\s*$', line):
            found = re.search(r'\b([0-9a-f]{40})\s*$', log_lines[index + 1])
            if found:
                checkouts.append(found[1])
    _producer_need(checkouts == [EXPECTED_BUILD_COMMIT],
                   'Original Ubuntu checkout log does not prove the exact merge producer')
    artifact = values['artifact_metadata']
    if 'artifacts' in artifact:
        matches = [item for item in artifact['artifacts'] if item.get('id') == binding['artifact']]
        _producer_need(len(matches) == 1, 'One original matching artifact API record required')
        artifact = matches[0]
    _producer_need(artifact.get('id') == binding['artifact'] and artifact.get('expired') is False
                   and artifact.get('name') == 'trime-ubuntu-26.04'
                   and artifact.get('workflow_run', {}).get('id') == EXPECTED_RUN
                   and artifact.get('workflow_run', {}).get('head_sha') == EXPECTED_SOURCE_HEAD,
                   'Original artifact API does not belong to this Ubuntu producer')
    audit = values['artifact_audit']
    _producer_need(Path(refs['artifact_audit']['path']).resolve()
                   == Path(binding['original_artifact_audit_path']).resolve()
                   and refs['artifact_audit']['sha256'] == binding['original_artifact_audit_sha256']
                   and audit.get('schema') == 'trime-original-cloud-apk-audit/v1'
                   and audit.get('source_head') == EXPECTED_SOURCE_HEAD
                   and audit.get('actual_build_commit') == EXPECTED_BUILD_COMMIT
                   and audit.get('source_tree') == EXPECTED_SOURCE_TREE
                   and audit.get('actual_build_tree') == EXPECTED_SOURCE_TREE
                   and audit.get('producer') == producer and audit.get('package') == binding['package'],
                   'Original APK audit is not bound to these exact source/producer identities')
    archive = audit.get('archive')
    _producer_reference(archive, 'original artifact archive', json_value=False)
    _producer_need(archive['sha256'] == binding['original_artifact_archive_sha256'],
                   'Original artifact archive SHA differs')
    if artifact.get('digest') is not None:
        _producer_need(artifact['digest'] == 'sha256:' + archive['sha256'],
                       'Original GitHub artifact digest differs from archive bytes')
    apk = audit.get('apks', {}).get('x86_64', {})
    _producer_need(Path(apk.get('path', '')).resolve() == Path(binding['original_apk_path']).resolve()
                   and apk.get('sha256') == binding['original_apk_sha256']
                   and apk.get('signer_sha256') == binding['original_apk_signer_sha256']
                   and apk.get('version_code') == binding['version_code']
                   and apk.get('version_name') == binding['version_name']
                   and apk.get('min_sdk') == 21 and apk.get('debuggable') is True
                   and apk.get('native_abis') == ['x86_64']
                   and type(apk.get('signature_warning_count')) is int
                   and isinstance(binding.get('original_signature_warnings_retained'), list)
                   and apk['signature_warning_count'] == len(binding['original_signature_warnings_retained']),
                   'Original x86_64 APK manifest/signature/ABI audit differs from binding')
    _producer_reference(dict(path=apk['path'], sha256=apk['sha256']),
                        'original x86_64 APK', json_value=False)
    audit_refs = audit.get('references')
    _producer_need(isinstance(audit_refs, dict)
                   and {'manifest', 'signer', 'elf'}.issubset(audit_refs),
                   'Original manifest/signer/ELF audit references are required')
    for label, reference in audit_refs.items():
        _producer_reference(reference, 'APK audit ' + label, json_value=False)
    manifest = Path(audit_refs['manifest']['path']).read_text(encoding='utf-8')
    package_line = re.findall(r'^package: (.*)$', manifest, re.M)
    _producer_need(len(package_line) == 1, 'One original manifest package line required')
    package_values = dict(re.findall(r"([A-Za-z]+)='([^']*)'", package_line[0]))
    native_lines = re.findall(r'^native-code: (.*)$', manifest, re.M)
    _producer_need(package_values.get('name') == binding['package']
                   and package_values.get('versionCode') == str(binding['version_code'])
                   and package_values.get('versionName') == binding['version_name']
                   and re.findall(r"^sdkVersion:'([^']+)'$", manifest, re.M) == ['21']
                   and re.search(r'^application-debuggable\s*$', manifest, re.M)
                   and len(native_lines) == 1
                   and re.findall(r"'([^']+)'", native_lines[0]) == ['x86_64'],
                   'Original aapt manifest bytes disagree with package/version/minSDK/Debug/ABI audit')
    signer = Path(audit_refs['signer']['path']).read_text(encoding='utf-8')
    certificates = re.findall(r'^Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]{64})\s*$',
                              signer, re.M)
    warnings = [line for line in signer.splitlines() if line.startswith('WARNING:')]
    _producer_need([value.lower() for value in certificates]
                   == [binding['original_apk_signer_sha256']]
                   and warnings == binding['original_signature_warnings_retained'],
                   'Original signer certificate/warnings disagree with the audited binding')
    elf = Path(audit_refs['elf']['path']).read_text(encoding='utf-8')
    _producer_need(re.findall(r'^\s*Class:\s*(\S+)\s*$', elf, re.M) == ['ELF64']
                   and re.findall(r'^\s*Machine:\s*(.*?)\s*$', elf, re.M)
                   == ['Advanced Micro Devices X86-64'],
                   'Original x86_64 native ELF identity differs')
    return dict(source_head=EXPECTED_SOURCE_HEAD, actual_build_commit=EXPECTED_BUILD_COMMIT,
                source_tree=EXPECTED_SOURCE_TREE, actual_build_tree=EXPECTED_SOURCE_TREE,
                merge_parents=[EXPECTED_BASE, EXPECTED_SOURCE_HEAD], producer=producer,
                proof_path=str(Path(proof_reference['path']).resolve()),
                proof_sha256=proof_reference['sha256'],
                source_tree_equivalence_does_not_equate_git_metadata_or_apk_bytes=True)


class Smoke(Gui):
    def __init__(self, args):
        global SOURCE_HEAD, ORIGINAL_APK, APK_SHA
        binding = json.loads(args.binding.read_bytes())
        require(binding.get('schema') == 'trime-targeted-debug-smoke-binding/v1'
                and binding.get('state') == 'FROZEN_ORIGINAL_CLOUD_APK',
                'New combined source/APK is UNFROZEN; no device operation is allowed')
        require(binding.get('helper_sha256') == sha(PRODUCER), 'Helper differs from frozen binding')
        SOURCE_HEAD = binding.get('source_head')
        producer_verification = validate_producer_binding(binding)
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
            source_producer_verification=producer_verification,
            limits=['Explicit touches only; no automatic image or business acceptance.',
                    'Original upstream PR merge-producer Debug APK; source Git tree equals PR head, while commit/Git build metadata differ. No signing or product code/cache/native modification. Explicit minimal test prefs, if needed, are a separately recorded preparation step before business checks.',
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
        frozen = self.report['source_binding']['value']
        require(int(version[1]) == frozen['version_code'] and name[1] == frozen['version_name'],
                'Actual manifest differs from the new audited original APK')
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

    def doubletap(self, x, y):
        self.stable()
        require(type(x) is int and type(y) is int and x >= 0 and y >= 0, 'Actual coordinates required')
        # One journaled shell operation avoids PID probes between the two real touches.
        # Debug release timestamps and dispatched behavior must prove actual double-tap timing.
        self.shell('sh', '-c', f'input tap {x} {y}; input tap {x} {y}')
        self.report['actions'].append(dict(action='doubletap', x=x, y=y, touch_count=2, at=now()))
        self.persist()
        self.stable()

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
    print('Prepared S. No ADB has run; start only under the current sole device lease.', flush=True)
