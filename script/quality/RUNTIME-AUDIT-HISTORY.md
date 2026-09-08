# Historical Runtime Audit Work Log

This file preserves superseded checkpoints. See [RUNTIME-AUDIT.md](RUNTIME-AUDIT.md)
for current status; pending statements below are historical, not final results.

Build under validation: `3.3.13-t9.7-dev.2` / `20261111`.
This is an **in-progress work log, not a passed validation report**.
Previous APKs and evidence archives remain immutable. Do not publish or label
the new APK validated until the checks below are completed.

## Latest Checkpoint

Build 12 completed both APKs, 253 application/four build-logic tests, clean
Lint/Spotless, and warning-free native compilation. Frozen identities are in
`build/t05-runtime/artifacts-v4/`; device evidence is in `final12/`.
On a fresh, dedicated API 35 emulator it passed unseeded deployment and actual
JNI interop, five setup runs, failure/retry, shutdown, feedback, T02, clipboard,
and all 14 T03/T05 layout runs (70 screenshots, both actual themes).
Native tests passed 16 deployment/recovery checks and 743 controller/assist
checks, plus the JNI mock contract. The same deployment regression fails against
the preserved build-11 library. Exact T05 candidate lists remain unchanged;
fixed-set recovery improves from 3/24 to 24/24 compared with build 6.

Real system-picker SAF tests passed round trip, process restart and revocation
(22 checks per run, including deliberate provider failures). They confirmed
binary-userdb protection and hash-backed cleanup, but exposed two additional
normal-flow API-usage warnings. **Build 12 is not a warning-clean final APK.**
Build 13 guards `checkError()` with `canDetectErrors()`, retaining reliable-pipe
failure detection and read-back verification. It also enables the platform back
dispatcher, and binds the input-panel callback to visibility/lifecycle instead
of disabling it permanently after ordinary navigation. These changes await
rebuilt-APK validation on API 21/35. The log gate now recognizes both diagnostics
even though Android framework components, rather than Timber, emit them.

An independent system Settings/LatinIME exercise reproduces ART finalizer and
SurfaceFlinger/IME-animation warnings. Its complete logs remain in `final12/`;
this is not permission to ignore arbitrary platform-tagged diagnostics.

## Build 11 Checkpoint

Build 11 completed 253 application and four build-logic tests, both ABI APKs,
the instrumentation APK, clean Lint/Spotless and warning-free native builds.
Source and artifacts are frozen in `build/t05-runtime/artifacts-v3/`:

- x86_64 APK: `93c7d8b88535ac880e02384484df50d9b95f4e11caa8b26d46a44eb1fdee9fdb`.
- ARM64 APK: `726227fd8ef035b221a851aca9ec2aaea5b6b193a7b43c3ac28c2583da2fc1d5`.
- Driver: `1ff84dd26ad510b655035bed1a26f84ccbe6b7d981ef78cf024a2baf40e410ab`.
- Both native libraries and APKs pass 16KB alignment checks; this is not a
  physical 16KB-page runtime test. All 48 packaged files/50 inventory entries
  match their checksums. All 193 excluded presets match prior encode failures.
- Fresh dedicated API 35: five setup runs pass (maximum main gaps 692, 1165,
  1036, 1237, 924ms). True unseeded deployment and actual-VM CheckJNI Unicode,
  NUL and exception tests pass; deployment heartbeat maximum gap 137ms, full
  engine-probe maximum gap 940ms. Shutdown/reconnect/restart passes (178ms).
- Engine normal flow contains zero project diagnostics. Its 14 project W/E
  lines are confined to the explicitly marked missing-theme injection window;
  11 platform diagnostic lines remain. Raw logs are not warning-free.
- **Build 11 is not accepted as final validation.** Failure recovery revealed
  that a failed native workspace still publishes `var/last_build_time`; retry
  can then skip rebuilding and retain the broken schema list. The new native
  regression fails against this exact library in `build/t9/444ea4b0cc32/`.
- The overlong-path test incorrectly required a thrown cause even when native
  deployment correctly returned false. The feedback driver also exposed a race
  between instrumentation `onStart` and application initialization. These are
  test defects, not accepted passes or production initialization crashes.

Build 12 invalidates the persisted completion marker before workspace work and
publishes success only after every schema succeeds. The driver waits for the
application's main queue to become idle and separately reports that wait before
starting probe-specific timing. Recovery assertions inspect the schema list
before explicitly selecting a schema; a separate-process native case verifies
that the failure marker survives process exit. Build 12 validated these changes
as recorded above; the older results below remain historical.

## Earlier Checkpoints

The following results are for the immutable build-6 x86_64 APK
`frozen/9a2c203fda1d-72cea8eaf2e2`, not for subsequent source edits:

- 239 JVM tests passed. Build 5 Lint found no issues; build 6 included later
  lifecycle changes, so final Lint must still run on the final source.
- Dedicated AOSP API 35, 2 CPU/2GB: five setup runs, actual unseeded cold engine
  deployment, asset-copy failure/retry, five shutdown/reconnect cycles, feedback
  allocation/release, full 360dp T05 and real-editor T02 checks passed.
