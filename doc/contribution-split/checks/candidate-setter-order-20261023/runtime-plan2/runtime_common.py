"""HOST identity helpers. Importing never runs ADB or starts a VM."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HEAD = '7f909060a1d42198e749eb1f9a66399f76bf8710'
SOURCE = 'c92ded471c62aa2a773510784237d60ac822d831bda9298929a6f99b5d3db8c1'
BUILD_HEAD = '9ff9572f2ed289d8af198f81ce08bdac66d8a69c'
REVIEWED_TREE = '2da497746bf3997515b816629ae92e6d2ffef0fb'
BUILD_TREE = '10f5abd560dc4d365553d8edadc62f47986cb7da'


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def frozen(path):
    value = json.loads(Path(path).read_bytes())
    assert value['state'] == 'FROZEN_PR2187_ORIGINAL_CLOUD_AND_DIAGNOSTIC', 'UNFROZEN'
    assert value['source_head'] == HEAD  # Legacy operator field means reviewed PR head only.
    assert value['actual_build_commit'] == BUILD_HEAD
    assert value['reviewed_source_tree'] == REVIEWED_TREE
    assert value['source_tree'] == BUILD_TREE and BUILD_TREE != REVIEWED_TREE
    assert value['whole_tree_equivalent'] is False
    assert value['production_view_source_sha256'] == value['actual_class_source_sha256'] == SOURCE
    assert value['expected_producer']['repository'] == 'osfans/trime'
    assert value['expected_producer']['run'] == 39889136561
    assert value['legacy_operator_source_head_semantics'] == 'reviewed_PR_head_not_actual_build_commit'
    for record in value['frozen_files']:
        p = Path(record['path'])
        assert p.is_file() and not p.is_symlink() and sha(p) == record['sha256'], p
    assert sha(value['original_apk_path']) == value['original_apk_sha256']
    assert value['original_apk_sha256'] != 'UNFROZEN'
    audit = Path(value['original_artifact_audit_path'])
    assert sha(audit) == value['original_artifact_audit_sha256']
    qualification = json.loads(audit.read_bytes())
    assert qualification['passed'] is True and qualification['source_head'] == BUILD_HEAD
    assert qualification['actual_class_source_sha256'] == SOURCE
    assert qualification['source_tree'] == value['source_tree']
    matches = [x for x in qualification['apks'] if x['abi'] == 'x86_64']
    assert len(matches) == 1 and matches[0]['sha256'] == value['original_apk_sha256']
    assert matches[0]['buildconfig_commit_hash'] == BUILD_HEAD
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
