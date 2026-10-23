#!/usr/bin/env python3
"""HOST-only fixture bookend qualification, using completed immutable child evidence."""
import argparse, datetime, hashlib, json, re
from pathlib import Path
import qualify as q
import bookend_rules as b

ROOT=q.ROOT; OUT=q.OUT
EXPECTED_WRAPPER='02b84048f879b2af3ab7335ea17676275bc613e7104723651787c2a9110eb4bb'
# The independently reviewed Root-frozen plan4 binding hash must be supplied explicitly.


def command(record, expected_message=None, allowed_statuses=(0,)):
    stdout=Path(record['stdout']);stderr=Path(record['stderr']);raw=stdout.read_bytes()
    nonce=record.get('shell_protocol_nonce','')
    statuses=re.findall(rb'__TRIME_T12_REMOTE_RC_'+re.escape(nonce.encode())+rb':(-?\d+)\r?\n?$',raw)
    checks=dict(stdoutHash=q.sha(stdout)==record.get('stdout_sha256'),stderrHash=q.sha(stderr)==record.get('stderr_sha256'),
        hostExit0=record.get('returncode')==0,completed=record.get('completed') is True,
        noncePresent=bool(re.fullmatch(r'[a-f0-9]{32}',nonce)),
        actualRemoteAllowed=len(statuses)==1 and int(statuses[0]) in allowed_statuses and record.get('remote_returncode')==int(statuses[0]))
    if expected_message is not None:
        argv=record.get('argv',[])
        checks['exactMarkerCommandPayload']=bool(argv and 'PR2187_CLASS_ONLY' in argv[-1] and expected_message in argv[-1])
    return dict(passed=all(checks.values()),checks=checks,stdout=q.meta(stdout),stderr=q.meta(stderr),actualRemoteStatuses=[int(x) for x in statuses])


def package_state(folder,label):
    p=folder/(label+'-state.json');d=folder/(label+'-package.raw.txt')
    obj=q.load(p);raw=d.read_text();users=re.findall(r'^\s*User 0:\s*(.*)$',raw,re.M)
    fields=dict(re.findall(r'\b(installed|hidden|stopped|enabled)=([^\s]+)',users[0])) if len(users)==1 else {}
    truth=dict(present=obj.get('present') is True,installed=fields.get('installed')=='true',
        enabled=len(users)==1 and fields.get('enabled')==str(obj.get('enabled')),
        stopped=fields.get('stopped')==str(obj.get('stopped')).lower(),
        installedRecord=obj.get('installed') is True,hidden=fields.get('hidden')==str(obj.get('hidden')).lower())
    identity=re.findall(r'^\s*Package \[([^\]]+)\][^\r\n]*:',raw,re.M)
    truth['exactPackageHeader']=identity==[obj.get('package')]
    cmd=obj.get('pm_path_command',{});path_audit=command(cmd,allowed_statuses=(0,1))
    truth['actualPathQueryCommand']=path_audit['passed']
    out=Path(cmd['stdout']).read_bytes(); nonce=cmd['shell_protocol_nonce'].encode()
    marker=re.search(rb'__TRIME_T12_REMOTE_RC_'+re.escape(nonce)+rb':(-?\d+)\r?\n?$',out)
    actual=(out[:marker.start()] if marker else b'').strip().decode(errors='replace')
    if obj.get('apk_path') is None:
        truth['hiddenAbsenceTruthful']=(obj.get('hidden') is True and obj.get('apk_path_query')=='absent_due_to_hidden' and not actual)
    else:
        truth['presentPathTruthful']=(obj.get('apk_path_query')=='PRESENT' and actual=='package:'+obj['apk_path'] and cmd.get('remote_returncode')==0)
    return dict(passed=all(truth.values()),checks=truth,state=obj,stateFile=q.meta(p),actualPackageDump=q.meta(d),user0Lines=users)


