# Runtime Audit: t9.7-dev.2

Builds 15/16: `3.3.13-t9.7-dev.2` / `20261111`, package `com.osfans.trime.debug`,
minimum API 21. This is local Debug validation, not a published release or a
claim that every possible defect has been eliminated.

Build 15's AOSP API 21/35 validation is complete. A subsequent Google-image run
found an additional menu-resource defect, fixed in build 16. Build 16 device
validation confirms the menu repair on the Google image and API 21. The
ARM64 native-bridge startup
queue warning below remains open: this is not an all-warnings-cleared report.

## Artifact Identity

Base Git SHA: `425e334803b93b0bff0111da289ccec542eda4b9`, with recorded working-tree
changes. `build/t05-runtime/artifacts-v7/identity.json` records the source patch,
untracked-source hashes, test driver and both APKs. The amended test-only driver
is recorded separately in `artifacts-v7/driver2/identity.json`; later host-driver
and documentation changes do not change the application APK identity.

| Artifact | SHA-256 |
| --- | --- |
| x86_64 APK | `bf1b7725c685b5854cd61af81b87969af9d011b8ba51a4981328959ebce0867b` |
| ARM64 APK | `b9092d48bc175b2edd64cd245f0c2a6e2e4269ad5b394e89556b6fb19d256a96` |
| Signing certificate | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| Driver 2 APK | `b34ff99db55dfd5b5911a3b365fdb275855e0627b24b71b86d6ca99be8eb3b4d` |

Both APKs pass 16KB ZIP alignment. Assets and native libraries are byte-identical
to build 14; native libraries also match builds 12/13. Alignment is not a physical
16KB-page runtime test.

### Build 16 Follow-Up

Build 16 changes only the menu helper, six main-toolbar item IDs and their Android
ID resources, plus tests/host diagnostics. `artifacts-v8/identity.json` records its
source snapshot. It retains the same internal development version; APK SHA-256,
not the version string alone, distinguishes these unpublished builds.

| Artifact | SHA-256 |
| --- | --- |
| x86_64 APK | `115a1ffe525160e1a2dd72cf8453b77fa09db9f4fc1ab06baacf3817b77a1d91` |
| ARM64 APK | `6a0c06589f6d9c9ddefc10c3472392df8b5ee8256179ef6167b8e2d0e2b3cf87` |

The certificate and driver-2 APK hashes are unchanged. All 50 packaged asset/native
entries per ABI match build 15 byte-for-byte; both APKs pass 16KB ZIP alignment.
Build 16 passes 256 application/four build-logic JVM tests, 72 host tests,
Lint (`No issues found`) and Spotless, with no warnings in the complete build log.
Build 15's device/matrix results below are not relabeled as build-16 runs.

On the Google API-35 image, build 15's normal navigation produced four
`Invalid resource ID 0x00000000` errors. A caught-exception JDWP breakpoint found
`ActionMenuItemView.mID == 0` in `View.onProvideStructure` during autofill traversal.
The menu helper used `Menu.add(title)`, assigning anonymous IDs even to action
items. Explicit resource IDs now preserve menu identity and resource lookup.
The initial keyboard-index hypothesis was disproven for this stack; `KeyboardView`
was not changed. See `final15/resource-id-diagnostic/debugger-finding.json`.

The host gate now recognizes this framework-tagged error and requires real
toolbar resource IDs in UIAutomator output. The old navigation report is retained;
its new-gate recheck records four defects in `final15/arm35-review-checkpoint.json`,
so its original functional success is not a clean diagnostic pass.

Build-16 Google API-35 follow-up (2 CPU/2GB, non-root, no profiler):

- x86_64 unseeded deployment/CheckJNI and theme/schema assertions pass. The
  deployment-phase maximum sampled main gap is 118ms; the full probe reaches
  2157ms during keyboard/schema rendering. This is not a no-jank acceptance.
  Its 14 application warning/error lines are the deliberate missing-theme case.
