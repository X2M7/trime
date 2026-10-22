#!/usr/bin/env python3
"""Lightweight immutable audit of the original startup-fix Ubuntu APK artifact."""
import hashlib, json, os, re, subprocess, zipfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
ROOT=HERE/'artifact-startup-ubuntu-12402467624'; FILES=ROOT/'files'; OUT=ROOT/'apk-audit'
REPO='X2M7/trime'; RUN=39723620625
HEAD='2d2fbce70b449ec8b6560c844ca5ddbc2e08a4c3'; TREE='f58d898da7b8c2cec4c8c613087adf8b7937d289'
SOURCE=Path('/home/xumin/Public/Trime-pr-deferred-input-view')
PIN_RESOLVER=Path('/home/xumin/Public/Trime-pr-t9-input')
T9_HEAD='9f86bc8f1d11d65d2e22e437f111a965bc2faba5'
SERVICE=SOURCE/'app/src/main/java/com/osfans/trime/ime/core/TrimeInputMethodService.kt'
SERVICE_SHA='152fbdbeef9fa0554eaa090568ba5a547a2c292508c4c342e43d59d56d1c2b5a'; SERVICE_BLOB='af74f764d2ee27ecbbd035ea7680b0c5ded2542c'
SDK=Path('/home/xumin/Public/android-toolchain/android-sdk'); JDK=Path('/home/xumin/Public/android-toolchain/jdk-17.0.19+10')
TOOLS=SDK/'build-tools/36.0.0'; ANALYZER=SDK/'cmdline-tools/latest/bin/apkanalyzer'
READELF=SDK/'ndk/28.0.13004108/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf'
ABIS={'armeabi-v7a','arm64-v8a','x86','x86_64'}
TOP_SUBMODULES=['app/data/rime/essay','app/data/rime/luna-pinyin','app/data/rime/prelude','app/data/rime/stroke','app/src/main/jni/librime','app/src/main/jni/librime-lua','app/src/main/jni/librime-lua-deps','app/src/main/jni/librime-octagram','app/src/main/jni/librime-predict','app/src/main/jni/snappy']
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''): h.update(b)
 return h.hexdigest()
def out(argv,env=None): return subprocess.check_output([str(x) for x in argv],env=env).decode().strip()
receipt=json.loads((ROOT/'download-report.json').read_text())
assert receipt['repository']==REPO and receipt['run']==RUN and receipt['expected_head']==HEAD
assert receipt['artifact']['name']=='trime-ubuntu-26.04' and receipt['artifact']['workflow_run']['head_sha']==HEAD
assert receipt['archive_digest_verified']=='sha256:'+sha(ROOT/'original.zip') and receipt['zip_crc_passed']
assert out(['git','-C',SOURCE,'rev-parse','HEAD'])==HEAD
assert out(['git','-C',SOURCE,'rev-parse','HEAD^{tree}'])==TREE
assert out(['git','-C',SOURCE,'status','--porcelain'])==''
assert sha(SERVICE)==SERVICE_SHA and out(['git','-C',SOURCE,'hash-object',SERVICE])==SERVICE_BLOB
metadata=json.loads((FILES/'output-metadata.json').read_text())
assert metadata['applicationId']=='com.osfans.trime.debug' and metadata['variantName']=='debug' and metadata['minSdkVersionForDexing']==21
assert {e['filters'][0]['value'] for e in metadata['elements']}==ABIS
assert {e['versionCode'] for e in metadata['elements']}=={20261101} and {e['versionName'] for e in metadata['elements']}=={'3.3.13'}
assert {e['outputFile'] for e in metadata['elements']}=={p.name for p in FILES.glob('*.apk')}
OUT.mkdir(exist_ok=False)
env=os.environ.copy(); env['JAVA_HOME']=str(JDK); env['PATH']=str(JDK/'bin')+os.pathsep+env.get('PATH',''); env['JAVA_OPTS']='-Xmx256m -XX:MaxMetaspaceSize=128m -XX:ActiveProcessorCount=1'
report={'schema':'trime-deferred-input-view-startup-cloud-apk-audit/v1','passed':False,'repository':REPO,'run':RUN,'source_head':HEAD,'source_tree':TREE,'source_checkout':str(SOURCE),'source_service_sha256':SERVICE_SHA,'source_service_blob':SERVICE_BLOB,'artifact':receipt['artifact'],'original_zip_sha256':sha(ROOT/'original.zip'),'output_metadata':{'path':str(FILES/'output-metadata.json'),'sha256':sha(FILES/'output-metadata.json'),'content':metadata},'commands':[],'apks':[]}
def run(label,argv):
 r=subprocess.run([str(x) for x in argv],env=env,capture_output=True,timeout=180,check=False)
 so=OUT/(label+'.stdout'); se=OUT/(label+'.stderr'); so.write_bytes(r.stdout); se.write_bytes(r.stderr)
 report['commands'].append({'label':label,'argv':[str(x) for x in argv],'exit_code':r.returncode,'stdout':str(so),'stdout_sha256':sha(so),'stderr':str(se),'stderr_sha256':sha(se)})
 (OUT/'result.json').write_text(json.dumps(report,indent=2)+'\n')
 if r.returncode: raise RuntimeError(f'{label} rc={r.returncode}: '+r.stderr.decode(errors='replace'))
 return r.stdout.decode(errors='replace')
