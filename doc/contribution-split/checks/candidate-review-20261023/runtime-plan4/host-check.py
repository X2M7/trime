"""HOST-only plan4 regressions; all device/child operations are substituted stubs."""
import ast,hashlib,importlib.util,json,subprocess,sys
from pathlib import Path
HERE=Path(__file__).resolve().parent;OLD=HERE.parent/'runtime-plan3';OUT=HERE/'host-check1'
assert not OUT.exists();OUT.mkdir();sys.path.insert(0,str(HERE))
spec=importlib.util.spec_from_file_location('pr2187_plan4_host',HERE/'run-isolated-probes.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest();checks=[]
def check(name,value):checks.append(dict(name=name,passed=bool(value)));assert value,name
for name in ('run-probes.py','runtime_common.py','audit-logs.py'):check(name+'-byte-exact',(HERE/name).read_bytes()==(OLD/name).read_bytes())
for api in (21,35):
 name=f'run-api{api}.py';expected=(OLD/name).read_bytes().replace(b'runtime-plan3/frozen-runtime-binding.json',b'runtime-plan4/frozen-runtime-binding.json').replace(b'-class-only3',b'-class-only4')
 check(name+'-only-path-and-OUT',(HERE/name).read_bytes()==expected)
oldast=ast.parse((OLD/'run-isolated-probes.py').read_text());newast=ast.parse((HERE/'run-isolated-probes.py').read_text())
getfn=lambda tree,name:next(n for n in ast.walk(tree) if isinstance(n,ast.FunctionDef) and n.name==name)
for name in ('__init__','ps'):check('fixture-'+name+'-AST-exact',ast.dump(getfn(oldast,name),include_attributes=False)==ast.dump(getfn(newast,name),include_attributes=False))
oldtry=next(n for n in getfn(oldast,'main').body if isinstance(n,ast.Try));newtry=next(n for n in getfn(newast,'main').body if isinstance(n,ast.Try))
check('main-finally-and-real-wait-END-restoration-contract-AST-exact',ast.dump(ast.Module(body=oldtry.finalbody,type_ignores=[]),include_attributes=False)==ast.dump(ast.Module(body=newtry.finalbody,type_ignores=[]),include_attributes=False))
def from_stop(nodes):return nodes[next(i for i,n in enumerate(nodes) if isinstance(n,ast.Expr) and isinstance(n.value,ast.Call) and n.value.args and isinstance(n.value.args[0],ast.Constant) and n.value.args[0].value=='before-explicit-old-target-stop'):]
check('all-target-stop-BEGIN-child-exit-actions-exact',ast.dump(ast.Module(body=from_stop(oldtry.body),type_ignores=[]),include_attributes=False)==ast.dump(ast.Module(body=from_stop(newtry.body),type_ignores=[]),include_attributes=False))
for f in HERE.glob('*.py'):compile(f.read_bytes(),str(f),'exec')
check('all-syntax',True)
# Exercise the actual package parser with original API21 dumps and hidden mutations.
actual_dump=(HERE.parent/'api21-class-only3/fixture1/before-com.android.documentsui-package.raw.txt').read_text()
parser_cases=[('original-observed',actual_dump,'package:/system/priv-app/DocumentsUI/DocumentsUI.apk',True,'PRESENT'),('hidden-empty-path',actual_dump.replace('hidden=false','hidden=true'),'',True,'absent_due_to_hidden'),('unhidden-empty-reject',actual_dump,'',False,None),('wrong-package-reject',actual_dump.replace('Package [com.android.documentsui]','Package [com.google.android.documentsui]'),'',False,None)]
parser_results=[]
for label,dump,path,accepted,path_state in parser_cases:
 obj=object.__new__(m.Fixture);obj.out=OUT/label;obj.out.mkdir();stderr=obj.out/'empty-stderr';stderr.write_text('');obj.report=dict(commands=[])
 def shell(*args,**kwargs):
  obj.report['commands'].append(dict(remote_returncode=0,stderr=str(stderr)))
  return path if args[:2]==('pm','path') else dump
 obj.shell=shell;error=None;result=None
 try:result=obj.package('com.android.documentsui','snapshot')
 except BaseException as e:error=str(e)
 check(label+'-decision',(error is None)==accepted)
 if accepted:check(label+'-path-not-invented',result['apk_path_query']==path_state and (result['apk_path'] is None)==(not path))
 parser_results.append(dict(label=label,accepted=accepted,result=result,error=error,HOST_only=True))
actual_fixture=m.Fixture;actual_frozen=m.frozen;actual_popen=m.subprocess.Popen;actual_argv=sys.argv[:];results=[]
scenarios=[dict(name='api21-hide-unhide',api=21,enabled=0,stopped=False,help='pm hide [--user USER_ID] PACKAGE\npm unhide [--user USER_ID] PACKAGE',exit=0),dict(name='api35-default',api=35,enabled=0,stopped=False,help='disable-user default-state',exit=0),dict(name='api35-explicit-enabled-child7',api=35,enabled=1,stopped=True,help='disable-user',exit=7),dict(name='api21-missing-unhide',api=21,enabled=0,stopped=False,help='pm hide PACKAGE',exit=None),dict(name='api35-missing-default',api=35,enabled=0,stopped=False,help='disable-user',exit=None),dict(name='initial-hidden-refusal',api=21,enabled=0,stopped=False,hidden=True,help='pm hide PACKAGE\npm unhide PACKAGE',exit=None),dict(name='api21-stopped-change-reject',api=21,enabled=0,stopped=False,help='pm hide PACKAGE\npm unhide PACKAGE',exit=None,change_stopped=True)]
for sc in scenarios:
 events=[];state=dict(enabled=sc['enabled'],stopped=sc['stopped'],hidden=sc.get('hidden',False));folder=OUT/sc['name'];instances=[]
 class Stub:
  restore=actual_fixture.restore
  def __init__(self,args,binding):
   self.args=args;self.out=args.output;self.out.mkdir();self.report=dict(commands=[]);self.empty=self.out/'stderr';self.empty.write_text('');instances.append(self)
  def persist(self):(self.out/'report.json').write_text(json.dumps(self.report,indent=2)+'\n')
  def ps(self,label):events.append(['ps',label]);return dict(target_rows=[])
  def package(self,pkg,label):
   events.append(['package',pkg,label]);return dict(package=pkg,present=pkg=='com.android.documentsui',installed=True,apk_path=None if state['hidden'] else '/system/priv-app/DocumentsUI/DocumentsUI.apk',**state)
  def shell(self,*args,**kwargs):
   events.append(list(args));self.report['commands'].append(dict(argv=list(args),remote_returncode=0,stderr=str(self.empty)))
   if args in (('pm',),('pm','help')):return sc['help']
   if args[:2]==('pm','hide'):
    state['hidden']=True
    if sc.get('change_stopped'):state['stopped']=True
   if args[:2]==('pm','unhide'):state['hidden']=False
   if args[:2]==('pm','disable-user'):state['enabled']=3
   if args[:2] in (('pm','default-state'),('pm','enable')):state['enabled']=0 if args[1]=='default-state' else 1
   return ''
 class Child:
  def __init__(self,argv,stdout,stderr):events.append(['Popen']);self.pid=999999;self.returncode=None;stdout.write(b'HOST stub only\n')
  def wait(self,timeout=None):events.append(['child-wait']);self.returncode=sc['exit'];return self.returncode
  def poll(self):return self.returncode
 m.Fixture=Stub;m.frozen=lambda _:dict(source_head='HOST-STUB');m.subprocess.Popen=Child
 sys.argv=['HOST-STUB','--binding',str(HERE/'binding-template.json'),'--api',str(sc['api']),'--output',str(folder),'--child-output',str(folder/'child')]
 code=m.main();rec=instances[0].report;started=any(x[0]=='Popen' for x in events)
 check(sc['name']+'-child-gate',started==(sc['exit'] is not None))
 if started:
  check(sc['name']+'-exit-and-exact-restore',code==sc['exit'] and state==dict(enabled=sc['enabled'],stopped=sc['stopped'],hidden=False))
  check(sc['name']+'-END-after-wait',next(i for i,x in enumerate(events) if x[0]=='child-wait')<next(i for i,x in enumerate(events) if x[0]=='log' and x[-1].startswith('END ')))
 else:check(sc['name']+'-failed-with-no-BEGIN',code==1 and not any(x[0]=='log' for x in events))
 if sc.get('change_stopped'):check(sc['name']+'-restore-error-not-hidden',bool(rec['cleanup_errors']))
 results.append(dict(scenario=sc,actual_exit=code,events=events,report=rec,HOST_only=True))
m.Fixture=actual_fixture;m.frozen=actual_frozen;m.subprocess.Popen=actual_popen;sys.argv=actual_argv
run=subprocess.run([sys.executable,str(HERE/'run-isolated-probes.py'),'--help'],capture_output=True);(OUT/'help.stdout').write_bytes(run.stdout);(OUT/'help.stderr').write_bytes(run.stderr);check('help-exit0-no-device',run.returncode==0)
m.frozen(OLD/'frozen-runtime-binding.json');check('all-plan3-frozen-inputs-unchanged',True)
f=OUT/'result.json';f.write_text(json.dumps(dict(host_only=True,no_VM_ADB_Gradle=True,passed=True,checks=checks,package_parser_cases=parser_results,fixture_scenarios=results,wrapper_sha256=sha(HERE/'run-isolated-probes.py')),indent=2)+'\n');print(json.dumps(dict(checks=len(checks),passed=True,path=str(f),sha256=sha(f)),indent=2))
