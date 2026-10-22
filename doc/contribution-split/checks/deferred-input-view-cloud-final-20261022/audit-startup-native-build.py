#!/usr/bin/env python3
"""Bind actual startup-fork native build-task evidence from all four terminal job logs."""
import hashlib, json, re, subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parent; OUT=ROOT/'startup-native-build-logs'
GH=Path('/home/xumin/Public/Trime/build/upstream19/gh-cli-2.102.0/gh')
REPO='X2M7/trime'; RUN=39723620625; HEAD='2d2fbce70b449ec8b6560c844ca5ddbc2e08a4c3'
PLATFORMS={'build (ubuntu-26.04)','build (macos-26)','build (macos-26-intel)','build (windows-2025)'}
ABIS={'arm64-v8a','armeabi-v7a','x86','x86_64'}
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''): h.update(b)
 return h.hexdigest()
def api(route,path):
 r=subprocess.run([str(GH),'api',route],capture_output=True,timeout=90,check=False)
 path.write_bytes(r.stdout); path.with_suffix(path.suffix+'.stderr').write_bytes(r.stderr)
 if r.returncode: raise RuntimeError(f'API {route} rc={r.returncode}')
 return json.loads(r.stdout)
def step(job,name):
 x=[s for s in job['steps'] if s['name']==name]; assert len(x)==1; return x[0]
OUT.mkdir(exist_ok=False)
run=api(f'repos/{REPO}/actions/runs/{RUN}',OUT/'run.json')
jobs=api(f'repos/{REPO}/actions/runs/{RUN}/jobs',OUT/'jobs.json')
assert run['head_sha']==HEAD and run['status']=='completed' and run['conclusion']=='success'
assert len(jobs['jobs'])==4 and {j['name'] for j in jobs['jobs']}==PLATFORMS
report={'schema':'trime-deferred-input-view-startup-native-build-evidence/v1','repository':REPO,'run':RUN,'source_head':HEAD,'jobs':[],'raw_api':[{'path':str(OUT/n),'sha256':sha(OUT/n)} for n in ('run.json','jobs.json')],'claim_boundary':{'four_abi_build_cmake_tasks_observed_per_os':True,'ninja_entry_observed_per_abi_per_os':True,'all_native_objects_recompiled':False,'reason':'The complete logs prove each ABI buildCMake task and Ninja invocation ran after a JNI cache miss. Ninja does not emit an object-by-object or link-command closure here, so the report does not claim that every native object was rebuilt.'}}
for job in jobs['jobs']:
 assert job['head_sha']==HEAD and job['status']=='completed' and job['conclusion']=='success'
 assert step(job,'Fetch JNI cache')['conclusion']=='success' and step(job,'Fetch submodules')['conclusion']=='success' and step(job,'Build debug Trime')['conclusion']=='success' and step(job,'Add JNI cache')['conclusion']=='success' and step(job,'Upload APK artifact')['conclusion']=='success'
 label=job['name'].removeprefix('build (').removesuffix(')')
 raw=OUT/f'{label}.original-job.log'; err=OUT/f'{label}.original-job.log.stderr'
 r=subprocess.run([str(GH),'api','--allow-escape-sequences',f'repos/{REPO}/actions/jobs/{job["id"]}/logs'],capture_output=True,timeout=180,check=False)
 raw.write_bytes(r.stdout); err.write_bytes(r.stderr)
 if r.returncode: raise RuntimeError(f'job log {job["id"]} rc={r.returncode}')
 text=r.stdout.decode(errors='replace'); lines=text.splitlines()
 assert HEAD in text and '> Task :app:assembleDebug' in text and 'BUILD SUCCESSFUL' in text
 miss=re.findall(r'Cache not found for input keys:\s*(\S+)',text); saved=re.findall(r'Cache saved with key:\s*(\S+)',text)
 assert len(miss)==1 and len(saved)==1 and miss[0]==saved[0]
 task_lines={}
 for abi in ABIS:
  matches=[line for line in lines if re.search(r'> Task :app:buildCMakeDebug\['+re.escape(abi)+r'\](?:\s*)$',line)]
  assert len(matches)==1,(job['name'],abi,matches)
  task_lines[abi]=matches[0]
 ninja_lines={}
 for abi in ABIS:
  matches=[line for line in lines if 'ninja: Entering directory' in line and re.search(r'(?:/|\\)'+re.escape(abi)+r"(?:'|`|/|\\|$)",line)]
  assert len(matches)==1,(job['name'],abi,matches[:5])
  ninja_lines[abi]=matches[0]
 explicit=[line for line in lines if re.search(r'\b(?:Building (?:C|CXX|ASM) object|Linking (?:C|CXX) (?:shared|static) library)\b',line)]
 report['jobs'].append({'id':job['id'],'name':job['name'],'started_at':job['started_at'],'completed_at':job['completed_at'],'conclusion':'success','raw_log':str(raw),'raw_log_bytes':raw.stat().st_size,'raw_log_sha256':sha(raw),'raw_log_stderr_sha256':sha(err),'jni_cache_result':'miss_then_saved','jni_cache_key':miss[0],'build_cmake_task_lines':task_lines,'ninja_entry_lines':ninja_lines,'explicit_building_or_linking_line_count':len(explicit),'explicit_building_or_linking_lines':explicit,'assemble_debug_and_workflow_step_success':True,'original_warnings_retained_in_full_log':True})
assert len(report['jobs'])==4 and all(set(j['build_cmake_task_lines'])==ABIS and set(j['ninja_entry_lines'])==ABIS for j in report['jobs'])
report['passed']=True
report['limits']=['This report preserves each complete original job log, including all compiler and dependency warnings; it makes no zero-warning claim.','Cache miss/save is recorded as a workflow fact and is not used alone to infer compilation.','Actual buildCMake task and Ninja-entry records support four-ABI native task execution; absent object-by-object/link closure is not invented.','Cloud build evidence does not replace device qualification.']
p=ROOT/'startup-native-build-evidence.json'; p.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'passed':True,'jobs':4,'abi_tasks':16,'report':str(p),'sha256':sha(p),'explicit_build_link_lines':sum(j['explicit_building_or_linking_line_count'] for j in report['jobs'])}))
