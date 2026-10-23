#!/usr/bin/env python3
"""HOST-only final qualification of an actually failed zero-stage API35 round; no retries/device calls."""
import datetime, io, json, re, tarfile
from pathlib import Path
import qualify as q
import qualify_fixture as f
import bookend_rules as b

ROOT=q.ROOT;OUT=q.OUT;root=ROOT/'api35-class-only5';fixture_dir=root/'fixture1';child_dir=root/'operator1'
output=OUT/'api35-round5-failed-terminal1.json'
if output.exists():raise FileExistsError(output)
if not (root/'completion.json').exists():raise ValueError('Wait for actual controller completion')
fixture=q.load(fixture_dir/'report.json');operator=q.load(child_dir/'report.json');completion=q.load(root/'completion.json')
if not fixture.get('finished') or not operator.get('finished'):raise ValueError('Reports not final')
binding=operator['binding'];nonce=fixture['begin_nonce'];begin='BEGIN '+nonce;end='END '+nonce
record=q.load(child_dir/'diagnostic-safe/phase-record.json')
exports=list((child_dir/'diagnostic-safe').glob('exported/*/result.json'))
if len(exports)!=1:raise ValueError('Unique actual exported failed result required')
raw_report=exports[0];actual=q.load(raw_report);stdout_path=child_dir/'diagnostic-safe/instrumentation.stdout.txt';stdout=stdout_path.read_text()
codes=[int(x) for x in re.findall(r'^INSTRUMENTATION_CODE:\s*(-?\d+)\s*$',stdout,re.M)]
encoded=re.findall(r'^INSTRUMENTATION_RESULT: report=(\{[^\r\n]*\})\s*$',stdout,re.M)
missing=[k for k in ('sourceApk','sourceApkSha256','sdk','pid','runId','diagnosticVariant','diagnosticClassSourceSha256') if k not in actual]
phase_truth=dict(actualCodeIsFailure=codes==[0],actualFailed=actual.get('passed') is False,
                actualStageCountZero=actual.get('stages')==[],expectedThirteenStillDeclared=actual.get('expectedStageCount')==13,
                exportedEqualsInstrumentationReport=len(encoded)==1 and json.loads(encoded[0])==actual,
                actualDriverMissingIdentityKeyError=record.get('failure')=={'type':'KeyError','message':"'sourceApkSha256'"},
                identityUnavailableNotInferred=bool(missing))
archive=child_dir/'diagnostic-safe/original-export.tar';tar_files=[]
with tarfile.open(archive) as ar:
    for member in ar.getmembers():
        if member.isfile():
            stream=ar.extractfile(member); data=stream.read();stream.close()
            disk=child_dir/'diagnostic-safe/exported'/member.name
            tar_files.append(dict(name=member.name,bytes=len(data),sha256=q.hashlib.sha256(data).hexdigest(),
                                  matchesExportedDisk=disk.is_file() and disk.read_bytes()==data))
phase_truth['rawTarExportMatchesActualFiles']=bool(tar_files) and all(x['matchesExportedDisk'] for x in tar_files)
terminal=q.terminal_audit(root,operator)
bookend=b.log_range(root/'device-logcat.txt','PR2187_CLASS_ONLY',begin,end)
state_before=f.package_state(fixture_dir,'before-com.android.documentsui')
state_isolated=f.package_state(fixture_dir,'isolated')
state_restored=f.package_state(fixture_dir,'restore-isolation')
before=state_before['state'];isolated=state_isolated['state'];restored=state_restored['state']
docs_checks=dict(actualStateDumpsAndQueries=all(x['passed'] for x in (state_before,state_isolated,state_restored)),
    actualIsolatedDisabledUser=isolated.get('enabled')==3,
    otherFieldsPreservedDuringIsolation=all(isolated.get(k)==before.get(k) for k in ('installed','hidden','stopped','apk_path')),
    allOriginalFieldsAndPathRestored=all(restored.get(k)==before.get(k) for k in ('package','installed','hidden','enabled','stopped','apk_path')),
    actualFixtureRestorationRecordExact=fixture.get('documents_restored_exactly')==restored)
ps_paths=[child_dir/n for n in ['baseline-ps.raw.txt','restored-ps.raw.txt','diagnostic-no-Trime-process.raw.txt',
                               'diagnostic-safe-before.raw.txt','diagnostic-safe-after.raw.txt']]
ps_paths.append(fixture_dir/'after-explicit-old-target-stop-ps.raw.txt')
ps=b.sampled_ps(ps_paths)
logs=q.log_audit(root);raw_log=(root/'device-logcat.txt').read_bytes();events=[];offset=0
for line_no,line in enumerate(raw_log.splitlines(keepends=True),1):
    if re.search(rb'\bam_anr\s*:|\bANR in\b|FATAL EXCEPTION|\bam_crash\s*:',line):
        relation='BEFORE_BEGIN' if offset<bookend['strictInterval']['firstByteInclusive'] else 'AFTER_END' if offset>=bookend['strictInterval']['lastByteExclusive'] else 'INSIDE_STRICT_INTERVAL'
        events.append(dict(line=line_no,byteOffset=offset,bytes=len(line),sha256=q.hashlib.sha256(line).hexdigest(),
                           relation=relation,raw=line.decode(errors='replace')))
    offset+=len(line)
