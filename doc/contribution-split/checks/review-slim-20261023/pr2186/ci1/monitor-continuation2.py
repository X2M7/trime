import pathlib,subprocess,json,time,datetime,re,hashlib,concurrent.futures,sys
R=pathlib.Path(__file__).parent
GH='/home/xumin/Public/Trime/build/upstream19/gh-cli-2.102.0/gh'
RUN=39897889043
EXPECTED='2ad1865fd97f09ffba231e5c0e23092cd383b059'
def fetch(endpoint,prefix):
 t=time.monotonic()
 q=subprocess.run([GH,'api',endpoint,'--allow-escape-sequences'],capture_output=True,timeout=60)
 (R/(prefix+'.stdout')).write_bytes(q.stdout);(R/(prefix+'.stderr')).write_bytes(q.stderr)
 meta={'argv':[GH,'api',endpoint,'--allow-escape-sequences'],'exit':q.returncode,'stdout_bytes':len(q.stdout),'stderr_bytes':len(q.stderr),'elapsed_seconds':time.monotonic()-t,'utc':datetime.datetime.now(datetime.timezone.utc).isoformat()}
 (R/(prefix+'.command.json')).write_text(json.dumps(meta,indent=2)+'\n')
 return q
start=time.monotonic(); seq=0; terminal=None; jobs=None
while datetime.datetime.now(datetime.timezone.utc)<datetime.datetime.fromisoformat('2026-10-23T04:59:55.718736+00:00'):
 seq+=1
 q=fetch(f'repos/osfans/trime/actions/runs/{RUN}',f'poll-continued2-{seq:03d}-run')
 j=fetch(f'repos/osfans/trime/actions/runs/{RUN}/jobs?per_page=100',f'poll-continued2-{seq:03d}-jobs')
 if q.returncode==0 and j.returncode==0:
  d=json.loads(q.stdout); jobs=json.loads(j.stdout)['jobs']; assert d['head_sha']==EXPECTED
  print(json.dumps({'poll':seq,'run':RUN,'status':d['status'],'conclusion':d['conclusion'],'jobs':[{k:x.get(k)for k in ['id','name','status','conclusion']}for x in jobs]}),flush=True)
  if d['status']=='completed':terminal=d;break
 time.sleep(30)
if terminal is None:
 (R/'timeout-continued2.json').write_text(json.dumps({'run':RUN,'elapsed_seconds':time.monotonic()-start,'state':'deadline without terminal result; no rerun dispatched'},indent=2)+'\n');sys.exit(2)
(R/'terminal-run.json').write_text(json.dumps(terminal,indent=2)+'\n');(R/'terminal-jobs.json').write_text(json.dumps(jobs,indent=2)+'\n')
def collect(job):
 p=f"job-continued2-{job['id']}"
 q=fetch(f"repos/osfans/trime/actions/jobs/{job['id']}/logs",p+'-log')
 if q.returncode!=0:return {'job':job,'download_exit':q.returncode,'log_qualified':False}
 data=q.stdout;text=data.decode('utf-8');lines=text.splitlines();warning=[{'line':n+1,'text':l}for n,l in enumerate(lines)if re.search(r'(?i)warning|deprecated|deprecation',l)]
 tasks=[{'line':n+1,'text':l}for n,l in enumerate(lines)if re.search(r'> Task :app:(?:testDebugUnitTest|lint|check)',l)]
 checkout=[{'line':n+1,'text':l}for n,l in enumerate(lines)if re.search(r'git log -1|format=%H|refs/pull/2186/merge|009a6e643ead0da836102df02370ed5901c938d2',l)]
 context=[]
 for n,l in enumerate(lines):
  if 'git log -1' in l or 'format=%H' in l:context.extend([{'line':k+1,'text':lines[k]}for k in range(max(0,n-2),min(len(lines),n+8))])
 result={'job':{k:job.get(k)for k in ['id','name','status','conclusion','started_at','completed_at']},'download_exit':q.returncode,'log_qualified':True,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'warning_candidates':warning,'app_task_lines':tasks,'checkout_lines':checkout,'checkout_context':context,'warning_limit':'Mechanical matching preserves summaries and duplicate lines, not distinct defects or zero-warning qualification.'}
 (R/(p+'-audit.json')).write_text(json.dumps(result,indent=2)+'\n');return result
with concurrent.futures.ThreadPoolExecutor(max_workers=5)as pool:collected=list(pool.map(collect,jobs))
result={'run_id':RUN,'expected_reviewed_head':EXPECTED,'head_sha':terminal['head_sha'],'run_url':terminal['html_url'],'status':terminal['status'],'conclusion':terminal['conclusion'],'job_count':len(jobs),'all_jobs_success':len(jobs)==5 and all(x['conclusion']=='success'for x in jobs),'all_job_logs_collected':all(x['log_qualified']for x in collected),'jobs':collected,'scope':'Read-only monitoring and original job-log collection. No APK downloads, reruns, dispatches, settings changes, local build, VM, PR metadata or comments. CI build producer is synthetic merge to be independently bound from checkout logs; app testcase count not inferred from old tests.'}
(R/'result-continued2.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({'terminal':result['conclusion'],'jobs':result['job_count'],'all_jobs_success':result['all_jobs_success'],'all_job_logs_collected':result['all_job_logs_collected']}),flush=True)
