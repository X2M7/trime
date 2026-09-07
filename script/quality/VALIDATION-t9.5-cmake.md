# T9.5 CMake Warning Follow-up

Date: 2026-09-07. This follows the frozen
[T02 functional verification](VALIDATION-t9.5-T02.md).
Android `minSdk = 21`, dependency pins, version name/code and release status
are unchanged. This is not a Release or `latest` update.

## Fix

The build-tree copies of snappy, leveldb and OpenCC declare CMake policy
compatibility through 3.10 while preserving their original minimum versions.
OpenCC's dictionary generator uses `FindPython3` with only the host Interpreter
component, retaining the executable variable its generation commands expect.

No submodule commit or original dependency file was modified. No warning or
deprecation diagnostic was disabled. Adaptations require exactly one matching
declaration and fail with a re-audit error if the expected source changes.
See [the adapter documentation](../../app/src/main/jni/cmake/README.md).

## Verification

- Four adapter regression tests pass on both installed CMake versions, 3.31.6
  and 3.22.1. They cover all four edits, executable host Python, unchanged
  originals, added/deleted files and rejection of missing/duplicate targets.
  All configurations use `-Werror=dev -Werror=deprecated`.
- Android ARM64 configuration with the same strict flags passes. The cache
  confirms developer/deprecation diagnostics remain enabled and fatal.
  A second, entirely fresh build directory also passes these strict checks.
- Source-copy audit checks 2,029 files: exactly the expected four CMake files
  differ. Dependency submodules have clean tracked-file status recursively.
- All 307 compilation commands are unchanged after normalizing the three
  relocated source paths. No translation unit was added or removed.
- CMake formatting and workflow YAML parsing pass. The adapter tests are added
  to the Ubuntu commit CI job.
- The native rebuild completes. The final combined Gradle invocation passes
  `assembleDebug`, `testDebugUnitTest`, `lintDebug` and unhooked `spotlessCheck`.
  The unchanged JVM results contain 216 tests with zero failures/errors/skips;
  unchanged Lint analysis contains zero issues. Configure/build logs contain
  no CMake or compiler warnings.
- All three OpenCC reverse-dictionary generation commands run using host Python
  in the fresh build tree. Their outputs match the packaged resources byte for
  byte. A subsequent native build reports `ninja: no work to do`, ruling out
  an immediate regeneration/rebuild loop.
- The exact new APK library passes **376 controller checks plus 5
  separate-process learning checks** on Android 15 / API 35, using the existing
  `hk_api35` read-only emulator and its ARM64 native bridge. Evidence:
  `build/t9/41d3d26ae6d7/`, state `passed`, empty `engine.log`.

The first aggregate build completed native compilation, packaging, JVM and
Lint tasks, then failed Gradle's implicit-dependency validation in Spotless.
Its root-wide Kotlin target overlapped generated OpenCC data. Explicitly
excluding build/cache directories, native third-party sources and app assets
fixes that validation without adding native generation as a formatting dependency
or excluding application/test Kotlin sources. The same combined invocation then
passes (`gradle-build-final.log`); the first failed log is retained separately.

## Build Identity

| Field | Value |
| --- | --- |
| Package / version / code | `com.osfans.trime.debug` / `3.3.13-t9.5-dev` / `20261106` |
| ABI / minSdk / signing | `arm64-v8a` / 21 / debug |
| Embedded base SHA | `b35e0ab7bdf3fe57c9142cea0a4e80e3287f5518` |
| APK SHA-256 | `10c981ca60e5a98c5d36e1a1ee2f2829fd494b72bb15ee4d79a52a02c1e7fd7f` |
| Native library SHA-256 | `1d5a6870960186bfe9ada329cca80f6451872f9b6a8035369c1cab423bf2a480` |
| Certificate SHA-256 | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |

Frozen APK: `build/baseline/10c981ca60e5-508c1c2fbee4/inputs/application.apk`.
This was built from the dirty worktree before the publishing commit, not the
clean embedded base SHA. The local evidence includes a source patch. APK entry
comparison against the T02 build shows only the native library changes, apart
from the expected v1 signature/manifest updates. DEX, Android resources, bundled
schemes/dictionaries and the signing certificate remain unchanged.

Local evidence is retained in `build/cmake-warning-audit/`, including strict
configure output, adapter tests, source-copy hashes and the compile-command
comparison. The old T02 APK and its 57 hash-verified evidence files remain
separate and are not relabelled as containing this build-system fix.
The new build's selected evidence is archived under its baseline run's
`cmake-evidence/`, with per-file hashes and identity sidecars. Cold/learned T00
ranking cohorts remain `not_run`; this is an artifact audit and native regression,
not a new end-to-end UI/SAF or performance baseline.

## Limits

The existing 11 audited dependency-update exceptions for Android 5.x remain
documented in [API21.md](API21.md). These are distinct from the four CMake
configuration warnings addressed here. Physical Android 5.x runtime, release
signing/R8, phone performance and extended SAF provider coverage remain outside
this build-system follow-up.
The emulator used 2 GiB guest RAM and two cores and never ran with Gradle. Native
compilation used one job; the emulator was closed after the regression completed.
