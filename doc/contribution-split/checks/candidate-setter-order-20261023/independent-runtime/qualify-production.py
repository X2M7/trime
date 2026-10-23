#!/usr/bin/env python3
"""HOST-only final production phase qualification. Never imports device tools."""
import argparse
import datetime
import json
import re
from pathlib import Path
import candidate_oracles as q
import fixture_rules as f
import bookend_rules as b

BUILD = '9ff9572f2ed289d8af198f81ce08bdac66d8a69c'
BUILD_TREE = '10f5abd560dc4d365553d8edadc62f47986cb7da'
REVIEWED_TREE = '2da497746bf3997515b816629ae92e6d2ffef0fb'
PROBE = '831e2a7da97c6880671108b628372655818c9dd3f19ad014118b2e34d2eac080'


def identity(binding):
    checks = {
        'reviewedPRHead': binding.get('source_head') == q.HEAD,
        'actualMergeBuild': binding.get('actual_build_commit') == BUILD,
        'reviewedFullTree': binding.get('reviewed_source_tree') == REVIEWED_TREE,
        'actualMergeFullTree': binding.get('source_tree') == BUILD_TREE,
        'wholeTreesExplicitlyDifferent': binding.get('whole_tree_equivalent') is False
            and BUILD_TREE != REVIEWED_TREE,
        'sameProductionClassSource': binding.get('production_view_source_sha256')
            == binding.get('actual_class_source_sha256') == q.SOURCE,
        'producerRepositoryAndRun': binding.get('expected_producer', {}).get('repository')
            == 'osfans/trime' and binding.get('expected_producer', {}).get('run') == 39889136561,
        'legacyHeadFieldMeaningExplicit': binding.get('legacy_operator_source_head_semantics')
            == 'reviewed_PR_head_not_actual_build_commit',
        'originalProbeUnchanged': binding.get('production_probe_sha256') == PROBE
            and q.sha(Path(binding['production_probe_path'])) == PROBE,
    }
    frozen = []
    for row in binding.get('frozen_files', []):
        actual = q.meta(Path(row['path']))
        actual['matchesFrozen'] = (actual['sha256'] == row['sha256']
            and actual['bytes'] == row['bytes'] and actual['unchangedDuringHash'])
        frozen.append(actual)
    checks['allActualFrozenBytes'] = bool(frozen) and all(x['matchesFrozen'] for x in frozen)
    checks['actualApkBytes'] = bool(re.fullmatch(r'[0-9a-f]{64}', binding['original_apk_sha256'])) \
        and q.sha(Path(binding['original_apk_path'])) == binding['original_apk_sha256']
    audit_path = Path(binding['original_artifact_audit_path'])
    checks['originalArtifactAuditDigest'] = q.sha(audit_path) == binding['original_artifact_audit_sha256']
    audit = q.load(audit_path)
    checks['auditActualMergeIdentity'] = (audit.get('passed') is True
        and audit.get('source_head') == BUILD and audit.get('source_tree') == BUILD_TREE
        and audit.get('actual_class_source_sha256') == q.SOURCE)
    apks = [x for x in audit.get('apks', []) if x.get('abi') == 'x86_64']
    checks['auditedOriginalApkAndBuildConfig'] = (len(apks) == 1
        and apks[0].get('sha256') == binding['original_apk_sha256']
        and apks[0].get('buildconfig_commit_hash') == BUILD)
    return {'passed': all(checks.values()), 'checks': checks, 'frozenFiles': frozen,
            'originalArtifactAudit': q.meta(audit_path), 'auditedX8664Apks': apks}


