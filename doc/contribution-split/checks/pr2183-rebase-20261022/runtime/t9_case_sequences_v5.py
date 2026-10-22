"""Import-only sequences for the existing v6 Smoke journal; importing runs no ADB.

After a new exact original APK is bound, native Success is independently observed,
and the operator has selected the stated theme/T9 schema using the public UI:

    exec(compile(Path(SEQUENCES).read_text(), SEQUENCES, 'exec'))
    run_and_stop(S, 'standard', 'R4')

Each call uses one already-started Observer9 session and really stops/waits at the
case boundary, including on a rejected business assertion. It does not start a
VM, install/change an APK, select an IME/theme, write prefs/YAML, or mark images
reviewed. Original v6 captures/oracles/rejections and fixed limits are retained.
The custom-drawn key coordinates require 720x1280/dpi320/font1 and the exact
unchanged theme assets below. A root reviewer must inspect the actual images;
strict text/node predicates alone do not qualify a whole case as passed.
"""

import atexit
import hashlib
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET
import zipfile


REQUIRED_HEAD = 'b14a2f1e0565cec4953f5a974bfcea023875369f'
ASSETS = {
    'trime': ('assets/shared/trime.yaml',
              'ff65e4a8ada630dcd14b9d7f036201579ddefa4758868df1b497b45dcfef2d08'),
    'standard': ('assets/shared/tongwenfeng.trime.yaml',
                 'e758ce8f9059f0fbe3c27c2de9c5faffff761b825feed437075f285e15af9147'),
}


def _need(value, message):
    if not value:
        raise AssertionError(message)