elf_expect={'armeabi-v7a':('ELF32','ARM'),'arm64-v8a':('ELF64','AArch64'),'x86':('ELF32','Intel 80386'),'x86_64':('ELF64','Advanced Micro Devices X86-64')}
apks=sorted(FILES.glob('*.apk')); assert len(apks)==4
observed_cert=None
for apk in apks:
 before=sha(apk)
 with zipfile.ZipFile(apk) as z:
  assert z.testzip() is None
  natives=sorted(n for n in z.namelist() if n.startswith('lib/') and n.endswith('.so'))
  abis=sorted({Path(n).parts[1] for n in natives}); assert len(abis)==1
  abi=abis[0]; assert f'lib/{abi}/librime_jni.so' in natives
  lib=OUT/f'{abi}-librime_jni.so'; lib.write_bytes(z.read(f'lib/{abi}/librime_jni.so'))
 badging=run(f'{abi}-badging',[TOOLS/'aapt','dump','badging',apk])
 assert "package: name='com.osfans.trime.debug'" in badging and "sdkVersion:'21'" in badging and "targetSdkVersion:'37'" in badging and 'application-debuggable' in badging and f"native-code: '{abi}'" in badging
 signer=run(f'{abi}-signer',[TOOLS/'apksigner','-JXmx256m','-JXX:MaxMetaspaceSize=128m','verify','--verbose','--print-certs',apk])
 certs=re.findall(r'Signer #\d+ certificate SHA-256 digest: (\S+)',signer); assert 'Verifies' in signer and len(certs)==1
 if observed_cert is None: observed_cert=certs[0]
 assert certs==[observed_cert]
 elf=run(f'{abi}-elf',[READELF,'-h','-d',lib]); assert re.search(r'Class:\s+'+elf_expect[abi][0]+r'\b',elf) and re.search(r'Machine:\s+'+re.escape(elf_expect[abi][1])+r'\b',elf)
 item={'path':str(apk),'bytes':apk.stat().st_size,'sha256':before,'abi':abi,'native_entry_count':len(natives),'librime_jni_sha256':sha(lib),'signer_certificates_sha256':certs,'original_signature_warnings':[line for line in signer.splitlines() if 'WARNING:' in line],'package':'com.osfans.trime.debug','min_sdk':21,'target_sdk':37,'elf_class':elf_expect[abi][0],'elf_machine':elf_expect[abi][1]}
 if abi=='x86_64':
  bc=run('x86_64-buildconfig',[ANALYZER,'dex','code','--class','com.osfans.trime.BuildConfig',apk])
  m=re.search(r'BUILD_COMMIT_HASH:Ljava/lang/String; = "([0-9a-f]{40})"',bc); assert m and m.group(1)==HEAD and 'BUILD_TYPE:Ljava/lang/String; = "debug"' in bc
  item['buildconfig_commit_hash']=m.group(1); report['x86_64_apk']=str(apk); report['x86_64_apk_sha256']=before
 assert sha(apk)==before
 report['apks'].append(item)
assert observed_cert is not None
assert {a['abi'] for a in report['apks']}==ABIS and {a['signer_certificates_sha256'][0] for a in report['apks']}=={observed_cert}
report['shared_debug_certificate_sha256']=observed_cert
# Startup checkout intentionally remains uninitialized. Resolve its exact recursive pin closure through
# the initialized T9 checkout only after proving .gitmodules and all ten root gitlinks are identical.
eq=run('startup-t9-submodule-gitlink-diff',['git','-C',PIN_RESOLVER,'diff','--raw',HEAD,T9_HEAD,'--','.gitmodules',*TOP_SUBMODULES])
assert eq==''
pins_raw=run('recursive-submodule-pins',['git','-C',PIN_RESOLVER,'submodule','status','--recursive'])
pins=pins_raw.splitlines(); assert len(pins)==20 and all(line.startswith(' ') for line in pins)
report['submodule_pin_binding']={'count':20,'pins':pins,'startup_checkout_mutated':False,'resolution_checkout':str(PIN_RESOLVER),'proof':'The startup and T9 commits have byte-identical .gitmodules and identical ten root gitlinks; recursive pins are therefore fixed by those same gitlink commits. The initialized, clean T9 checkout supplies the readable recursive status without initializing or mutating the startup checkout.','startup_t9_gitlink_diff_stdout_sha256':sha(OUT/'startup-t9-submodule-gitlink-diff.stdout'),'recursive_pins_stdout_sha256':sha(OUT/'recursive-submodule-pins.stdout')}
assert out(['git','-C',SOURCE,'status','--porcelain'])=='' and out(['git','-C',SOURCE,'rev-parse','HEAD'])==HEAD
report['passed']=True
report['limits']=['Debug APK artifact audit only; no device execution or Release claim.','The startup worktree was not mutated; recursive pins were resolved as documented through unchanged root gitlinks and a clean initialized checkout.','A native-cache miss alone is not treated as proof of a complete native rebuild; build/link evidence is adjudicated separately.','Signed APK bytes were inspected in place and remained unchanged.']
(OUT/'result.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'passed':True,'apks':4,'result':str(OUT/'result.json'),'result_sha256':sha(OUT/'result.json'),'x86_64_apk':report['x86_64_apk'],'x86_64_sha256':report['x86_64_apk_sha256'],'cert_sha256':observed_cert,'pins':len(pins)}))
