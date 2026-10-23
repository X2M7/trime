#!/usr/bin/env python3
"""Read-only primitives for externally frozen fixture bookends. No implicit pass or device calls."""
import hashlib, re
from pathlib import Path

LINE = re.compile(rb'^\d\d-\d\d\s+\S+\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+([^:]+):\s?(.*?)(?:\r?\n)?$')


def sha_bytes(data): return hashlib.sha256(data).hexdigest()


def log_range(log_path, marker_tag, begin_message, end_message, target_family='com.osfans.trime.debug'):
    """Exact tag/message marker identity; raw byte boundaries and all whole-log starts retained."""
    p=Path(log_path);before=p.stat();raw=p.read_bytes();after=p.stat()
    begin=begin_message.encode();end=end_message.encode();tag=marker_tag.encode()
    pattern=re.compile(rb'(?:Start proc|am_proc_start).*'+re.escape(target_family.encode())+rb'(?:[:/\s,}\]]|$)')
    begin_rows=[];end_rows=[];starts=[];offset=0
    for number,line in enumerate(raw.splitlines(keepends=True),1):
        m=LINE.match(line)
        if m:
            record=dict(line=number,byteOffset=offset,bytes=len(line),pid=int(m[1]),tid=int(m[2]),
                        priority=m[3].decode(),tag=m[4].strip().decode(errors='replace'),
                        message=m[5].decode(errors='replace'),rawSha256=sha_bytes(line),raw=line.decode(errors='replace'))
            if m[4].strip()==tag and m[5]==begin:begin_rows.append(record)
            if m[4].strip()==tag and m[5]==end:end_rows.append(record)
            if pattern.search(line):starts.append(record)
        offset+=len(line)
    stable=(before.st_ino,before.st_size,before.st_mtime_ns)==(after.st_ino,after.st_size,after.st_mtime_ns)
    result=dict(source=dict(path=str(p),bytes=len(raw),sha256=sha_bytes(raw),inode=after.st_ino,stableDuringRead=stable),
        exactBeginMatches=begin_rows,exactEndMatches=end_rows,wholeLogTargetProcessStarts=starts,
        scope='Whole-log starts are retained even outside the strict interval. Empty interval starts never means Application never started in this VM.',
        strictRangeQualified=False)
    if not stable:
        return dict(result,state='PENDING_SOURCE_STILL_CHANGING')
    if len(begin_rows)!=1 or len(end_rows)!=1 or begin_rows[0]['byteOffset']>=end_rows[0]['byteOffset']:
        return dict(result,state='UNQUALIFIED_MISSING_DUPLICATE_OR_REVERSED_MARKERS')
    b,e=begin_rows[0],end_rows[0];left=b['byteOffset']+b['bytes'];right=e['byteOffset']
    inside=[x for x in starts if left<=x['byteOffset']<right]
    prefix=raw[:e['byteOffset']+e['bytes']]
    result.update(state='BOUNDED_SOURCE_COMPLETE',strictInterval=dict(firstByteInclusive=left,lastByteExclusive=right,
        bytes=right-left,sha256=sha_bytes(raw[left:right]),lineCount=len(raw[left:right].splitlines())),
        prefixThroughActualEndMarker=dict(bytes=len(prefix),sha256=sha_bytes(prefix)),strictRangeTargetProcessStarts=inside,
        targetStartsBefore=[x for x in starts if x['byteOffset']<left],targetStartsAfter=[x for x in starts if x['byteOffset']>=right],
        strictRangeQualified=not inside)
    return result


def sampled_ps(paths,target_family='com.osfans.trime.debug'):
    """Sampled process facts only; no invented continuity or /proc starttime."""
    records=[]
    for item in paths:
        p=Path(item);data=p.read_bytes();lines=data.decode(errors='replace').splitlines()
        if not lines:
            records.append(dict(path=str(p),qualified=False,failure='empty ps output'));continue
        cols=lines[0].split()
        if 'PID' not in cols or 'NAME' not in cols:
            records.append(dict(path=str(p),qualified=False,failure='unrecognized ps header',header=lines[0]));continue
        matches=[]
        for line in lines[1:]:
            values=line.split()
            if len(values)<=cols.index('PID'):continue
            # Both frozen API21 and API35 commands place the NAME column last.
            name=values[-1]
            if name==target_family or name.startswith(target_family+':'):
                matches.append(dict(pid=int(values[cols.index('PID')]),name=name,raw=line))
        records.append(dict(path=str(p),bytes=len(data),sha256=sha_bytes(data),qualified=not matches,
                            actualTargetProcesses=matches))
    return dict(qualified=bool(records) and all(x['qualified'] for x in records),samples=records,
                limit='Only these actual ps samples. Not proof of process absence at unsampled times; continuous log checked separately.')