- x86_64 and ARM64 normal navigation each pass four cycles with actual toolbar
  resource-ID assertions. Neither has the invalid-resource error or another
  recognized application diagnostic. The complete app logs still contain 83/80
  platform warnings/errors, including frame/transaction timing diagnostics.
- ARM64 clipboard focus/keyboard/cancel passes; the retained PNG shows the actual
  full-pinyin keyboard, not a claimed T9 screenshot. No clipboard row is changed.
- ARM64 T02 editor assertions pass, but the diagnostic gate FAILS on a 2920ms
  Rime queue wait. It remains an open defect/risk, not an accepted retry.
  These ARM runs reuse the x86_64-deployed data in this disposable emulator;
  neither is an unseeded ARM deployment benchmark.

Raw evidence is under `final16/google35-*`. First-boot preparation encountered a
System UI ANR; selecting its actual Wait button recovered the system. Subsequent
host setup attempts expected the wrong storage label or encountered the actual
notification dialog. Those failed captures remain intact. Storage was eventually
selected through Profile's real `Use app-specific storage` option; the pre-probe
directory inventory is empty. No shared preferences were written by the host.

Build-16 AOSP API-21 follow-up (2 CPU/2GB, fresh dedicated SD image):

- Unseeded deployment/CheckJNI and engine assertions pass: maximum sampled main
  gap 55ms in deployment and 436ms across the full probe. The 16 raw warnings/errors
  comprise 14 deliberate missing-theme lines and two SQLite WAL recovery notices.
- Four normal navigation cycles, real toolbar IDs and own-UID clipboard focus,
  keyboard display and cancellation pass, with zero recognized application
  diagnostics. All six platform messages are retained. The actual screenshot
  shows the full-pinyin keyboard; this is not additional T9 matrix coverage.
- Eight Room save/lifecycle cases pass with the system LatinIME explicitly
  selected and recorded by the driver. This stress run is not warning-free:
  28 lines describe deliberately removed database rows; 13 inactive cursor
  requests occur among 62 InputConnection warnings with that external IME.
  The latter are not silently counted as a clean Trime-IME regression.

See `final16/api21-*`. All owned emulator/probe processes were stopped after
capture. No physical ARM device was connected for native-performance validation.

## Repairs

- Clipboard/collection edits await real Room writes before closing. Confirmed
  writes survive Activity destruction; duplicate saves are disabled. Write errors
  retain the editor text. Cache updates follow an affected-row check under the
  existing mutex. New intents supersede reads; loading cannot overwrite typing.
- Main-toolbar actions have stable Android resource IDs rather than anonymous
  zero IDs, preventing invalid resource lookups during autofill structure capture.
- Cursor monitoring is configured on new input views, not on an input connection
  being finished. Stale anchor callbacks are ignored. Clipboard close no longer
  issues duplicate `finish()` calls.
- Back callbacks follow input-panel visibility/lifecycle; platform back dispatch
  is enabled. Ordinary navigation no longer permanently disables panel dismissal.
- SAF checks error-channel capability before requesting descriptor errors.
  Reliable-pipe checks/read-back hashes remain enabled. Atomic publication,
  content-backed deletion history, cancellation and shared maintenance locking
  protect local data. Raw binary user databases cannot overwrite live databases;
  portable text merging remains supported.
- Failed deployment cannot publish a success marker or accept stale schema/theme
  data as ready. Startup, retry, shutdown and queued JNI work have explicit
  lifecycle/thread ownership. JNI preserves UTF-8/UTF-16, supplementary characters,
  embedded NUL and pending exceptions; OpenCC preserves text after NUL separators.
- Resource installation publishes checksums last and propagates failures. Built-in
  theme dependency cycles and 193 unencodable dictionary presets are removed from
  generated assets without editing source submodules. Literal keys/macros parse
  correctly; debug logs no longer expose typed text/key labels.
- Feedback resources are lazy, asynchronous and generation-guarded; descriptors
  close and melody indices stay valid after effect changes. Setup refreshes use
  coalesced IO work rather than blocking the main thread.

Contracts: [SAF-SAFETY.md](SAF-SAFETY.md), [RIME-ASSETS.md](RIME-ASSETS.md),
[API21.md](API21.md).

