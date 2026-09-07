# T03 Layout Verification

Date: 2026-09-07. Status: checks below passed; limitations remain documented.
Development build only; no Release or `latest` update.
See [T03.md](T03.md) for layout, settings and key contracts.

## Build Identity

| Field | Value |
| --- | --- |
| Package / label | `com.osfans.trime.debug` / `Trime` |
| Version / code | `3.3.13-t9.6-dev` / `20261107` |
| ABI / minSdk / signing | `arm64-v8a` / 21 / debug |
| Embedded base SHA | `431973e7e7344b75ed0c0b7b234a55f410d4209d` |
| APK SHA-256 | `902197cbb2958506f1eaf9508d56da5bf502eaef430ab41881751413ce48c108` |
| Test APK SHA-256 | `c904d279acc32e0ea797dc6338767f84cfe77ef031ca134028dbc0cc07e08bf4` |
| Certificate SHA-256 | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| Tracked patch SHA-256 | `67072a90d46d3921e9aa1f93f649237a0193f904b2079c4ef9816e92482357f3` |
| New-source archive SHA-256 | `1155200ff486b13f4559b6ed5904387b6a54820e9ebb2970d775ff7352693dd6` |
| Native library SHA-256 | `1d5a6870960186bfe9ada329cca80f6451872f9b6a8035369c1cab423bf2a480` |

Frozen inputs: `build/baseline/902197cbb295-bb58419e5dfa/inputs/`.
The embedded SHA is the base, not the dirty worktree used to build this APK.
`build/t03/compact-source.patch` and `compact-new-sources.tar` preserve the
application, test and runner sources; this report is excluded from the archive.
No application or Android-test source changed after the successful build.
The test APK is outside the immutable `inputs/` tree.

Installed identity: `build/t03/compact-device/device-969a55d0688a/inventory.json`.
APK hash, signer, version and ABI match the frozen package. Scheme/dictionary
hashes and bundled-resource digest are in the frozen build report.

## Build Checks

- `build/t03/build-compact-final.log`: build succeeds in 13m 21s.
- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`,
  `lintDebug`, `spotlessKotlinCheck` and `spotlessCheck` pass.
- 221 JVM tests: zero failures, errors or skips. Lint: no issues found.
- No new dependencies, native-library changes or minimum-Android increase.
  Existing feedback and long-press preferences are reused.
- One Gradle worker, 1280 MiB heap, 768 MiB metaspace, two active processors.
  Gradle and the emulator never run together.

## Emulator Matrix

API 35 / Android 15, read-only `hk_api35`, ARM64 native bridge, 2 GiB guest RAM,
two guest cores. Density is 320; `run_t03.py` restores display/font overrides
after success or failure. Each invocation uses `build/t03/compact-<case>/`.

| Case | Display pixels | Font scale | Mode | Result |
| --- | --- | --- | --- | --- |
| 360dp phone | 720x1600 | 1.0 | Full gestures | Pass |
| 412dp phone | 824x1800 | 1.0 | Geometry | Pass |
| 640dp tablet | 1280x1600 | 1.0 | Geometry | Pass |
| 800dp landscape | 1600x824 | 1.0 | Full gestures | Pass |
| Large font | 824x1800 | 2.0 | Geometry | Pass |
| Short landscape | 1600x720 | 1.0 | Geometry | Pass |
| Large-font landscape | 1600x824 | 2.0 | Geometry | Pass |

Each case checks both `trime` and `tongwenfeng.trime`, default width/height,
left/right one-handed mode at 48dp, and requested 80dp height. Each snapshot
asserts 17 targets >=48dp, stable 3x3 geometry through candidate/lock/clear
updates, onscreen targets and no key/composition/sidebar overlap. It also checks
candidate/status-bar separation, AppCompat label theming and absence of duplicate
floating preedit. Geometry-only runs retain all these checks and transitions.
PNG hashes, application PID and targeted runtime layout diagnostics are in each
case's `report.json`; complete and app-PID logcat are retained. All seven cases
passed: 56 screenshots and zero targeted runtime layout diagnostics.

Full runs dispatch touch events to actual key views with a real InputConnection:
normal 2-9 taps, literal long presses, locked input and middle-caret digit commits,
swipe-back, cancellation, multi-step Space sliding, separator/punctuation,
numeric-page round trip, and full-pinyin taps followed by Hanzi selection.
Non-T9 floating preedit must remain visible. Layout snapshots lock syllables
through the engine API, not by tapping pinyin labels.

## Editing And Storage

- `compact-t02.log`: real editor selection, locks, undo, cancellation, single
  Hanzi commit, middle-caret raw commit and symbol-page round trips pass.
  Held Backspace repeats; release also cancels repeat queued behind a blocked
  native job. Screenshot and app-PID logcat are retained (PID 5766).
- `compact-saf.log`: 21 isolated provider checks pass, including create,
  replace/read-back, revoked permissions and malformed provider responses.
  This is not a rerun of real system-provider tree grants or Android 5.x SAF.
- `compact-native.log` / `build/t9/6dbcd705509f/`: 376 controller checks and
  five restart/learning checks pass; `engine.log` is empty. The supplied
  library was extracted from the frozen APK and its hash matches exactly.
  Coverage includes `xi'an`/`xian`, `nv/nue/lv/lue`, incomplete tails, canonical
  learned codes, UTF-16 caret mapping, full pinyin and double pinyin.

