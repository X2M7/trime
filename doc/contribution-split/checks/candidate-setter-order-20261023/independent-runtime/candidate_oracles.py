"""HOST-only reused oracles, not a reuse of previous execution results.
Function source bytes are preserved from the prior independent verifier.
Actual APK digest is configured from a new externally frozen binding by main.
"""
import datetime, hashlib, json, re
from pathlib import Path
from PIL import Image
ROOT = Path('/home/xumin/Public/Trime/build/pr2187-settext-order-20261023')
OUT = ROOT / 'independent-runtime'
HEAD = '7f909060a1d42198e749eb1f9a66399f76bf8710'
SOURCE = 'c92ded471c62aa2a773510784237d60ac822d831bda9298929a6f99b5d3db8c1'
APK = None
DIAG = None


NAMES = ['constructor','hanzi','latin','null','rebind','buffer-normal','buffer-spannable',
         'buffer-editable','buffer-back-normal','buffer-repeat-normal','buffer-repeat-spannable',
         'granularity-selectable-precondition','granularity-line-next-previous']

TYPES = {'buffer-normal':['NORMAL'], 'buffer-spannable':['NORMAL','SPANNABLE'],
         'buffer-editable':['NORMAL','SPANNABLE','EDITABLE'],
         'buffer-back-normal':['NORMAL','SPANNABLE','EDITABLE','NORMAL'],
         'buffer-repeat-normal':['NORMAL','NORMAL'],
         'buffer-repeat-spannable':['NORMAL','SPANNABLE','SPANNABLE']}

def sha(p):
    with p.open('rb') as f: return hashlib.file_digest(f,'sha256').hexdigest()

def meta(p):
    a = p.stat(); digest = sha(p); b = p.stat()
    return dict(path=str(p), bytes=b.st_size, sha256=digest, inode=b.st_ino,
                unchangedDuringHash=(a.st_size,a.st_mtime_ns,a.st_ino)==(b.st_size,b.st_mtime_ns,b.st_ino))

def load(p): return json.loads(p.read_bytes())

def ink(im, box):
    image = im.crop(tuple(box)).convert('RGBA')
    try: return sum(1 for r,g,b,a in image.get_flattened_data() if a>=128 and r+g+b<=162)
    finally: image.close()

def type_truth(snapshot, expected):
    if snapshot.get('text') != '你': return False
    s,e = snapshot.get('spannable'), snapshot.get('editable')
    return {'NORMAL':s is False and e is False, 'SPANNABLE':s is True and e is False,
            'EDITABLE':e is True and s is True}[expected]

def expected(name):
    return '' if name in ('constructor','null') else 'ni' if name=='latin' else '你好' if name.startswith('granularity-') else '你'

def buffer_truth(value, name):
    calls = value.get('calls',[])
    wanted = TYPES[name]
    results=[]
    for i,call in enumerate(calls):
        kind=call.get('requestedType')
        truthful = (i<len(wanted) and kind==wanted[i] and call.get('sameContentBeforeSet') is True
                    and type_truth(call.get('immediateAfterSet',{}), kind)) if kind in ('NORMAL','SPANNABLE','EDITABLE') else False
        results.append(dict(index=i,requested=kind, independentlyPassed=truthful,
                            reportedPassed=call.get('textAndTypePassed'), snapshot=call.get('immediateAfterSet')))
    return dict(passed=len(calls)==len(wanted) and all(x['independentlyPassed'] and x['reportedPassed'] is True for x in results)
                and value.get('passed') is True, calls=results, expectedRequests=wanted)

def action_truth(v):
    checks = dict(focus=v.get('requestFocusReturned') is True and v.get('focusedBeforeActions') is True,
                  initialZero=v.get('initialSelectionStart')==0 and v.get('initialSelectionEnd')==0,
                  nextTrueAndAtTwo=v.get('nextReturned') is True and v.get('nextSelectionStart')==v.get('nextSelectionEnd')==2,
                  previousTrueAndAtZero=v.get('previousReturned') is True and v.get('previousSelectionStart')==v.get('previousSelectionEnd')==0,
                  actionsAdvertised=256 in v.get('advertisedActionIds',[]) and 512 in v.get('advertisedActionIds',[]),
                  lineAdvertised=v.get('lineAdvertised') is True and bool(v.get('movementGranularities',0)&4),
                  beforeAndAfterText=all(v.get(k,{}).get('text')=='你好' and v.get(k,{}).get('spannable') is True
                                         and v.get(k,{}).get('editable') is False for k in ('before','after')),
                  reportedPassed=v.get('passed') is True)
    return dict(passed=all(checks.values()),checks=checks,observed=v)

