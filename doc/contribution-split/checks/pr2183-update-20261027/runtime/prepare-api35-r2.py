from pathlib import Path
import os,json,hashlib,subprocess,datetime,difflib,ast
R=Path('/home/xumin/Public/Trime/build/pr2183-update-20261027/runtime')
SRC=Path('/home/xumin/Public/Trime/build/readiness-20261022/api35')
DST=R/'vm-assets-api35-r2';REPORT=R/'vm-assets-api35-r2-copy1'
assert not DST.exists() and not REPORT.exists()
REPORT.mkdir();DST.mkdir();started=datetime.datetime.now(datetime.timezone.utc).isoformat()
def digest(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
def inventory(root):
 result={}
 for p in sorted(root.rglob('*')):
  assert not p.is_symlink(),p
  if not p.is_file():assert p.is_dir(),p;continue
  s0=p.stat();h=digest(p);s1=p.stat();assert (s0.st_dev,s0.st_ino,s0.st_size,s0.st_mtime_ns)==(s1.st_dev,s1.st_ino,s1.st_size,s1.st_mtime_ns),p
  result[str(p.relative_to(root))]={'bytes':s0.st_size,'sha256':h,'device':s0.st_dev,'inode':s0.st_ino,'nlink':s0.st_nlink,'mode':s0.st_mode,'mtime_ns':s0.st_mtime_ns,'allocated_bytes':s0.st_blocks*512}
 return result
def writejson(name,value):(REPORT/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
pre={kind:inventory(SRC/kind) for kind in ['avd','system-images']}
writejson('source-before.json',pre)
commands=[]
for kind in ['avd','system-images']:
 dest=DST/'api35'/kind;dest.mkdir(parents=True)
 argv=['cp','--reflink=auto','--sparse=always','-a','--no-preserve=links',str(SRC/kind)+'/.',str(dest)]
 c=subprocess.run(argv,capture_output=True);(REPORT/(kind+'.stdout')).write_bytes(c.stdout);(REPORT/(kind+'.stderr')).write_bytes(c.stderr)
 commands.append({'argv':argv,'exit':c.returncode,'stdout_sha256':hashlib.sha256(c.stdout).hexdigest(),'stderr_sha256':hashlib.sha256(c.stderr).hexdigest()});assert c.returncode==0
copied={kind:inventory(DST/'api35'/kind) for kind in pre}
for kind in pre:
 assert set(pre[kind])==set(copied[kind])
 for f,m in pre[kind].items():
  d=copied[kind][f];assert m['bytes']==d['bytes'] and m['sha256']==d['sha256'],f
  assert (m['device'],m['inode'])!=(d['device'],d['inode']) and d['nlink']==1,f
writejson('destination-before-relocation.json',copied)
name='trime_pr2183_api35_20261027_r2';avd=DST/'api35/avd';sysdir=DST/'api35/system-images'
changed=[]
def edit_file(p,new):
 old=p.read_text();assert old!=new
 (REPORT/(p.name+'.before')).write_text(old);p.write_text(new)
 diff=''.join(difflib.unified_diff(old.splitlines(keepends=True),new.splitlines(keepends=True),fromfile=str(SRC/'avd'/p.name),tofile=str(p)))
 (REPORT/(p.name+'.patch')).write_text(diff)
 changed.append({'path':str(p),'source_relative':str(p.relative_to(DST/'api35')),'before_sha256':hashlib.sha256(old.encode()).hexdigest(),'after_sha256':digest(p),'patch':str(REPORT/(p.name+'.patch'))})
p=avd/'config.ini';old=p.read_text();new=old
updates={'avd.id':name,'avd.name':name,'disk.dataPartition.path':str(avd/'userdata-qemu.img'),'image.sysdir.1':str(sysdir)+'/', 'hw.cpu.ncore':'4','hw.ramSize':'3072'}
for k,v in updates.items():
 matches=[l for l in new.splitlines() if l.split('=',1)[0].strip()==k];assert len(matches)==1,(k,matches)
 new=new.replace(matches[0],k+' = '+v)
edit_file(p,new)
p=avd/'hardware-qemu.ini';old=p.read_text();new=old.replace(str(SRC),str(DST/'api35')).replace('trime_readiness_api35_20261022',name)
for k,v in {'hw.cpu.ncore':'4','hw.ramSize':'3072'}.items():
 matches=[l for l in new.splitlines() if l.split('=',1)[0].strip()==k];assert len(matches)==1
 new=new.replace(matches[0],k+' = '+v)
edit_file(p,new)
assert str(SRC) not in p.read_text()
(DST/'avd').mkdir();(DST/'android-user').mkdir()
reg=DST/'avd'/(name+'.ini');reg.write_text('avd.ini.encoding=UTF-8\npath='+str(avd)+'\ntarget=android-35\n')
oldctrl=R/'run-api35-8a1e-v10.py';newctrl=R/'run-api35-8a1e-v11.py';assert not newctrl.exists()
old=oldctrl.read_text();new=old
changes=[("ASSET_ROOT = ROOT / 'vm-assets'","ASSET_ROOT = ROOT / 'vm-assets-api35-r2'"),("OUT = ROOT / 'api35-8a1e-v10-run1'","OUT = ROOT / 'api35-8a1e-v11-run2'"),("'-avd', 'trime_pr2183_api35_20261027'","'-avd', 'trime_pr2183_api35_20261027_r2'"),("'-gpu', 'swangle', '-cores', '2', '-memory', '2304'","'-gpu', 'swangle', '-cores', '4', '-memory', '3072'")]
for a,b in changes:assert new.count(a)==1,a;new=new.replace(a,b)
newctrl.write_text(new);newctrl.chmod(oldctrl.stat().st_mode & 0o777)
ast.parse(new)
# Inverting exactly four substitutions must reproduce all old code byte for byte.
inverted=new
for a,b in reversed(changes):assert inverted.count(b)==1;inverted=inverted.replace(b,a)
assert inverted==old
ctrlpatch=''.join(difflib.unified_diff(old.splitlines(keepends=True),new.splitlines(keepends=True),fromfile=str(oldctrl),tofile=str(newctrl)))
(REPORT/'controller-v10-to-v11.patch').write_text(ctrlpatch)
post={kind:inventory(SRC/kind) for kind in pre};assert pre==post
writejson('source-after.json',post)
final={kind:inventory(DST/'api35'/kind) for kind in pre}
changed_rel={x['source_relative'] for x in changed}
for kind in pre:
 for f,m in pre[kind].items():
  d=final[kind][f];assert (m['device'],m['inode'])!=(d['device'],d['inode']) and d['nlink']==1
  if f'{kind}/{f}' not in changed_rel:assert (m['bytes'],m['sha256'])==(d['bytes'],d['sha256'])
writejson('destination-after-relocation.json',final)
prior=json.loads((R/'vm-assets-copy1/result.json').read_text());prior_by_kind={('avd' if x['kind']=='avd' else 'system-images'):x for x in prior['copies'] if x['api']==35}
prior_mismatches=[]
for kind,files in pre.items():
 for f,m in files.items():
  prior_m=prior_by_kind[kind]['files'].get(f)
  if prior_m is None or (f!='config.ini' and (m['bytes'],m['sha256'])!=(prior_m['bytes'],prior_m['sha256'])):prior_mismatches.append({'kind':kind,'file':f,'reason':'prior copy baseline mismatch'})
assert not prior_mismatches,prior_mismatches
result={'schema':'trime-fresh-api35-r2-assets-copy/v1','started_utc':started,'finished_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'source':str(SRC),'destination':str(DST),'source_is_original_readiness_api35_not_modified_r1':True,'all_original_source_pre_post_metadata_and_sha_equal':True,'source_file_count':sum(map(len,pre.values())),'logical_source_bytes':sum(x['bytes'] for fs in pre.values() for x in fs.values()),'all_destination_bytes_equal_before_control_relocation':True,'all_destination_non_control_bytes_still_equal':True,'only_control_files_relocated':changed,'source_and_destination_no_shared_inodes_no_symlinks_and_destination_nlink1':True,'copy_commands':commands,'copy_mode':'cp --reflink=auto --sparse=always; actual filesystem may fall back to an independent sparse copy; no hardlink claim','prior_copy1_non_config_hashes_equal_original_now':True,'registration':{'path':str(reg),'sha256':digest(reg),'name':name},'controller':{'old_path':str(oldctrl),'old_sha256':digest(oldctrl),'new_path':str(newctrl),'new_sha256':digest(newctrl),'patch':str(REPORT/'controller-v10-to-v11.patch'),'exact_four_text_substitutions_inversion_pass':True,'AST_parse_only_no_import_or_execution':True,'changed_fields':['ASSET_ROOT','OUT','AVD_name','guest_cores_2_to_4_and_memory_2304_to_3072'],'binding_1GiB_business_cleanup_guards_unchanged':True},'source_before':{'path':str(REPORT/'source-before.json'),'sha256':digest(REPORT/'source-before.json')},'source_after':{'path':str(REPORT/'source-after.json'),'sha256':digest(REPORT/'source-after.json')},'destination_before':{'path':str(REPORT/'destination-before-relocation.json'),'sha256':digest(REPORT/'destination-before-relocation.json')},'destination_after':{'path':str(REPORT/'destination-after-relocation.json'),'sha256':digest(REPORT/'destination-after-relocation.json')},'VM_started':False,'ADB_called':False,'source_or_SDK_or_r1_written':False,'independent_review':'PENDING_ROOT_AND_VALIDATOR_BEFORE_START'}
writejson('result.json',result)
print(json.dumps({'result':str(REPORT/'result.json'),'result_sha256':digest(REPORT/'result.json'),'new_controller':str(newctrl),'controller_sha256':digest(newctrl),'source_files':result['source_file_count'],'source_logical_bytes':result['logical_source_bytes'],'source_unchanged':True,'VM_started':False,'ADB_called':False},ensure_ascii=False))
