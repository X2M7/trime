# t9.4 Warning and SAF Audit: Stage A

Date: 2026-09-06. This follows the [t9.3 upstream merge](../t9/UPSTREAM-t9.3.zh-CN.md).
The minimum Android version remains API 21. No upstream push, Release publication,
or latest-release change is part of this audit. This is the initial frozen build;
the final follow-up also addresses two issues discovered during this verification.

## Frozen Build

| Field | Value |
| --- | --- |
| Package | `com.osfans.trime.debug` |
| Display name / signing | Trime; debug signing, not a release-signed APK |
| versionName / versionCode | `3.3.13-t9.4` / `20261105` |
| minSdk / targetSdk | 21 / 37 |
| Production Git SHA | `5a02dcd5566e28199cf93b75d6cb74f9e633b5d7` |
| APK SHA-256 | `da453c93969c0c7ae5efd11e3fe26d1a70c0e5f0bd283eace3830b513de4e776` |
| Signer SHA-256 | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| ABI | `arm64-v8a` |
| Nine-key schema | `luna_pinyin_t9` |
| Schema SHA-256 | `9f04415df4bc0342dfa9a11212ef34d57445c60c6b234c9e73835c685308c41f` |
| Rime resource digest | `05df6f1e0cd576a4955b23452ef18b0bda60095341c3cd149f840dd0a39cc413` |
| Native library SHA-256 | `d0a44d7e4ac7c0656e633f786505905abfeacb813f8feb03e7f4410cbeb2dcf9` |
| T00 identity directory | `build/baseline/da453c93969c-0f1f0fba537a/` |

Archived APK: `build/baseline/da453c93969c-0f1f0fba537a/inputs/application.apk`.
Production was built from a clean worktree. The later test-only commit
`72cde2aa962f3d3f2f67b799e265a483ec70c4cd` strengthened keyboard visibility checks;
the production APK was not rebuilt or replaced during device testing.
The enhanced test APK SHA-256 is
`9bb7af446f3877e161ba9667807cfd44c1b18e1e11afd5a90900a2b8ff12c1de`.
The original test APK used for the first API 35 SAF/startup run has SHA-256
`7b211b8faffa46944444828f829d870e6f5b8dda65f14935db4ab1cb806daf5c`.

## Changes

- SAF copies verify streamed contents, handle unknown metadata and nonseekable
  pipes, follow document IDs returned by rename, and preserve recovery backups
  when rollback fails. Unsupported replacement fails without damaging the original.
- Failed or incomplete provider listings abort before orphan cleanup. Duplicate
  names/IDs, unsafe paths and symbolic-link aliases are rejected. Folder creation
  is synchronized and an existing path must actually be a directory.
- Trime's document provider validates containment, refuses root mutation, reports
  accurate flags, and checks copy/move/delete results and directory cycles.
- Keyboard ownership is instance-scoped; UI dependencies no longer retain a static
  keyboard context. Candidate updates use range notifications or DiffUtil.
- Clipboard-editor IME visibility uses the insets controller. Crash reporting
  delegates to the previous exception handler even if report generation fails.
- Compatible dependency upgrades, resource cleanup, plural translations, API
  replacements and explicit view-constructor decisions address the warning audit.

## Build Checks

- JVM tests: **212 passed**, no failures, errors or skips.
- Baseline-tool tests: **26 passed**. These are tool tests, not T00 performance runs.
- `assembleDebug`, `assembleDebugAndroidTest` and scoped formatting passed.
- Final `lintDebug`: **0 errors, 0 warnings** with warnings treated as errors.
  This includes **11 narrowly exempted dependency-update notices** for versions
  incompatible with API 21, not 11 completed upgrades. See [API21.md](API21.md).
  Seven anchored patterns match exactly the audited coordinate/version pairs;
  changed versions are not exempt. No general baseline or `NewApi` override exists.
- Two programmatically constructed views have justified local `ViewConstructor`
  annotations. Release-build Lint checking is re-enabled, but this audit executed
  the debug variant, not a release/R8 build.
- APK-extracted native controller: **185 checks plus 3 separate-process learning
  checks passed**, using an isolated test dictionary and temporary data directory.

Build, Lint and raw device evidence is under `build/saf-warning-audit/`; native
results are under `build/t9/c0e8a3396ba7/`. These generated files remain local.
The APK reuses the verified unchanged native binary; JNI and packaged Rime
resources are byte-identical to t9.3. This was not a fresh native-library compile.

## Device Verification