systemui=[x for x in events if 'com.android.systemui' in x['raw']]
installed_diag=q.meta(child_dir/'diagnostic-installed-apk.bin')
identities=dict(frozenBindingActual=q.sha(Path(operator['binding_path']))=='f771761117ce350889251162499fa4821ce9d50164cddd4c0c341dc62acad00a',
    wrapperActual=q.sha(ROOT/'runtime-plan5/run-isolated-probes.py')=='3c557910050dc7269e86ad55716ff8e0447615c73cb37170091c21c62d936884',
    installedDiagnosticReadbackMatchesOriginal=installed_diag['sha256']==q.DIAG,
    frozenInputs=q.input_audit(binding))
report=dict(createdUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),reviewer='candidate_review_prepare HOST independent',
    sourceHead=q.HEAD,sourceSha256=q.SOURCE,overallQualified=False,disposition='NOT_QUALIFIED_ZERO_STAGE_READY_PRECONDITION_FAILURE',
    productSourceRejected=False,productSourcePassed=False,originalProductionPhase='NOT_RUN',
    script=q.meta(Path(__file__).resolve()),helperScripts={x:q.meta(OUT/x) for x in ('qualify.py','qualify_fixture.py','bookend_rules.py')},
    originalInputs={x:q.meta(root/x) for x in ('fixture1/report.json','operator1/report.json','completion.json')},
    failedPhase=dict(name='diagnostic-safe',notProductionApk=True,actualInstrumentationCodes=codes,
      originalReport=q.meta(raw_report),actualReport=actual,instrumentationStdout=q.meta(stdout_path),
      rawTar=q.meta(archive),rawTarFiles=tar_files,checks=phase_truth,qualifiedAsObservedFailure=all(phase_truth.values()),
      missingIdentityFields=missing,selfReportedProbePID='NOT_RECORDED',osProbeStarttime='NOT_OBSERVED',
      readyPredicate='owner.hasWindowFocus() && owner.actual.isShown() && owner.actual.getWidth() > 0',
      predicateComponentsNotIndividuallyObserved=True,uniqueRootCauseNotEstablished=True),
    remainingPhases=dict(diagnostic_equal_return='NOT_RUN',diagnostic_no_super_measure='NOT_RUN',production_class_only='NOT_RUN'),
    identityMetadata=identities,installedDiagnosticReadback=installed_diag,
    markerCommands=dict(begin=f.command(fixture['begin_command'],begin),end=f.command(fixture['end_command'],end)),
    exactLogBookend=bookend,strictActualPsSamples=ps,
    documentsStateIndependent=dict(checks=docs_checks,restorationQualified=all(docs_checks.values()),
                                   before=state_before,isolated=state_isolated,restored=state_restored),
    childStateRestoreDeclared=operator.get('restored_exactly'),
    actualChildExit=fixture.get('child_actual_exit'),actualFixtureExit=fixture.get('actual_exit'),
    actualOperatorFailure=operator.get('operator_failure'),fixtureCleanupErrors=fixture.get('cleanup_errors'),
    terminalEvidence=terminal,actualVMExit=completion.get('emulator_exit'),actualCollectorExit=completion.get('collector_exit'),
    actualControllerReason=completion.get('reason'),controllerCleanupErrors=completion.get('cleanup_errors'),
    fullRawLog=logs,actualAnrCrashEventLines=events,systemUiAnrPrecededStrictBegin=bool(systemui) and all(x['relation']=='BEFORE_BEGIN' for x in systemui),
    scope='Observed failed external namespace diagnostic, 0/13 stages, no original production APK class outcome. Fixture/SDK/VM cleanup evidence is separate from business qualification; collector actual255 remains255. Prior SystemUI ANR establishes temporal ordering only, not the unique cause of the three-part ready guard failure.',
    allWarningsRetained=True,allWarningsEliminated=False,humanImageReviewClaimed=False,
    physicalDevice='NOT_RUN',TalkBack='NOT_RUN',nativeCandidateIntegration='NOT_RUN',noFurtherRuntimeRequestedByRoot=True)
with output.open('x') as out:json.dump(report,out,indent=2,ensure_ascii=False);out.write('\n')
print(json.dumps(dict(path=str(output),sha256=q.sha(output),overallQualified=False,
    observedFailureIntegrity=report['failedPhase']['qualifiedAsObservedFailure'],docsRestore=report['documentsStateIndependent']['restorationQualified'],
    actualVMExit=report['actualVMExit'],actualCollectorExit=report['actualCollectorExit'],priorityCounts=logs['priorities'],
    systemUiAnrPrecededBegin=report['systemUiAnrPrecededStrictBegin']),ensure_ascii=False))
