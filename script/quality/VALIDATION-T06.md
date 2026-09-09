# T06 Editor And Lifecycle Regression

Work in progress. This is not a claim of a warning-free release.

## Stable Candidate Follow-up (2026-09-09)

The user has authorized publication of `v3.3.13-t9.8` as a stable release after
acceptance. Its planned versionCode is `20261113`; the existing package and
signing certificate remain compatible. The final stable APKs are not yet built
or accepted at this checkpoint. Older results below remain historical evidence.

The exact Build11 x86_64 APK passed `api21-build11-editors`: deferred overlay
attachment/detachment, the editor/action matrix, theme/schema restoration,
third-party fallback, WebView and 100 visibility cycles. The tested Trime interval
has zero recognized project diagnostics. The full preparation log retains one
LatinIME inactive-connection diagnostic and 71 warning/error lines; this is not
a warning-free run.

Additional review found and repaired attached-but-invisible overlay ownership,
full-pinyin overlay hide/show, physical-keyboard input leaking into the Chinese
engine in restricted editors, and dropped commit/key messages when the old
15-frame broadcast overflowed. The new broadcast has a bounded 64-frame buffer
and applies backpressure on native/maintenance workers; consumers must enqueue
native work asynchronously. Four-consumer burst, cancellation, no-subscriber and
nested cache notification tests cover its delivery contract. Build timestamps
now normalize epoch seconds into the milliseconds used by Android date helpers.
These new changes still require the frozen final build and device matrix.

`stable-host1` passed 91 host tests (quality 47, T04 13, T05 5, baseline 26).
`stable-format1` passed Spotless application and nine build-logic tests, including
five timestamp regressions. No build warning occurred in that preparation step.

## Release Boundary

The requested pre-optimization publication is complete:
`v3.3.13-t9.7-dev.2`, commit
`5bd0bc92dfb57151e669bd3688c7825ed2531194`, on `X2M7/trime` only.
The release is a prerelease; stable Latest remains `v3.3.13-t9.6`.
See [the release](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.7-dev.2)
for its tested APK/source identities, debug-signing caveat and open risks.
No changes were pushed to `osfans/trime`.

T06 targets a separate dirty-worktree development version:
`3.3.13-t9.8-dev.1`, versionCode `20261112`, package
`com.osfans.trime.debug`, label `Trime`, minSdk 21, targetSdk 37.
ARM64 and x86_64 APKs are built separately. APKs are debug-signed, not R8
release artifacts. A passed host test is not evidence of a tested APK.
Builds through `build6` still report Android `versionName=3.3.13-t9.7-dev.2`
despite their t9.8 filename/BUILD_VERSION_NAME and increased versionCode.
This discrepancy is fixed and APK-manifest-verified in build7; those earlier APKs
are identified by their retained installed-package dump and SHA-256.

## State Ownership

- Creating a Rime client requests startup. Native deployment stays on the
  engine worker; keyboard UI waits asynchronously for READY.
- STARTING displays an engine-independent numeric fallback. FAILED preserves
  the cause and exposes Retry and the system input-method picker.
- A failed copy/deploy may use existing resources only after the current
  schema's native translator dictionary successfully loads. File presence
  alone is not considered successful recovery. Missing external-sync folder
  permission can likewise use validated local resources, without claiming an
  external import/export succeeded.
- READY initializes or retains a decoded theme and replaces the fallback.
  A failed theme reload does not reinterpret a partially deployed disk file
  as the previous good theme. STOPPING/FAILED cancels active gestures.
- Each new editor receives a process-unique ownership token. A queued job
  checks it before execution; commit/preedit/key and candidate/status frames
  carry their editor token. Candidate clicks and candidate menus use the same
  queue. In-place native redeploy retains the editor's identity. A new editor
  clears displayed composition/candidates before asynchronous native binding.
- Joining a queued job waits for completion; it must not start a lazy job
  ahead of the queue. Canceling its completion handle cancels the queued job.