- Cold deployment took roughly 48 seconds; main-thread heartbeat during engine
  preparation remained responsive. It revealed 193 unencodable preset entries,
  two theme dependency cycles, repeated process-wide logging initialization and
  a mistaken `.default` schema lookup. These are not accepted as a clean run.
- All raw W/E/F messages remain in `build/t05-runtime/api35-*/`. The T05 capture
  includes both actual theme IDs. ART, optional dex-metadata discovery and GPU
  fallback diagnostics also appear in the independent system-Settings baseline.
- `api35-clip-before` printed functional PASS, but its host log recorder was
  interrupted when the emulator was closed. Its evidence is incomplete and is
  **not an accepted run**; it must be repeated.
- Build 7 was cancelled after it exposed a newly deprecated Gradle assets API.
  Build 8 passed 242 JVM and four build-logic tests and compiled ARM64, but was
  cancelled during x86_64 compilation when further JNI risks were confirmed.
  Neither cancelled run is accepted as final validation.
- Build 9 completed both ABI APKs, the instrumentation APK, 242 JVM tests and
  four build-logic tests. Lint reported `No issues found`; Spotless passed, and
  native build/configuration logs contained no warnings. This is build/static
  validation, not device validation. Build 10 adds the SAF/OpenCC fixes below.
- Build 10 completed both APKs and the driver, 253 JVM and four build-logic
  tests, clean Lint and Spotless. Build 11 additionally prohibits routine SAF
  import of raw binary user databases, with dedicated real-provider fixtures.

Additional source changes awaiting new-APK validation:

- Process-wide Rime setup runs once; per-restart traits/modules still reload.
- Built-in theme uses YAML aliases, retaining keys and mode overrides without
  root-level include cycles. Keyboard import cycles now terminate safely.
- `.default` opens the default config, not `default.schema.yaml`; configuration
  handle close is idempotent. Input text/key labels are removed from debug logs.
- Build-only Luna vocabulary adaptation uses the current single-syllable
  dictionary translations, the engine's 5% reading threshold, bounded word
  segmentation and 32-code-point limit. It preserves explicit dictionary weights,
  accepted rows, raw dictionary body and original essay. It never guesses codes.
- Prepared assets and checksums have separate build outputs. Checksums describe
  actual packaged bytes, including adapted data; source submodules are unchanged.
- Scoped native source overlays distinguish absent optional/new files from
  malformed/required files. Failed config builds propagate failure rather than
  returning partial data or allowing a stale theme. Startup deployment failure
  now prevents READY. Fault tests cover recovery and stale-theme rejection.
- Two additional CMake overlay tests pass (unchanged originals, stable output
  mtimes on reconfiguration and fail-closed upstream drift). Twelve runtime-audit
  host tests pass; unknown application W/E/F messages also fail normal-flow gates.
- JNI uses checked UTF-8/UTF-16 conversion, preserves supplementary characters
  and embedded NUL in string APIs, normalizes malformed input, and detaches only
  threads attached by the native callback. Host JNI contract tests pass, including
  nested/repeated calls, attachment failure and pending-exception preservation.
- Native startup/deployment/OpenCC/response exceptions cross into Java instead
  of escaping the JNI boundary. Device probes cover Unicode configuration paths,
  invalid OpenCC paths, overlong deployment paths and malformed optional YAML.
  These new device probes have not yet passed and must not be inferred from mocks.
- OpenCC's segmenter also truncates its `std::string` input at NUL. The bridge
  now converts NUL-separated spans and preserves every separator/tail; the
  actual-VM probe covers leading, trailing and repeated NUL, not just JNI mocks.
- SAF cleanup now requires content-backed synchronization history. New/switched
  trees, legacy metadata-only entries, unknown metadata, local edits (including
  same-size/same-timestamp edits), and unrelated empty directories are retained.
  Partial imports fail and stop follow-up export/theme selection. Index writes
  are atomic; cancellation does not trigger fallback, error logs or stuck import
  progress. Profile/background/schema/theme storage operations share maintenance
  serialization. Eleven new JVM tests passed build 10; real-provider probes
  still await execution. Binary database copying is disabled even on first
  import; portable text dumps remain supported for native merging.
- Instrumentation now emits its own PID. Host logs are bound to that PID instead
  of a preferences value that a later automatic IME restart can overwrite. Both
  APK hashes are recorded for each layout run; missing/duplicate PIDs fail closed.

