"""HOST-only AST and synthetic abort checks; never invokes ADB/VM."""
import ast
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
from types import SimpleNamespace

HERE=Path(__file__).resolve().parent
OLD=HERE.parent/'runtime-plan'
OUT=HERE/'host-check1'
assert not OUT.exists(); OUT.mkdir()
sha=lambda p: hashlib.sha256(Path(p).read_bytes()).hexdigest()
sys.path.insert(0,str(HERE))
spec=importlib.util.spec_from_file_location('pr2187_driver2_host_test',HERE/'run-probes.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
checks=[]
def check(name,value):
 checks.append(dict(name=name,passed=bool(value)));assert value,name
before=ast.parse((OLD/'run-probes.py').read_text());after=ast.parse((HERE/'run-probes.py').read_text())
after2=copy.deepcopy(after)
phase=next(n for n in ast.walk(after2) if isinstance(n,ast.FunctionDef) and n.name=='phase')
block=next(n for n in phase.body if isinstance(n,ast.Try)).body
idx=next(i for i,n in enumerate(block) if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='short_messages' for t in n.targets))
check('added-only-shortmessage-parse-and-abort-if',isinstance(block[idx+1],ast.If))
del block[idx:idx+2]
check('all-prior-runner-AST-including-finally-and-13-39-oracles-exact',ast.dump(before,include_attributes=False)==ast.dump(after2,include_attributes=False))
for name in ('runtime_common.py','audit-logs.py'):
 check(name+'-byte-identical',(OLD/name).read_bytes()==(HERE/name).read_bytes())
for api in (21,35):
 name=f'run-api{api}.py'
 expected=(OLD/name).read_bytes().replace(b'runtime-plan/frozen-runtime-binding.json',b'runtime-plan2/frozen-runtime-binding.json').replace(b'-class-only1',b'-class-only2')
 check(name+'-only-new-binding-and-OUT',(HERE/name).read_bytes()==expected)
for p in HERE.glob('*.py'):compile(p.read_bytes(),str(p),'exec')
check('all-tool-Python-syntax',True)
old_report=json.loads((HERE.parent/'api21-class-only1/operator1/report.json').read_text())
raw_command=next(c for c in old_report['commands'] if '-e variant equal_return ' in str(c.get('argv')))
actual=(HERE.parent/'api21-class-only1/operator1/diagnostic-equal_return/instrumentation.stdout.txt').read_text()
check('actual-abort-raw-original-hash',sha(raw_command['stdout'])==raw_command['stdout_sha256'])
class Fake:
 def __init__(self,label,output):
  self.out=OUT/label; self.out.mkdir();self.output=output;self.args=SimpleNamespace(api=21)
  self.report=dict(commands=[],phases=[],binding={});self.calls=[]
 def ps(self,label):self.calls.append(['ps',label]);return 'USER PID NAME\n'
 def persist(self):pass
 def shell(self,*args,**kwargs):
  self.calls.append(list(args))
  if args[:2]==('am','instrument'):
   self.report['commands'].append(copy.deepcopy(raw_command));return self.output
  assert args[:2]==('am','force-stop');return ''
cases=[('equal-return-abort','equal_return',actual,True),('no-measure-abort','no_super_measure',actual,True),
 ('safe-must-reject','safe',actual,False),('production-must-reject',None,actual,False),
 ('duplicate-code-reject','equal_return',actual+'\nINSTRUMENTATION_CODE: 0\n',False),
 ('duplicate-message-reject','equal_return',actual+'\nINSTRUMENTATION_RESULT: shortMsg=extra\n',False),
 ('missing-message-reject','equal_return','INSTRUMENTATION_CODE: 0\n',False),
 ('code-minus-one-reject','equal_return',actual.replace('INSTRUMENTATION_CODE: 0','INSTRUMENTATION_CODE: -1'),False),
 ('empty-directory-reject','equal_return',actual+'\nINSTRUMENTATION_RESULT: directory=\n',False)]
results=[]
for label,variant,output,allowed in cases:
 fake=Fake(label,output);error=None
 try:module.Journal.phase(fake,'test-phase',module.DIAG,'HOST-STUB-NOT-AN-APK',variant)
 except BaseException as failure:error=dict(type=type(failure).__name__,message=str(failure))
 rec=json.loads((fake.out/'test-phase/phase-record.json').read_text())
 check(label+'-decision',(error is None)==allowed)
 check(label+'-finally-stop-and-ps',fake.calls[-2:]==[['am','force-stop',module.DIAG],['ps','test-phase-after']])
 if allowed:
  check(label+'-no-invented-qualification',rec['state']=='CONTROL_ABORTED_UNQUALIFIED' and rec['independently_qualified'] is False and rec['actual_stage_count']=='NOT_EXPORTED' and rec['original_png_count']=='NOT_EXPORTED' and 'reported_passed' not in rec and 'result_sha256' not in rec)
  check(label+'-actual-raw-command-bound',rec['instrumentation_command']==raw_command)
 results.append(dict(label=label,variant=variant,expected_abort_continuation=allowed,error=error,record=rec,calls=fake.calls,synthetic_host_stub=True))
help_run=subprocess.run([sys.executable,str(HERE/'run-probes.py'),'--help'],capture_output=True)
(OUT/'help.stdout').write_bytes(help_run.stdout);(OUT/'help.stderr').write_bytes(help_run.stderr)
check('help-exits-zero-without-device',help_run.returncode==0)
guardout=HERE.parent/'api21-class-only2-UNFROZEN-MUST-NOT-EXIST'
guard=subprocess.run([sys.executable,str(HERE/'run-probes.py'),'--binding',str(HERE/'binding-template.json'),'--api','21','--output',str(guardout)],capture_output=True)
(OUT/'guard.stdout').write_bytes(guard.stdout);(OUT/'guard.stderr').write_bytes(guard.stderr)
check('unfrozen-rejected-before-Journal-and-device',guard.returncode!=0 and b'UNFROZEN' in guard.stderr and not guardout.exists())
module.frozen(OLD/'frozen-runtime-binding.json')
check('all-23-old-frozen-inputs-unchanged',True)
summary=dict(host_only=True,no_ADB_VM_Gradle=True,passed=True,checks=checks,cases=results,help_actual_exit=help_run.returncode,guard_actual_exit=guard.returncode,driver_sha256=sha(HERE/'run-probes.py'))
(OUT/'result.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(dict(passed=True,checks=len(checks),result=str(OUT/'result.json'),sha256=sha(OUT/'result.json')),indent=2))