## Validation

- Build 15c: 253 application/four build-logic JVM tests pass. Final host tests:
  70 pass (26 runtime/build-adapter, 26 baseline, 13 T04, five T05).
  Lint: `No issues found`; Spotless passes; full Gradle/CMake log has no warnings.
  Driver-only build 15d also passes Lint/Spotless with no build warnings.
  Earlier build-15/15b compile failures remain recorded, not counted as successes.
- Dedicated API 21, 2 CPU/2GB: unseeded deployment and actual-VM CheckJNI pass
  (maximum sampled main gap 379ms). Setup, shutdown/reconnect, feedback, four
  navigation cycles and own-UID clipboard focus/keyboard/cancel pass. Normal
  application diagnostic counts are zero, not raw platform warning counts.
- Eight real Room clipboard/collection lifecycle cases pass on both APIs. The preserved
  build-14 APK with the same driver fails with `Editor closed before its database
  write completed`. Only UUID-tagged probe rows are removed.
- Initial startup-failure run rejected: a file written in the same second as
  deployment does not exceed Rime's `time_t` modification marker. Driver 2 assigns
  a distinct fixture timestamp, as the malformed-file/theme cases already do.
  Driver 2 passes twice on API 21 and once on API 35. This does not redesign
  native timestamp discovery: explicitly deploy after
  external configuration edits, especially with preserved/coarse timestamps.
- Build 14 passed API-21 system-picker SAF round trip, restart and revocation,
  without descriptor-channel warnings, and actual popup selection/closing. Those
  results are not silently relabeled build 15; its SAF code/assets/native library
  are unchanged in builds 15/16. Each API-21 run reports one aggregate real-tree
  case with multiple assertions. The virtual provider fault cases use an API-29
  resolver wrapper and do not run on API 21; do not call these 22-case runs.
- API 35, 2 CPU/2GB: fresh unseeded engine/actual-VM CheckJNI, setup, failure/retry,
  shutdown/reconnect, feedback, T02 real-editor corrections and clipboard focus
  pass. Four normal navigation/input-panel/back cycles pass without replacing
  the IME process. Deployment-phase maximum sampled main gap is 142ms; the full
  engine probe, including first keyboard construction, reaches 1294ms. The smaller
  deployment-only figure is not the full startup/UI latency.
- Build 15 API-35 SAF: real system-picker grant to a dedicated UUID directory;
  round trip, app/provider restart and revocation each pass 22 checks. Descriptor
  communication-channel and disabled-back-dispatch warnings are absent in all
  three runs. Injected provider errors are retained. No broad tree grants or
  privileged permission bypasses are used.
- Build 15 API-35 T03/T05: all 14 runs pass across 360dp, 412dp, tablet, landscape,
  360dp-height landscape and portrait/landscape large-font settings. Both actual
  built-in themes are covered; all 70 PNG hashes and nonblank keyboard regions
  are verified, with visual spot checks. Geometry checks cover stable main keys,
  48dp touch targets and one-handed/height variants. The 360dp runs additionally
  execute full gestures and the fixed correction corpus; other sizes test geometry.
- Build 12's unchanged native library passed 16 deployment/recovery, 362 assist
  and 381 controller checks plus the JNI contract. A preserved build-11 library
  fails the deployment-marker regression, validating the negative control.

### ARM64 Investigation

Build 15 runs used the actual ARM64 APK on Google API 35 x86_64 with
`libndk_translation.so` (Berberis 0.2.3), 2 CPU/2GB, not a native ARM device.
`primaryCpuAbi=arm64-v8a` and successful loading of `lib/arm64/librime_jni.so`
are recorded. Unseeded native deployment/CheckJNI, eight Room save cases and
normal clipboard focus/keyboard/cancel pass. All raw diagnostics remain available.

The T02 editing assertions pass, but two complete build-15 unprofiled captures fail
the diagnostic gate because a Rime job waits 3086ms and 2138ms at first keyboard
setup. Build 16 reproduces it at 2920ms; the menu repair is not a latency repair.
An x86_64 APK run on the same Google image passes T02 without that warning.
The normal ARM navigation run also has no queue warning, but has the menu error
described above. These observations do not close the intermittent queue risk.