JNI ownership and encoding constraints follow the [Android JNI guidance](https://developer.android.com/ndk/guides/jni-tips).
Mock/sanitizer success is not a substitute for CheckJNI on the actual Android VM.

The sections below retain earlier investigation checkpoints; they are not a
final status summary. See the latest checkpoint for build-specific results.

## Identified Issues

| Issue | Implementation | Validation |
| --- | --- | --- |
| Empty composing/candidate spans | Avoid empty composing and styling ranges | Real editor regression pending |
| Text macros and literal plus misparsed as keys/modifiers | Route macros to interpreter, parse trailing plus as literal | Instrumentation contracts pending |
| Dispatcher reused stop timer and stop-before-start race | Fresh signal, stop completion barrier, drain accepted work | 4 new JVM cases passed |
| NonCancellable rejection can reach JNI on the IO fallback executor | Native API entry checks dedicated engine thread | JVM regression passed |
| Client detach/reconnect and main-thread engine shutdown | Serialized IO lifecycle worker and per-client cancellation | Emulator stress pending |
| Late lifecycle cancellation kills new clients | Cancel at STOPPING, not twice | New JVM regression passed |
| Unnecessary debug full deployment and unoptimized native work | Modification-aware startup, Debug `-O2` retaining symbols/assertions | Cold deployment and <500ms enabled-first-key gate pending |
| Eager TTS/SoundPool and sound-file descriptor leak | Lazy allocation, asynchronous loading, close readers | Runtime feedback checks pending |
| Melody changes can overrun shorter sequences | Bound current index on each use, reset on effect load | Runtime check pending |
| Failed TTS initialization cannot recover | Shutdown failed engine and rate-limit retry | Runtime check pending |
| Main-thread storage status, sync scheduling and sound discovery | IO dispatch and cached/coalesced setup status | API 21/35 startup and SAF pending |
| Setup storage mutations race with maintenance | Shared maintenance mutex, disable busy picker, identity-bound cleanup | SAF and setup stress pending |
| Deployment clears startup logcat | Removed application logcat clearing | Full-log collection pending |
| Wrong T05 second-theme ID silently falls back | Correct ID and verify return value/current setting | Full two-theme matrix pending |
| Missing screenshots or contradictory test output can pass | Host protocol/artifact assertions and retained runtime warnings | 6 host tests passed, including legacy PID filtering |
| Asset copy errors swallowed before checksum commit | Propagate copy errors; atomic file replacement; publish checksum last | Failure/retry JVM test passed |
| Removed asset uses basename instead of relative subdirectory | Validate complete plan and resolve contained subpaths | Namesake/user-data preservation JVM tests passed |
| Unavailable external directory cached as a relative runtime path | Retryable lazy path initialization; expected storage-unavailable status | API 21/35 startup pending |
| Async sound loading can overwrite a newly selected effect | Short selection lock; picker reads on IO and reuses decoded effects | Runtime feedback checks pending |

## Failure Paths Under Validation

- Added explicit FAILED lifecycle state, failure propagation to queued callers,
  cleanup barriers and retry after startup/finalization/in-session redeploy errors.
  New JVM regressions and a dedicated-emulator asset-installation fault probe
  are being built; they are not yet claimed to pass.
- Plain literal text now uses quiet key autodetection, while explicit
  `preset.send` retains strict diagnostics. Added literal Chinese and plus-bearing
  text contracts alongside the four macro cases.
- `final-build-2.log` completed both ABI APKs and 234 JVM tests, then failed Lint
  with two StaticFieldLeak errors in InputFeedbackManager. The Context field has
  been removed instead of suppressing the inspection; final Lint is pending.
- Review TTS package visibility, sound-discovery exceptions, actual protected
  checksum paths in the fault probe, and explicit retry thread confinement.

## Execution Constraints

- Preserve API 21 and the ARM64 phone APK; native x86_64 tests avoid ABI translation.
- Dedicated `trime_runtime_api35` and `trime_runtime_api21` AVDs, each 2 CPU/2GB;
  only one runs at a time and never alongside Gradle. No personal AVD resets.
- Gradle one worker, native one compile job. No blanket warning suppression,
  privileged Android permission additions or SELinux changes.
- First application deployment must use APK resources without seeding compiled
  system dictionaries. User dictionaries are not copied/reset from real profiles.
- Test source and application APK hashes must be recorded; a dirty Git SHA alone
  does not identify a build.
- Expected injected SAF/fallback failures and platform diagnostics must be
  distinguished from normal-flow application faults, with original logs retained.

## Checks Completed So Far

- JVM: 234 tests, zero failures/errors/skips in `final-build-2.log`; includes
  resource-copy failure/retry and NonCancellable thread-confinement cases.
- Python: T00 26, T04 13, T05 5, runtime audit 6 and CMake adapters 4 passed.
- First instrument-test compile caught an incorrect test `Keyboard` constructor;
  corrected in source, not relabelled as an initial successful build.
- `arm64-build.log` is a native-compilation/intermediate build: resource installer
  changes were added after its Kotlin/unit tasks completed. Rebuild Kotlin/tests
  and both final APKs before freezing any identity or claiming new validation.
- `final-build.log` failed one new test because coroutine debugging decorates
  thread names. The test now checks the actual thread object and has an external
  timeout; `final-build-2.log` passed all 234 cases and built both ABIs, but failed
  the final Lint gate as recorded above.
- Old T05 two-theme claims corrected in both reports and evidence README; old
  archive contents/hashes and summaries are unchanged.

The native build, final APK identity, API 21/35 runs, complete UI matrix, real
SAF grant round trip/revocation, final Lint and final evidence archive are still
pending. This file must be updated with actual results, not inferred outcomes.
