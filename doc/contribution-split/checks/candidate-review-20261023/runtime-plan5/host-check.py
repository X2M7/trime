"""HOST-only exact actual-dump parser check; no device actions."""
import ast,copy,hashlib,importlib.util,json,subprocess,sys
from pathlib import Path
HERE=Path(__file__).resolve().parent;OLD=HERE.parent/'runtime-plan4';OUT=HERE/'host-check1';assert not OUT.exists();OUT.mkdir();sys.path.insert(0,str(HERE))
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest();checks=[]
def check(name,value):checks.append(dict(name=name,passed=bool(value)));assert value,name
spec=importlib.util.spec_from_file_location('pr2187_plan5_host',HERE/'run-isolated-probes.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
for name in ('run-probes.py','runtime_common.py','audit-logs.py'):check(name+'-byte-exact',(HERE/name).read_bytes()==(OLD/name).read_bytes())
expected=(OLD/'run-api35.py').read_bytes().replace(b'runtime-plan4/frozen-runtime-binding.json',b'runtime-plan5/frozen-runtime-binding.json').replace(b'api35-class-only4',b'api35-class-only5')
check('controller-only-binding-and-OUT',(HERE/'run-api35.py').read_bytes()==expected)
a=ast.parse((OLD/'run-isolated-probes.py').read_text());b=ast.parse((HERE/'run-isolated-probes.py').read_text())
def find_users(tree):return next(n for n in ast.walk(tree) if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='users' for t in n.targets))
find_users(b).value=copy.deepcopy(find_users(a).value)
check('all-wrapper-AST-except-one-users-expression-identical',ast.dump(a,include_attributes=False)==ast.dump(b,include_attributes=False))
for p in HERE.glob('*.py'):compile(p.read_bytes(),str(p),'exec')
check('all-syntax',True)
actual35=HERE.parent/'api35-class-only4/fixture1/before-com.android.documentsui-package.raw.txt';dump=actual35.read_text()
state_line=next(line for line in dump.splitlines() if 'User 0:' in line and 'installed=' in line)
actual21=HERE.parent/'api21-class-only3/fixture1/before-com.android.documentsui-package.raw.txt'
cases=[('actual-api35-permission-User0',dump,'package:/system/priv-app/DocumentsUI/DocumentsUI.apk',True),('duplicate-true-state-reject',dump+'\n'+state_line+'\n','package:/system/priv-app/DocumentsUI/DocumentsUI.apk',False),('only-permissions-User0-reject',dump.replace(state_line,''),'package:/system/priv-app/DocumentsUI/DocumentsUI.apk',False),('actual-api21-regression',actual21.read_text(),'package:/system/app/DocumentsUI/DocumentsUI.apk',True),('hidden-API21-no-fabricated-path',actual21.read_text().replace('hidden=false','hidden=true'),'',True)]
results=[]
for label,data,path,accepted in cases:
 x=object.__new__(m.Fixture);x.out=OUT/label;x.out.mkdir();stderr=x.out/'stderr';stderr.write_text('');x.report=dict(commands=[])
 def shell(*args,**kwargs):
  x.report['commands'].append(dict(remote_returncode=0,stderr=str(stderr)))
  return path if args[:2]==('pm','path') else data
 x.shell=shell;error=None;value=None
 try:value=x.package('com.android.documentsui','state')
 except BaseException as e:error=str(e)
 check(label,(error is None)==accepted)
 if accepted and not path:check(label+'-explicit-absent-due-to-hidden',value['apk_path'] is None and value['apk_path_query']=='absent_due_to_hidden')
 results.append(dict(label=label,input_dump_sha256=hashlib.sha256(data.encode()).hexdigest(),accepted=accepted,value=value,error=error,HOST_only=True))
help_run=subprocess.run([sys.executable,str(HERE/'run-isolated-probes.py'),'--help'],capture_output=True);(OUT/'help.stdout').write_bytes(help_run.stdout);(OUT/'help.stderr').write_bytes(help_run.stderr);check('help-actual0-with-no-device',help_run.returncode==0)
bad=HERE.parent/'api35-class-only5-UNFROZEN-MUST-NOT-EXIST';argv=[sys.executable,str(HERE/'run-isolated-probes.py'),'--binding',str(HERE/'binding-template.json'),'--api','35','--output',str(bad),'--child-output',str(bad/'child')]
g=subprocess.run(argv,capture_output=True);(OUT/'guard.stdout').write_bytes(g.stdout);(OUT/'guard.stderr').write_bytes(g.stderr);check('actual-new-UNFROZEN-refusal-before-fixture',g.returncode!=0 and b'UNFROZEN' in g.stderr and not bad.exists())
m.frozen(OLD/'frozen-runtime-binding.json');check('all-old-plan4-frozen-inputs-unchanged',True)
f=OUT/'result.json';f.write_text(json.dumps(dict(host_only=True,no_VM_ADB_Gradle=True,passed=True,checks=checks,parser_cases=results,actual35_input=dict(path=str(actual35),sha256=sha(actual35)),actual21_input=dict(path=str(actual21),sha256=sha(actual21)),wrapper_sha256=sha(HERE/'run-isolated-probes.py'),guard_actual_exit=g.returncode),indent=2)+'\n');print(json.dumps(dict(passed=True,checks=len(checks),path=str(f),sha256=sha(f)),indent=2))
