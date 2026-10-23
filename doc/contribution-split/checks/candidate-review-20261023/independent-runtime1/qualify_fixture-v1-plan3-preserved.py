#!/usr/bin/env python3
"""HOST-only fixture bookend qualification, using completed immutable child evidence."""
import argparse, datetime, hashlib, json, re
from pathlib import Path
import qualify as q
import bookend_rules as b

ROOT=q.ROOT; OUT=q.OUT
EXPECTED_WRAPPER='14da2c385fae2fedf2ea4f0f5ff33c9c7efff9ec58a8bb5ce5bb075db5f5bbfb'
EXPECTED_BINDING='ff698f628a3f2958846ec25b7948b72b14f32d5ccb28f68e23928a7b93a4760c'


def command(record, expected_message=None):
    stdout=Path(record['stdout']);stderr=Path(record['stderr']);raw=stdout.read_bytes()
    nonce=record.get('shell_protocol_nonce','')
    statuses=re.findall(rb'__TRIME_T12_REMOTE_RC_'+re.escape(nonce.encode())+rb':(-?\d+)\r?\n?$',raw)
    checks=dict(stdoutHash=q.sha(stdout)==record.get('stdout_sha256'),stderrHash=q.sha(stderr)==record.get('stderr_sha256'),
        hostExit0=record.get('returncode')==0,completed=record.get('completed') is True,
        noncePresent=bool(re.fullmatch(r'[a-f0-9]{32}',nonce)),
        actualRemote0=len(statuses)==1 and int(statuses[0])==0 and record.get('remote_returncode')==0)
    if expected_message is not None:
        argv=record.get('argv',[])
        checks['exactMarkerCommandPayload']=bool(argv and 'PR2187_CLASS_ONLY' in argv[-1] and expected_message in argv[-1])
    return dict(passed=all(checks.values()),checks=checks,stdout=q.meta(stdout),stderr=q.meta(stderr),actualRemoteStatuses=[int(x) for x in statuses])


def package_state(folder,label):
    p=folder/(label+'-state.json');d=folder/(label+'-package.raw.txt')
    obj=q.load(p);raw=d.read_text();users=re.findall(r'^\s*User 0:\s*(.*)$',raw,re.M)
    fields=dict(re.findall(r'\b(installed|stopped|enabled)=([^\s]+)',users[0])) if len(users)==1 else {}
    truth=dict(present=obj.get('present') is True,installed=fields.get('installed')=='true',
        enabled=len(users)==1 and fields.get('enabled')==str(obj.get('enabled')),
        stopped=fields.get('stopped')==str(obj.get('stopped')).lower())
    return dict(passed=all(truth.values()),checks=truth,state=obj,stateFile=q.meta(p),actualPackageDump=q.meta(d),user0Lines=users)


def main():
    a=argparse.ArgumentParser(description=__doc__);a.add_argument('--round',required=True);a.add_argument('--sdk',type=int,choices=(21,35),required=True)
    a.add_argument('--child-audit',required=True,type=Path);a.add_argument('--output',required=True);args=a.parse_args()
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
        check('fixtureFrozenBinding',fixture.get('binding_sha256')==EXPECTED_BINDING and q.sha(Path(fixture['binding_path']))==EXPECTED_BINDING)
        check('actualWrapperBytes',q.sha(ROOT/'runtime-plan3/run-isolated-probes.py')==EXPECTED_WRAPPER)
        check('sameChildDriverBytes',fixture.get('original_child_driver_sha256')==q.sha(ROOT/'runtime-plan2/run-probes.py')==q.sha(ROOT/'runtime-plan3/run-probes.py'))
        check('childFrozenBinding',operator.get('binding_sha256')==EXPECTED_BINDING)
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
        before_state=package_state(folder,'before-'+package);disabled=package_state(folder,'disabled')
        restore_label='restore-stopped' if (folder/'restore-stopped-state.json').exists() else 'restore-enabled'
        restored=package_state(folder,restore_label)
        report['actualDocumentsStates']=dict(before=before_state,disabled=disabled,restored=restored)
        check('allDocsUIDumpsAgreeWithStateRecords',all(x['passed'] for x in (before_state,disabled,restored)))
        check('beforeRecordExact',before==before_state['state'])
        check('DocsUIOriginallyDefaultOrEnabled',before.get('enabled') in (0,1))
        check('DocsUIDisabledUserForChild',disabled['state'].get('enabled')==3 and disabled['state'].get('stopped')==before.get('stopped'))
        check('DocsUIEnabledStoppedPathExactlyRestored',all(restored['state'].get(k)==before.get(k) for k in ('package','apk_path','present','enabled','stopped')))
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