A separate SIGQUIT diagnostic catches `rime-main` inside
`Rime.deployRimeConfigFile`; a bounded simpleperf capture also records native-bridge
translation/optimization on that thread. Diagnostic captures are not acceptance
or latency measurements. The 2000ms watchdog remains unchanged. Theme deployment
checks cannot simply be skipped: stale/malformed configuration must still fail.
Native ARM startup/first-input latency and a safe reduction of this deployment
cost remain unverified; no latency fix is claimed by the menu-only build 16.

The first ARM engine attempt is retained as an ANR failure. Its captured main
thread was Runnable with cumulative scheduler counters of 2.84s CPU and 23.30s
runqueue wait; other Google processes also ANR during first boot. This supports
environmental starvation, not a demonstrated Rime native deadlock. The later
unseeded engine pass does not erase that failure. Several other runs lose ADB/logcat
transport and are rejected as incomplete; two subsequently report a framework
`UiAutomation.disconnect()` exception on the instrumentation thread. They are not
silently retried into an all-pass aggregate.

Evidence: `final15/arm35-first-anr-trace.gz`, `arm35-profile-1/`,
`arm35-stack-diagnostic/`, `arm35-review-checkpoint.json` and the named raw run
directories. Root was used only for owned-emulator trace/profiling diagnostics;
adbd was returned to non-root before normal navigation. No SELinux or application
permission bypass was applied.

### Correction Metrics

Build 15's fixed 24 exact inputs retain identical first-80 candidate lists with
assistance off/on and relative to build 12 (which matched build 6). Fixed-set
recovery is 3/24 off versus 24/24 on. Recovery means selecting the suggested
canonical syllable and finding its representative Hanzi, not automatic Top-1
accuracy or a representative everyday typing score.

| Measurement | Off | On |
| --- | ---: | ---: |
| First key, ms | 23.82 | 47.33 |
| Median first-key latency, ms | 23.75 | 20.26 |
| Maximum first-key latency, ms | 191.34 | 51.96 |
| Median final key plus 80 candidates, ms | 30.46 | 27.56 |
| Whole-app PSS snapshot, KiB | 142262 | 139322 |

These are single sequential Debug/emulator measurements. GC/JIT affect whole-app
PSS and timings; the lower on-sample does not establish a memory improvement or
zero assistance overhead. See `final15/t05-comparison.json` and retained metrics.

## Warning Accounting

All W/E/F messages and full-duration captures are retained. Functional PASS does
not grant fault probes a warning-free label. Host guards catch application misuse
even under framework tags; per-device locking prevents overlapping audits.

| Diagnostic | Disposition |
| --- | --- |
| Invalid resource ID 0 in main action-menu views | Fixed in build 16; caught-exception stack confirms application menu IDs as the cause |
| First-theme Rime queue wait under ARM translation | Open; 3086ms/2138ms in build 15 and 2920ms in build 16; no threshold/filter change |
| Missing theme, failed asset/config/provider write, deleted DB row | Deliberate faults; check recovery/preservation and retain errors |
| Inactive cursor monitoring when finishing Trime input | Repaired; absent from normal own-UID navigation/clipboard |
| Inactive connections during rapid clipboard lifecycle stress with system LatinIME | External IME calls are visible in the full log; not a clean Trime IME run; driver 2 records the selected IME |
| SQLite `(283)` after process replacement | WAL recovery notice; stopped snapshots previously passed integrity checks |
| API-21 AudioTrack fast-output denial | AOSP SoundPool fallback for an 8kHz fixture versus hardware rate; also reproduced by system audio; sound was not disabled |
| API-35 `sendCancelIfRunning: isInProgress=false` | Platform back-dispatch cleanup logs this even without an active animation; removing callback registration would reintroduce a real navigation defect |
| Instrumentation ABI mismatch / missing optional `base.dm` | Pure-DEX test APK versus native app ABI, and optional dex metadata discovery; neither proves a 32-bit application package |
| ART/EGL/emulator diagnostics | Retained and individually compared with platform/source evidence; tags alone are not exemptions |
| ARM guest `/dev/pmsg0` denied `getattr` | Seen during injected native log errors; guest system liblog contains this probe, app JNI does not. Attribution is an inference, not a captured access stack; SELinux remains enforcing |
| First-layout slow frames / IME animation tracking | Debug/two-core emulator observations remain in raw logs; no claim of zero jank or physical-phone performance |

