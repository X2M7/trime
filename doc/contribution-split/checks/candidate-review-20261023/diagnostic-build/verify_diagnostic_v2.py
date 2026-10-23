#!/usr/bin/env python3
"""Read-only checks of diagnostic inputs, writing only the host qualification."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import zipfile

HERE = Path(__file__).resolve().parent


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def method(text, name):
    found = re.search(r'(?m)^    (?:private|public|protected)[^\n]*\b' + name + r'\(', text)
    if not found:
        raise RuntimeError('Missing protected probe method ' + name)
    start = found.start()
    brace = text.index('{', start)
    depth = 0
    for end in range(brace, len(text)):
        if text[end] == '{':
            depth += 1
        elif text[end] == '}':
            depth -= 1
            if depth == 0:
                return text[start:end + 1]
    raise RuntimeError('Unterminated protected method ' + name)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('build')
    args = parser.parse_args()
    b = Path(args.build).resolve()
    if b.parent != HERE:
        raise RuntimeError('Only a diagnostic-build child is accepted')
    result = json.loads((b / 'result.json').read_text())
    binding = json.loads((b / 'diagnostic-source-binding.json').read_text())
    source = b / 'source'
    original = (source / 'AutoScaleTextView-production-input.kt').read_text()
    checks = {}
    for variant in ('safe', 'equal_return', 'no_super_measure'):
        path = source / variant / 'AutoScaleTextView.kt'
        current = path.read_text().replace('package org.x2m7.trime.diagnostic.' + variant,
                                          'package com.osfans.trime.ime.core', 1)
        checks[variant + '_source_binding_exact'] = sha(path) == binding['variants'][variant]['sourceSha256']
        if variant == 'safe':
            checks['safe_only_package_changed'] = current == original
        elif variant == 'no_super_measure':
            checks['measure_only_one_call_removed'] = current == original.replace(
                '        super.onMeasure(widthMeasureSpec, heightMeasureSpec)\n', '', 1)
        else:
            review = json.loads((HERE.parent / 'review-comments.stdout.json').read_text())[0]
            suggestion = review['body'].split('```suggestion\n', 1)[1].split('\n```', 1)[0]
            start = current.index('    override fun setText(')
            end = current.index('    override fun onMeasure(', start)
            a = original.index('    override fun setText(')
            z = original.index('    override fun onMeasure(', a)
            desired = ('    override fun setText(\n        charSequence: CharSequence?,\n'
                       '        bufferType: BufferType,\n    ) {\n' + suggestion + '\n    }\n\n')
            checks['literal_review_setter_exact'] = current[start:end] == desired
            checks['literal_review_other_lines_exact'] = current[:start] == original[:a] and current[end:] == original[z:]
    original_probe = (HERE.parent / 'probe-source/CandidateProbe.java').read_text()
    derived_probe = (source / 'CandidateProbe.java').read_text()
    for name in ('sha', 'png', 'ink', 'capture', 'recycleNode', 'textType', 'hasType',
                 'sameContentBuffers', 'bothBufferMutations', 'selectablePrecondition',
                 'prepareGranularity', 'lineActions', 'bothLineActions', 'captureAdditionalStage'):
        checks['oracle_method_exact_' + name] = method(original_probe, name) == method(derived_probe, name)
    checks['thirteen_stage_count_retained'] = '.put("expectedStageCount", 13)' in derived_probe
    commands = json.loads((b / 'commands.json').read_text())
    labels = ['kotlinc', 'javac', 'javap', 'd8-version', 'd8', 'aapt2', 'zipalign', 'sign', 'verify-signature', 'badging']
    checks['ten_host_commands_all_exit_zero'] = [c['label'] for c in commands] == labels and all(c['exit'] == 0 for c in commands)
    checks['all_host_stderr_empty'] = all(p.stat().st_size == 0 for p in b.glob('*.stderr'))
    checks['actual_d8_version_9_4_24'] = (b / 'd8-version.stdout').read_text().startswith('D8 9.4.24 ')
    checks['all_generated_source_hashes_exact'] = all(sha(b / path) == value for path, value in binding['generatedSources'].items())
    checks['all_dependency_hashes_exact'] = all(sha(Path(v['path'])) == v['sha256'] for v in binding['dependencies'].values())
    checks['production_source_input_still_exact'] = sha(Path(binding['productionInput']['path'])) == binding['productionInput']['sha256']
    checks['no_runtime_androidx_in_variant_bytecode'] = b'androidx/core/' not in (b / 'javap.stdout').read_bytes()
    with zipfile.ZipFile(b / 'diagnostic.apk') as archive:
        checks['embedded_binding_exact'] = archive.read('assets/diagnostic-source-binding.json') == (b / 'diagnostic-source-binding.json').read_bytes()
        checks['no_native_libraries'] = not any(name.startswith('lib/') for name in archive.namelist())
        checks['single_dex_035_min21'] = archive.read('classes.dex')[:8] == b'dex\n035\x00'
    signature = (b / 'verify-signature.stdout').read_text()
    checks['signature_v1_v2_verifies'] = ('Verified using v1 scheme (JAR signing): true' in signature
                                        and 'Verified using v2 scheme (APK Signature Scheme v2): true' in signature)
    checks['signature_no_warning_output'] = 'WARNING:' not in signature
    checks['apk_hash_exact'] = sha(b / 'diagnostic.apk') == result['apkSha256']
    qualification = {
        'allPassed': all(checks.values()), 'checks': checks,
        'apk': str(b / 'diagnostic.apk'), 'apkSha256': sha(b / 'diagnostic.apk'),
        'apkBytes': (b / 'diagnostic.apk').stat().st_size,
        'sourceBindingSha256': sha(b / 'diagnostic-source-binding.json'),
        'productionInputSha256': binding['productionInput']['sha256'],
        'variants': binding['variants'],
        'verifierSha256': sha(Path(__file__)),
        'stderrBytes': {p.name: p.stat().st_size for p in sorted(b.glob('*.stderr'))},
        'deviceTestsRun': False, 'notProductionApk': True,
        'prAgpVersion': '9.3.0', 'diagnosticHostBuilderVersion': '9.4.1',
        'sameAgpToolchainAsPr': False,
        'toolchainScope': 'Independent host builder9.4.1 / D8 9.4.24 was selected only to handle Kotlin metadata parsing for the common three-variant diagnostic. Production APK acceptance uses original CI output.',
        'scope': 'HOST diagnostic source derivation, protected oracle identity, compilation, APK signature and provenance only. No emulator, real device, production APK or native-candidate acceptance.',
    }
    (b / 'host-qualification2.json').write_text(json.dumps(qualification, indent=2) + '\n')
    print(json.dumps(qualification, indent=2))
    if not qualification['allPassed']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
