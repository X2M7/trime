#!/usr/bin/env python3
"""Build an explicitly non-production A/B/C Android diagnostic, without Gradle/ADB.

The input AutoScaleTextView is copied byte-for-byte as provenance. Runtime copies
only change its package; the two negative controls have one additionally logged
source alteration each. The existing thirteen-stage probe oracle is unchanged.
"""
import argparse
import difflib
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

HERE = Path(__file__).resolve().parent
PR = HERE.parent
SDK = Path('/home/xumin/Public/android-toolchain/android-sdk')
JDK = Path('/home/xumin/Public/android-toolchain/jdk-17.0.19+10')
CACHE = Path('/home/xumin/.gradle/caches/modules-2/files-2.1')
INPUT_SOURCE = Path('/home/xumin/Public/Trime-pr-candidate-text-layout/app/src/main/java/com/osfans/trime/ime/core/AutoScaleTextView.kt')
PROBE_SOURCE = PR / 'probe-source'
PACKAGE = 'org.x2m7.trime.readiness.candidatediagnostic'
ORIGINAL_PACKAGE = 'org.x2m7.trime.readiness.candidate'
VARIANTS = ('safe', 'equal_return', 'no_super_measure')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def dependency(module, version, suffix='jar'):
    paths = sorted((CACHE / module / version).glob(f'*/*.{suffix}'))
    if len(paths) != 1:
        raise RuntimeError(f'Expected one {module}:{version}:{suffix}, found {paths}')
    return paths[0]


def replace_once(value, old, new):
    if value.count(old) != 1:
        raise RuntimeError(f'Expected exactly one replacement of {old[:100]!r}')
    return value.replace(old, new, 1)