- Email, address, password and FORCE_ASCII fields use temporary ASCII;
  phone, numeric and decimal fields use a numeric layout. Returning to a
  user editor restores its selected schema/layout/mode. Password content
  is not retained in last-commit helpers or handed to speech feedback.
- Hiding the same editor's keyboard cancels held/repeating keys but retains
  composition and locks. Finishing an editor isolates its composition; it
  must not submit raw digits into the next InputConnection.
- Enter display and dispatch share one editor-action policy. NO_ENTER_ACTION,
  TYPE_NULL and NONE/UNSPECIFIED use a real Enter; valid custom IDs/labels and
  standard actions retain their actual dispatch semantics.
- Cursor monitoring belongs to one editor connection. Ordinary virtual-keyboard
  starts no longer send redundant unsubscribe requests. New/restarted editors
  reset the subscription state without calling a replaced connection.

The cursor subscription uses the existing API21-compatible monitor request.
Zero disables monitoring; rejected requests remain retryable. See the
[Android InputConnection contract](https://developer.android.com/reference/android/view/inputmethod/InputConnection#requestCursorUpdates(int)).

## Latest Correction: Touch Overlay Ownership

The final API21 reruns found another real lifecycle crash. In
`api21-build10-editors4`, `PreeditDelegate.onCompositionUpdate` tried to show
`TouchEventReceiverWindow` before its replacement anchor had a window token.
Android threw `BadTokenException` on the main thread. That run and build10
are not a final acceptance pass, despite earlier successful matrices.

Build11 defers an overlay request until its anchor is attached, visible and
laid out. Detachment dismisses it and clears pending ownership, so reattaching
an old anchor cannot resurrect its popup. An expired WindowManager token
discards the failed popup instead of crashing or reusing its invalid state.
The editor probe now exercises 100 pre-attachment requests, deferred show,
detach, reattachment without resurrection, explicit show/dismiss and another
detached request. Build11 completed in 6m03s: 280 application JVM tests passed
with zero failures/errors/skips, Spotless passed and Lint XML has zero issues.
The 91 host tests passed in `host-checks7` (quality 47, T04 13, T05 5, baseline
26). All frozen application/build-logic source hashes still match the worktree.
Build11 device verification has NOT started; earlier device passes do not
establish this correction's acceptance.

| Build11 Artifact | SHA-256 |
| --- | --- |
| ARM64 APK | `4691546d3130a70e706ef8438d02169deaa149fbbba43f4942f41a7cc385f6d7` |
| x86_64 APK | `3d51aebe4d7f04d3ee3880cb65d844057b143691a3a8f0670475adfe11df7f54` |
| Android test APK | `a456c1bb5ddd416e5c757cdbf197c2c9d80e7eb7abe2b13469d9c7f9648a8f91` |

API21 setup also needs a separate provenance boundary. The first build10
matrix passed all assertions but its full-log gate caught an inactive cursor
request. A controlled rerun (`api21-build10-editors3`) caught the same request
at 12:43:24.221, before Trime's service was created at 12:43:26.081; LatinIME
was still serving the fixture. The previous script switched IMEs as soon as
the activity window had focus, even when InputMethodManager still served the
launcher. The driver now waits for four observations of the actual fixture's
LatinIME connection and visible input view. A unique marker is emitted BEFORE
switching to Trime, so the Trime gate includes service startup, initial binding
and the entire matrix. Full preparation logs/diagnostics remain in
`runtime-audit.json`; the additional `tested-ime-audit.json` does not grant the
full run a warning-free label. API21 shell `log` appends whitespace to messages;
the marker parser accepts surrounding whitespace but rejects missing, duplicate,
wrong-tag or prefix-only matches. The crash run's original marker-parser failure
and raw crash stack are retained.

## Frozen Build9 (Superseded)

Build9 completed in 10m52s: 280 JVM tests with no failures/errors/skips,
Spotless and Lint passed, no build warnings. The 85 host tests passed in
`host-checks6` (quality 41, T04 13, T05 5, baseline 26). App/build-logic source
hashes match before/after compilation; only `KeyboardWindow.kt` and
`EditorLifecycleProbe.kt` changed from build8. Native libraries and all packaged
assets remain byte-identical to build8. This is the same unreleased
`3.3.13-t9.8-dev.1` version/code; distinguish attempts by these APK hashes.

| Artifact | SHA-256 |
| --- | --- |
| ARM64 APK | `55338aec713bf9e121ff99bb066259b0d1bb5eafa726dcfc31e298c65db11b3f` |
| x86_64 APK | `61fea1219a7af354319c8e04e9bab9f6a3cedcadea44c3035ca09f5a7e989a11` |
| Android test APK | `4a6efea5d35e11e90b2342fd3c274727cab7107f751d29de73333a87aad87956` |

Both manifests, ABI declarations, signatures, 16KB ZIP alignment and all 48
packaged resource checksums were checked again. The three v1 META-INF signing
warnings below remain. Build9 device acceptance is recorded separately from
the superseded build8 results.

| Build9 Evidence | Result |
| --- | --- |
| `api35-build9-engine` | Fresh installation/deployment and engine checks passed; full-probe maximum main-loop gap 747 ms. Deliberate missing-theme diagnostics retained. |
| `api35-build9-t03` | Full 360dp T03 passed: both themes, eight geometry screenshots, gestures, literal numbers, cancel, symbol/number round trips and actual full-pinyin letter taps/Hanzi commit. The build8 schema-lock failure is fixed. 42 raw platform warning/error lines; zero recognized project diagnostics. Other T03 viewports were not rerun here. |
| `api35-build9-t05` | Failed after completing the corpus and both themes' candidate/source/commit sequence: the test reset its EditText and issued cleanup before Android's asynchronous connection restart completed. The editor ownership guard correctly rejected the old-token job. Two screenshots and the failure log are retained, not called a complete T05 pass. Build10 changes only this test reset to wait for the acknowledged new connection. |

Build10 completed in 4m33s, with no build warnings and successful Spotless/Lint.
Only `T9AssistProbe.kt` changed. Both application APKs were compared byte for
byte with build9 and are identical; the 280 JVM tests and application Lint were
up-to-date, not rerun. Android-test Lint was rerun. The new signed test APK is
`9cdc9ae2d143246ad705d145be4636877d219a44e86c27762eabd95cc5581842`.
`build10/verification-reuse.json` records reuse of build9's application artifact
checks. T06 remains local and unpublished while final device checks continue.

## Build10 Device Runs (Superseded)

`build10` contains the exact same application APKs as build9, with the corrected
T05 test APK. All runs below record installed package hashes independently.

| Evidence | Result |
| --- | --- |
| `api35-build10-engine` | Fresh deployment/engine checks passed; full-probe maximum main-loop gap 779 ms. The 13 intentionally missing-theme diagnostic lines are retained. |
| `api35-build10-t05` | Full 360dp T05 passed: corpus, per-rule recovery, both themes' source labels, exact priority, repair locking, undo, Hanzi commit and acknowledged editor reset. 24 raw platform warning/error lines, zero recognized project diagnostics. Both screenshots and measured corpus results retained. Other T05 viewport cases were not rerun. |
| `api35-build10-editors` | Both builtin themes switch T9 to full pinyin, survive email/chat focus and return to T9. Full field/action matrix, stale candidate/native output rejection, third-party layout fallback, redeploy, real WebView, 100 show/hide cycles, lock retention and cancel passed. 217 raw platform warning/error lines, zero recognized project diagnostics. |
| `api35-build10-t02` | Real editor/caret/selection/locks/undo/cancel/commit/symbol/repeat-cancellation tests passed. 18 raw platform warning/error lines, zero recognized project diagnostics. |
| `api35-build10-startupFailure` | Failed copy/configuration/permission and malformed-patch recovery checks passed, with 72 intentionally injected project diagnostic lines retained. |
| `api35-build10-feedback` | Passed, with 14 raw platform warning/error lines and zero recognized project diagnostics. |
| `api35-build10-saf` | 21 isolated provider checks passed, with zero recognized project diagnostics. Not a new real-system tree-grant test. |
| `api35-build10-external` | 23 independent checkpoints passed, including actual PID replacement (3625 to 4181), rotation, app switching and both split-screen positions. All six split screenshots passed the key-pixel gate; originals manually inspected. Zero recognized project diagnostics. |
| `api35-build10-unavailable` | Actual numeric fallback input/delete, 18 controls without navigation overlap, Retry and T9 recovery passed. Original resources/checksums restored byte for byte; screenshot inspected. |
| `api21-build10-engine` | Fresh API21 installation and deployment/engine checks passed; 13 intentional missing-theme diagnostic lines retained. |
| `api21-build10-editors` / `api21-build10-editors3` | All field/WebView/schema/theme/stale-output and 100-cycle assertions passed. Full-log diagnostic gates failed on LatinIME preparation's inactive cursor request. The second run establishes the request precedes Trime service creation. Neither is relabeled a full-log pass. |
| `api21-build10-editors2` | Preflight failed on an incorrect local test APK filename, before instrumentation or IME changes. |
| `api21-build10-editors4` | FAILED with a real preedit touch-overlay BadTokenException. Build11 addresses the missing anchor lifecycle check. |

## Frozen Build8

Build8 completed in 10m54s with 280 application JVM tests, zero failures/errors/
skips, Spotless success and a Lint XML containing no issues. No build warning
was emitted. Source hashes match before/after compilation. The test APK is
unchanged from build7. Four additional tests cover cursor subscription ownership.
The unchanged build-logic tests were not rerun as a separate test task in build8.

| Artifact | SHA-256 |
| --- | --- |
| ARM64 APK | `e900d2f41112106ac26ed23abc12eb9453349529937462ac8897c4dfeee9d744` |
| x86_64 APK | `42368970c9cc809b24bd1c9d39cc3c5c306d9d1ad46640126d92d3f34b0302e4` |
| Android test APK | `6a04e790f570125f3b61bb194b659404ffe26ce9635444a1a182e61f4dd865cb` |
| Signing certificate | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| Shared resource digest | `3f0d830b93a67d15dbd0c5329039594760e7fc0bf0ead6f4866c2145fe6d26bf` |
| Packaged T9 schema | `61fb794f90e2a3eba15e2e07a2d21f13737d67f69a5758b3929e7f7f1836667e` |
| Packaged luna dictionary | `53e438d22eab81b430d36d608c51abae53c90c714720cce6948eaa36fcaff1cb` |

Both manifests, ABI declarations, v1/v2 signatures and 16KB ZIP alignment were
verified. All 48 shared resource hashes match the packaged manifest; all assets
and native libraries are byte-identical to build7, not a fresh native rebuild.
Three v1-signature META-INF warnings remain (serialization verification metadata
and the two coroutine ServiceLoader entries). Required service entries are not
deleted just to silence them. These remain debug-signed/debuggable previews,
not production-signing or R8 validation. Physical 16KB-page compatibility is
not established by ZIP alignment.

## Theme Contract

`trime/t9: true` explicitly identifies the pinyin nine-key scheme;
`trime/keyboard: luna_pinyin_t9` requests its layout. A numeric alphabet is
not enough to enable pinyin T9. Existing named scheme layouts take priority.
Missing T9/ASCII/numeric/symbol layouts can use namespaced in-memory copies
of packaged builtin layouts and key actions. Third-party action names cannot
override those copies. User theme files are not rewritten. Compatibility
layouts do not become extra entries in the theme's next/previous cycle.
Changing schemas also invalidates the previous schema's locked layout. A
non-locking full-pinyin default must not reselect an old T9 layout on the
current or subsequent focus update. This additional regression was found by
the build8 T03 run; the fix and both-theme focus assertions are in build9.

## Build8 Device Results

Build8 is superseded by the schema-lock correction, not a final acceptance APK.
All runs below installed the exact build8 APK/test hashes above. Host quality
tests subsequently grew from 32 to 41: six nonblank-key checks and three
capture-boundary checks. Other host suites remain T04 13, T05 5 and baseline 26.

| Evidence | Result |
| --- | --- |
| `api21-build8-engine` / `api35-build8-engine` | Fresh install/deployment and engine/theme checks passed. Each includes 13 deliberately injected missing-theme diagnostic lines. |
| `api21-build8-editors` / `api35-build8-editors` | Field/action matrix, stale queued/native response rejection, third-party T9 fallback, in-place redeploy, real WebView, 100 show/hide cycles, lock retention and cancel passed on each API. Respectively 63/209 raw warning/error lines; zero recognized project diagnostics. The inactive cursor-monitor requests from build7 did not recur. |
| `api21-build8-external` | Failed after nine checkpoints because API21 redacts the entire password accessibility value, not just its characters. No password content verification is claimed. |
| `api21-build8-external2` | 17 independent editor checkpoints passed, including actual IME PID replacement, rotation, app switching and restricted-field returns. Password evidence is restricted input type and caret position, not contents; the later current-caret assertion was checked against the retained dump, not called a new device run. Landscape screenshot inspected in native screenshot orientation. |
| `api35-build8-external` | 23 functional checkpoints passed, including both split-screen positions and actual Hanzi commits. Zero recognized project diagnostics. Original top/bottom screenshots and key-pixel checks confirm visible T9; an initial manual screenshot misreading was corrected, not treated as an application bug. |
| `api21-build8-unavailable` / `api35-build8-unavailable` | Real numeric fallback input/delete, 18 visible controls with no navigation overlap, UI Retry, T9 recovery and byte-for-byte resource/checksum restoration passed. Screenshots inspected. Deliberate fault logs retained. |
| `api21-build8-startupFailure` / `api35-build8-startupFailure` | Functional passes with 72 deliberate copy/configuration/permission/malformed-patch diagnostic lines each. The build7 2448 ms fault-sequence queue wait did not recur; this is not proof of a performance fix. |
| `api21-build8-feedback` / `api35-build8-feedback` | Passed; 107/14 platform warning/error lines, zero recognized project diagnostics. |
| `api35-build8-t02` / `api35-build8-saf` | T02 editing and 21 isolated provider checks passed. T02 has zero recognized project diagnostics. Not a new real-system SAF grant test. |
| `api35-build8-split-pixels` | All six functional/pixel checkpoints passed, but the overall diagnostic gate FAILED: logcat replayed earlier fault-injection records from the reused PID 6897. Full historical log retained. The independent editor driver now records a unique start marker and audits only subsequent records, without deleting device logs or suppressing any severity/tag within the capture interval. |
| `api35-build8-t03` | Both themes' eight geometry screenshots, long presses, swipes, cancel and number-page round trips passed. Overall FAILED waiting for full pinyin: focus policy reselected Tongwenfeng's previous locked T9 after the schema changed. Build9 clears that schema-owned lock. This run is not relabeled a pass. |

API21's remaining `showStatusIcon on inactive InputConnection` line is retained.
In [AOSP Android 5.0's InputConnection wrapper](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-5.0.0_r1/core/java/com/android/internal/view/IInputConnectionWrapper.java)
that message is emitted by the fullscreen-mode reporting branch, not cursor
monitoring. Its occurrence during focus cleanup does not establish a fixed
framework issue or a warning-free run. API35 also retains an inactive
`finishComposingTextFromImm` cleanup diagnostic. Its WebView renderer log is
paired with the system killing the isolated process as no longer needed and
reporting clean exit code 0; it is not relabeled a Trime crash.

## Evidence So Far

Evidence lives in ignored `build/t06-runtime/`; each probe records installed
APK hashes and all logcat levels, including platform warnings. No personal
dictionary is reset. Fault injection requires an explicitly marked emulator.

| Evidence | Result |
| --- | --- |
| `build2` JVM / Lint | 273 app + 4 build-logic tests passed; Lint reported no issues. Superseded APK because source edits overlapped compilation. |
| `build4` build | Successful; app/build-logic source hashes identical before/after compilation. Frozen APKs retained. |
| `api35-build4-editors` | Failed at the email assertion; waiting on a lazy returned job could bypass the serial queue. Corrected and covered by the later build7/build8 editor runs. |
| `api35-build4-startup` | Passed asset/config failure, usable old dictionary, retry, candidate generation and stale-theme rejection. Expected injected diagnostics retained. |
| `api35-build4-feedback` | Passed lazy feedback, real SoundPool loading/release and sensitive-editor speech isolation. Platform warnings retained. |
| `api35-build4-unavailable3` | Passed real external editor, numeric fallback input/delete, UI Retry, actual T9 recovery and `64` Hanzi/pinyin candidates. Screenshots inspected. |
| `build5` build | Source-consistent ARM64/x86_64 APKs; 273 app tests, Spotless and Lint passed. Superseded by later candidate-frame/queue changes. |
| `api35-build5-engine` | Passed cold deployment and engine/theme/cache probes. No invalid-metadata diagnostic; the intentional missing-theme fault is retained. Cold-ready heartbeat max gap 98 ms; full-probe max gap 987 ms. |
| `api35-build5-editors` | Field/action matrix, user ASCII restoration, queue order, stale output and held-key cancellation passed. Failed after the fixture cleared an editor and typed during Android's connection restart. The fixture now waits for the new editor token; later editor runs passed. |
| `api35-build5-startup` | Passed failure/retry and validated local dictionary recovery, including unavailable external-sync permission. This is not real persisted SAF grant revocation. |
| Host quality harness | 32 tests passed, including all-window capture freshness and requiring every expected control. |
| `host-checks2` | 76 host tests passed: quality 32, T04 13, T05 5, baseline 26. |
| `build6` | Source-consistent APKs, 273 JVM tests; one redundant safe-call compiler warning. Two new JUnit 4 tests were not discovered by the Kotest runner. Both issues corrected for build7. |
| `api35-build6-engine` | Passed cold deployment and engine checks; full-probe heartbeat max gap 803 ms, no invalid-metadata diagnostic. |
| `api35-build6-editors` | All field/action, queued candidate isolation, third-party T9, redeploy, real WebView, 100 visibility cycles, lock retention and cancel assertions passed. Overall audit FAILED on two literal-parenthesis key parsing errors. Fixed for build7; this run is not relabeled a pass. |
| `api35-build6-startupFailure` / `feedback` | Functional passes, including missing external permission/local recovery. Fault/platform diagnostics retained. |
| `api35-build6-external3` | 17 external-editor checkpoints passed, including process replacement, rotation and restricted-field returns. No recognized project diagnostic. Landscape screenshot exposed the fixture header obscuring its editor; fixture3 puts the editor first for the final rerun. |
| `api35-build6-split-explore` | Actual AOSP API35 multi-window tasks confirmed. Moving an already-visible IME target directly with WMShell initially obscured the bottom editor; hiding/refocusing let the system reposition it. Final driver tests both positions after explicit focus and verifies actual input. |
| `api35-build6-unavailable` | Numeric input/delete/UI retry and resource restoration passed. Visual review FAILED on navigation buttons overlapping the bottom row. Insets-on-attach and overlap assertions added for the next run. |
| `api35-build6-t02` | Old test helper failed by calling runOnMainSync from the main thread after the queue ownership change. T02/T03/T05 helpers now handle either thread and canceled/skipped jobs explicitly. T02 passed on build7/build8; the separate build8 T03 schema-lock failure is recorded above. |
| `api35-build6-saf` | 21 isolated Android provider checks passed. Not evidence of real persisted system grants or every cloud provider. |
| `build7` | Source-consistent ARM64/x86_64/test APKs; 276 discovered JVM tests passed, including both message-ownership tests. Spotless/Lint passed, no compiler/native build warning. Both manifests have the intended t9.8 version. All 48 packaged shared resources match the resource manifest. |
| `api35-build7-engine` / `t02` | Passed startup/engine/theme and actual T02 editing/caret/selection/undo/symbol/repeat-cancellation tests. Full engine-probe maximum main-loop gap 755 ms. T02 has zero recognized project diagnostics. |
| `api35-build7-editors` | Passed all input/action fields, stale queued candidate/native output rejection, third-party fallback, in-place redeploy, real WebView, 100 show/hide cycles, lock retention and cancel. 213 raw platform warnings/errors; zero recognized project diagnostics. |
| `api35-build7-startupFailure` / `feedback` | Functional passes. All 72 project diagnostic lines in the startup probe correspond to deliberate copy/configuration/permission/malformed-patch failures and validated recovery. Feedback has zero recognized project diagnostics. |
| `api35-build7-saf` | 21 isolated provider checks passed. This is not a real system grant test. |
| `api35-build7-unavailable` | Real fallback numeric input/delete/UI Retry and T9 recovery passed. All 18 controls visible and separate from navigation controls; corrected screenshot inspected. All injected resources restored. |
| `api35-build7-external` | All 17 fullscreen checkpoints and the pre-split checkpoint passed. Landscape editor visible with fixture3. Overall command FAILED on bottom-pane focus after a direct WMShell transition; retained as failed. |
| `api35-build7-split2` | Separate split-only rerun passed six checkpoints, actual multi-window state, both positions and one Hanzi committed per input. Screenshots inspected. The driver explicitly hides/refocuses after WMShell transitions. Zero recognized project diagnostics. |
| `api21-build7-engine` | Fresh API21 installation/deployment passed; cold-ready maximum main-loop gap 66 ms, full probe 307 ms. Intentional missing-theme diagnostics retained. |
| `api21-build7-editors` | Timed out before initial keyboard visibility while instrumentation replaced the selected IME process. No editor matrix acceptance claimed. |
| `api21-build7-editors2` | Selecting Trime after the test editor was focused allowed the entire matrix, real WebView and 100 cycles to pass. Overall diagnostic gate FAILED on two inactive-connection cursor requests. Subscription ownership repaired for build8; failed log retained. |
| `api21-build7-external` | Rejected initial QWERTY because the engine probe restored the original non-T9 scheme. No process-recovery/rotation pass claimed for this attempt. Select T9 explicitly before rerunning. |
| `api21-build7-startupFailure` / `feedback` | Functional passes. Startup also records a 2448 ms engine-queue wait during the fault sequence; this is not classified as an expected missing-file diagnostic or a no-latency-regression pass. Feedback has 107 platform warnings/errors, primarily AudioTrack, with zero recognized project diagnostics. |
| `api21-build7-saf-roundtrip` / `saf-restart` / `saf-revoke` | Real system picker grant, create/replace/read-back/cleanup/import history, persistence across app process restart and actual SecurityException after revocation passed. API21 runs one aggregate system-provider probe per invocation, not the 21 API29+ virtual fixtures. |

Earlier failed harness attempts are retained, not relabeled as application
passes. `unavailable` used a single-window system dumper that omitted the
IME. `unavailable2` hit Android's run-as external-path permissions; its saved
resource/checksum were restored explicitly before the third attempt.
The final harness moves only `shared/default.yaml`, never the whole directory.
Build5's failed editor run has no recognized project diagnostic, but retains
68 warnings/errors including an IME animation FrameTracker timeout, system
back-dispatch warnings, graphics-driver warnings and instrumentation packaging
diagnostics. It is not a warning-free run or a completed editor acceptance run.
Build6's WebView destruction logged a Chromium renderer "crash detected (code -1)".
The same retained system log reports PID 3541 killed as "isolated not needed"
and then exiting cleanly with code 0. The warning is retained as a WebView
cleanup diagnostic, not evidence of a Trime crash or a warning-free result.

## Reproduction

Use one dedicated emulator at a time, 2 GB guest RAM / 2 cores. Stop it before
Gradle; build with one worker and bounded Java/native concurrency. Do not
reuse a personal AVD or clear a normal application's data.

1. Install the frozen app and Android-test APKs. Complete the storage choice,
   enable Trime and select it using the normal setup UI. Mark only this
   disposable installation with external-files `runtime-audit-dedicated`.
   Before independent editor tests, explicitly select `luna_pinyin_t9` in the
   schema picker; engine probes restore the previously selected schema.
2. Run `run_runtime.py --probe editors` with explicit adb, serial, app APK,
   test APK and a new output directory. It exercises real Android fields,
   a real WebView, third-party theme fallback and 100 show/hide cycles.
   On API21 add `--bind-ime-after-editor-focus`: the driver temporarily selects
   AOSP LatinIME, starts instrumentation, then selects Trime when the test editor
   is focused. This avoids the instrumentation process-replacement binding issue;
   it is not used by the separate background-process-recovery regression.
3. Build `editor-fixture/build.py` normally and with `--observer`; install
   both APKs. The observer has no network permission and runs in a separate
   process, so window capture does not restart the editor or input method.
4. Run `run_editor_lifecycle.py` with pinned app/fixture/observer APKs for
   external-editor process death, rotation and application switching. On the
   verified AOSP API35 image, add `--split-screen` for actual multi-window
   state, both positions, Hanzi input and exit. Do not use this flag on API21.
   This portrait split-screen mode requires host Pillow and checks pixels inside
   all eight T9 letter keys. Missing/blank keys or mismatched screenshot dimensions
   fail; this gate is not OCR or proof that every UI element is unobscured.
   `logcat-full.txt` keeps the device backlog; the unique marker in `identity.json`
   bounds this independent driver's application audit to the current probe.
5. Run `run_unavailable_keyboard.py` for actual fallback/Retry UI. Its fault
   injection is expected to log errors; review them instead of suppressing
   them. Run the engine fault probes with the system Latin IME selected.

## Pending Acceptance

Build9's schema-lock correction passed the frozen build and API35 T03 rerun.
Build10's API35 lifecycle matrix, corrected T05 harness, independent editor/
split-screen checks and fallback/Retry passed. A subsequent API21 run discovered
the touch-overlay crash; build11 has the correction and deterministic regression
but has only passed host/build checks so far. Next, install its exact pinned APKs
on API21 and run the new overlay regression, field/action matrix and 100 cycles;
then independent process recovery/rotation/app switches and failure/Retry.
Repeat relevant API35 regressions including both split positions before accepting
or publishing T06. No emulator or Gradle daemon is being left running at this
status checkpoint. T06 source remains local, uncommitted and unpublished.
Build8's subscription fix passed build/Lint and both API editor runs; independent
API21 process recovery/rotation passed with T9 selected. API21 has no platform
split-screen. Build7's real SAF tree was selected
through Profile and the system picker, not a fabricated permission grant:
`0000-0000:Documents/trime-saf-87390960-18de-41e8-b0f5-5a924e9c796e`.
Only UUID-scoped test entries were created/removed. Raw binary user dictionaries
were not reset. This does not establish every OEM/cloud-provider compatibility.

Active composition across orientation changes or editor restarts is not a
retention claim: connection restart/configuration changes may cancel it. The
tested preservation case is hide/show within the same active editor. Split-screen
tests use explicit hide/refocus after WMShell transitions; the initial direct
transition's obscured bottom editor remains a documented limitation.
The first build4 deployment emitted `invalid metadata`. Retained logs show
PID 2359 began deployment at 08:40:12, and the instrumentation launcher
force-stopped it at 08:40:35 before completion. PID 2600 reported the metadata
error at 08:40:41 and completed deployment at 08:41:25. A partial compiled
artifact left by the interrupted deployment is the likely explanation, not
a proven identification of the specific file. Normal runs must wait for
deployment before instrumentation; interrupted deployment remains a separate
fault case. The emulator also had a boot-time System UI ANR,
recorded separately from Trime tests. Neither is called warning-free.

Native ARM64 phones/OEM editors and physical 16 KB-page devices are absent.
Prior ARM-translation queue-latency and slow-frame findings are still open;
these functional tests do not establish their resolution. Existing engine
message buffering is not a universal losslessness guarantee under arbitrary
backpressure. Recovery of nonstandard plugin-only translators is not proven
by validating a standard dictionary.
