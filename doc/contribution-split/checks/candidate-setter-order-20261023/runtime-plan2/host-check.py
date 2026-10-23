"""HOST-only reviewed/actual merge identity checks; fixture payloads are synthetic."""
import ast,copy,hashlib,importlib.util,json,subprocess,sys
from pathlib import Path
HERE=Path(__file__).resolve().parent;OLD=HERE.parent/'runtime-plan1';OUT=HERE/'host-check1';assert not OUT.exists();OUT.mkdir();sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest();checks=[]
def check(name,value):checks.append(dict(name=name,passed=bool(value)));assert value,name
for name in ('run-probes.py','run-isolated-probes.py','audit-logs.py'):check(name+'-byte-exact',(HERE/name).read_bytes()==(OLD/name).read_bytes())
expected=(OLD/'run-api21.py').read_bytes().replace(b"OUT = ROOT / 'api21-production1'",b"OUT = ROOT / 'api21-production-merge1'").replace(b"BINDING = ROOT / 'runtime-plan1/frozen-runtime-binding.json'",b"BINDING = ROOT / 'runtime-plan2/frozen-runtime-binding.json'")
check('controller-only-new-path-and-OUT',(HERE/'run-api21.py').read_bytes()==expected)
for p in HERE.glob('*.py'):compile(p.read_bytes(),str(p),'exec')
check('all-syntax',True)
spec=importlib.util.spec_from_file_location('pr2187_merge_identity_HOST',HERE/'runtime_common.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
check('expected-reviewed-vs-build-trees-are-distinct',m.REVIEWED_TREE!=m.BUILD_TREE and m.HEAD!=m.BUILD_HEAD)
template=json.loads((HERE/'binding-template.json').read_text())
synthetic_apk=OUT/'HOST_SYNTHETIC_NOT_AN_APK.bin';synthetic_apk.write_bytes(b'HOST identity checker payload, NOT an Android APK or device result\n')
base=copy.deepcopy(template);base.update(state='FROZEN_PR2187_ORIGINAL_CLOUD_AND_DIAGNOSTIC',original_apk_path=str(synthetic_apk),original_apk_sha256=sha(synthetic_apk),frozen_files=[])
audit=dict(passed=True,source_head=m.BUILD_HEAD,source_tree=m.BUILD_TREE,actual_class_source_sha256=m.SOURCE,apks=[dict(abi='x86_64',sha256=sha(synthetic_apk),buildconfig_commit_hash=m.BUILD_HEAD)],HOST_SYNTHETIC_NOT_ARTIFACT_QUALIFICATION=True)
cases=[('correct-distinct-tree-same-class',{}, {},True),('reject-fork-as-actual-APK',dict(actual_build_commit=m.HEAD),{},False),('reject-merge-as-reviewed-head',dict(source_head=m.BUILD_HEAD),{},False),('reject-whole-tree-equivalence-label',dict(whole_tree_equivalent=True),{},False),('reject-reviewed-tree-as-build-tree',dict(source_tree=m.REVIEWED_TREE),{},False),('reject-audit-reviewed-head-as-build',{},dict(source_head=m.HEAD),False),('reject-class-source-mismatch',{},dict(actual_class_source_sha256='0'*64),False),('reject-APK-BuildConfig-reviewed-head',{},dict(apks=[dict(abi='x86_64',sha256=sha(synthetic_apk),buildconfig_commit_hash=m.HEAD)]),False),('reject-fork-producer-label',dict(expected_producer=dict(repository='X2M7/trime',run=39889136561)),{},False),('reject-legacy-field-as-actual-label',dict(legacy_operator_source_head_semantics='actual_build_commit'),{},False)]
results=[]
for name,changes,audit_changes,accepted in cases:
 b=copy.deepcopy(base);b.update(changes);a=copy.deepcopy(audit);a.update(audit_changes);folder=OUT/name;folder.mkdir();ap=folder/'synthetic-audit.json';ap.write_text(json.dumps(a,indent=2)+'\n');b.update(original_artifact_audit_path=str(ap),original_artifact_audit_sha256=sha(ap));bp=folder/'synthetic-binding.json';bp.write_text(json.dumps(b,indent=2)+'\n');error=None
 try:m.frozen(bp)
 except BaseException as e:error=dict(type=type(e).__name__,message=str(e))
 check(name,(error is None)==accepted);results.append(dict(name=name,expected_identity_acceptance=accepted,error=error,binding_sha256=sha(bp),audit_sha256=sha(ap),HOST_SYNTHETIC_NOT_DEVICE_OR_APK_ACCEPTANCE=True))
help_run=subprocess.run([sys.executable,str(HERE/'run-isolated-probes.py'),'--help'],capture_output=True);(OUT/'help.stdout').write_bytes(help_run.stdout);(OUT/'help.stderr').write_bytes(help_run.stderr);check('help-actual0',help_run.returncode==0)
bad=HERE.parent/'api21-upstream-UNFROZEN-MUST-NOT-EXIST';argv=[sys.executable,str(HERE/'run-isolated-probes.py'),'--binding',str(HERE/'binding-template.json'),'--api','21','--output',str(bad),'--child-output',str(bad/'child')];x=subprocess.run(argv,capture_output=True);(OUT/'guard.stdout').write_bytes(x.stdout);(OUT/'guard.stderr').write_bytes(x.stderr);check('actual-UNFROZEN-before-device',x.returncode!=0 and b'UNFROZEN' in x.stderr and not bad.exists())
f=OUT/'result.json';f.write_text(json.dumps(dict(host_only=True,no_VM_ADB_APK_compilation=True,passed=True,checks=checks,identity_controls=results,synthetic_controls_are_not_real_APK_or_device_tests=True,expected_reviewed_head=m.HEAD,expected_reviewed_tree=m.REVIEWED_TREE,expected_actual_build=m.BUILD_HEAD,expected_actual_tree=m.BUILD_TREE,whole_tree_equivalent=False,actual_artifact_audit='PENDING_ROOT'),indent=2)+'\n');print(json.dumps(dict(passed=True,checks=len(checks),path=str(f),sha256=sha(f)),indent=2))
