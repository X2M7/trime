"""HOST-only exact command/package-state checks; no device calls."""
import json,re
from pathlib import Path
import candidate_oracles as q

def command(record, expected_message=None, allowed_statuses=(0,)):
    stdout=Path(record['stdout']);stderr=Path(record['stderr']);raw=stdout.read_bytes()
    nonce=record.get('shell_protocol_nonce','')
    statuses=re.findall(rb'__TRIME_T12_REMOTE_RC_'+re.escape(nonce.encode())+rb':(-?\d+)\r?\n?$',raw)
    checks=dict(stdoutHash=q.sha(stdout)==record.get('stdout_sha256'),stderrHash=q.sha(stderr)==record.get('stderr_sha256'),
        hostExit0=record.get('returncode')==0,completed=record.get('completed') is True,
        noncePresent=bool(re.fullmatch(r'[a-f0-9]{32}',nonce)),
        actualRemoteAllowed=len(statuses)==1 and int(statuses[0]) in allowed_statuses and record.get('remote_returncode')==int(statuses[0]))
    if expected_message is not None:
        argv=record.get('argv',[])
        checks['exactMarkerCommandPayload']=bool(argv and 'PR2187_CLASS_ONLY' in argv[-1] and expected_message in argv[-1])
    return dict(passed=all(checks.values()),checks=checks,stdout=q.meta(stdout),stderr=q.meta(stderr),actualRemoteStatuses=[int(x) for x in statuses])

def package_state(folder,label):
    p=folder/(label+'-state.json');d=folder/(label+'-package.raw.txt')
    obj=q.load(p);raw=d.read_text();all_users=re.findall(r'^\s*User 0:\s*(.*)$',raw,re.M)
    # Runtime-permission sections can also print a plain User 0: header.
    # Only one actual package-state row is allowed; malformed partial state rows still fail.
    state_keys=('installed','hidden','stopped','enabled')
    users=[row for row in all_users if any(re.search(r'\b'+key+r'=',row) for key in state_keys)]
    fields=dict(re.findall(r'\b(installed|hidden|stopped|enabled)=([^\s]+)',users[0])) if len(users)==1 else {}
    truth=dict(present=obj.get('present') is True,installed=fields.get('installed')=='true',
        enabled=len(users)==1 and fields.get('enabled')==str(obj.get('enabled')),
        stopped=fields.get('stopped')==str(obj.get('stopped')).lower(),
        installedRecord=obj.get('installed') is True,hidden=fields.get('hidden')==str(obj.get('hidden')).lower())
    identity=re.findall(r'^\s*Package \[([^\]]+)\][^\r\n]*:',raw,re.M)
    truth['exactPackageHeader']=identity==[obj.get('package')]
    cmd=obj.get('pm_path_command',{});path_audit=command(cmd,allowed_statuses=(0,1))
    truth['actualPathQueryCommand']=path_audit['passed']
    out=Path(cmd['stdout']).read_bytes(); nonce=cmd['shell_protocol_nonce'].encode()
    marker=re.search(rb'__TRIME_T12_REMOTE_RC_'+re.escape(nonce)+rb':(-?\d+)\r?\n?$',out)
    actual=(out[:marker.start()] if marker else b'').strip().decode(errors='replace')
    if obj.get('apk_path') is None:
        truth['hiddenAbsenceTruthful']=(obj.get('hidden') is True and obj.get('apk_path_query')=='absent_due_to_hidden' and not actual)
    else:
        truth['presentPathTruthful']=(obj.get('apk_path_query')=='PRESENT' and actual=='package:'+obj['apk_path'] and cmd.get('remote_returncode')==0)
    return dict(passed=all(truth.values()),checks=truth,state=obj,stateFile=q.meta(p),actualPackageDump=q.meta(d),
                actualPackageUser0StateLines=users,allUser0LinesIncludingPermissionHeaders=all_users,
                ignoredNoStatePropertyHeaders=[row for row in all_users if row not in users])
