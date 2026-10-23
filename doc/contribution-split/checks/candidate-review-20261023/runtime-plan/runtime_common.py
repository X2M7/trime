"""HOST identity helpers. Importing never runs ADB or starts a VM."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HEAD = '92b13711162c5fa56c701267c3a06e5fbceeb6ce'
SOURCE = '59cf13b4b42b09acbb06f41922c2480844ffa96a7399494b565e718a4a12a05c'


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def frozen(path):
    value = json.loads(Path(path).read_bytes())
    assert value['state'] == 'FROZEN_PR2187_ORIGINAL_CLOUD_AND_DIAGNOSTIC', 'UNFROZEN'
    assert value['source_head'] == value['actual_build_commit'] == HEAD
    assert value['production_view_source_sha256'] == SOURCE
    for record in value['frozen_files']:
        p = Path(record['path'])
        assert p.is_file() and not p.is_symlink() and sha(p) == record['sha256'], p
    assert sha(value['original_apk_path']) == value['original_apk_sha256']
    assert value['original_apk_sha256'] != 'UNFROZEN'
    audit = Path(value['original_artifact_audit_path'])
    assert sha(audit) == value['original_artifact_audit_sha256']
    qualification = json.loads(audit.read_bytes())
    assert qualification['passed'] is True and qualification['source_head'] == HEAD
    assert qualification['source_tree'] == value['source_tree']
    matches = [x for x in qualification['apks'] if x['abi'] == 'x86_64']
    assert len(matches) == 1 and matches[0]['sha256'] == value['original_apk_sha256']
    assert matches[0]['buildconfig_commit_hash'] == HEAD
    return value


def sdk_snapshot(reference, output):
    """Read original SDK files only; never read/write private AVD disks here."""
    assert not output.exists()
    prior = json.loads(Path(reference).read_bytes())
    rows = []
    for record in prior['files']:
        p = Path(record['path'])
        digest = sha(p)
        rows.append(dict(path=str(p), bytes=p.stat().st_size, sha256=digest,
                         unchanged=digest == record['sha256']))
    result = dict(reference=str(reference), reference_sha256=sha(reference), files=rows,
                  all_unchanged=all(x['unchanged'] for x in rows), read_only=True)
    output.write_text(json.dumps(result, indent=2) + '\n')
    return result
