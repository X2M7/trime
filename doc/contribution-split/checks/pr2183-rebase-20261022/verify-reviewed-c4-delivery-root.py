import copy
import datetime
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import zipfile

root = Path(__file__).resolve().parent
delivery = root / 'device-delivery-c4-reviewed'
worktree = Path('/home/xumin/Public/Trime-pr-t9-rebased')

def sha(data):
    return hashlib.sha256(data).hexdigest()

def read_json(path):
    return json.loads(path.read_text())

manifest = read_json(delivery / 'manifest.json')
identity = read_json(delivery / 'candidate-identity.json')
checks = {}
actual = {str(p.relative_to(delivery)) for p in delivery.rglob('*') if p.is_file()}
assert not any(p.is_symlink() for p in delivery.rglob('*'))
expected = {x['path'] for x in manifest['files']} | {'manifest.json', 'SHA256SUMS'}
checks['exact_regular_file_inventory'] = actual == expected and len(actual) == 19
for item in manifest['files']:
    data = (delivery / item['path']).read_bytes()
    assert (len(data), sha(data)) == (item['bytes'], item['sha256']), item['path']
checks['every_manifest_payload_byte_verified'] = True
sums = subprocess.run(['sha256sum', '-c', 'SHA256SUMS'], cwd=delivery,
                      capture_output=True, text=True, check=False)
(root / 'device-delivery-c4-reviewed-sha256sum-root.stdout').write_text(sums.stdout)
checks['sha256sum_complete_exit_zero'] = sums.returncode == 0

original_dir = root / 'schema-layout-regression/c4-fork-run-39806420824-ubuntu1'
apk = delivery / identity['apk']['file']
checks['original_apk_exact_bytes'] = apk.read_bytes() == (original_dir / 'files' / apk.name).read_bytes()
checks['full_audit_exact_bytes'] = (delivery / 'apk-audit-result.json').read_bytes() == (original_dir / 'apk-audit/result.json').read_bytes()
checks['runtime_binding_exact_bytes'] = (delivery / 'runtime-x86_64-binding.json').read_bytes() == (root / 'runtime/original-apk-binding-c4-v7.json').read_bytes()
checks['source_review_exact_bytes'] = (delivery / 'source-independent-review.json').read_bytes() == (root / 'schema-layout-regression/ascii-mode-source-independent1/review1.json').read_bytes()
doc = subprocess.run(['git', 'show', 'c4b613f78835e7b914e02447955c6b89fe9839ab:doc/t9-input.md'],
                     cwd=worktree, check=True, capture_output=True).stdout
checks['behavior_doc_exact_git_blob'] = (delivery / 't9-input.md').read_bytes() == doc
with zipfile.ZipFile(apk) as z:
    native = z.read(identity['native_payload']['entry'])
    checks['native_arm64_elf_and_hash'] = (
        native[:5] == b'\x7fELF\x02' and struct.unpack_from('<H', native, 18)[0] == 183
        and sha(native) == identity['native_payload']['sha256'])
    checks['both_theme_asset_hashes'] = all(sha(z.read(name)) == value for name, value in identity['theme_assets_sha256'].items())
checks['expected_source_and_producer_identity'] = (
    identity['source']['head'] == 'c4b613f78835e7b914e02447955c6b89fe9839ab'
    and identity['source']['tree'] == 'd7639dfce3b6b5646bb004479a688148c1b19900'
    and identity['producer']['run_id'] == 39806420824
    and identity['producer']['artifact_id'] == 12438099455
    and identity['apk']['sha256'] == '12fd34dad063e76a0cdd132bbed83e802e02cd5f5ada80671f7f6935c09619c0'
    and identity['signing']['certificate_sha256'] == 'f6409a2d9db2800f1acc9b2e614d73e4609df3652d265cf2c40eb8a64456deb2'
    and identity['same_producer_as_x86_64_runtime']['runtime_x86_64_apk_sha256'] == '453c71544f67685db3b85fc91855ed27c5ff264d0a25671669642b0dc4b8732e')