def precondition_truth(v):
    checks = dict(text=v.get('text')=='你好', spannable=v.get('spannable') is True and v.get('editable') is False,
                  selectable=v.get('textIsSelectable') is True,focusable=v.get('focusable') is True,
                  touchFocus=v.get('focusableInTouchMode') is True,movement=bool(v.get('movementMethodClass')),
                  reportedPassed=v.get('passed') is True)
    return dict(passed=all(checks.values()),checks=checks,observed=v)

def stage_audit(stage,folder):
    name=stage['name']; text=expected(name); checks=[]
    def check(k,b): checks.append(dict(check=k,passed=bool(b)))
    screen_path=folder/(name+'-screen.png')
    check('hardwarePngHash',sha(screen_path)==stage['screenSha256'])
    check('onDrawBeforeCapture',stage['bindUptimeMillis']<=stage['observedOnDrawUptimeMillis']<=stage['hardwareCaptureUptimeMillis'])
    check('atLeast1000msAfterObservedDraw',stage['hardwareCaptureUptimeMillis']-stage['observedOnDrawUptimeMillis']>=1000)
    check('declaredSettleUnchanged',stage['minimumCompositorSettleMillis']==1000)
    labels={}
    with Image.open(screen_path) as screen:
        if screen.format!='PNG': raise ValueError('Not PNG')
        screen.load()
        for label in ('production','reference'):
            v=stage[label]; p=folder/(name+'-'+label+'.png')
            check(label+'SoftwarePngHash',sha(p)==v['softwareSha256'])
            box=v['bounds']; valid=(len(box)==4 and 0<=box[0]<box[2]<=screen.width and 0<=box[1]<box[3]<=screen.height)
            check(label+'InScreenBounds',valid)
            with Image.open(p) as sw:
                if sw.format!='PNG': raise ValueError('Not PNG')
                sw.load(); sw_count=ink(sw,(0,0,sw.width,sw.height))
                check(label+'SoftwareSize',sw.size==(box[2]-box[0],box[3]-box[1]))
            screen_count=ink(screen,box) if valid else -1
            glyph=(screen_count==0 and sw_count==0) if text=='' else screen_count>0 and sw_count>0 and screen_count>=max(1,sw_count//4)
            text_layout=v.get('text')==text and v.get('frameworkLayoutText')==text
            before_after={k:v.get(k) for k in ('textBeforeAccessibilityNode','frameworkLayoutTextBeforeAccessibilityNode',
                'textAfterAccessibilityNode','frameworkLayoutTextAfterAccessibilityNode')}
            # Preserve the actual producer oracle: it asserts final text/Layout, not every recorded pre-node field.
            accessible=v.get('accessibilityText')==text
            truth=dict(textAndLayout=text_layout,accessibility=accessible,glyph=glyph)
            producer=dict(textAndLayout=v.get('textAndLayoutPassed'),accessibility=v.get('accessibilityPassed'),glyph=v.get('glyphPassed'))
            check(label+'InkCountsMatch',screen_count==v['screenInk'] and sw_count==v['softwareInk'])
            # Failed negative controls may legitimately report false. Check oracle honesty separately from pass.
            check(label+'ReportedOracleMatchesIndependent',all(producer[k] is truth[k] for k in truth))
            labels[label]=dict(passed=all(truth.values()),oracle=truth,reportedOracle=producer,
                               screenInk=screen_count,softwareInk=sw_count,expectedText=text,
                               observedBeforeAfter=before_after,beforeAndAfterAllExpected=all(x==text for x in before_after.values()))
    mutation={}
    if name in TYPES:
        mutation={label:buffer_truth(stage['mutation'][label],name) for label in ('production','reference')}
    elif name=='granularity-selectable-precondition':
        mutation={label:precondition_truth(stage['mutation'][label]) for label in ('production','reference')}
    elif name=='granularity-line-next-previous':
        mutation={label:action_truth(stage['mutation'][label]) for label in ('production','reference')}
    if mutation:
        check('MutationAggregateTruth',stage['mutation'].get('passed') is all(x['passed'] for x in mutation.values()))
    independent=all(x['passed'] for x in checks) and all(x['passed'] for x in labels.values()) and all(x['passed'] for x in mutation.values())
    return dict(name=name,independentlyPassed=independent,reportedPassed=stage.get('passed'),integrityChecks=checks,
                labels=labels,mutation=mutation,reportMatchesIndependent=stage.get('passed') is independent)

def phase_audit(round_path,record,sdk,binding):
    name=record['name']; folder=round_path/'operator1'/name; variant=record.get('variant')
    result=dict(name=name,variant=variant,nonProductionDiagnostic=bool(variant),rawPhaseRecord=meta(folder/'phase-record.json'))
    stdout=(folder/'instrumentation.stdout.txt').read_text() if (folder/'instrumentation.stdout.txt').exists() else ''
    result['instrumentationStdout']=meta(folder/'instrumentation.stdout.txt') if stdout else None
    codes=re.findall(r'^INSTRUMENTATION_CODE:\s*(-?\d+)\s*$',stdout,re.M)
    result['actualInstrumentationCodes']=[int(x) for x in codes]
    if not record.get('original_folder'):
        result.update(disposition='UNQUALIFIED_NO_EXPORTED_REPORT',positiveQualified=False,
            noBufferTypeOrLayoutOutcomeClaimed=True,rawFailure=record.get('failure'))
        return result
    original=Path(record['original_folder']); report=load(original/'result.json'); issues=[]
    def require(k,b):
        if not b: issues.append(k)
    result['originalReport']=meta(original/'result.json')
    require('phase report digest',sha(original/'result.json')==record.get('result_sha256'))
    wanted_apk=DIAG if variant else APK
    require('original exact APK hash',report.get('sourceApkSha256')==wanted_apk)
    require('SDK',report.get('sdk')==sdk)
    require('13 expected stages declared',report.get('expectedStageCount')==13)
    require('original five order',report.get('originalFiveStageNames')==NAMES[:5])
    require('hardware captured before software declared',report.get('hardwareCapturedBeforeSoftwareDraw') is True)
    require('no TalkBack/native integration claim',report.get('talkBackServiceTested') is False and report.get('nativeCandidateIntegrationTested') is False)
    if variant:
        require('nonproduction diagnostic identity',report.get('notProductionApk') is True and report.get('diagnosticVariant')==variant)
        require('diagnostic source input',report.get('originalProductionSourceSha256')==SOURCE)
        require('variant source input',report.get('diagnosticClassSourceSha256')==binding['diagnostic_variants'][variant]['sourceSha256'])
    else:
        require('production report not diagnostic',not report.get('notProductionApk',False) and not report.get('diagnosticVariant'))
        require('production installed source path',bool(re.fullmatch(r'/data/app/(?:[^/]+/)?com\.osfans\.trime\.debug[^/]*/(?:base|split[^/]+)\.apk',report.get('sourceApk',''))))
    require('instrumentation code matches actual report',len(codes)==1 and int(codes[0])==(-1 if report.get('passed') else 0))
    encoded=re.findall(r'^INSTRUMENTATION_RESULT: report=(\{[^\r\n]*\})\s*$',stdout,re.M)
    require('device-exported report equals instrumentation JSON',len(encoded)==1 and json.loads(encoded[0])==report)
    stages=report.get('stages',[])
    require('stage order exact prefix', [x.get('name') for x in stages]==NAMES[:len(stages)] and len(stages)<=13)
    actual_files={str(p.relative_to(original)):dict(bytes=p.stat().st_size,sha256=sha(p)) for p in original.rglob('*') if p.is_file()}
    recorded={x['path']:dict(bytes=x['bytes'],sha256=x['sha256']) for x in record.get('files',[])}
    require('all original exported files match phase manifest',actual_files==recorded)
    actual_pngs=sorted(x for x in actual_files if x.endswith('.png'))
    require('PNG set exact for actual stage prefix',actual_pngs==sorted(x['name']+'-'+label+'.png' for x in stages for label in ('screen','production','reference')))
    reviewed=[stage_audit(x,original) for x in stages]
    require('all stage reports independently truthful',all(x['reportMatchesIndependent'] and all(c['passed'] for c in x['integrityChecks']) for x in reviewed))
    allpass=len(stages)==13 and all(x['independentlyPassed'] for x in reviewed)
    require('aggregate report truth',report.get('passed') is allpass)
    result.update(actualStageCount=len(stages),actualPngCount=len(actual_pngs),stages=reviewed,issues=issues,
                  sourceApkSha256=report.get('sourceApkSha256'),sourceApkPath=report.get('sourceApk'),selfReportedPid=report.get('pid'),
                  osIdentity=record.get('actual_probe_process_identity'),nativeFalseNotProcessEvidence=True,
                  fileManifestIndependent=actual_files)
    if variant in (None,'safe'):
        result.update(positiveQualified=not issues and allpass and len(actual_pngs)==39,
                      disposition='QUALIFIED_SCOPED_POSITIVE' if not issues and allpass and len(actual_pngs)==39 else 'UNQUALIFIED_POSITIVE')
    elif allpass and not issues:
        result.update(positiveQualified=False,disposition='NEGATIVE_NOT_DISTINGUISHED_13_OF_13')
    else:
        actual_type_fail=[s['name'] for s in reviewed if s['name'] in TYPES and not s['mutation']['production']['passed']
                         and s['mutation']['reference']['passed'] and s['labels']['reference']['passed']]
        actual_layout_fail=[s['name'] for s in reviewed if not s['labels']['production']['oracle']['textAndLayout']
                            and s['labels']['reference']['passed'] and s['labels']['production']['oracle']['accessibility']]
        result.update(positiveQualified=False,sameStageActualTypeFailuresWithReferencePass=actual_type_fail,
                      sameStageActualLayoutFailuresWithReferencePass=actual_layout_fail,
                      disposition='ACTUAL_DIAGNOSTIC_TYPE_DISCRIMINATION' if variant=='equal_return' and actual_type_fail and not issues
                      else 'ACTUAL_DIAGNOSTIC_LAYOUT_DISCRIMINATION' if variant=='no_super_measure' and actual_layout_fail and not issues
                      else 'UNQUALIFIED_DIAGNOSTIC_OUTCOME')
    return result

def terminal_audit(round_path,operator):
    completion=round_path/'completion.json'
    if not completion.exists(): return dict(state='PENDING',qualified=False)
    c=load(completion); baseline=load(round_path/'operator1'/'baseline.json'); restored=load(round_path/'operator1'/'restored.json')
    checks=dict(normalStop=c.get('normal_stop',{}).get('exit')==0,emulatorExit0=c.get('emulator_exit')==0,
        collectorExit0=c.get('collector_exit')==0,noControllerCleanupErrors=c.get('cleanup_errors')==[],
        operatorCleanupErrorsEmpty=operator.get('cleanup_errors')==[],restoredDeclared=operator.get('restored_exactly') is True,
        exactSampledStateRestored=all(baseline.get(k)==restored.get(k) for k in ('default','enabled','size','density','font','boot')),
        SDKUnchangedDeclared=c.get('original_sdk_unchanged') is True,sourceHead=c.get('source_head')==HEAD,
        originalApk=c.get('original_apk_sha256')==APK)
    before=load(round_path/'sdk-before.json'); after=load(round_path/'sdk-after.json')
    def sdk_records(v):
        if isinstance(v,list): return v
        for key in ('files','records','paths'):
            if key in v:return v[key]
        return v
    b,a=sdk_records(before),sdk_records(after)
    def normalize(v):
        if isinstance(v,list):return {x['path']:(x.get('bytes',x.get('size')),x.get('sha256')) for x in v}
        if isinstance(v,dict):return {k:(x.get('bytes',x.get('size')),x.get('sha256')) for k,x in v.items() if isinstance(x,dict) and 'sha256' in x}
        return None
    checks['actualSDKPathSizeHashMapsEqual']=normalize(a)==normalize(b) and bool(normalize(a))
    checks['SDKAfterReferencesExactBefore']=after.get('reference_sha256')==sha(round_path/'sdk-before.json')
    return dict(state='COMPLETE',qualified=all(checks.values()),checks=checks,
                inputs={x:meta(round_path/x) for x in ('completion.json','sdk-before.json','sdk-after.json')},
                sampledStateRestorationOnly=True,allWarningsEliminated=False)

def log_audit(round_path):
    p=round_path/'device-logcat.txt'
    if not p.exists():return dict(state='MISSING')
    m=meta(p); text=p.read_text(errors='replace'); lines=text.splitlines(True)
    starts=[]; diag=[]; fatals=[]; counts={k:0 for k in 'VDIWEF'}
    for i,line in enumerate(lines,1):
        match=re.match(r'^\d\d-\d\d\s+\S+\s+(\d+)\s+(\d+)\s+([VDIWEF])(?:\s|$)',line)
        if match:counts[match[3]]+=1
        if re.search(r'(?:Start proc|am_proc_start).*com\.osfans\.trime(?:\.debug)?(?:[/:\s},]|$)',line): starts.append(dict(line=i,raw=line))
        if ('Start proc' in line or 'am_proc_start' in line) and 'org.x2m7.trime.readiness.candidate' in line:diag.append(dict(line=i,raw=line))
        if 'FATAL EXCEPTION' in line:
            # Keep full adjacent stack context; never infer a buffer-stage result from a constructor crash.
            fatals.append(dict(line=i,rawContext=''.join(lines[max(0,i-2):min(len(lines),i+38)])))
    return dict(source=m,lineCount=len(lines),priorities=counts,actualTrimeStartRecords=starts,
                actualDiagnosticStartRecords=diag,fatalContexts=fatals,
                limits='Collected log and sampled ps only. Hardcoded native/Application false is not process proof. No all-system absence/zero-warning claim.')
