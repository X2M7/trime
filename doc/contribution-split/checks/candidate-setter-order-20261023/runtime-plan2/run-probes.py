"""Explicit #2187 class-only sequence; no device action on import or --help.

Requires Root's frozen binding and exclusive device lease. Never selects Trime,
launches its Application/activity/service, writes its prefs, or re-signs an APK.
Each diagnostic is a namespace copy, not the producer's application APK.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import sys
import tarfile

from runtime_common import ROOT, HEAD, SOURCE, frozen, sha

TOOLS = Path('/home/xumin/Public/Trime/build/t12/tools')
sys.path.insert(0, str(TOOLS))
from t12_release_setup_gui_v31 import Gui, now, require, save

TARGET = 'com.osfans.trime.debug'
DIAG = 'org.x2m7.trime.readiness.candidatediagnostic'
PROBE = 'org.x2m7.trime.readiness.candidate'
LATIN = 'com.android.inputmethod.latin/.LatinIME'
IME = TARGET + '/com.osfans.trime.ime.core.TrimeInputMethodService'
STAGES = ['constructor', 'hanzi', 'latin', 'null', 'rebind', 'buffer-normal',
          'buffer-spannable', 'buffer-editable', 'buffer-back-normal',
          'buffer-repeat-normal', 'buffer-repeat-spannable',
          'granularity-selectable-precondition', 'granularity-line-next-previous']


class Journal(Gui):
    def __init__(self, args, binding):
        self.args = args
        self.out = args.output.resolve()
        require(self.out.is_relative_to(ROOT) and not self.out.exists(), 'Fresh PR2187 output only')
        self.out.mkdir(parents=True)
        (self.out / 'commands').mkdir()
        self.command_count = self.capture_count = 0
        self.report = dict(task='PR2187 class-only diagnostic and original-production probe',
            started=now(), api=args.api, serial=args.serial, commands=[], phases=[],
            business_qualified=False, source_head=HEAD, binding=binding,
            binding_path=str(args.binding), binding_sha256=sha(args.binding),
            physical_device='NOT_RUN', TalkBack='NOT_RUN', native_candidate_integration='NOT_RUN')
        self.persist()

    def snapshot(self, label):
        values = {k: self.shell(*argv).strip() for k, argv in {
            'default': ('settings', 'get', 'secure', 'default_input_method'),
            'enabled': ('settings', 'get', 'secure', 'enabled_input_methods'),
            'size': ('wm', 'size'), 'density': ('wm', 'density'),
            'font': ('settings', 'get', 'system', 'font_scale'),
            'boot': ('cat', '/proc/sys/kernel/random/boot_id'), 'id': ('id',)}.items()}
        save(self.out / (label + '.json'), values)
        self.ps(label + '-ps')
        (self.out / (label + '-screen.png')).write_bytes(self.run('exec-out', 'screencap', '-p'))
        return values

    def ps(self, label):
        text = self.shell('ps', *([] if self.args.api == 21 else ['-A']))
        (self.out / (label + '.raw.txt')).write_text(text)
        names = [x.split()[-1] for x in text.splitlines()[1:] if x.split()]
        require(TARGET not in names, 'Unexpected Trime process: preserve precondition, do not qualify')
        return text

    def installed(self, package, label, expected=None):
        dump = self.shell('dumpsys', 'package', package)
        (self.out / (label + '-package.raw.txt')).write_text(dump)
        paths = self.shell('pm', 'path', package, check=False).splitlines()
        command = self.report['commands'][-1]
        remote_status = command['remote_returncode']
        stderr = Path(command['stderr']).read_text().strip()
        require(remote_status in (0, 1), 'Unexpected PackageManager query failure')
        if not paths:
            absence = (not stderr or bool(re.fullmatch(
                r'(?:Error:\s*)?(?:package\s+[\'\"]?' + re.escape(package)
                + r'[\'\"]?\s+(?:was\s+)?not\s+found|Unknown package:\s*'
                + re.escape(package) + r')\.?', stderr, re.I)))
            require(absence, 'Empty path with an unrelated PackageManager error')
            command['expected_package_absence'] = dict(package=package,
                remote_status=remote_status, empty_path=True, stderr=stderr)
            self.persist()
            return None
        require(remote_status == 0, 'PackageManager returned paths together with failure')
        require(not stderr, 'Unexpected PackageManager stderr with an installed path')
        require(len(paths) == 1 and paths[0].startswith('package:'), 'One original APK required')
        original = self.raw(paths[0][8:], label + '-installed-apk')
        if expected:
            require(original['sha256'] == expected, 'Installed APK differs from frozen original')
        return original

    def backup_package(self, package, label):
        """Preserve only isolated test-package data; never restore it into a new APK."""
        installed = self.installed(package, label)
        if installed is None:
            return False
        folder = self.out / (label + '-backup')
        folder.mkdir()
        remote = '/data/data/' + package
        (folder / 'private-ls.raw.txt').write_text(self.shell(*self.root, 'ls', '-la', remote))
        if self.args.api == 21:
            # Avoid the obsolete lib symlink; record the listing and preserve real data paths.
            for name in ('cache', 'code_cache', 'databases', 'files', 'no_backup',
                         'shared_prefs', 'checksums.json'):
                self.shell('test', '-e', remote + '/' + name, check=False)
                if self.report['commands'][-1]['remote_returncode'] == 0:
                    self.run('pull', remote + '/' + name, folder / name, timeout=120)
        else:
            data = self.run('exec-out', *self.root, 'tar', '-cf', '-', '-C', remote, '.', timeout=120)
            (folder / 'private.tar').write_bytes(data)
        external = ('/storage/sdcard' if self.args.api == 21 else '/storage/emulated/0')
        external += '/Android/data/' + package
        self.shell(*self.root, 'test', '-e', external, check=False)
        if self.report['commands'][-1]['remote_returncode'] == 0:
            if self.args.api == 21:
                self.run('pull', external, folder / 'external', timeout=120)
            else:
                data = self.run('exec-out', *self.root, 'tar', '-cf', '-', '-C', external, '.', timeout=120)
                (folder / 'external.tar').write_bytes(data)
        save(folder / 'manifest.json', dict(installed=installed, restored=False, files=[
            dict(path=str(p.relative_to(folder)), bytes=p.stat().st_size, sha256=sha(p))
            for p in sorted(folder.rglob('*')) if p.is_file()]))
        return True

    def replace(self, package, apk, expected, label):
        if self.backup_package(package, label + '-previous'):
            self.run('uninstall', package, timeout=90)
        self.run('install', '-t', apk, timeout=120)
        self.installed(package, label, expected)
        self.ps(label + '-no-Trime-process')

    def phase(self, name, package, expected_apk, variant=None):
        folder = self.out / name
        folder.mkdir()
        self.ps(name + '-before')
        args = ['am', 'instrument', '-w', '-r']
        if variant:
            args += ['-e', 'variant', variant]
        args += [package + '/.CandidateProbe']
        record = dict(name=name, package=package, variant=variant,
            notProductionApk=bool(variant), started=now(), independently_qualified=False)
        self.report['phases'].append(record)
        self.persist()
        try:
            output = self.shell(*args, timeout=180)
            (folder / 'instrumentation.stdout.txt').write_text(output)
            codes = re.findall(r'^INSTRUMENTATION_CODE:\s*(-?\d+)\s*$', output, re.M)
            directories = re.findall(r'^INSTRUMENTATION_RESULT: directory=(\S+)\s*$', output, re.M)
            short_messages = re.findall(r'^INSTRUMENTATION_RESULT: shortMsg=(.+)$', output, re.M)
            if (variant in ('equal_return', 'no_super_measure') and codes == ['0']
                    and len(short_messages) == 1 and not directories
                    and not re.search(r'^INSTRUMENTATION_RESULT:\s*directory=', output, re.M)):
                record.update(state='CONTROL_ABORTED_UNQUALIFIED', instrumentation_code=0,
                    instrumentation_short_message=short_messages[0],
                    instrumentation_command=dict(self.report['commands'][-1]),
                    instrumentation_output=dict(path=str(folder / 'instrumentation.stdout.txt'),
                        sha256=sha(folder / 'instrumentation.stdout.txt')),
                    actual_stage_count='NOT_EXPORTED', original_png_count='NOT_EXPORTED',
                    result_export='NOT_EXPORTED', finished=now(),
                    negative_control_semantics='Actual instrumentation abort without an exported directory; '
                        'no stage/BufferType/reference verdict or qualified negative-control result.')
                save(folder / 'phase-record.json', record)
                return
            require(len(codes) == len(directories) == 1, 'Actual instrumentation status/directory missing')
            remote = directories[0]
            require(re.fullmatch(r'/data/(?:user/0|data)/' + re.escape(package)
                + r'/cache/candidate-layout-[0-9a-f-]{36}', remote), 'Unexpected diagnostic output path')
            run_id = remote.rsplit('/', 1)[1]
            if self.args.api == 21:
                self.run('pull', remote, folder / 'original', timeout=120)
                original = folder / 'original'
            else:
                raw = self.run('exec-out', 'run-as', package, 'tar', '-cf', '-', '-C', 'cache', run_id,
                               timeout=120)
                (folder / 'original-export.tar').write_bytes(raw)
                with tarfile.open(fileobj=io.BytesIO(raw)) as archive:
                    for member in archive.getmembers():
                        p = Path(member.name)
                        require(not p.is_absolute() and '..' not in p.parts and p.parts[0] == run_id
                                and (member.isfile() or member.isdir()), 'Unsafe export entry')
                    archive.extractall(folder / 'exported', filter='data')
                original = folder / 'exported' / run_id
            result = json.loads((original / 'result.json').read_bytes())
            require(result['sourceApkSha256'] == expected_apk, 'Probe source APK identity differs')
            require(result['sdk'] == self.args.api and result['expectedStageCount'] == 13,
                    'Probe API/oracle differs')
            if variant:
                require(result['notProductionApk'] is True and result['diagnosticVariant'] == variant
                    and result['originalProductionSourceSha256'] == SOURCE,
                    'Namespace diagnostic misidentified as production')
                require(result['diagnosticClassSourceSha256'] ==
                    self.report['binding']['diagnostic_variants'][variant]['sourceSha256'],
                    'Wrong namespace diagnostic class')
            stages = result['stages']
            names = [s['name'] for s in stages]
            require(names == STAGES[:len(names)] and len(names) <= 13, 'Changed/duplicate stage ordering')
            for stage in stages:
                for suffix, digest in [('screen', stage['screenSha256']),
                        ('production', stage['production']['softwareSha256']),
                        ('reference', stage['reference']['softwareSha256'])]:
                    image = original / (stage['name'] + '-' + suffix + '.png')
                    require(image.read_bytes().startswith(b'\x89PNG\r\n\x1a\n') and sha(image) == digest,
                            'Original PNG bytes differ')
            files = [dict(path=str(p.relative_to(original)), bytes=p.stat().st_size, sha256=sha(p))
                     for p in sorted(original.rglob('*')) if p.is_file()]
            pngs = [x for x in files if x['path'].endswith('.png')]
            # Instrumentation may already have exited. Do not invent a starttime
            # from its self-reported PID when no actual process remains.
            ps = self.ps(name + '-probe-identity')
            rows = [x.split() for x in ps.splitlines()[1:] if x.split()]
            columns = ps.splitlines()[0].split()
            pid = int(result['pid'])
            live = [x for x in rows if x[-1] == package and int(x[columns.index('PID')]) == pid]
            identity = dict(self_reported_pid=pid, os_pid_start='NOT_OBSERVED')
            if len(live) == 1:
                stat = self.shell(*self.root, 'cat', '/proc/' + str(pid) + '/stat', check=False)
                stat_status = self.report['commands'][-1]['remote_returncode']
                cmdline = self.run('exec-out', *self.root, 'cat', '/proc/' + str(pid) + '/cmdline', check=False)
                if stat_status == 0 and self.report['commands'][-1]['returncode'] == 0 and cmdline.rstrip(b'\0') == package.encode():
                    identity.update(os_pid_start='OBSERVED_AFTER_INSTRUMENTATION', pid=pid,
                        starttime=stat.rsplit(') ', 1)[1].split()[19], cmdline_exact=True)
            record.update(instrumentation_code=int(codes[0]), reported_passed=result['passed'],
                actual_stage_count=len(stages), original_png_count=len(pngs), original_folder=str(original),
                result_sha256=sha(original / 'result.json'), files=files, finished=now(),
                actual_probe_process_identity=identity,
                process_identity_is_not_inferred_from_hardcoded_native_false=True,
                negative_control_semantics='Not automatically qualified: equal_return needs actual type failure '
                    'with same-stage reference pass; no_super_measure 13/13 means not distinguished.')
            require(int(codes[0]) == (-1 if result['passed'] else 0), 'Reported/instrumentation result mismatch')
            if variant in (None, 'safe'):
                require(result['passed'] and names == STAGES and len(pngs) == 39,
                        'Positive source did not complete all original 13 oracles/39 PNGs')
            save(folder / 'phase-record.json', record)
        except BaseException as error:
            record['failure'] = dict(type=type(error).__name__, message=str(error))
            save(folder / 'phase-record.json', record)
            raise
        finally:
            self.persist()
            self.shell('am', 'force-stop', package)
            self.ps(name + '-after')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--binding', type=Path, required=True)
    parser.add_argument('--api', type=int, choices=(21, 35), required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--adb', type=Path, default=Path('/home/xumin/Public/android-toolchain/android-sdk/platform-tools/adb'))
    args = parser.parse_args()
    args.serial = 'emulator-5582' if args.api == 21 else 'emulator-5584'
    binding = frozen(args.binding)
    journal = Journal(args, binding)
    baseline = None
    try:
        baseline = journal.snapshot('baseline')
        require(baseline['default'] == LATIN, 'Latin baseline required before class-only testing')
        journal.root = [] if baseline['id'].startswith('uid=0(') else ['su', '0']
        journal.shell(*journal.root, 'id')
        if IME in baseline['enabled'].split(':'):
            journal.shell('ime', 'disable', IME)
        journal.replace(TARGET, binding['original_apk_path'], binding['original_apk_sha256'], 'production-original')
        journal.replace(PROBE, binding['production_probe_path'], binding['production_probe_sha256'], 'production-probe')
        journal.phase('production-class-only', PROBE, binding['original_apk_sha256'])
        journal.report['operator_sequence_completed'] = True
    except BaseException as error:
        journal.report['operator_failure'] = dict(type=type(error).__name__, message=str(error))
        raise
    finally:
        cleanup_errors = []
        for package in (PROBE,):
            try:
                journal.shell('am', 'force-stop', package)
            except BaseException as error:
                cleanup_errors.append(str(error))
        if baseline is not None:
            try:
                journal.shell('ime', 'set', baseline['default'])
                journal.shell('settings', 'put', 'secure', 'enabled_input_methods', baseline['enabled'])
                final = journal.snapshot('restored')
                require(all(final[k] == baseline[k] for k in ('default', 'enabled', 'size', 'density', 'font', 'boot')),
                        'Actual baseline/display/font restoration differs')
                journal.report['restored_exactly'] = True
            except BaseException as error:
                cleanup_errors.append(str(error))
        journal.report['cleanup_errors'] = cleanup_errors
        journal.report['finished'] = now()
        journal.persist()
        if cleanup_errors:
            raise RuntimeError('Cleanup failed; raw evidence retained: ' + repr(cleanup_errors))


if __name__ == '__main__':
    main()