signer = (delivery / 'apk-audit-arm64-signer.stdout').read_text()
checks['retained_original_signature_audit_and_two_warnings'] = (
    signer.count('WARNING:') == 2
    and identity['signing']['certificate_sha256'] in signer
    and 'Verified using v1 scheme (JAR signing): true' in signer
    and 'Verified using v2 scheme (APK Signature Scheme v2): true' in signer)

old = read_json(root / 'device-delivery-b14/device-result-template.json')
new = read_json(delivery / 'device-result-template.json')
checks['legacy_17_exact_objects'] = len(new['legacy_17_cases']) == 17 and new['legacy_17_cases'] == old['legacy_17_cases']
before = {x['case_id']: x for x in old['rebased_upstream_supplemental']}
after = {x['case_id']: x for x in new['rebased_upstream_supplemental']}
for case in ['R1', 'R2', 'R3']:
    checks[case + '_exact_object'] = after[case] == before[case]
projection = copy.deepcopy(after['R4'])
addition = projection.pop('c4_forced_ascii_capability')
projection['source_fix_head'] = before['R4']['source_fix_head']
checks['R4_old_projection_unchanged_except_current_head'] = projection == before['R4']
checks['R4_extra_capability_is_additive'] = (
    addition['source_fix_head'] == identity['source']['head']
    and len(addition['theme_subresults']) == 2
    and all(len(x['subchecks']) == 5 for x in addition['theme_subresults']))
statuses = []
def walk(value, path='$'):
    if isinstance(value, dict):
        for k, v in value.items():
            if k == 'status':
                statuses.append({'path': path + '.' + k, 'value': v})
            walk(v, path + '.' + k)
    elif isinstance(value, list):
        for i, v in enumerate(value):
            walk(v, f'{path}[{i}]')
walk(new)
checks['all_44_recursive_statuses_NOT_RUN'] = len(statuses) == 44 and {x['value'] for x in statuses} == {'NOT_RUN'}
checks['superseded_v1_template_unchanged'] = sha((root / 'device-delivery-c4/device-result-template.json').read_bytes()) == 'f87d8d6e5f42c2691fdab37d6e4a373343b1d57ae0dd4e6f351b4a2c6ac6cf1c'
guide = (delivery / 'REAL-DEVICE-ACCEPTANCE.md').read_text()
checks['guide_keeps_data_and_stops_certificate_mismatch'] = '不要通过卸载、清除数据或允许降级来绕过' in guide and '签名不匹配导致无法覆盖安装时立即停止' in guide
report = {
    'utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'scope': 'Root host-only verification of the reviewed v2 handoff, exact payload bytes and protected case objects; no ADB, installation, physical-device or emulator behavioral claim.',
    'manifest_sha256': sha((delivery / 'manifest.json').read_bytes()),
    'checks': checks,
    'verdict': 'PASS_HOST_HANDOFF_ONLY' if all(checks.values()) else 'FAIL',
    'physical_device': 'NOT_RUN',
    'status_fields': statuses,
    'immutable_audit_terminal_status_addendum': {
        'path': 'schema-layout-regression/c4-cloud-terminal1/terminal-status-addendum1.json',
        'sha256': '1899bf46d570f3078b20a617c6b5e3606cc93dc4f3c7f9cda039b7ceed2862a4',
        'effect': 'Only the stale historical endpoint-lag wording is superseded by later official completed/success run/job readback; original payload bytes remain unchanged.'
    },
    'limits': ['Signature is checked through byte identity with the independently audited original APK and its exact signer output; this script does not generate a new signature.',
               'The original ARM64 Debug artifact is not a Release, stable release, upgrade or physical-device acceptance.',
               'All prior v1 payload bytes remain historical; its failed template is not the recording entry point.']
}
out = root / 'device-delivery-c4-reviewed-root-verification.json'
assert not out.exists()
out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'path': str(out), 'sha256': sha(out.read_bytes()), 'checks': checks, 'verdict': report['verdict']}, ensure_ascii=False))
assert all(checks.values())
