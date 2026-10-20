#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Build exact Git JEnv headers against NDK jni.h and execute HotSpot with -Xcheck:jni.

Linux host only. Requires clang++, a JDK 17 installation, an Android NDK, and
an existing Git checkout containing both commits. No Gradle, Android emulator,
Rime classes, GlobalRefSingleton instance, or repository modification is used.
The baseline must contain the attachment leak; the fixed head must detach it.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import traceback

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--repo',type=Path,required=True)
parser.add_argument('--base',required=True)
parser.add_argument('--head',required=True)
parser.add_argument('--ndk',type=Path,required=True)
parser.add_argument('--jdk',type=Path,required=True)
parser.add_argument('--out',type=Path,required=True)
parser.add_argument('--compiler',default=shutil.which('clang++'))
args=parser.parse_args()
assert args.compiler, 'clang++ is required'
repo=args.repo.resolve();out=args.out.resolve();out.mkdir(parents=True,exist_ok=False)
jni=args.ndk.resolve()/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/jni.h'
vm=args.jdk.resolve()/'lib/server/libjvm.so'
source=Path(__file__).resolve().parent/'host-vm-check.cc'
relative='app/src/main/jni/librime_jni/jni-utils.h'
report={'schema':'jenv-actual-host-vm-check/v1','passed':False,'scope':'One native std::thread plus borrowed/nested main-thread guards in actual HotSpot; no Android/ART/Rime/GlobalRefSingleton.',
        'same_cases_as_non_check_jni_run':True,'vm_heap_mib':128,'vm_option':'-Xcheck:jni','commands':[]}

def binding(p):
    b=p.read_bytes()
    return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest()}

def git(*a):
    return subprocess.check_output(['git','-C',str(repo),*a])

def run(argv,name,expected):
    p=subprocess.run([str(a) for a in argv],capture_output=True,timeout=60)
    stdout=out/(name+'.stdout');stderr=out/(name+'.stderr')
    stdout.write_bytes(p.stdout);stderr.write_bytes(p.stderr)
    report['commands'].append({'argv':[str(a) for a in argv],'exit':p.returncode,'expected_exit':expected,
                               'stdout':binding(stdout),'stderr':binding(stderr)})
    assert p.returncode==expected,(name,p.returncode,p.stderr.decode(errors='replace'))
    return p

try:
    base=git('rev-parse',args.base).decode().strip();head=git('rev-parse',args.head).decode().strip()
    report['base']=base;report['head']=head
    local_jni=out/'jni-headers';local_jni.mkdir();shutil.copyfile(jni,local_jni/'jni.h')
    assert (local_jni/'jni.h').read_bytes()==jni.read_bytes()
    shutil.copyfile(source,out/'check.cc')
    compiler=Path(args.compiler).resolve()
    inputs=[Path(__file__).resolve(),source,out/'check.cc',jni,local_jni/'jni.h',vm,compiler]
    for label,sha in [('baseline',base),('fixed',head)]:
        folder=out/label;folder.mkdir();p=folder/'jni-utils.h'
        p.write_bytes(git('show',sha+':'+relative));inputs.append(p)
    report['inputs_before']=[binding(p) for p in inputs]
    for label in ('baseline','fixed'):
        binary=out/(label+'-check')
        p=run([compiler,'-std=c++17','-fno-elide-constructors','-O0','-g0','-Wall','-Wextra','-Werror','-pthread',
               '-I',local_jni,'-I',out/label,out/'check.cc','-L',vm.parent,'-Wl,-rpath,'+str(vm.parent),'-ljvm','-o',binary],label+'-compile',0)
        assert not p.stderr,'Unexpected compiler diagnostic'
        p=run([binary],label+'-run',1 if label=='baseline' else 0)
        assert not p.stderr,'Unexpected checked-JNI/JVM stderr; inspect the complete preserved output'
        rows=[json.loads(line) for line in p.stdout.decode().splitlines()]
        assert len(rows)==3 and rows[0]['passed'] and rows[2]['destroy']==0
        assert rows[1]['before']==-2 and rows[1]['inside']==0 and rows[1]['afterNested']==0
        assert rows[1]['stringUsable'] and rows[1]['nestedSameEnvironment']
        assert rows[1]['after']==(0 if label=='baseline' else -2) and rows[1]['cleanup']==0
        assert rows[1]['passed']==(label=='fixed') and rows[2]['passed']==(label=='fixed')
        report[label]=rows
    report['inputs_after']=[binding(p) for p in inputs]
    assert report['inputs_after']==report['inputs_before']
    report['checked_jni_diagnostics_observed']=False
    report['passed']=True
except BaseException:
    report['error']=traceback.format_exc()
finally:
    target=out/'report.json';target.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'passed':report['passed'],'report':binding(target),'baseline':report.get('baseline'),'fixed':report.get('fixed'),'error':report.get('error')},indent=2))
raise SystemExit(0 if report['passed'] else 1)