def probe_identity(round_path, phase):
    """Keep unobserved /proc identity unobserved; inspect the actual post-probe ps."""
    p = round_path / 'operator1/production-class-only-probe-identity.raw.txt'
    rows = p.read_text().splitlines()
    columns = rows[0].split()
    assert 'PID' in columns and 'NAME' in columns
    value = phase.get('actual_probe_process_identity', {})
    reported_pid = value.get('self_reported_pid')
    actual = [line for line in rows[1:] if line.split()
              and line.split()[-1] == 'org.x2m7.trime.readiness.candidate'
              and int(line.split()[columns.index('PID')]) == reported_pid]
    state = value.get('os_pid_start')
    checks = {'reportedPidMatchesExportedResult': reported_pid == q.load(
        Path(phase['original_folder']) / 'result.json').get('pid')}
    if state == 'NOT_OBSERVED':
        checks['noLiveMatchingProcessInActualPs'] = not actual
        checks['noInventedObservedStart'] = not any(k in value for k in ('pid', 'starttime', 'cmdline_exact'))
    else:
        # A live /proc branch must receive a separate raw-command audit, rather
        # than treating its producer label as proof. Do not silently qualify it.
        checks['requiresAdditionalActualProcCommandAudit'] = False
    return {'passed': all(checks.values()), 'checks': checks, 'observed': value,
            'actualMatchingPsRows': actual, 'actualPs': q.meta(p),
            'hardcodedNativeFalseIsNotProcessEvidence': True}


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--round', required=True)
    ap.add_argument('--binding', required=True, type=Path)
    ap.add_argument('--expected-binding-sha', required=True)
    ap.add_argument('--expected-wrapper-sha', required=True)
    ap.add_argument('--output', required=True)
    args = ap.parse_args()
    assert Path(args.round).name == args.round
    assert Path(args.output).name == args.output and args.output.endswith('.json')
    assert re.fullmatch(r'[0-9a-f]{64}', args.expected_binding_sha)
    assert re.fullmatch(r'[0-9a-f]{64}', args.expected_wrapper_sha)
    root = q.ROOT / args.round
    out = q.OUT / args.output
    assert not out.exists(), 'Refuse overwrite of independent result'
    # Never turn a still-growing live journal into a terminal product verdict.
    assert (root / 'completion.json').is_file(), 'Wait for actual controller completion'
    operator = q.load(root / 'operator1/report.json')
    fixture = q.load(root / 'fixture1/report.json')
    assert operator.get('finished') and fixture.get('finished'), 'Wait for final journals'
    binding = q.load(args.binding)
    q.APK = binding['original_apk_sha256']
    report = {
        'createdUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'reviewer': 'candidate_review_prepare independent HOST',
        'reviewedPRHead': q.HEAD, 'reviewedFullTree': REVIEWED_TREE,
        'actualBuildCommit': BUILD, 'actualBuildTree': BUILD_TREE,
        'wholeTreeEquivalent': False, 'productionClassSourceSha256': q.SOURCE,
        'originalApkSha256': q.APK, 'round': args.round, 'sdk': 21,
        'operatorSourceHeadFieldMeans': 'reviewed_PR_head_not_actual_build_commit',
        'originalOperatorReport': q.meta(root / 'operator1/report.json'),
        'originalFixtureReport': q.meta(root / 'fixture1/report.json'),
        'humanImageReviewClaimed': False, 'overallScopedQualified': False,
        'physicalDevice': 'NOT_RUN', 'TalkBack': 'NOT_RUN',
        'nativeCandidateIntegration': 'NOT_RUN', 'diagnosticVariants': 'NOT_RERUN',
        'scope': 'Only the same production class from an actual upstream merge APK, original 13 stages/39 PNGs and authenticated class-only interval. No full fork-tree or full merge-tree acceptance.',
    }
    checks = {}
    try:
        checks['rootFrozenBindingDigest'] = q.sha(args.binding) == args.expected_binding_sha
        checks['operatorExactBinding'] = (operator.get('binding') == binding
            and operator.get('binding_sha256') == args.expected_binding_sha)
        checks['fixtureExactBinding'] = (fixture.get('binding_sha256') == args.expected_binding_sha
            and q.sha(Path(fixture['binding_path'])) == args.expected_binding_sha)
        checks['legacyReviewedHeadAndAPI'] = (operator.get('source_head') == q.HEAD
            and fixture.get('source_head') == q.HEAD and operator.get('api') == 21)
        report['identityAudit'] = identity(binding)
        checks['actualAndReviewedIdentitiesCorrect'] = report['identityAudit']['passed']
        phases = operator.get('phases', [])
        checks['onlyOriginalProductionPhase'] = [x.get('name') for x in phases] == ['production-class-only']
        assert checks['onlyOriginalProductionPhase'], 'Unexpected or missing phase'
        phase = q.phase_audit(root, phases[0], 21, binding)
        report['phase'] = phase
        checks['allOriginal13Stage39PngOracles'] = phase.get('positiveQualified') is True
        report['actualProbeIdentity'] = probe_identity(root, phases[0])
        checks['probePidIdentityNotInvented'] = report['actualProbeIdentity']['passed']
        report['terminal'] = q.terminal_audit(root, operator)
        checks['controllerVmCollectorSdkAndImeRestoration'] = report['terminal']['qualified']
        completion = q.load(root / 'completion.json')
        checks['requestedNormalStopWithoutControllerError'] = (completion.get('reason') == 'requested-normal-stop'
            and not completion.get('controller_error') and not completion.get('stop_escalated_to_term')
            and not completion.get('emulator_term_requested') and not completion.get('emulator_kill_requested'))
        checks['operatorSequenceCompleted'] = (operator.get('operator_sequence_completed') is True
            and not operator.get('operator_failure') and operator.get('cleanup_errors') == [])
        checks['fixtureActualExit0'] = fixture.get('actual_exit') == fixture.get('child_actual_exit') == 0
        checks['fixtureCompleteWithoutFailure'] = (fixture.get('fixture_operator_completed') is True
            and not fixture.get('fixture_failure') and fixture.get('cleanup_errors') == [])
        wrapper = q.ROOT / 'runtime-plan2/run-isolated-probes.py'
        child = q.ROOT / 'runtime-plan2/run-probes.py'
        checks['actualWrapperBytes'] = q.sha(wrapper) == args.expected_wrapper_sha
        checks['actualChildBytes'] = fixture.get('original_child_driver_sha256') == q.sha(child)
        nonce = fixture.get('begin_nonce', '')
        checks['randomNonceShape'] = bool(re.fullmatch(r'[a-f0-9]{32}', nonce))
        report['beginCommand'] = f.command(fixture['begin_command'], 'BEGIN ' + nonce)
        report['endCommand'] = f.command(fixture['end_command'], 'END ' + nonce)
        checks['actualMarkerCommandsAndRemoteStatuses'] = report['beginCommand']['passed'] and report['endCommand']['passed']
        report['bookend'] = b.log_range(root / 'device-logcat.txt', 'PR2187_CLASS_ONLY', 'BEGIN ' + nonce, 'END ' + nonce)
        checks['uniqueOrderedMarkerInterval'] = report['bookend']['state'] == 'BOUNDED_SOURCE_COMPLETE'
        checks['noTargetFamilyStartInsideInterval'] = report['bookend']['strictRangeQualified']
        before = fixture['documents_before']
        package = before.get('package')
        checks['allowlistedDocsPackage'] = package in ('com.android.documentsui', 'com.google.android.documentsui')
        states = {
            'before': f.package_state(root / 'fixture1', 'before-' + package),
            'isolated': f.package_state(root / 'fixture1', 'isolated'),
            'restored': f.package_state(root / 'fixture1', 'restore-stopped'
                if (root / 'fixture1/restore-stopped-state.json').exists() else 'restore-isolation'),
        }
        report['actualDocumentsStates'] = states
        checks['actualDocsDumpsAgree'] = all(x['passed'] for x in states.values())
        checks['beforeStateExact'] = before == states['before']['state']
        isolated = states['isolated']['state']
        checks['actualAPI21HidePreservesOtherStates'] = (isolated.get('hidden') is True
            and all(isolated.get(k) == before.get(k) for k in ('enabled', 'installed', 'stopped')))
        restored = states['restored']['state']
        checks['docsFiveFieldsAndPathExactlyRestored'] = all(restored.get(k) == before.get(k)
            for k in ('package', 'present', 'apk_path', 'installed', 'hidden', 'enabled', 'stopped'))
        checks['restoreRecordExact'] = fixture.get('documents_restored_exactly') == restored
        streams = []
        for row in fixture.get('child_streams', []):
            actual = q.meta(Path(row['path']))
            actual['matchesRecorded'] = actual['sha256'] == row['sha256']
            streams.append(actual)
        report['actualChildStreams'] = streams
        checks['childWaitStreamsExact'] = len(streams) == 2 and all(x['matchesRecorded'] for x in streams)
        readbacks = []
        for name, digest in [('production-original-installed-apk.bin', q.APK), ('production-probe-installed-apk.bin', PROBE)]:
            value = q.meta(root / 'operator1' / name)
            value['expectedSha256'] = digest
            value['matchesExpected'] = value['sha256'] == digest
            readbacks.append(value)
        report['actualInstalledByteReadbacks'] = readbacks
        checks['exactInstalledOriginalApkAndProbe'] = all(x['matchesExpected'] for x in readbacks)
        paths = [root / 'operator1/baseline-ps.raw.txt', root / 'operator1/restored-ps.raw.txt',
            root / 'fixture1/after-explicit-old-target-stop-ps.raw.txt',
            root / 'operator1/production-class-only-before.raw.txt',
            root / 'operator1/production-class-only-after.raw.txt']
        paths += [p for p in sorted((root / 'operator1').glob('*.raw.txt'))
            if p.name.endswith(('-probe-identity.raw.txt', '-no-Trime-process.raw.txt')) and p not in paths]
        report['strictActualPs'] = b.sampled_ps(paths)
        checks['allStrictActualPsNoTargetFamily'] = report['strictActualPs']['qualified']
        report['outsideActualPs'] = b.sampled_ps([root / 'fixture1/before-fixture-ps.raw.txt',
            root / 'fixture1/before-explicit-old-target-stop-ps.raw.txt',
            root / 'fixture1/after-fixture-restoration-ps.raw.txt'])
        report['wholeLog'] = q.log_audit(root)
        checks['closedLogStable'] = report['wholeLog']['source']['unchangedDuringHash']
        checks['bookendAndFullLogUseIdenticalClosedBytes'] = (report['bookend']['source']['sha256']
            == report['wholeLog']['source']['sha256'] and report['bookend']['source']['bytes']
            == report['wholeLog']['source']['bytes'])
        report['checks'] = checks
        report['overallScopedQualified'] = all(checks.values())
    except BaseException as error:
        report['checks'] = checks
        report['auditFailure'] = {'type': type(error).__name__, 'message': str(error)}
    report['limits'] = [
        'No historical source92 or prior API21 pass is transferred to the new APK.',
        'Source-head legacy fields refer to reviewed7f only; actual producer is merge9ff and whole trees differ.',
        'Raw whole-log warnings, errors, fatal contexts and target starts outside BEGIN..END remain evidence.',
        'Hardcoded native/Application false is not proof of absence of an OS process.',
        'PNG hashes and ink oracles are independent machine checks; Root performs separate actual image review.',
        'Controller process wait/reap receipt, if separately recorded by the operator, is supplementary and must not be invented from a completion label.',
    ]
    with out.open('x') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
    print(json.dumps({'output': str(out), 'sha256': q.sha(out),
        'overallScopedQualified': report['overallScopedQualified'],
        'failedChecks': [k for k, v in checks.items() if not v], 'failure': report.get('auditFailure')}))


if __name__ == '__main__':
    main()
