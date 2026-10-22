#!/usr/bin/env python3
"""Prepare an explicit small-file payload manifest and compact cloud supplement summary."""
import hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parent
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''): h.update(b)
 return h.hexdigest()
def load(n): return json.loads((ROOT/n).read_text())
def rec(p):
 p=p.resolve(); return {'path':str(p.relative_to(ROOT.resolve())),'bytes':p.stat().st_size,'sha256':sha(p)}
four=load('four-platform-terminal-receipt.json'); native=load('startup-native-build-evidence.json'); apk=load('artifact-startup-ubuntu-12402467624/apk-audit/result.json')
service=load('service-cloud-diagnostics.json'); control=load('control-independent-audit.json'); combo=load('startup-t9-combination-equivalence.json')
assert four['passed'] and four['all_16_platform_jobs_success'] and native['passed'] and apk['passed'] and service['passed'] and control['passed'] and combo['passed']
assert len(native['jobs'])==4 and all(len(j['build_cmake_task_lines'])==4 and len(j['ninja_entry_lines'])==4 for j in native['jobs'])
assert len(apk['apks'])==4 and apk['submodule_pin_binding']['count']==20
summary={'schema':'trime-deferred-input-view-final-cloud-supplement-summary/v1','heads':{'startup':'2d2fbce70b449ec8b6560c844ca5ddbc2e08a4c3','t9_combo':'9f86bc8f1d11d65d2e22e437f111a965bc2faba5','split_control':'3ae8709db6c17b45e50e686a58e6d93228b3a398'},'terminal_runs':{k:{'repository':v['repository'],'run':v['run'],'head':v['head'],'event':v['event'],'conclusion':v['conclusion'],'jobs':[{'name':j['name'],'conclusion':j['conclusion'],'style':j['style'],'jni_cache':j['jni_cache'],'started_at':j['started_at'],'completed_at':j['completed_at']} for j in v['jobs']]} for k,v in four['runs'].items()},'all_16_platform_jobs_success':True,'startup_native_build':{'four_os_jobs_success':True,'four_abi_build_cmake_tasks_per_os':True,'four_abi_ninja_entries_per_os':True,'observed_task_pairs':sum(len(j['build_cmake_task_lines']) for j in native['jobs']),'observed_ninja_entries':sum(len(j['ninja_entry_lines']) for j in native['jobs']),'explicit_building_or_linking_line_count':sum(j['explicit_building_or_linking_line_count'] for j in native['jobs']),'all_native_objects_recompiled_claim':False,'raw_logs':[{'job':j['name'],'sha256':j['raw_log_sha256'],'bytes':j['raw_log_bytes'],'cache':j['jni_cache_result']} for j in native['jobs']]},'startup_ubuntu_apk_audit':{'artifact_id':apk['artifact']['id'],'original_zip_sha256':apk['original_zip_sha256'],'source_head':apk['source_head'],'source_tree':apk['source_tree'],'service_sha256':apk['source_service_sha256'],'certificate_sha256':apk['shared_debug_certificate_sha256'],'submodule_pins':apk['submodule_pin_binding']['count'],'apks':[{'abi':a['abi'],'sha256':a['sha256'],'bytes':a['bytes'],'elf_class':a['elf_class'],'elf_machine':a['elf_machine'],'buildconfig_commit_hash':a.get('buildconfig_commit_hash')} for a in apk['apks']]},'split_junit':{'startup':{'tests':301,'failures':0,'errors':0,'skipped':0},'t9':{'tests':330,'failures':0,'errors':0,'skipped':0},'receipt_sha256':sha(ROOT/'control-independent-audit.json')},'limits':['All complete startup native job logs are retained with their original compiler/dependency warnings; this supplement makes no global zero-warning claim.','The narrow changed-Service diagnostic audit found no warning/error naming TrimeInputMethodService.kt and records style/Kotlin/assemble success; it does not suppress unrelated warnings.','Four ABI buildCMake tasks and Ninja entries on each startup OS are observed. The logs do not expose an object-by-object/link-command closure, so no all-native-object rebuild claim is made.','The APK evidence covers original Debug cloud artifacts and does not establish Release publication or device behavior.','Cloud CI and APK audits supplement, but do not replace, Android runtime qualification.']}
sp=ROOT/'final-cloud-supplement-summary.json'; sp.write_text(json.dumps(summary,indent=2)+'\n')
para=("The final cloud supplement binds the exact startup head `2d2fbce70b449ec8b6560c844ca5ddbc2e08a4c3` and T9-combination head `9f86bc8f1d11d65d2e22e437f111a965bc2faba5` to four successful OS jobs in both fork and upstream runs (16/16 total), plus the split controls (301 startup tests and 330 T9 tests, zero failures/errors/skips). For the cache-miss startup fork, every OS log records successful `buildCMakeDebug` and Ninja entry for all four Android ABIs; the complete logs and their existing dependency/compiler warnings are retained, while no object-by-object full-native-rebuild claim is made. The original Ubuntu startup artifact independently passes four signed APK/ABI/ELF checks, exact BuildConfig head binding, and a 20-pin recursive submodule closure. These are Debug cloud and packaging results; Android runtime qualification remains separate.\n")
rp=ROOT/'README-final-cloud-supplement-proposed.md'; rp.write_text(para)
files=[]
for p in sorted((ROOT/'terminal-runs').rglob('*')):
 if p.is_file(): files.append(p)
