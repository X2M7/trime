"""PR2187 explicit emulator DocsUI fixture around the unchanged class-only driver.

No device action on import/--help; requires Root-frozen identity and device lease.
Only the authenticated BEGIN..END child interval can support class-only claims.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import secrets
import subprocess
import sys

from runtime_common import ROOT, frozen, sha

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('pr2187_unchanged_child', HERE / 'run-probes.py')
child_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(child_module)
Gui, require, save, now = (child_module.Gui, child_module.require, child_module.save, child_module.now)
DOCS_PACKAGES = ('com.android.documentsui', 'com.google.android.documentsui')
TARGET = child_module.TARGET


class Fixture(Gui):
    def __init__(self, args, binding):
        self.args = args
        self.out = args.output.resolve()
        require(self.out.is_relative_to(ROOT) and not self.out.exists(), 'Fresh fixture output required')
        require(args.child_output.resolve().is_relative_to(ROOT) and not args.child_output.exists(),
                'Fresh child output required')
        require(self.out != args.child_output.resolve(), 'Separate fixture and child journals required')
        self.out.mkdir(parents=True)
        (self.out / 'commands').mkdir()
        self.command_count = self.capture_count = 0
        self.report = dict(task='PR2187 DocsUI fixture bookends', started=now(), commands=[],
            binding_path=str(args.binding), binding_sha256=sha(args.binding), source_head=binding['source_head'],
            fixture_qualified=False, class_only_qualified=False,
            scope='Only BEGIN..END plus child actual baseline/phase ps; not whole-VM absence of Trime Application',
            original_child_driver_sha256=sha(HERE / 'run-probes.py'))
        self.persist()

    def package(self, package, label):
        require(package in DOCS_PACKAGES, 'Only emulator DocumentsUI allowlist may be selected')
        paths = self.shell('pm', 'path', package, check=False).splitlines()
        path_command = dict(self.report['commands'][-1])
        status = path_command['remote_returncode']
        stderr = Path(path_command['stderr']).read_text().strip()
        require(status in (0, 1), 'Unexpected package query exit')
        dump = self.shell('dumpsys', 'package', package)
        (self.out / (label + '-package.raw.txt')).write_text(dump)
        users = re.findall(r'^\s*User 0:\s*(.*)$', dump, re.M)
        if not paths:
            known_absent = (not stderr or bool(re.fullmatch(
                r'(?:Error:\s*)?(?:package\s+[\'\"]?' + re.escape(package)
                + r'[\'\"]?\s+(?:was\s+)?not\s+found|Unknown package:\s*'
                + re.escape(package) + r')\.?', stderr, re.I)))
            require(known_absent and not users, 'Ambiguous or unrelated package absence')
            result = dict(package=package, present=False, pm_path_command=path_command)
        else:
            require(status == 0 and not stderr and len(paths) == 1 and paths[0].startswith('package:'),
                    'Ambiguous package identity')
            require(len(users) == 1, 'Unique User 0 package state required')
            fields = dict(re.findall(r'\b(installed|stopped|enabled)=([^\s]+)', users[0]))
            require(fields.get('installed') == 'true' and fields.get('stopped') in ('true', 'false')
                    and re.fullmatch(r'[0-4]', fields.get('enabled', '')), 'Unreadable User 0 state')
            result = dict(package=package, present=True, apk_path=paths[0][8:],
                enabled=int(fields['enabled']), stopped=fields['stopped'] == 'true',
                user0_raw=users[0], pm_path_command=path_command)
        save(self.out / (label + '-state.json'), result)
        return result

    def ps(self, label):
        text = self.shell('ps', *([] if self.args.api == 21 else ['-A']))
        path = self.out / (label + '-ps.raw.txt')
        path.write_text(text)
        rows = [row for row in text.splitlines()[1:] if row.split() and row.split()[-1] == TARGET]
        value = dict(path=str(path), sha256=sha(path), target_rows=rows,
                     does_not_imply_no_historical_Trime_start=True)
        self.report.setdefault('process_snapshots', []).append(dict(label=label, **value))
        self.persist()
        return value

    def restore(self, before, help_text):
        package = before['package']
        command = 'default-state' if before['enabled'] == 0 else 'enable'
        self.shell('pm', command, '--user', '0', package)
        actual = self.package(package, 'restore-enabled')
        if actual['stopped'] != before['stopped']:
            require(bool(re.search(r'\bset-stopped-state\b', help_text)),
                    'Stopped state changed and no supported public restoration command exists')
            self.shell('pm', 'set-stopped-state', '--user', '0', package,
                       'true' if before['stopped'] else 'false')
            actual = self.package(package, 'restore-stopped')
        require(actual['enabled'] == before['enabled'] and actual['stopped'] == before['stopped'],
                'DocumentsUI enabled/stopped state was not restored exactly')
        self.report['documents_restored_exactly'] = actual
        self.persist()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--binding', type=Path, required=True)
    parser.add_argument('--api', type=int, choices=(21, 35), required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--child-output', type=Path, required=True)
    parser.add_argument('--adb', type=Path, default=Path('/home/xumin/Public/android-toolchain/android-sdk/platform-tools/adb'))
    args = parser.parse_args()
    args.serial = 'emulator-5582' if args.api == 21 else 'emulator-5584'
    binding = frozen(args.binding)
    fixture = Fixture(args, binding)
    before = None
    mutation_attempted = False
    help_text = ''
    child = None
    nonce = None
    returncode = 1
    cleanup_errors = []
    try:
        fixture.ps('before-fixture')
        candidates = [fixture.package(package, 'before-' + package) for package in DOCS_PACKAGES]
        present = [value for value in candidates if value['present']]
        require(len(present) == 1, 'Exactly one actual DocumentsUI package must exist')
        before = present[0]
        require(before['enabled'] in (0, 1), 'Fixture requires original DEFAULT or ENABLED state')
        help_text = fixture.shell('pm', 'help', check=False)
        help_command = dict(fixture.report['commands'][-1])
        require(help_command['remote_returncode'] in (0, 1), 'PackageManager help query failed')
        help_text += '\n' + Path(help_command['stderr']).read_text()
        require('disable-user' in help_text, 'PackageManager must advertise disable-user before mutation')
        if before['enabled'] == 0:
            require('default-state' in help_text, 'Cannot restore DEFAULT; refuse before mutation')
        fixture.report.update(documents_before=before, help_command=help_command,
                              help_text_sha256=hashlib.sha256(help_text.encode()).hexdigest())
        fixture.persist()
        mutation_attempted = True
        fixture.shell('pm', 'disable-user', '--user', '0', before['package'])
        disabled = fixture.package(before['package'], 'disabled')
        require(disabled['enabled'] == 3, 'Actual DocumentsUI is not disabled-user')
        require(disabled['stopped'] == before['stopped'], 'Disable unexpectedly changed stopped precondition')
        fixture.ps('before-explicit-old-target-stop')
        fixture.shell('am', 'force-stop', TARGET)
        require(not fixture.ps('after-explicit-old-target-stop')['target_rows'],
                'Trime remained active before the class-only interval')
        nonce = secrets.token_hex(16)
        fixture.shell('log', '-p', 'i', '-t', 'PR2187_CLASS_ONLY', 'BEGIN ' + nonce)
        fixture.report.update(begin_nonce=nonce, begin_at=now(), begin_command=dict(fixture.report['commands'][-1]))
        fixture.persist()
        argv = [sys.executable, str(HERE / 'run-probes.py'), '--binding', str(args.binding),
                '--api', str(args.api), '--output', str(args.child_output), '--adb', str(args.adb)]
        fixture.report['child_argv'] = argv
        fixture.persist()
        with (fixture.out / 'child.stdout').open('wb') as stdout, (fixture.out / 'child.stderr').open('wb') as stderr:
            child = subprocess.Popen(argv, stdout=stdout, stderr=stderr)
            fixture.report['child_host_pid'] = child.pid
            fixture.persist()
            returncode = child.wait()
        fixture.report['child_actual_exit'] = returncode
        if returncode != 0:
            fixture.report['child_failure_preserved'] = True
    except BaseException as error:
        fixture.report['fixture_failure'] = dict(type=type(error).__name__, message=str(error))
        returncode = 1
    finally:
        if child is not None:
            try:
                if child.poll() is None:
                    fixture.report['child_cleanup_terminated'] = True
                    child.terminate()
                    try:
                        child.wait(timeout=30)
                    except subprocess.TimeoutExpired:
                        fixture.report['child_cleanup_kill_requested'] = True
                        child.kill()
                        child.wait(timeout=30)
                fixture.report['child_actual_exit'] = child.returncode
                fixture.report['child_streams'] = [dict(path=str(fixture.out / name), sha256=sha(fixture.out / name))
                                                   for name in ('child.stdout', 'child.stderr')]
                if nonce is not None:
                    fixture.shell('log', '-p', 'i', '-t', 'PR2187_CLASS_ONLY', 'END ' + nonce)
                    fixture.report.update(end_at=now(), end_command=dict(fixture.report['commands'][-1]))
            except BaseException as error:
                cleanup_errors.append(dict(stage='child-terminal-and-END', message=str(error)))
        if mutation_attempted and before is not None:
            try:
                fixture.restore(before, help_text)
            except BaseException as error:
                cleanup_errors.append(dict(stage='restore-DocumentsUI', message=str(error)))
        try:
            fixture.ps('after-fixture-restoration')
        except BaseException as error:
            cleanup_errors.append(dict(stage='final-actual-ps', message=str(error)))
        fixture.report.update(cleanup_errors=cleanup_errors, finished=now(),
            fixture_operator_completed=returncode == 0 and not cleanup_errors,
            actual_exit=returncode if not cleanup_errors else 1)
        fixture.persist()
    return fixture.report['actual_exit']


if __name__ == '__main__':
    raise SystemExit(main())