Both devices run the frozen ARM64 APK through the x86_64 native bridge, at
720 x 1600 / 280 dpi. Only one emulator ran at a time, with 2 GiB guest RAM and
two virtual CPUs. Gradle used one worker, 1280 MiB heap and 768 MiB Metaspace;
Gradle and the emulators were not run together. Guest RAM is not total host RSS.

| Device | System fingerprint |
| --- | --- |
| `hk_api35`, Android 15 / API 35, read-only session | `google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.043/12960925:userdebug/dev-keys` |
| `trime_saf_api37`, Android 17 / API 37, dedicated test AVD | `google/sdk_gphone64_x86_64/emu64xa:17/CE2A.260420.019/15611780:userdebug/dev-keys` |

- Both APIs passed 21 fixture/Trime-provider checks: 16 injected-provider cases,
  four provider security/flag assertions and one own-provider round trip.
- Both APIs passed real `com.android.externalstorage.documents` create/replace/
  read-back/cleanup with a directory selected through the system picker, a
  repeated round trip after force-stop, and actual access denial after releasing
  the persisted grant. These are process-restart checks, not device-reboot tests.
- API 37 also completed the app's picker/import/deploy workflow: the loading
  dialog closed, external-storage mode and selected UUID directory remained, and
  the engine reported deployment success. The earlier API 35 picker deployment
  was interrupted before the separate provider probes; it is not an end-to-end
  import/deploy pass on that API.
- Startup/theme/cache/T9 portrait-landscape regression passed on both APIs.
  API 35: 3073 main-loop ticks, largest gap 633 ms. API 37: 4078 ticks, largest
  gap 1769 ms. These are below the probe's 4000 ms stall threshold, not phone
  latency benchmarks.
- The enhanced clipboard-editor probe passed on both APIs with real keyboard
  bounds `[0,1079][720,1516]` and nonblank screenshots. API 35 showed nine-key
  input; API 37 showed full-pinyin input.
- API 35 actual keyboard touches verified full-pinyin `nihao` followed by selecting
  and submitting the first candidate. Nine-key `64426` offered `mi/ni`; selecting
  `mi`, undoing and selecting `ni` retained `426` and left the editor empty. Only
  selecting the Chinese candidate submitted text. The editor XML assertions
  confirmed both final commits were U+4F60 U+597D and all lock states were empty.

The archive contains 163 hash-verified artifacts under the T00 identity directory's
`saf-regression/`. `api35-final/` contains enhanced visibility checks and actual UI
touch evidence. Installed APK identities were verified by T00 capture; stage-A
capture directories are `capture-b393671c8ca1`, `capture-d6bfc253b591` and
`capture-9abfae99a882`. The first capture was after app exit and is not memory
performance evidence; the last capture shows the live nine-key lock state.

The original API 35 insets-only clipboard probe first failed behind a System UI
ANR dialog; a retry reported success but its screenshot was blank. That false
positive is explicitly discarded. Commit `72cde2aa` now requires focused editor
and window, a visible `keyboard_view`, stable bounds and actual screenshot pixels.

API 37 first boot recorded 14 system-service ANRs and one Trime instrumentation
startup ANR. The latter trace stops in ART Dex verification while initializing
instrumentation, before application/test execution. A stable-system retry passed;
this is evidence consistent with boot contention, not a proven production ANR fix.
Two startup attempts were then intentionally stopped to correct root-imported
test-cache ownership/SELinux labels. Their raw failures remain archived. No
SELinux enforcement or application permissions were relaxed. The final API 37
crash buffer is empty and there was no later Trime ANR in the captured events.

Runtime logs still include intentional fault-injection warnings, missing-theme
fallback warnings, emulator/native-bridge notices and slow background-query
diagnostics. The zero-warning result refers to this build and Lint checks,
not to eliminating all diagnostic runtime logging.

Two additional observations are addressed by the final follow-up, not by this
frozen APK: the optional absent `candidate_border_color` generated an unnecessary
warning; entering Rime's F4 switcher and then the app schema picker incorrectly
reported no available schema. Escape restored the normal picker. The fixes keep
the no-border appearance and use the actual schema list to determine emptiness.

## Scope

See [SAF.md](SAF.md) for repeatable probes and provider limitations. Existing-file
replacement requires provider rename support. Incomplete cloud listings fail
closed; unknown metadata is streamed and verified rather than assumed unchanged.

API 21 manifest/dependency checks do not establish an Android 5.x runtime pass.
Android 5.x hardware, OEM/cloud providers, release signing/R8 and long-duration
use remain untested. The current APK's T00 cold/learned cohorts remain `not_run`;
UI checks using a system-dictionary cache are not cold-start or ranking benchmarks.
No personal user dictionary was cleared. Passing this defined regression set is
not a claim that every possible bug has been eliminated.