for p in sorted((ROOT/'startup-native-build-logs').rglob('*')):
 if p.is_file(): files.append(p)
files += [ROOT/'four-platform-terminal-receipt.json',ROOT/'startup-native-build-evidence.json',sp,rp,ROOT/'download-startup-ubuntu.py',ROOT/'audit-startup-apk.py',ROOT/'audit-startup-native-build.py',ROOT/'audit-four-runs.py',ROOT/'prepare-final-cloud-supplement.py',ROOT/'artifact-startup-ubuntu-12402467624/download-report.json',ROOT/'artifact-startup-ubuntu-12402467624/artifact-metadata.json',ROOT/'artifact-startup-ubuntu-12402467624/artifact-metadata.stderr',ROOT/'artifact-startup-ubuntu-12402467624/download.stderr',ROOT/'artifact-startup-ubuntu-12402467624/files/output-metadata.json']
for p in sorted((ROOT/'artifact-startup-ubuntu-12402467624/apk-audit').iterdir()):
 if p.is_file() and p.suffix!='.so': files.append(p)
for name in ('attempt-result.json','result.json','arm64-v8a-badging.stdout','arm64-v8a-badging.stderr','arm64-v8a-signer.stdout','arm64-v8a-signer.stderr'):
 files.append(ROOT/'artifact-startup-ubuntu-12402467624/apk-audit-attempt1'/name)
seen=set(); unique=[]
for p in files:
 p=p.resolve()
 if p in seen: continue
 assert p.is_file(),p
 seen.add(p); unique.append(p)
manifest={'schema':'trime-deferred-input-view-final-cloud-supplement-payload/v1','prior_index_dependency':{'short_commit':'754b46ba','immutable':True,'files':[rec(ROOT/'service-cloud-diagnostics.json'),rec(ROOT/'control-independent-audit.json'),rec(ROOT/'startup-t9-combination-equivalence.json'),rec(ROOT/'artifact-t9-ubuntu-12402430914/apk-audit/result.json')]},'new_payload':sorted((rec(p) for p in unique),key=lambda x:x['path']),'excluded_large_files':[{'path':'artifact-startup-ubuntu-12402467624/original.zip','reason':'77 MB original GitHub artifact; exact digest and CRC-bound extracted file hashes are in download-report.json'},{'path':'artifact-startup-ubuntu-12402467624/files/*.apk','reason':'Original signed APK bytes are large; all four exact hashes/sizes plus signature/ELF/BuildConfig results are in the audit receipt'},{'path':'artifact-startup-ubuntu-12402467624/apk-audit/*-librime_jni.so','reason':'Temporary extracted libraries are large; their SHA-256 and readelf results are in the audit receipt/raw outputs'}],'limits':summary['limits']}
mp=ROOT/'final-cloud-supplement-payload.json'; mp.write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps({'passed':True,'summary':str(sp),'summary_sha256':sha(sp),'readme':str(rp),'readme_sha256':sha(rp),'manifest':str(mp),'manifest_sha256':sha(mp),'new_payload_files':len(manifest['new_payload']),'new_payload_bytes':sum(f['bytes'] for f in manifest['new_payload'])}))
