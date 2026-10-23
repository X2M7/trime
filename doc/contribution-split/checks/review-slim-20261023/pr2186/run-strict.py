#!/usr/bin/env python3
"""Strict compile-only check of each JNI translation unit after source-ready handoff."""
import argparse, datetime, hashlib, json, pathlib, shlex, subprocess, time
P=argparse.ArgumentParser();P.add_argument('--source-ready',action='store_true',required=True);args=P.parse_args()
ROOT=pathlib.Path(__file__).resolve().parent
OUT=ROOT/'ndk-strict1';OUT.mkdir(exist_ok=False)
REPO=pathlib.Path('/home/xumin/Public/Trime-pr-jni-unicode-transport')
JNI=REPO/'app/src/main/jni/librime_jni'
RIME=pathlib.Path('/home/xumin/Public/Trime-pr-t9-input/app/src/main/jni/librime')
OLD=pathlib.Path('/home/xumin/Public/Trime/build/upstream-split-20261020/next-pr8/core-native-inventory/host-native1')
NDK=pathlib.Path('/home/xumin/Public/android-toolchain/android-sdk/ndk/28.0.13004108')
COMPILER=NDK/'toolchains/llvm/prebuilt/linux-x86_64/bin/clang++'
def bind(p):
 p=pathlib.Path(p);digest=hashlib.sha256()
 with p.open('rb') as f:
  for block in iter(lambda:f.read(1024*1024),b''):digest.update(block)
 return {'path':str(p),'resolved_path':str(p.resolve()),'bytes':p.stat().st_size,'sha256':digest.hexdigest()}
def git(*args,cwd=REPO):return subprocess.check_output(['git','-C',str(cwd),*args],text=True).strip()
units=sorted(JNI.glob('*.cc'));assert len(units)==6
assert {p.name for p in units} == {'config.cc', 'frontend.cc', 'key.cc', 'levers.cc', 'opencc.cc', 'rime_jni.cc'}
assert git('rev-parse', 'HEAD:app/src/main/jni/librime') == git('rev-parse','HEAD',cwd=RIME)
own=sorted([*JNI.glob('*.cc'),*JNI.glob('*.h')]);before=[bind(p) for p in own]
include=OUT/'includes';include.mkdir();(include/'opencc').symlink_to(RIME/'deps/opencc/src',target_is_directory=True)
old=json.loads((OLD/'production-compilation2.json').read_text());flags=old['commands'][0]['command'][1:];flags=flags[:flags.index('-c')];flags=[flag.replace('/home/xumin/Public/Trime-upstream-pr/app/src/main/jni/librime',str(RIME)) for flag in flags]
# Use unmodified pinned Glog headers instead of host /usr/include, and pinned OpenCC headers.
flags+=['-isystem',str(RIME/'deps/glog/src'),'-isystem',str(RIME/'deps/marisa-trie/include'),'-isystem',str(include),'-fPIC','-g0']
# NDK provides its own JNI header. The prior copied JNI header has no precedence here.
i=flags.index('-isystem',flags.index('-Wno-unused-parameter'))
assert flags[i+1]==str(OLD/'jni-headers');del flags[i:i+2]
flags=['--target=x86_64-none-linux-android21','--sysroot='+str(NDK/'toolchains/llvm/prebuilt/linux-x86_64/sysroot'),*flags]
for flag in ['-Wall','-Wextra','-Werror','-Wno-unused-parameter']:assert flag in flags
v=subprocess.run([str(COMPILER),'--version'],capture_output=True,check=True)
(OUT/'compiler-version.log').write_bytes(v.stdout+v.stderr)
report={'schema':'trime-jni-all-translation-units-strict-compile/v1','started_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'source_head_at_start':git('rev-parse','HEAD'),'working_tree_dirty_at_start':bool(git('status','--porcelain')),'ndk':bind(NDK/'source.properties'),'compiler':bind(COMPILER),'target':'x86_64-none-linux-android21','strict_flags':['-Wall','-Wextra','-Werror','-Wno-unused-parameter'],'parameter_reference':bind(OLD/'production-compilation2.json'),'boost_headers_version':bind(OLD/'boost-headers/boost/version.hpp'),'rime_pin':git('rev-parse','HEAD',cwd=RIME),'rime_gitlink_expected':git('rev-parse', 'HEAD:app/src/main/jni/librime'),'rime_source_tracked_clean':not bool(git('status','--porcelain','--untracked-files=no',cwd=RIME)),'generated_build_config':bind(OLD/'engine3/src/rime/build_config.h'),'source_files_before':before,'commands':[],'limits':['Actual NDK28 compile-only for all six upstream JNI translation units at one Android x86_64/API21 target, not linking, packaging, installation or Android runtime.','Rime and OpenCC/Glog source headers are taken from the unmodified pinned checkout; generated Rime logging configuration is reused from the separately recorded pinned host build.','Boost1.92 headers are the same recorded host-engine header tree, not a claim that the complete production Android dependency build was reproduced.','Dependency headers use isystem to restrict Wall/Wextra/Werror acceptance to owned JNI source; this does not mean dependency warnings were fixed.','Source identity is bound by per-file bytes, including current uncommitted changes, not asserted to be a clean final Git build.']}
assert report['rime_source_tracked_clean']
for source in units:
 output=OUT/(source.name+'.o');log=OUT/(source.name+'.log');cmd=[str(COMPILER),*flags,'-c',str(source),'-o',str(output)]
 start=time.monotonic()
 with log.open('wb') as f:p=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=180)
 item={'unit':source.name,'source':bind(source),'argv':cmd,'exit_code':p.returncode,'duration_seconds':time.monotonic()-start,'log':bind(log)}
 if output.exists():item['object']=bind(output)
 report['commands'].append(item)
 (OUT/'result.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps({'unit':source.name,'exit_code':p.returncode,'log_bytes':log.stat().st_size}),flush=True)
report['source_files_after']=[bind(p) for p in sorted([*JNI.glob('*.cc'),*JNI.glob('*.h')])];report['source_bytes_unchanged_during_check']=before==report['source_files_after'];report['finished_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat();report['all_six_passed']=all(x['exit_code']==0 and x['log']['bytes']==0 for x in report['commands']) and report['source_bytes_unchanged_during_check'];(OUT/'result.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'all_six_passed':report['all_six_passed'],'result':str(OUT/'result.json'),'sha256':hashlib.sha256((OUT/'result.json').read_bytes()).hexdigest()}));raise SystemExit(0 if report['all_six_passed'] else 1)