Primary references: [JNI](https://developer.android.com/ndk/guides/jni-tips),
[descriptor capabilities](https://developer.android.com/reference/android/os/ParcelFileDescriptor),
[back dispatch](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture),
[API-35 back-dispatch cleanup](https://android.googlesource.com/platform/prebuilts/fullsdk/sources/+/dc3f885ebe8ddc75bd9cf2d567eef4d1ed433a09/android-35/android/window/WindowOnBackInvokedDispatcher.java),
[SQLite recovery notice](https://www.sqlite.org/rescode.html#notice_recover_wal),
[Android 5 AudioTrack](https://android.googlesource.com/platform/frameworks/av/+/android-5.0.2_r1/media/libmedia/AudioTrack.cpp),
[Android 5 ART](https://android.googlesource.com/platform/art/+/android-5.0.2_r1/runtime/class_linker.cc),
[View structure resource lookup](https://android.googlesource.com/platform/frameworks/base/+/ce8e50cfcea0756066a739a6390907770949b1b0/core/java/android/view/View.java),
[per-thread scheduler statistics](https://docs.kernel.org/scheduler/sched-stats.html),
[liblog pmsg availability](https://android.googlesource.com/platform/system/core/+/09158b1d5a2da7d71b7046734e1f96c98d8fc81f/liblog/pmsg_writer.cpp).

## Boundaries

- Dedicated read-only emulators/owned data only; personal dictionaries are never
  reset. Failed runs and old APKs remain intact.
- Android 5 Java filesystem handling of supplementary filename characters is a
  platform limitation. The JNI probe uses a genuinely UTF-8 native-created path;
  it does not claim all Java/SAF Unicode filename paths are fixed.
- Physical phones, the OEM editor matrix, physical 16KB-page devices and the
  release/R8 variant have not been validated in this round. Activity-destruction
  save survival is not guaranteed persistence after OS process termination.
- Rejected host-navigation runs are retained: delayed IME reset, permission
  dialog, stale UIAutomator hierarchy and modern Android's own-UID `am` rejection.
  The corrected host waits for stable IME selection, requires fresh XML, handles
  the actual dialog and uses normal navigation plus the separate instrumentation
  clipboard probe on API 35. It does not export private Activities to bypass checks.
- No upstream/fork push or release is performed by this runtime audit.

## Evidence

The generated [runtime summary](t05/results/t9.7-dev.2-runtime-summary.json)
explicitly records `all_runtime_warnings_resolved: false`, APK identities, each
build-16 result and the older native/SAF/matrix evidence under its actual build.
It does not aggregate fault-injection PASS or an incomplete capture into a clean
runtime acceptance.

Local archive: `build/t05-runtime/evidence/t9.7-dev.2-runtime-20260909.tar.gz`.
Its adjacent `.sha256` and `.tar.verification.json` files record the archive
checksum and streamed verification count. The internal manifest gives each
included file's SHA-256 and size. Raw failed captures, complete runtime logs,
screenshots, build/test reports and source snapshots are retained. This is a
local audit bundle, not a GitHub upload; APKs, ELF libraries/executables, AVD
images and the copied platform `liblog.so` are excluded.

The original build-16 source snapshot in `artifacts-v8/` is immutable. Later
report/summary changes are recorded in `final16/source-supplement/`; application
sources still match the built snapshot. No assertion threshold, warning gate,
system permission or SELinux policy was relaxed to obtain a pass.

[Historical checkpoints](RUNTIME-AUDIT-HISTORY.md) preserve rejected runs and
superseded investigation status rather than presenting them as final passes.
