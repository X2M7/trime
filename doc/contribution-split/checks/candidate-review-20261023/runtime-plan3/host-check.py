"""HOST-only fixture control-flow checks. All child/ADB calls below are stubs."""
import ast, hashlib, importlib.util, json, subprocess, sys
from pathlib import Path
HERE=Path(__file__).resolve().parent; OLD=HERE.parent/'runtime-plan2'
OUT=HERE/'host-check1';assert not OUT.exists();OUT.mkdir()
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
sys.path.insert(0,str(HERE))
spec=importlib.util.spec_from_file_location('pr2187_fixture_host_test',HERE/'run-isolated-probes.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
checks=[]
def check(name,value):checks.append(dict(name=name,passed=bool(value)));assert value,name
for name in ('run-probes.py','runtime_common.py','audit-logs.py'):
 check(name+'-byte-identical',(HERE/name).read_bytes()==(OLD/name).read_bytes())
for api in (21,35):
 name=f'run-api{api}.py'; expected=(OLD/name).read_bytes().replace(b'runtime-plan2/frozen-runtime-binding.json',b'runtime-plan3/frozen-runtime-binding.json').replace(b'-class-only2',b'-class-only3')
 check(name+'-only-binding-and-OUT',(HERE/name).read_bytes()==expected)
for p in HERE.glob('*.py'):compile(p.read_bytes(),str(p),'exec')
check('all-Python-syntax',True)
for extra in ('--help',):
 run=subprocess.run([sys.executable,str(HERE/'run-isolated-probes.py'),extra],capture_output=True)
 (OUT/'help.stdout').write_bytes(run.stdout);(OUT/'help.stderr').write_bytes(run.stderr)
 check('wrapper-help-no-device',run.returncode==0)
# Before binding creation, no real frozen guard may construct any device fixture.
guardout=HERE.parent/'api21-class-only3-UNFROZEN-MUST-NOT-EXIST'
run=subprocess.run([sys.executable,str(HERE/'run-isolated-probes.py'),'--binding',str(OLD/'binding-template.json'),'--api','21','--output',str(guardout),'--child-output',str(guardout/'child')],capture_output=True)
(OUT/'guard.stdout').write_bytes(run.stdout);(OUT/'guard.stderr').write_bytes(run.stderr)
check('UNFROZEN-refused-before-fixture-device',run.returncode!=0 and b'UNFROZEN' in run.stderr and not guardout.exists())
actual_fixture=m.Fixture; actual_frozen=m.frozen; actual_popen=m.subprocess.Popen; actual_argv=sys.argv[:]
results=[]
scenarios=[dict(name='aosp-default-success',present=['com.android.documentsui'],enabled=0,stopped=False,help='disable-user default-state',child_exit=0),dict(name='google-enabled-child-failure',present=['com.google.android.documentsui'],enabled=1,stopped=True,help='disable-user',child_exit=7),dict(name='missing-default-support',present=['com.android.documentsui'],enabled=0,stopped=False,help='disable-user',child_exit=None),dict(name='two-packages-reject',present=list(m.DOCS_PACKAGES),enabled=0,stopped=False,help='disable-user default-state',child_exit=None),dict(name='restore-failure',present=['com.android.documentsui'],enabled=0,stopped=False,help='disable-user default-state',child_exit=0,restore_fail=True)]
for scenario in scenarios:
 events=[]; folder=OUT/scenario['name']; state=dict(enabled=scenario['enabled'],stopped=scenario['stopped']);instance=[]
 class StubFixture:
  def __init__(self,args,binding):
   self.args=args;self.out=args.output;self.out.mkdir();self.report=dict(commands=[]);instance.append(self)
   self.empty=self.out/'empty-stderr';self.empty.write_text('')
  def persist(self):
   (self.out/'report.json').write_text(json.dumps(self.report,indent=2)+'\n')
  def ps(self,label):events.append(['ps',label]);return dict(target_rows=[])
  def package(self,pkg,label):
   events.append(['package',pkg,label]);return dict(package=pkg,present=pkg in scenario['present'],enabled=state['enabled'],stopped=state['stopped'])
  def shell(self,*args,**kwargs):
   events.append(list(args));self.report['commands'].append(dict(argv=list(args),remote_returncode=0,stderr=str(self.empty)))
   if args==('pm','help'):return scenario['help']
   if args[:2]==('pm','disable-user'):state['enabled']=3
   if args[:2] in (('pm','default-state'),('pm','enable')):
    if scenario.get('restore_fail'):raise RuntimeError('HOST stub actual restore failure')
    state['enabled']=0 if args[1]=='default-state' else 1
   return ''
  restore=actual_fixture.restore
 class StubChild:
  def __init__(self,argv,stdout,stderr):events.append(['Popen',argv]);self.pid=999999;self.returncode=None;stdout.write(b'HOST-only child stub\n')
  def wait(self,timeout=None):events.append(['child-wait']);self.returncode=scenario['child_exit'];return self.returncode
  def poll(self):return self.returncode
 m.Fixture=StubFixture;m.frozen=lambda _:dict(source_head='HOST-STUB');m.subprocess.Popen=StubChild
 sys.argv=['HOST-STUB','--binding',str(HERE/'binding-template.json'),'--api','21','--output',str(folder),'--child-output',str(folder/'child')]
 code=m.main();report=instance[0].report
 started=any(e[0]=='Popen' for e in events); changed=any(e[:2]==['pm','disable-user'] for e in events)
 check(scenario['name']+'-child-and-mutation-gates',started==(scenario['child_exit'] is not None) and changed==(scenario['child_exit'] is not None))
 if started:
  check(scenario['name']+'-END-after-actual-wait',next(i for i,e in enumerate(events) if e[0]=='child-wait')<next(i for i,e in enumerate(events) if e[0]=='log' and e[-1].startswith('END ')))
  if scenario.get('restore_fail'):check(scenario['name']+'-tool-failure',code==1 and report['cleanup_errors'])
  else:
   check(scenario['name']+'-exact-state-and-exit-preserved',state==dict(enabled=scenario['enabled'],stopped=scenario['stopped']) and code==scenario['child_exit'])
 else:check(scenario['name']+'-no-BEGIN-no-child',code==1 and not any(e[0]=='log' for e in events))
 results.append(dict(scenario=scenario,actual_exit=code,events=events,report=report,synthetic_host_only=True))
m.Fixture=actual_fixture;m.frozen=actual_frozen;m.subprocess.Popen=actual_popen;sys.argv=actual_argv
# Frozen plan2 including old APKs/probe is read-only revalidated; no device action.
m.frozen(OLD/'frozen-runtime-binding.json');check('plan2-all-frozen-inputs-unchanged',True)
result=dict(host_only=True,no_VM_ADB_Gradle=True,passed=True,checks=checks,scenarios=results,wrapper_sha256=sha(HERE/'run-isolated-probes.py'))
f=OUT/'result.json';f.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(dict(checks=len(checks),passed=True,path=str(f),sha256=sha(f)),indent=2))
