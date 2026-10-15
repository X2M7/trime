#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Stage only tested stable releases: two APKs and complete recursive source.

Qualification binds actual build, Lint, test and upgrade results. Reports remain
local or in source documentation, never extra Release attachments.
"""
import argparse
import json
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET

from release import (ROOT, binding, git, metadata, pages, repositories, require,
                     source_archive, verify_binding, write_json)


def read_bound(row):
    verify_binding(row)
    return json.loads(Path(row['path']).read_text())


def validate_apk_history(history, meta):
    """Validate the original per-APK audit without rewriting its signer arrays."""
    require(isinstance(history, list) and history and
            all(isinstance(row, dict) and row.get('passed') is True for row in history),
            'Published APK identity audit incomplete')
    for row in history:
        certificates = row.get('signer_certificate_sha256')
        require(isinstance(certificates, list) and len(certificates) == 1 and
                isinstance(certificates[0], str) and re.fullmatch(r'[0-9a-f]{64}', certificates[0]),
                'Published APK must have exactly one audited signing certificate')
        require(type(row.get('version_code')) is int and 0 < row['version_code'] <= 2100000000,
                'Published APK versionCode is invalid')
    same = [row for row in history if row.get('package_name') == meta['applicationId']]
    require(same, 'No audited APK has the release applicationId')
    require(max(row['version_code'] for row in same) == meta['previousVersionCode'],
            'Published maximum versionCode differs')
    require(all(row['signer_certificate_sha256'] == [meta['certificateSha256']] for row in same),
            'Published signing certificate differs')


def qualification(path):
    q = json.loads(path.read_text())
    require(q.get('format_version') == 1 and q.get('task') == 'T14', 'Unknown qualification format')
    record = read_bound(q['build_record'])
    meta = metadata()
    require(not re.search(r'(?:^|[.-])(?:alpha|beta|dev|rc)(?:[.-]|[0-9]|$)', meta['versionName'], re.I),
            'Experimental versions must be prereleases, never this stable promotion path')
    code = record['build_git_sha']
    require(record['inspection_passed'] and record['metadata'] == meta, 'Release metadata differs from inspected APKs')
    require(len(record['apks']) == 2 and {a['abi'] for a in record['apks']} == set(meta['abis']), 'Two inspected ABI artifacts required')
    for a in record['apks']:
        verify_binding(a['apk'])
        require(a['debuggable'] is False and a['version_code'] == meta['versionCode'] and
                a['package'] == meta['applicationId'] and a['certificate_sha256'] == meta['certificateSha256'], 'APK identity differs')
    require(len(q['build_results']) == 3, 'Exactly three build phases required')
    phases = {row['phase']: row for row in (read_bound(b) for b in q['build_results'])}
    require(set(phases) == {'debug', 'release', 'lint'}, 'Debug, Release and independent Lint must all finish')
    for phase in phases.values():
        require(phase['exit_code'] == 0 and phase['stopped_reason'] is None and phase['source_unchanged'] and
                phase['build_git_sha'] == code and phase['minimum_available_kib'] >= 1024 * 1024 and
                not phase.get('freeze_error'), 'Incomplete or mismatched build')
        for key in ('environment', 'command', 'log'):
            verify_binding(phase[key])
        require(not re.search(r'(?im)^(?:w:|warning:|.*\bWARNING\b)', Path(phase['log']['path']).read_text()), 'Unresolved build warning')
    require(record['build_result'] in q['build_results'], 'Inspected Release differs from qualified build')
    tests = 0
    require(q['unit_tests'] == phases['debug']['unit_tests'] and q['lint'] == phases['lint']['lint'],
            'Test/Lint XML must be the actual frozen build results')
    require(phases['debug']['build_logic_tests'], 'Build-logic test results missing')
    for row in phases['debug']['build_logic_tests']:
        verify_binding(row)
        suite = ET.parse(row['path']).getroot()
        require(suite.tag == 'testsuite' and all(int(suite.get(k, '0')) == 0 for k in ('errors', 'failures', 'skipped')),
                'Build-logic tests failed or skipped')
    require(q['unit_tests'], 'No JVM test results')
    for row in q['unit_tests']:
        verify_binding(row)
        suite = ET.parse(row['path']).getroot()
        require(suite.tag == 'testsuite' and all(int(suite.get(k, '0')) == 0 for k in ('errors', 'failures', 'skipped')), 'JVM test did not pass')
        tests += int(suite.get('tests', '0'))
    require(tests > 0, 'No JVM tests ran')
    require(len(q['lint']) == 2, 'Both Lint variants required')
    for row in q['lint']:
        verify_binding(row)
        require(not ET.parse(row['path']).getroot().findall('issue'), 'Lint issues remain')
    host = read_bound(q['host_tests'])
    require(host['exit_code'] == 0 and host['tests'] > 0 and host['source_commit'] == code, 'Host tests incomplete')
    verify_binding(host['log'])
    upgrades = [read_bound(row) for row in q['upgrades']]
    require(len(upgrades) == 2 and {r['api'] for r in upgrades} == {21, 35}, 'Both Android upgrade results required')
    expected = next(a['apk'] for a in record['apks'] if a['abi'] == 'x86_64')
    for r in upgrades:
        require(r['passed'] is True and r['candidate_commit'] == code and r['new_apk'] == expected and
                r.get('stopped_reason') is None, 'Upgrade did not pass with this APK')
    history = read_bound(q['published_history'])
    validate_apk_history(history, meta)
    require(q['limitations'] and isinstance(q['limitations'], list) and all(isinstance(s, str) for s in q['limitations']), 'Validation limits must be stated')
    pages(check=True)
    return q, record


def validate_published_history(releases, manifest):
    q = read_bound(manifest['qualification'])
    history = read_bound(q['published_history'])
    expected = {a['asset_id']: a['remote_digest'] for a in history}
    actual = {a['id']: a['digest'] for r in releases if not r['draft'] for a in r['assets'] if a['name'].endswith('.apk')}
    require(actual == expected, 'Published APK inventory changed since actual identity audit; audit it again')


def source_binding(record):
    current = repositories()
    code, pub = record['build_git_sha'], current['']
    git('merge-base', '--is-ancestor', code, pub)
    changed = git('diff', '--name-only', code, pub).decode().splitlines()
    require(all(p.startswith(('doc/releases/', 'script/quality/VALIDATION-', 'docs/')) or
                p in ('README.md', 'README_sc.md', 'README_tc.md') for p in changed), 'Untested code changed after build')
    require({k: v for k, v in current.items() if k} == {k: v for k, v in record['repositories'].items() if k}, 'Submodule changed after build')
    return pub


def stage(args):
    q, record = qualification(args.qualification)
    pub = source_binding(record)
    meta = metadata()
    notes = ROOT / 'doc/releases' / (meta['tag'] + '.md')
    require(notes.is_file(), 'Committed release notes required')
    args.output.mkdir(parents=True, exist_ok=False)
    assets = args.output / 'assets'
    assets.mkdir()
    for apk in record['apks']:
        shutil.copyfile(apk['apk']['path'], assets / apk['public_name'])
        require(binding(assets / apk['public_name'])['sha256'] == apk['apk']['sha256'], 'Staged APK changed')
    archive = assets / f"trime-{meta['tag']}-source-with-submodules.tar.gz"
    archive_report = source_archive(Path(q['build_record']['path']), archive)
    write_json(args.output / 'source-verification.json', archive_report)
    manifest = dict(format_version=1, tag=meta['tag'], repository=meta['repository'], stable=True,
                    publication_git_sha=pub, build_git_sha=record['build_git_sha'],
                    qualification=binding(args.qualification), release_notes=binding(notes),
                    source_verification=binding(args.output / 'source-verification.json'),
                    public_assets={p.name: binding(p) for p in sorted(assets.iterdir())})
    write_json(args.output / 'manifest.json', manifest)
    load_manifest(args.output / 'manifest.json')
    return manifest


def load_manifest(path):
    m = json.loads(path.read_text())
    meta = metadata()
    require(m['format_version'] == 1 and m['tag'] == meta['tag'] and m['repository'] == 'X2M7/trime' and m['stable'] is True,
            'Only qualified stable X2M7/trime releases are supported; experiments need a separate prerelease')
    verify_binding(m['qualification'])
    _, record = qualification(Path(m['qualification']['path']))
    require(m['build_git_sha'] == record['build_git_sha'] and m['publication_git_sha'] == source_binding(record), 'Publication source changed')
    verify_binding(m['release_notes'])
    source = read_bound(m['source_verification'])
    verify_binding(source['archive'])
    require(source['repositories'] == repositories(), 'Source archive repository map differs from publication')
    names = {f"trime-{meta['tag']}-{abi}.apk" for abi in meta['abis']} | {f"trime-{meta['tag']}-source-with-submodules.tar.gz"}
    require(set(m['public_assets']) == names, 'Release attachments must be only two APKs and recursive source')
    paths = {}
    for name, row in m['public_assets'].items():
        verify_binding(row)
        paths[name] = Path(row['path'])
        require(paths[name].name == name, 'Public filename differs')
        if name.endswith('.apk'):
            actual = next(a['apk'] for a in record['apks'] if a['public_name'] == name)
            require((row['bytes'], row['sha256']) == (actual['bytes'], actual['sha256']), 'Staged APK is not tested bytes')
        else:
            require(row == source['archive'], 'Source archive differs')
    return m, paths


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--qualification', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    print(json.dumps(stage(parser.parse_args()), indent=2))


if __name__ == '__main__':
    main()
