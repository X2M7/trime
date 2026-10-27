#!/usr/bin/env python3
from pathlib import Path
import collections,datetime,hashlib,json,os,re
OUT=Path(__file__).resolve().parent
COMPLETION=OUT.parent/'api35-8a1e-v11-run2/completion.json'
terminal=json.loads(COMPLETION.read_bytes())
assert terminal['reason']=='requested-normal-stop' and terminal['emulator_exit']==0 and terminal['collector_exit'] is not None and terminal['cleanup_errors']==[]
assert terminal['source_head']=='8a1ee0ea6538e220fcddf31ed6fd491f5bc3cca7' and terminal['original_apk_sha256']=='12ad11114e2db508e8b9946d410d035f41f0facf70603dce0ec22ae7f841bcda'
LOG=OUT.parent/'api35-8a1e-v11-run2/device-logcat.txt'
pattern=re.compile(rb'^(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+(.*?):\s?(.*?)(?:\r?\n)?$')
critical_pattern=re.compile(r'FATAL EXCEPTION:|Fatal signal \d+|signal \d+ \(SIG|ANR in |am_anr|Out of memory:|Killed process |(?:lowmemorykiller|lmkd).*\b(?:Kill|Killing)\b|Disabling IRQ',re.I)
levels=collections.Counter();target=collections.Counter();other=collections.Counter();tags=collections.Counter();groups={};critical=[];parsed=unparsed=number=offset=0
with LOG.open('rb') as stream:
 st=os.fstat(stream.fileno());remaining=st.st_size;digest=hashlib.sha256()
 while remaining:
  raw=stream.readline(remaining);assert raw;remaining-=len(raw);number+=1;digest.update(raw)
  m=pattern.match(raw)
  if m:
   parsed+=1;timestamp=m[1].decode();pid=int(m[2]);tid=int(m[3]);level=m[4].decode();tag=m[5].decode('utf-8',errors='replace').strip();message=m[6].decode('utf-8',errors='replace').rstrip('\r\n')
   levels[level]+=1;(target if pid==2047 else other)[level]+=1
   location={'line_number_1based':number,'byte_start_0based':offset,'byte_end_exclusive':offset+len(raw),'timestamp':timestamp,'pid':pid,'tid':tid,'level':level,'tag':tag,'message':message,'raw_sha256':hashlib.sha256(raw).hexdigest()}
   if level in 'WEF':
    normalized=re.sub(r'0x[0-9a-fA-F]+','<hex>',message)
    normalized=re.sub(r'\b[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}\b','<uuid>',normalized)
    normalized=re.sub(r'\b\d+(?:\.\d+)*\b','<number>',normalized)
    tagbase=re.sub(r'^\[[^]]+\]\s*','',tag)
    key=('target_PID2047' if pid==2047 else 'other_PID',level,tagbase,normalized)
    if key not in groups:groups[key]={'process_scope':key[0],'level':level,'tag':tagbase,'normalized_message':normalized,'count':0,'pids':set(),'first':location,'last':location}
    g=groups[key];g['count']+=1;g['pids'].add(pid);g['last']=location
    tags[(key[0],level,tagbase)]+=1
   if level=='F' or critical_pattern.search(tag+': '+message):
    critical.append(location)
  else:unparsed+=1
  offset+=len(raw)
 assert offset==st.st_size
 after=os.fstat(stream.fileno());assert st.st_ino==after.st_ino and st.st_dev==after.st_dev and after.st_size==st.st_size and after.st_mtime_ns==st.st_mtime_ns
entries=[]
for g in groups.values():g['pids']=sorted(g['pids']);entries.append(g)
entries.sort(key=lambda g:(g['process_scope'],g['level'],-g['count'],g['tag'],g['normalized_message']))
report={'schema':'trime-runtime-warning-bounded-prefix-classification/v1','utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'scope':'Final closed captured API35 r2 log; actual collector exit is recorded separately and not presumed zero','log':{'path':str(LOG),'inode':st.st_ino,'device':st.st_dev,'fixed_byte_prefix_length':st.st_size,'fixed_byte_prefix_sha256':digest.hexdigest(),'original_line_count_including_possible_final_fragment':number,'parsed_threadtime_rows':parsed,'unparsed_rows':unparsed,'stat_size_after_read':after.st_size},'all_PID_level_counts':dict(levels),'target_PID2047_level_counts':dict(target),'other_PID_level_counts':dict(other),'WEF_tag_counts':[{'process_scope':k[0],'level':k[1],'tag':k[2],'count':v} for k,v in sorted(tags.items(),key=lambda x:(x[0][0],x[0][1],-x[1],x[0][2]))],'WEF_normalized_message_groups':entries,'critical_or_fatal_candidate_rows':critical,'candidate_pattern':critical_pattern.pattern,'source_not_modified':True,'no_ADB_device_controller_or_logging_mutation':True,'causal_attribution':'Membership in PID2047 is process attribution only, not proof that Trime caused every framework/native warning. Other PID/system/kernel rows remain separate and unexcused.','warning_free_claim':False,'completion':{'path':str(COMPLETION),'sha256':hashlib.sha256(COMPLETION.read_bytes()).hexdigest(),'VM_exit':terminal['emulator_exit'],'collector_exit':terminal['collector_exit'],'normal_stop_exit':terminal['normal_stop']['exit']},'limits':['This final fixed byte range equals the entire captured file after actual terminal completion; absence claims do not extend beyond that captured log.','All original W/E/F remain in the continuous source. Normalization groups repeated messages but does not remove, fix or exempt them.','Critical regex matches are review candidates, not automatically unique incidents or target crashes.']}
path=OUT/'warning-final-classification1.json';assert not path.exists();path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'prefix_bytes':st.st_size,'prefix_sha256':digest.hexdigest(),'all_PID_levels':dict(levels),'target_PID2047_levels':dict(target),'warning_error_groups':len(entries),'critical_candidates':len(critical),'report':str(path),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()},indent=2),flush=True)
