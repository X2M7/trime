#!/usr/bin/env python3
import datetime, hashlib, json, os, subprocess, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
GH=Path('/home/xumin/Public/Trime/build/upstream19/gh-cli-2.102.0/gh')
REPO='X2M7/trime'; RUN=39723620625
HEAD='2d2fbce70b449ec8b6560c844ca5ddbc2e08a4c3'; ARTIFACT_ID=12402467624
EXPECTED_NAME='trime-ubuntu-26.04'; EXPECTED_DIGEST='sha256:9bde82b8cee21d6dfd14e8da17ba3bda690c19fef24e5c8ce924f9bac98fbf6c'
OUT=ROOT/f'artifact-startup-ubuntu-{ARTIFACT_ID}'
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''): h.update(b)
 return h.hexdigest()
def safe_extract(z,d):
 with zipfile.ZipFile(z) as a:
  assert a.testzip() is None
  for i in a.infolist():
   p=Path(i.filename)
   if p.is_absolute() or '..' in p.parts: raise RuntimeError('unsafe path '+i.filename)
   if ((i.external_attr>>16)&0o170000)==0o120000: raise RuntimeError('symlink '+i.filename)
  a.extractall(d)
if OUT.exists(): raise RuntimeError(f'exclusive output exists: {OUT}')
OUT.mkdir()
meta=subprocess.run([str(GH),'api',f'repos/{REPO}/actions/artifacts/{ARTIFACT_ID}'],capture_output=True,timeout=45,check=True)
(OUT/'artifact-metadata.json').write_bytes(meta.stdout); (OUT/'artifact-metadata.stderr').write_bytes(meta.stderr)
a=json.loads(meta.stdout)
assert a['id']==ARTIFACT_ID and a['name']==EXPECTED_NAME and not a['expired']
assert a['digest']==EXPECTED_DIGEST and a['workflow_run']['id']==RUN and a['workflow_run']['head_sha']==HEAD
with (OUT/'original.zip').open('wb') as f:
 r=subprocess.run([str(GH),'api',f'repos/{REPO}/actions/artifacts/{ARTIFACT_ID}/zip'],stdout=f,stderr=subprocess.PIPE,timeout=240,check=False)
(OUT/'download.stderr').write_bytes(r.stderr)
if r.returncode: raise RuntimeError(f'download rc {r.returncode}')
assert 'sha256:'+sha(OUT/'original.zip')==EXPECTED_DIGEST
safe_extract(OUT/'original.zip',OUT/'files')
receipt={
 'downloaded_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
 'repository':REPO,'run':RUN,'expected_head':HEAD,'artifact':a,
 'archive_bytes':(OUT/'original.zip').stat().st_size,'archive_digest_verified':EXPECTED_DIGEST,'zip_crc_passed':True,
 'files':[{'path':str(p.relative_to(OUT)),'bytes':p.stat().st_size,'sha256':sha(p)} for p in sorted((OUT/'files').rglob('*')) if p.is_file()]
}
tmp=OUT/'download-report.json.tmp'; tmp.write_text(json.dumps(receipt,indent=2)+'\n'); os.replace(tmp,OUT/'download-report.json')
print(json.dumps({'path':str(OUT),'zip_sha256':sha(OUT/'original.zip'),'files':len(receipt['files']),'receipt_sha256':sha(OUT/'download-report.json')}))