## Screen-Input Check

Additional ADB screen taps on the installed APK, outside instrumentation:
412dp portrait, density 320, font scale 1.0, `trime` theme, process 5918.
The temporary ClipEditActivity has no source record and was cancelled, not saved.

1. Tap the actual keys for `64426`: raw segments `64` and `426`, editor empty.
2. Tap sidebar `mi`: locked `mi` plus `426`, candidate `米高`, editor still empty.
3. Tap Undo: restores raw `64` and `426`.
4. Tap sidebar `ni`: locked `ni` plus `426`, candidate `你好`, editor still empty.
5. Tap Hanzi `你好`: editor contains exactly `你好`; composition clears.
6. Swipe Space left: editor caret moves from 2 to 0, text remains exactly `你好`.

Evidence: `build/t03/compact-manual-{64426,mi,undo,ni,committed,slid}.png`,
editor UI XML, caret dumps and `compact-manual-app-logcat.txt`.
The log contains exactly one nonempty Hanzi commit for this flow and no targeted
layout diagnostic. This supplements API-driven locks in the geometry probe.
Ordinary debug startup redeployed resources before this check; the loading
screens are retained separately and are not a phone-performance result.

The emulator was closed after restoring the pre-manual display size.

## Failures Retained

- Candidate `8dd56cbaa27a...` exposed private `~ni~` encoding after locked input
  plus a literal digit. Read the native commit before any raw-input snapshot
  can discard its mapping; the exact touch regression is now included.
- An early probe used `tongwenfeng` instead of config ID `tongwenfeng.trime`
  and correctly failed on theme fallback. Original logs remain in
  `candidate1/`, `candidate1-large/` and `candidate2/` under `build/t03/`.
- Candidate `3a3e1a8865cc...` overlapped controls with requested 80dp keys in
  landscape (`final-landscape/`). Compact sidebars, a height budget and removal
  of duplicate floating preedit fix it. Posting width updates outside layout
  and giving labels an AppCompat context address observed runtime diagnostics.
- An intermediate build failed strict Lint's `UseKtx` check; the correction
  is in the successful build. `build-compact.log` retains the failure.
- Cold startup showed a System UI ANR before testing. Its dialog was closed;
  Google Search/Wellbeing were temporarily force-stopped in this read-only
  session. `compact-startup-logcat.txt` retains the environment logs. These are
  not reported as Trime crashes or phone performance measurements.

## Limits

No personal dictionary was reset. Only compiled system resources were seeded
from `compiled-candidate2/`; all their source resources are byte-identical to
the final APK's bundled resources. No user database was copied. Ranking/latency
comparisons and T00 cold/learned cohorts remain `not_run`.

At 360dp-high landscape, minimum targets leave very little application-editor
space. These checks establish IME control bounds, not comfortable editor space
in every app. Extremely short, floating/multi-window configurations and other
devices' system insets are not covered. Physical haptics, Android 5.x runtime,
release signing/R8 and phone performance are not established by this API 35
run. Existing dependency exceptions remain in [API21.md](API21.md).