def _write(S, name, value):
    (S.out / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def _nodes(S, **attributes):
    _need(S.last_xml is not None, 'An actual current capture is required')
    return [n for n in S.last_xml.iter('node')
            if all(n.get(k) == v for k, v in attributes.items())]


def _visible_click(S, node, reason):
    # Reuse the existing helper's ancestor-aware clipped rectangle, not raw text
    # bounds (candidate annotations are not independent candidate click targets).
    import t12_release_setup_gui_v31 as journal
    left, top, right, bottom = journal.clipped_box(S.last_xml, node)
    _need(left < right and top < bottom, 'Actual clickable rectangle is empty')
    S.report['actions'].append(dict(action='sequence_actual_node_binding',
        reason=reason, attributes=dict(node.attrib),
        clipped_bounds=[left, top, right, bottom]))
    S.persist()
    S.tap((left + right) // 2, (top + bottom) // 2)


def _description(S, value):
    nodes = _nodes(S, **{'content-desc': value, 'clickable': 'true', 'enabled': 'true'})
    _need(len(nodes) == 1, 'Exact accessible action is missing/ambiguous: ' + value)
    _visible_click(S, nodes[0], value)


def _candidate(S, text):
    leaves = _nodes(S, text=text, package='com.osfans.trime.debug',
                    **{'class': 'android.widget.TextView', 'enabled': 'true'})
    parents = {child: parent for parent in S.last_xml.iter() for child in parent}
    targets = []
    for leaf in leaves:
        node = leaf
        while node is not None and node.get('clickable') != 'true':
            node = parents.get(node)
        if node is not None and node.get('long-clickable') == 'true':
            targets.append((leaf, node))
    _need(len(targets) == 1, 'One actual candidate parent required: ' + text)
    S.report['actions'].append(dict(action='sequence_candidate_leaf_binding',
                                   text=text, leaf=dict(targets[0][0].attrib)))
    S.persist()
    _visible_click(S, targets[0][1], 'native candidate ' + text)


def _cap(S, label, expected='', oracle=True):
    S.capture(label, expected=expected, oracle=oracle)


def _fresh(S, label, mode='multiline'):
    S.launch(mode, text='')
    S.tap(240, 100)  # Actual independent editor focus, never input-event injection.
    _cap(S, label)


def _digits64(S):
    S.tap(468, 884)
    S.tap(94, 884)


def _cancel(S):
    _description(S, 'Cancel composition')


def _lock(S, pinyin):
    S.label(pinyin)


def _locked(S, value):
    _need(len(_nodes(S, **{'content-desc': 'Current: Locked syllable: ' + value,
                          'selected': 'true'})) == 1,
          'Current locked syllable is not ' + value)


def _k1(S, theme, prefix):
    _fresh(S, prefix + '-initial')
    _digits64(S)
    _cap(S, prefix + '-64')
    _lock(S, 'ni')
    _cap(S, prefix + '-ni-locked')
    _locked(S, 'ni')
    for x, y in [(94, 884), (280, 764), (468, 884)]:
        S.tap(x, y)
    _cap(S, prefix + '-hao-options')
    _lock(S, 'hao')
    _cap(S, prefix + '-nihao-locked')
    _candidate(S, '你好')
    _cap(S, prefix + '-selected-nihao', '你好')
    S.tap(468, 884)
    _cap(S, prefix + '-new6', '你好')
    _cancel(S)
    _cap(S, prefix + '-cancel6', '你好')


def _k2(S, theme, prefix):
    _fresh(S, prefix + '-raw-initial')
    _digits64(S)
    _cap(S, prefix + '-raw64-before')
    S.tap(640, 884)
    _cap(S, prefix + '-raw64-enter', '64')
    _fresh(S, prefix + '-chat-before', 'chat')
    S.tap(640, 884)
    _cap(S, prefix + '-chat-action4')
    _need(bool(_nodes(S, text='action: 4')), 'Actual chat action4 was not recorded')
    _fresh(S, prefix + '-multiline-before')
    S.tap(640, 884)
    _cap(S, prefix + '-multiline-newline', '\n')


def _k3(S, theme, prefix):
    _fresh(S, prefix + '-initial')
    S.longpress(64, 1124)
    _cap(S, prefix + '-ascii-letter')
    if theme == 'standard':
        S.tap(72, 872)
        _cap(S, prefix + '-literal-a', 'a')
        S.tap(134, 1124)
        _cap(S, prefix + '-literal-symbol-page', 'a')
        S.tap(252, 720)
        _cap(S, prefix + '-literal-exclamation', 'a！')
        S.tap(60, 1140)  # Actual 返回, not right-bottom Return.
        expected = 'a！'
    else:
        S.tap(36, 934)
        _cap(S, prefix + '-literal-a', 'a')
        S.longpress(108, 1134)  # Existing letter -> numeric -> remembered T9.
        _cap(S, prefix + '-number-from-ascii', 'a')
        S.tap(216, 1134)
        _cap(S, prefix + '-restored-T9', 'a')
        S.tap(640, 1004)
        _cap(S, prefix + '-literal-symbol-page', 'a')
        S.tap(108, 834)
        _cap(S, prefix + '-literal-exclamation', 'a!')
        S.tap(54, 1134)
        expected = 'a!'
    _digits64(S)
    _cap(S, prefix + '-returned64', expected)
    _cancel(S)
    _cap(S, prefix + '-cancel64', expected)
    _fresh(S, prefix + '-pending-initial')
    _digits64(S)
    _cap(S, prefix + '-pending-switch-before')
    S.longpress(64, 1124)
    _cap(S, prefix + '-pending-switch-after', '你')


def _k4(S, theme, prefix):
    _fresh(S, prefix + '-old-initial')
    _digits64(S)
    _cap(S, prefix + '-old64')
    _lock(S, 'ni')
    _cap(S, prefix + '-old-ni')
    _locked(S, 'ni')
    old = json.loads((S.out / (prefix + '-old-ni-after-oracle.json')).read_bytes())
    _fresh(S, prefix + '-new-initial', 'chat')
    S.tap(468, 884)
    _cap(S, prefix + '-fresh-chat6')
    new = json.loads((S.out / (prefix + '-fresh-chat6-after-oracle.json')).read_bytes())
    _need(old['fixture_pid'] != new['fixture_pid']
          and old['activity_instance'] != new['activity_instance'],
          'K4 requires an actual independent new editor instance')
    _cancel(S)
    _cap(S, prefix + '-cancelled-new-editor')


def _s1(S, theme, prefix):
    _fresh(S, prefix + '-initial')
    S.longpress(64, 1124)
    _cap(S, prefix + '-plain-letter-before')
    S.doubletap(*( (54, 996) if theme == 'standard' else (36, 1134) ))
    _cap(S, prefix + '-plain-double-shift')
    for unused in range(2):
        S.tap(*( (72, 872) if theme == 'standard' else (36, 934) ))
    _cap(S, prefix + '-plain-aa', 'aa')


def _s2(S, theme, prefix):
    _need(theme == 'standard', 'Only standard default explicitly binds Shift double_click')
    # Start from T9, then use the actual public schema selector. A fresh editor
    # must retain full default; no manual layout workaround is allowed.
    _fresh(S, prefix + '-initial-t9')
    _select_schema(S, theme, '朙月拼音', prefix + '-to-full', t9=True)
    _fresh(S, prefix + '-full-default-before')
    S.longpress(360, 1124)  # default space long_click Mode_switch, no layout change.
    _cap(S, prefix + '-full-default-ascii')
    S.doubletap(54, 996)
    _cap(S, prefix + '-explicit-double-shift')
    S.tap(72, 872)
    S.tap(72, 872)
    _cap(S, prefix + '-explicit-AA', 'AA')


def _category(S, theme, prefix):
    _need(theme == 'standard', 'The /bd category macro is a standard-theme control')
    _fresh(S, prefix + '-initial')
    S.tap(640, 1004)
    _cap(S, prefix + '-symbol-page')
    S.tap(44, 1020)
    _cap(S, prefix + '-punctuation-candidates')
    _candidate(S, '、')
    _cap(S, prefix + '-selected', '、')
    S.tap(44, 1124)
    S.tap(468, 884)
    _cap(S, prefix + '-new6', '、')
    _cancel(S)
    _cap(S, prefix + '-cancel6', '、')
    S.tap(64, 1124)
    _cap(S, prefix + '-numeric-page', '、')
    S.tap(416, 746)
    _cap(S, prefix + '-literal2', '、2')
    S.tap(58, 1124)
    _digits64(S)
    _cap(S, prefix + '-return64', '、2')
    _cancel(S)
    _cap(S, prefix + '-cancel64', '、2')


def _select_schema(S, theme, label, prefix, t9):
    if t9:
        S.longpress(640, 1004)  # T9_symbols long_click Menu in both exact assets.
    else:
        S.longpress(45, 1124)  # stock full default: Menu on bottom-left key.
    _cap(S, prefix + '-chooser', oracle=False)
    checked = _nodes(S, **{'class': 'android.widget.CheckedTextView', 'checked': 'true'})
    _need(len(checked) == 1, 'One public selected schema required before changing it')
    S.label(label)
    _cap(S, prefix + '-target-layout-ready', oracle=False)
    # A same-layout luna -> luna_simp change cannot be proven by keyboard glyphs.
    # Reopen the actual public chooser and read its checked target. Wrong layout
    # or an ignored selection must reject, not silently count the branch as run.
    if label == '朙月拼音九键':
        S.longpress(640, 1004)
    else:
        S.longpress(45, 1124)
    _cap(S, prefix + '-current-schema-readback', oracle=False)
    current = _nodes(S, **{'class': 'android.widget.CheckedTextView', 'checked': 'true'})
    _need(len(current) == 1 and current[0].get('text') == label,
          'Public current schema did not change to ' + label)
    S.report['actions'].append(dict(action='sequence_current_schema_readback',
                                   expected=label, actual=dict(current[0].attrib)))
    S.persist()
    _description(S, 'Back')  # Actual system navigation node, no schema injection.
    _cap(S, prefix + '-schema-readback-dismissed', oracle=False)
    _need(not _nodes(S, **{'class': 'android.widget.CheckedTextView', 'checked': 'true'}),
          'Public schema readback chooser did not close')


def _full_mode_cycle(S, theme, prefix):
    # No event, schema, option or YAML injection. Each new mode uses the actual
    # fixture EditorInfo and a real new Activity; screenshots/logs prove layout.
    _fresh(S, prefix + '-new-normal-full')
    _fresh(S, prefix + '-forced-integer', 'integer')
    _fresh(S, prefix + '-forced-email', 'email')
    S.tap(72, 872)  # Actual full QWERTY 'a'; stale number/T9 cannot fake oracle 'a'.
    _cap(S, prefix + '-email-a', 'a')
    _fresh(S, prefix + '-normal-after-forced')
    S.tap(504, 996)  # Actual full QWERTY n.
    S.tap(540, 748)  # Actual full QWERTY i.
    _cap(S, prefix + '-normal-ni')
    _candidate(S, '你')
    _cap(S, prefix + '-normal-selected-ni', '你')


def _r4(S, theme, prefix):
    _fresh(S, prefix + '-initial-t9')
    _select_schema(S, theme, '朙月拼音', prefix + '-T9-to-full', t9=True)
    _full_mode_cycle(S, theme, prefix + '-full')
    if theme == 'trime':
        # Both stock luna schemas resolve to the same locked qwerty family.
        # Public chooser absence is a retained precondition rejection, not a skip.
        _select_schema(S, theme, '朙月拼音·简化字', prefix + '-same-id-simp', t9=False)
        _full_mode_cycle(S, theme, prefix + '-simp')
    _select_schema(S, theme, '朙月拼音九键', prefix + '-full-to-T9', t9=False)
    _k1(S, theme, prefix + '-new-T9-K1')


CASES = dict(K1=_k1, K2=_k2, K3=_k3, K4=_k4,
             S1=_s1, S2=_s2, CATEGORY=_category, R4=_r4)


def run_and_stop(S, theme, case):
    """Run one existing case under the current lease, then normal stop/wait.

    Errors retain raw frames and original exceptions. No case or image is marked
    passed. The current source must be the newly audited replacement of 1754.
    """
    # Even a rejected APK/theme/geometry precondition must close an already
    # started Observer. Keep every potentially failing precondition in this try.
    record = dict(case=case, theme=theme,
        requires_external_visual_qualification=True, business_passed=False,
        raw_journal_pass_flag_is_not_case_qualification=True)
    error = None
    try:
        _need(theme in ASSETS and case in CASES, 'Known explicit case/theme required')
        binding = S.report['source_binding']['value']
        _need(binding['source_head'] == REQUIRED_HEAD
              and binding['actual_build_commit'] == REQUIRED_HEAD,
              'This round requires the exact newly audited b14a source head')
        sequence_path = Path(run_and_stop.__code__.co_filename).resolve()
        _need(sequence_path == Path(binding['sequence_runner_path']).resolve(),
              'Actual loaded sequence path differs from frozen binding')
        sequence_sha = hashlib.sha256(sequence_path.read_bytes()).hexdigest()
        _need(sequence_sha == binding['sequence_runner_sha256'],
              'Actual sequence runner bytes differ from frozen binding')
        record['sequence_runner'] = dict(path=str(sequence_path), sha256=sequence_sha,
            expected_path=binding['sequence_runner_path'],
            expected_sha256=binding['sequence_runner_sha256'])
        _need(S.observer_session is not None, 'Start the fixed existing Observer session first')
        asset, expected = ASSETS[theme]
        with zipfile.ZipFile(binding['original_apk_path']) as apk:
            actual = hashlib.sha256(apk.read(asset)).hexdigest()
        _need(actual == expected, 'Drawn-key coordinates require unchanged exact theme asset')
        record.update(source_head=binding['source_head'],
            actual_apk_sha256=binding['original_apk_sha256'], theme_asset=asset,
            theme_asset_sha256=actual, frozen_pid=S.report['frozen_pid'])
        prefix = theme + '-' + case
        size_raw = S.shell('wm', 'size')
        density_raw = S.shell('wm', 'density')
        size = re.findall(r'^(Physical|Override) size:\s*(\d+x\d+)\s*$', size_raw, re.M)
        density = re.findall(r'^(Physical|Override) density:\s*(\d+)\s*$', density_raw, re.M)
        _need(size and density, 'Actual physical/override display values are missing')
        # Android WM applies Override when present; Physical alone is not the
        # effective geometry if an override is also reported.
        size_effective = dict(size).get('Override', dict(size).get('Physical'))
        density_effective = dict(density).get('Override', dict(density).get('Physical'))
        record['display'] = dict(size_raw=size_raw, density_raw=density_raw,
            effective_size=size_effective, effective_density=density_effective)
        _need(size_effective == '720x1280', 'Expected effective 720x1280 display')
        _need(density_effective == '320', 'Expected effective dpi320')
        font = S.shell('settings', 'get', 'system', 'font_scale').strip()
        _need(font in ('null', '1', '1.0'), 'Default font1 required for explicit coordinates')
        prefs = S.shell('cat', '/data/data/com.osfans.trime.debug/shared_prefs/'
                        'com.osfans.trime.debug_preferences.xml')
        (S.out / (prefix + '-precondition-prefs.xml')).write_text(prefs)
        nodes = ET.fromstring(prefs)
        selected = next((n.text for n in nodes if n.get('name') == 'selected_theme'), None)
        _need(selected == ('tongwenfeng.trime' if theme == 'standard' else 'trime'),
              'Actual selected theme differs from the stated sequence')
        CASES[case](S, theme, prefix)
        record['strict_sequence_completed'] = True
    except BaseException as exc:
        error = exc
        record['strict_sequence_completed'] = False
        record['business_rejection'] = dict(type=type(exc).__name__, message=str(exc))
    finally:
        observer = S.observer_session
        try:
            if observer is not None:
                record['observer_stop_result'] = S.finish()
                record['direct_finish_succeeded'] = True
            else:
                record['observer_was_not_started'] = True
        except BaseException as exc:
            record['direct_finish_succeeded'] = False
            record['stop_rejection'] = dict(type=type(exc).__name__, message=str(exc))
            try:
                S.cleanup()
            except BaseException as cleanup_exc:
                record['cleanup_rejection'] = dict(type=type(cleanup_exc).__name__,
                                                  message=str(cleanup_exc))
            if error is None:
                error = exc
        # A cleanup exception must retain the helper's final atexit attempt. Do
        # not discard that fallback merely because this sequence is returning.
        normal_stop = bool(observer is not None and observer.normal_stop)
        record['authenticated_normal_stop'] = normal_stop
        if observer is None or normal_stop:
            atexit.unregister(S.cleanup)
            record['atexit_fallback_retained'] = False
        else:
            record['atexit_fallback_retained'] = True
        _write(S, 'sequence-result.json', record)
    if error is not None:
        raise error
    return record
