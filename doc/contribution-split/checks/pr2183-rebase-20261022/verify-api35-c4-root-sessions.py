import datetime
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
HEAD = 'c4b613f78835e7b914e02447955c6b89fe9839ab'
APK = '453c71544f67685db3b85fc91855ed27c5ff264d0a25671669642b0dc4b8732e'
RUNNERS = {
    'v7': '3f7739c86af44fd08b97598286f9e697ce500473c7dc5d642466cf77229b6846',
    'v8': '554eb1f704b058d8591bf2ae57a76c5372f56deb549c3612ee5efb9da5d787ea',
}
THEMES = {
    'trime': 'ff65e4a8ada630dcd14b9d7f036201579ddefa4758868df1b497b45dcfef2d08',
    'standard': 'e758ce8f9059f0fbe3c27c2de9c5faffff761b825feed437075f285e15af9147',
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


sessions = []
for theme, cases in (
    ('trime', ['R4', 'K2', 'K3', 'K4', 'S1-v8']),
    ('standard', ['R4', 'CATEGORY', 'K2', 'K3', 'K4', 'S1-v8', 'S2-v8']),
):
    for case in cases:
        name = theme + '-' + case
        seq = ROOT / 'runtime' / ('api35-c4-' + name) / 'sequence-result.json'
        report = seq.with_name('report.json')
        stop = seq.parent / 'observer8-session/finished.json'
        qual = ROOT / 'runtime/api35-c4-independent1' / (name + '-qualification1.json')
        data = json.loads(seq.read_text())
        raw = json.loads(report.read_text())
        finished = json.loads(stop.read_text())
        q = json.loads(qual.read_text())
        version = 'v8' if case.endswith('-v8') else 'v7'
        checks = {
            'case_matches': data['case'] == case.removesuffix('-v8'),
            'theme_matches': data['theme'] == theme,
            'exact_c4': data['source_head'] == HEAD,
            'exact_original_apk': data['actual_apk_sha256'] == APK,
            'same_target_pid_start': data['frozen_pid'] == {'pid': 2228, 'starttime': '18495'},
            'theme_asset_matches': data['theme_asset_sha256'] == THEMES[theme],
            'correct_v7_or_v8_runner': data['sequence_runner']['sha256'] == RUNNERS[version],
            'strict_and_authenticated_stop': data['strict_sequence_completed'] is True
                and data['authenticated_normal_stop'] is True
                and data['direct_finish_succeeded'] is True
                and data['atexit_fallback_retained'] is False,
            'raw_business_flag_retained': data['business_passed'] is False
                and raw['passed'] is False and raw['checks'] == [],
            'raw_journal_completed': raw['completed'] is True,
            'observer_normal_stop': finished['passed'] is True and finished['stop_received'] is True
                and finished['capture_count'] == finished['completed_capture_count']
                and finished['capture_failures'] == 0,
            'independent_scope_pass': str(q['verdict']).startswith('PASS_API35'),
        }
        assert all(checks.values()), (name, checks)
        sessions.append({
            'case': name,
            'sequence_result': str(seq.relative_to(ROOT)),
            'sequence_result_sha256': sha(seq),
            'raw_journal': str(report.relative_to(ROOT)),
            'raw_journal_sha256': sha(report),
            'observer_finished': str(stop.relative_to(ROOT)),
            'observer_finished_sha256': sha(stop),
            'captures': finished['capture_count'],
            'independent_qualification': str(qual.relative_to(ROOT)),
            'qualification_sha256': sha(qual),
            'qualification_verdict': q['verdict'],
            'checks': checks,
        })

assert len(sessions) == 12
result = {
    'utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'scope': 'Root HOST-only independent identity/strict-stop binding for current c4 API35; '
        'separate immutable oracle qualifications establish actual case behavior. '
        'No ADB, VM or Gradle operations performed by this script.',
    'source': HEAD,
    'successful_sessions': len(sessions),
    'logical_items': 14,
    'K1_rule': 'K1 is embedded in each theme R4 exactly once, not separate sessions.',
    'captures': sum(x['captures'] for x in sessions),
    'physical': 'NOT_RUN',
    'system_events': 'Preinstall phone and SystemUI ANRs remain in original full stream. '
        'No preinstall ps-A was captured; no exhaustive no-process or unique cause claim.',
    'terminal_cleanup_and_system_log_audit': 'BOUND_SEPARATELY_IN_FINAL_COMBINED_SCOPE',
    'limits': 'Not full historical 17 cases, expanded-candidate scroll recovery, whole-app '
        'acceptance, real device, TalkBack, Release, upgrade or performance qualification.',
    'sessions': sessions,
}
out = ROOT / 'runtime/api35-c4-root-session-index1.json'
assert not out.exists(), 'Do not overwrite a frozen review'
out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'path': str(out), 'sha256': sha(out), 'sessions': len(sessions),
                  'captures': result['captures']}, ensure_ascii=False))