def main():
    a=argparse.ArgumentParser(description=__doc__);a.add_argument('--round',required=True);a.add_argument('--sdk',type=int,choices=(21,35),required=True)
    a.add_argument('--child-audit',required=True,type=Path);a.add_argument('--output',required=True)
    a.add_argument('--expected-binding-sha',required=True);args=a.parse_args()
    if not re.fullmatch(r'[0-9a-f]{64}',args.expected_binding_sha):raise ValueError('Exact Root-frozen binding SHA required')
    if '/' in args.output or not args.output.endswith('.json'):raise ValueError('Output basename only')
    root=ROOT/args.round
    if root.parent!=ROOT:raise ValueError('Round direct child only')
    output=OUT/args.output
    if output.exists():raise FileExistsError(output)
    report=dict(createdUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),reviewer='candidate_review_prepare HOST independent',
        sourceHead=q.HEAD,sourceSha256=q.SOURCE,sdk=args.sdk,round=args.round,script=q.meta(Path(__file__).resolve()),
        childAudit=q.meta(args.child_audit),overallScopedQualified=False,humanImageReviewClaimed=False,
        physicalDevice='NOT_RUN',TalkBack='NOT_RUN',nativeCandidateIntegration='NOT_RUN',
        scope='Strict unique BEGIN..END class-only interval plus separately retained whole-VM process/fatal records. No global Application-never-started or zero-warning claim.')
    try:
        # Completion is mandatory before reading live report.json paths.
        if not (root/'completion.json').is_file():raise ValueError('Controller completion not present: wait, do not classify live JSON')
        folder=root/'fixture1';fixture=q.load(folder/'report.json');operator=q.load(root/'operator1/report.json');child=q.load(args.child_audit)
        if not fixture.get('finished') or not operator.get('finished'):raise ValueError('Reports not final')
        report['actualFixtureReport']=q.meta(folder/'report.json');report['actualOperatorReport']=q.meta(root/'operator1/report.json')
        checks={}
        def check(name,value):checks[name]=bool(value)
        check('childAuditExactActualOperatorBytes',child.get('originalOperatorReport',{}).get('sha256')==q.sha(root/'operator1/report.json'))
        check('childAuditExactSourceAPI',child.get('sourceHead')==q.HEAD and child.get('sourceSha256')==q.SOURCE and child.get('sdk')==args.sdk and child.get('round')==args.round)
        check('childPositiveEvidence',child.get('positiveEvidenceQualified') is True)
        check('childTerminal',child.get('roundTerminalQualified') is True)
        check('childOriginalPhaseOrder',child.get('phaseOrderExact') is True)
        check('childSequenceComplete',operator.get('operator_sequence_completed') is True and not operator.get('operator_failure'))
        check('childAuditOriginalOverall',child.get('overallQualified') is True)
        check('fixtureActualExit0',fixture.get('actual_exit')==0 and fixture.get('child_actual_exit')==0)
        check('fixtureOperatorComplete',fixture.get('fixture_operator_completed') is True)
        check('fixtureNoFailure',not fixture.get('fixture_failure') and fixture.get('cleanup_errors')==[])
        check('fixtureExactSource',fixture.get('source_head')==q.HEAD)
        check('fixtureFrozenBinding',fixture.get('binding_sha256')==args.expected_binding_sha and q.sha(Path(fixture['binding_path']))==args.expected_binding_sha)
        check('actualWrapperBytes',q.sha(ROOT/'runtime-plan4/run-isolated-probes.py')==EXPECTED_WRAPPER)
        check('sameChildDriverBytes',fixture.get('original_child_driver_sha256')==q.sha(ROOT/'runtime-plan2/run-probes.py')==q.sha(ROOT/'runtime-plan4/run-probes.py'))
        check('childFrozenBinding',operator.get('binding_sha256')==args.expected_binding_sha)
        nonce=fixture.get('begin_nonce','');check('realRandomNonceShape',bool(re.fullmatch(r'[a-f0-9]{32}',nonce)))
        begin='BEGIN '+nonce;end='END '+nonce
        report['beginCommandAudit']=command(fixture['begin_command'],begin)
        report['endCommandAudit']=command(fixture['end_command'],end)
        check('authenticatedBeginCommand',report['beginCommandAudit']['passed'])
        check('authenticatedEndCommand',report['endCommandAudit']['passed'])
        report['exactLogBookend']=b.log_range(root/'device-logcat.txt','PR2187_CLASS_ONLY',begin,end)
        check('uniqueActualMarkerIntervalComplete',report['exactLogBookend'].get('state')=='BOUNDED_SOURCE_COMPLETE')
        check('noActualTargetStartInsideStrictInterval',report['exactLogBookend']['strictRangeQualified'])
        before=fixture['documents_before'];package=before.get('package')
        check('onlyActualAllowlistedDocsUI',package in ('com.android.documentsui','com.google.android.documentsui'))
        before_state=package_state(folder,'before-'+package);disabled=package_state(folder,'isolated')
        restore_label='restore-stopped' if (folder/'restore-stopped-state.json').exists() else 'restore-isolation'
        restored=package_state(folder,restore_label)
        report['actualDocumentsStates']=dict(before=before_state,disabled=disabled,restored=restored)
        check('allDocsUIDumpsAgreeWithStateRecords',all(x['passed'] for x in (before_state,disabled,restored)))
        check('beforeRecordExact',before==before_state['state'])
        check('DocsUIOriginallyDefaultOrEnabled',before.get('enabled') in (0,1))
        if args.sdk==21:
            isolation=(disabled['state'].get('hidden') is True and disabled['state'].get('enabled')==before.get('enabled')
                and disabled['state'].get('installed')==before.get('installed') and disabled['state'].get('stopped')==before.get('stopped'))
            method='API21_PUBLIC_PM_HIDE_UNHIDE'
        else:
            isolation=(disabled['state'].get('enabled')==3 and disabled['state'].get('hidden')==before.get('hidden')
                and disabled['state'].get('installed')==before.get('installed') and disabled['state'].get('stopped')==before.get('stopped'))
            method='API35_PUBLIC_PM_DISABLE_USER_RESTORE_ENABLED'
        report['actualIsolationMethod']=method
        check('ActualPlatformIsolationPreservesOtherPackageState',isolation)
        check('DocsUIEnabledStoppedPathExactlyRestored',all(restored['state'].get(k)==before.get(k) for k in ('package','apk_path','present','installed','hidden','enabled','stopped')))
        check('restoreRecordExact',fixture.get('documents_restored_exactly')==restored['state'])
        streams=[]
        for item in fixture.get('child_streams',[]):
            stream=q.meta(Path(item['path']));stream['matchesRecorded']=stream['sha256']==item['sha256'];streams.append(stream)
        report['actualChildStreams']=streams
        check('actualChildWaitStreams',len(streams)==2 and all(x['matchesRecorded'] for x in streams))
        readbacks=[]
        for name,digest in [('diagnostic-installed-apk.bin',q.DIAG),('production-original-installed-apk.bin',q.APK),
                            ('production-probe-installed-apk.bin',operator['binding']['production_probe_sha256'])]:
            item=q.meta(root/'operator1'/name);item['expectedSha256']=digest;item['matchesExpected']=item['sha256']==digest;readbacks.append(item)
        report['actualDeviceReadbackApks']=readbacks;check('allInstalledOriginalByteReadbacksExact',all(x['matchesExpected'] for x in readbacks))
        phase_names=['diagnostic-safe','diagnostic-equal_return','diagnostic-no_super_measure','production-class-only']
        paths=[root/'operator1/baseline-ps.raw.txt',root/'operator1/restored-ps.raw.txt',folder/'after-explicit-old-target-stop-ps.raw.txt']
        for name in phase_names:paths.extend([root/'operator1'/(name+'-before.raw.txt'),root/'operator1'/(name+'-after.raw.txt')])
        optional=[]
        for p in sorted((root/'operator1').glob('*.raw.txt')):
            if p.name.endswith(('-probe-identity.raw.txt','-no-Trime-process.raw.txt')) and p not in paths:optional.append(p)
        report['strictActualPsSamples']=b.sampled_ps(paths+optional)
        check('allStrictActualPsSamplesNoTargetFamily',report['strictActualPsSamples']['qualified'])
        report['outsideActualFixturePsSamples']=b.sampled_ps([folder/'before-fixture-ps.raw.txt',folder/'before-explicit-old-target-stop-ps.raw.txt',folder/'after-fixture-restoration-ps.raw.txt'])
        report['wholeVmActualProcessAndFatalEvidence']=dict(priorityCounts=child.get('log',{}).get('priorities'),
            actualTargetStartRecords=child.get('log',{}).get('actualTrimeStartRecords'),fatalContexts=child.get('log',{}).get('fatalContexts'),
            outsideStartsAreNotErasedOrClassifiedBenign=True)
        report.update(checks=checks,overallScopedQualified=all(checks.values()),
            independentlyQualifiedPositivePhases=[p['name'] for p in child['phases'] if p.get('positiveQualified')],
            diagnosticDispositions=[dict(name=p['name'],disposition=p['disposition']) for p in child['phases'] if p.get('variant')],
            noAllWarningsOrNoWholeVmApplicationStartClaim=True)
    except BaseException as e:
        report['auditFailure']=dict(type=type(e).__name__,message=str(e))
    with output.open('x') as f:json.dump(report,f,indent=2,ensure_ascii=False);f.write('\n')
    print(json.dumps(dict(output=str(output),sha256=q.sha(output),overallScopedQualified=report['overallScopedQualified'],
        failedChecks=[k for k,v in report.get('checks',{}).items() if not v],failure=report.get('auditFailure')),ensure_ascii=False))

if __name__=='__main__':main()