def dump(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--expected-source-sha256', required=True)
    parser.add_argument('--out', required=True,
                        help='Fresh child directory of diagnostic-build; never reused')
    args = parser.parse_args()
    out = Path(args.out).resolve()
    if out.parent != HERE or out.exists():
        raise RuntimeError('A fresh immediate child of diagnostic-build is required')
    if sha(INPUT_SOURCE) != args.expected_source_sha256:
        raise RuntimeError('Production source changed since the approved snapshot')
    original = INPUT_SOURCE.read_text()
    if 'val value = charSequence ?: ""' in original:
        raise RuntimeError('Expected the new nullable safe setter, not the old normalized setter')
    if original.count('super.onMeasure(widthMeasureSpec, heightMeasureSpec)') != 1:
        raise RuntimeError('Expected exactly one superclass measurement')
    if 'super.onLayout(' in original:
        raise RuntimeError('Safe input must already remove redundant super.onLayout')
    start = original.index('    override fun setText(')
    end = original.index('    override fun onMeasure(', start)
    setter = original[start:end]
    if not (setter.index('needsMeasureText = true') < setter.index('super.setText(charSequence, bufferType)')):
        raise RuntimeError('Dirty caches must precede unconditional superclass setter')
    # This exactly follows review comment 4350112633, including its null branch.
    suggested = '''    override fun setText(
        charSequence: CharSequence?,
        bufferType: BufferType,
    ) {
        if (charSequence == null || !text.contentEquals(charSequence)) {
            needsMeasureText = true
            needsCalculateTransform = true
            super.setText(charSequence, bufferType)
            requestLayout()
            invalidate()
        }
    }

'''
    controls = {
        'safe': original,
        'equal_return': original[:start] + suggested + original[end:],
        'no_super_measure': replace_once(original,
            '        super.onMeasure(widthMeasureSpec, heightMeasureSpec)\n', ''),
    }
    out.mkdir()
    sources = out / 'source'; sources.mkdir()
    (sources / 'AutoScaleTextView-production-input.kt').write_bytes(INPUT_SOURCE.read_bytes())
    variant_records = {}
    for variant, value in controls.items():
        namespace = 'org.x2m7.trime.diagnostic.' + variant
        derived = replace_once(value, 'package com.osfans.trime.ime.core', 'package ' + namespace)
        folder = sources / variant; folder.mkdir()
        path = folder / 'AutoScaleTextView.kt'; path.write_text(derived)
        delta = ''.join(difflib.unified_diff(original.splitlines(True), value.splitlines(True),
                    fromfile='safe-production-input', tofile=variant))
        (sources / f'{variant}-vs-safe.diff').write_text(delta)
        variant_records[variant] = {
            'className': namespace + '.AutoScaleTextView',
            'sourceSha256': sha(path),
            'sourceChangeBeyondPackage': 'none' if variant == 'safe' else
                ('literal review comment 4350112633 setter' if variant == 'equal_return' else
                 'remove exactly one super.onMeasure call'),
            'diffSha256': sha(sources / f'{variant}-vs-safe.diff'),
        }

    activity = (PROBE_SOURCE / 'CandidateActivity.java').read_text()
    activity = replace_once(activity, 'package ' + ORIGINAL_PACKAGE + ';', 'package ' + PACKAGE + ';')
    old_loader = '''            Context production = createPackageContext("com.osfans.trime.debug",
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            sourceApk = production.getApplicationInfo().sourceDir;
            Class<?> type = production.getClassLoader().loadClass("com.osfans.trime.ime.core.AutoScaleTextView");'''
    new_loader = '''            sourceApk = getApplicationInfo().sourceDir;
            String variant = getIntent().getStringExtra("variant");
            if (!variant.equals("safe") && !variant.equals("equal_return") && !variant.equals("no_super_measure")) {
                throw new IllegalArgumentException("Unknown diagnostic variant " + variant);
            }
            Class<?> type = getClassLoader().loadClass("org.x2m7.trime.diagnostic." + variant + ".AutoScaleTextView");'''
    activity = replace_once(activity, old_loader, new_loader)
    activity = replace_once(activity,
        'Class<?> mode = production.getClassLoader().loadClass(type.getName() + "$Mode");',
        'Class<?> mode = getClassLoader().loadClass(type.getName() + "$Mode");')
    activity = activity.replace('Loads the installed, unmodified production view; does not start Trime or Rime.',
            'Loads a separately compiled local diagnostic variant; never loads production APK code.')
    activity = activity.replace('Production scale mode absent', 'Diagnostic scale mode absent')
    activity = activity.replace('Could not construct installed production view', 'Could not construct local diagnostic variant')
    activity = activity.replace('Actual production AutoScale', 'NON-PRODUCTION diagnostic AutoScale')
    (sources / 'CandidateActivity.java').write_text(activity)

    probe = (PROBE_SOURCE / 'CandidateProbe.java').read_text()
    probe = replace_once(probe, 'package ' + ORIGINAL_PACKAGE + ';', 'package ' + PACKAGE + ';')
    probe = replace_once(probe,
        '    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }',
        '''    private String diagnosticVariant;
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        diagnosticVariant = args == null ? "safe" : args.getString("variant", "safe");
        if (!diagnosticVariant.equals("safe") && !diagnosticVariant.equals("equal_return") && !diagnosticVariant.equals("no_super_measure")) {
            throw new IllegalArgumentException("Unknown diagnostic variant " + diagnosticVariant);
        }
        start();
    }''')
    probe = replace_once(probe,
        'new Intent(getTargetContext(), CandidateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)',
        'new Intent(getTargetContext(), CandidateActivity.class).putExtra("variant", diagnosticVariant).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)')
    probe = replace_once(probe,
        '            report.put("runId", id).put("sourceApk", activity.sourceApk)',
        '            report.put("notProductionApk", true).put("diagnosticVariant", diagnosticVariant)'
        '.put("originalProductionSourceSha256", "' + args.expected_source_sha256 + '")'
        '.put("diagnosticClassSourceSha256", DiagnosticIdentity.sourceSha256(diagnosticVariant))'
        '.put("runId", id).put("sourceApk", activity.sourceApk)')
    probe = probe.replace('Installed production AutoScale view in a separate Activity.',
            'Separately compiled NON-PRODUCTION source variant in its own diagnostic APK.')
    (sources / 'CandidateProbe.java').write_text(probe)
    identity = 'package ' + PACKAGE + ';\npublic final class DiagnosticIdentity {\n'
    identity += ' public static String sourceSha256(String variant) { switch (variant) {\n'
    for variant, record in variant_records.items():
        identity += f' case "{variant}": return "{record["sourceSha256"]}";\n'
    identity += ' default: throw new IllegalArgumentException(variant); } }\n}\n'
    (sources / 'DiagnosticIdentity.java').write_text(identity)
    manifest = (PROBE_SOURCE / 'AndroidManifest.xml').read_text()
    manifest = replace_once(manifest, 'package="' + ORIGINAL_PACKAGE + '"', 'package="' + PACKAGE + '"')
    manifest = replace_once(manifest, 'android:targetPackage="' + ORIGINAL_PACKAGE + '"', 'android:targetPackage="' + PACKAGE + '"')
    manifest = manifest.replace('Production candidate layout control', 'NON-PRODUCTION candidate A B C diagnostic')
    # No installed application is queried or loaded by this diagnostic.
    manifest = replace_once(manifest, '    <queries><package android:name="com.osfans.trime.debug" /></queries>\n', '')
    (sources / 'AndroidManifest.xml').write_text(manifest)

    dependencies = {
        'compiler': dependency('org.jetbrains.kotlin/kotlin-compiler-embeddable', '2.3.21'),
        'stdlib': dependency('org.jetbrains.kotlin/kotlin-stdlib', '2.3.21'),
        'script': dependency('org.jetbrains.kotlin/kotlin-script-runtime', '2.3.21'),
        'daemon': dependency('org.jetbrains.kotlin/kotlin-daemon-embeddable', '2.3.21'),
        'reflect': dependency('org.jetbrains.kotlin/kotlin-reflect', '1.6.10'),
        'coroutines': dependency('org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm', '1.10.2'),
        'annotations': dependency('org.jetbrains/annotations', '13.0'),
        'coreKtxAar': dependency('androidx.core/core-ktx', '1.16.0', 'aar'),
        'agpBuilderD8': dependency('com.android.tools.build/builder', '9.4.1'),
        'androidJar': SDK / 'platforms/android-37.0/android.jar',
    }
    dependency_dir = out / 'dependencies'; dependency_dir.mkdir()
    core_ktx = dependency_dir / 'core-ktx-1.16.0-classes.jar'
    with zipfile.ZipFile(dependencies['coreKtxAar']) as archive:
        core_ktx.write_bytes(archive.read('classes.jar'))
    classes = out / 'classes'; classes.mkdir()
    dex = out / 'dex'; dex.mkdir()
    records = []
    env = dict(os.environ, JAVA_HOME=str(JDK))

    def run(label, argv):
        result = subprocess.run([str(x) for x in argv], stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env)
        (out / (label + '.stdout')).write_bytes(result.stdout)
        (out / (label + '.stderr')).write_bytes(result.stderr)
        records.append({'label': label, 'command': [str(x) for x in argv], 'exit': result.returncode})
        dump(out / 'commands.json', records)
        if result.returncode:
            raise RuntimeError(f'{label} failed: {result.stderr.decode(errors="replace")}')

    compiler_cp = ':'.join(str(dependencies[key]) for key in ('compiler', 'stdlib', 'script', 'daemon', 'reflect', 'coroutines', 'annotations'))
    source_cp = ':'.join(str(x) for x in (dependencies['androidJar'], dependencies['stdlib'], dependencies['annotations'], core_ktx))
    run('kotlinc', [JDK / 'bin/java', '-Xmx384m', '-cp', compiler_cp,
        'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-Werror',
        '-jvm-target', '1.8', '-classpath', source_cp, '-d', classes,
        *(sources / variant / 'AutoScaleTextView.kt' for variant in VARIANTS)])
    run('javac', [JDK / 'bin/javac', '-J-Xmx128m', '--release', '8', '-Xlint:all', '-Werror',
        '-classpath', dependencies['androidJar'], '-d', classes,
        sources / 'CandidateActivity.java', sources / 'CandidateProbe.java', sources / 'DiagnosticIdentity.java'])
    run('javap', [JDK / 'bin/javap', '-classpath', classes, '-c', '-p',
        *(record['className'] for record in variant_records.values())])
    if b'androidx/core/' in (out / 'javap.stdout').read_bytes():
        raise RuntimeError('Expected withSave to inline completely; unexpected runtime AndroidX dependency')
    class_jar = out / 'diagnostic-classes.jar'
    with zipfile.ZipFile(class_jar, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(classes.rglob('*')):
            if path.is_file(): archive.write(path, path.relative_to(classes))
    bt = SDK / 'build-tools/36.0.0'
    run('d8-version', [JDK / 'bin/java', '-Xmx128m', '-cp', dependencies['agpBuilderD8'],
        'com.android.tools.r8.D8', '--version'])
    run('d8', [JDK / 'bin/java', '-Xmx384m', '-cp', dependencies['agpBuilderD8'], 'com.android.tools.r8.D8',
        '--release', '--min-api', '21', '--lib', dependencies['androidJar'], '--output', dex,
        class_jar, dependencies['stdlib']])
    run('aapt2', [bt / 'aapt2', 'link', '-I', dependencies['androidJar'], '--manifest', sources / 'AndroidManifest.xml',
        '-o', out / 'unsigned.apk'])
    binding = {
        'notProductionApk': True,
        'purpose': 'A/B/C source diagnostic only. No production or native-candidate acceptance.',
        'productionInput': {'path': str(INPUT_SOURCE), 'sha256': args.expected_source_sha256},
        'variants': variant_records,
        'originalProbeInputs': {name: {'path': str(PROBE_SOURCE / name), 'sha256': sha(PROBE_SOURCE / name)}
            for name in ('CandidateActivity.java', 'CandidateProbe.java', 'AndroidManifest.xml')},
        'generatedSources': {str(p.relative_to(out)): sha(p) for p in sorted(sources.rglob('*')) if p.is_file()},
        'dependencies': {key: {'path': str(path), 'sha256': sha(path)} for key, path in dependencies.items()},
        'coreKtxCompileOnly': True,
        'noAdbOrGradle': True,
    }
    dump(out / 'diagnostic-source-binding.json', binding)
    with zipfile.ZipFile(out / 'unsigned.apk', 'a', compression=zipfile.ZIP_DEFLATED) as archive:
        for path in dex.glob('*.dex'): archive.write(path, path.name)
        archive.write(out / 'diagnostic-source-binding.json', 'assets/diagnostic-source-binding.json')
    run('zipalign', [bt / 'zipalign', '-f', '-P', '16', '4', out / 'unsigned.apk', out / 'aligned.apk'])
    signer = [JDK / 'bin/java', '-Xmx128m', '-jar', bt / 'lib/apksigner.jar']
    run('sign', signer + ['sign', '--ks', PR.parent / 'readiness-20261022/diagnostic.keystore',
        '--ks-key-alias', 'readiness', '--ks-pass', 'pass:android', '--key-pass', 'pass:android',
        '--min-sdk-version', '21', '--out', out / 'diagnostic.apk', out / 'aligned.apk'])
    run('verify-signature', signer + ['verify', '--verbose', '--print-certs', '--min-sdk-version', '21', out / 'diagnostic.apk'])
    run('badging', [bt / 'aapt2', 'dump', 'badging', out / 'diagnostic.apk'])
    if sha(INPUT_SOURCE) != args.expected_source_sha256:
        raise RuntimeError('Production source changed during diagnostic build')
    dump(out / 'result.json', {
        'hostBuildPassed': True, 'notProductionApk': True, 'deviceTestsRun': False,
        'apk': str(out / 'diagnostic.apk'), 'apkSha256': sha(out / 'diagnostic.apk'),
        'sourceBindingSha256': sha(out / 'diagnostic-source-binding.json'),
        'productionInputSha256': args.expected_source_sha256,
        'variants': variant_records,
        'invocationTemplate': 'am instrument -w -r -e variant <safe|equal_return|no_super_measure> ' + PACKAGE + '/.CandidateProbe',
        'scope': 'HOST build only; serial actual Android checks remain NOT_RUN.',
    })
    print((out / 'result.json').read_text())


if __name__ == '__main__':
    main()
