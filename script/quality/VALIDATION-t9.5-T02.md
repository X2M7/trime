# T02 Preedit Correction Verification

Date: 2026-09-07. Development build only, not a Release or `latest` update.
Minimum Android version remains API 21. No changes were pushed upstream.

## Build Identity

| Field | Value |
| --- | --- |
| Package / label | `com.osfans.trime.debug` / Trime |
| Version / code | `3.3.13-t9.5-dev` / `20261106` |
| Signing / minSdk / targetSdk / ABI | Debug / 21 / 37 / `arm64-v8a` |
| Base Git SHA | `b35e0ab7bdf3fe57c9142cea0a4e80e3287f5518` |
| Application APK SHA-256 | `496a940f8a56ffcea356ed2e6aba091aab6189074d8d603461b2c45b19f92132` |
| Signer SHA-256 | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| Native library SHA-256 | `f6d82bb309ccf02f6309bbeea79a682963e775d80d599fa14e16ae208364e31a` |
| Build-14 test APK SHA-256 | `9707d4052f76a780102aa7a7baacdd2351a77312e417b7c2f29f429f8edbe5b1` |
| Build-14 source patch SHA-256 | `9a312c404389b5e5d3409c450ab4c3e00d7b3b17c6c512f738fbf2eb84f8be65` |
| Nine-key schema | `luna_pinyin_t9` |
| Schema SHA-256 | `9f04415df4bc0342dfa9a11212ef34d57445c60c6b234c9e73835c685308c41f` |
| Bundled resource digest | `05df6f1e0cd576a4955b23452ef18b0bda60095341c3cd149f840dd0a39cc413` |

This is a **dirty-worktree** development build, not an APK of the clean base
commit. The frozen application is
`build/baseline/496a940f8a56-2fba2eca9fb2/inputs/application.apk`.
The build-14 test APK and source patch are in `build/t02/496a940f8a56/`.
The patch includes the untracked test sources, but not this report or subsequent
documentation-only changes. Build 11 contains the symbol-page fix; build 14
also fixes raw Return at a middle caret. Earlier evidence remains separate in
`build/t02/2ff0e8260b49/` and `build/t02/c226ea611748/`; those application APKs
are not final deliverables.
T00 capture `capture-8de0b840e55e` verifies the installed APK against the frozen
application. Final logs, screenshots, test APK and source patch are archived as
hash-identified artifacts under the same baseline run's `t02-evidence/` directory.
Artifact hashes are verified separately; the T00 cold/learned cohorts were not run.
Build timestamp: `1788711651123`.

## Changes

- Preserve raw encoding for edits and undo. Only explicitly locked syllables
  show determined pinyin; unresolved digits stay visible. Confirmed Hanzi and
  hidden atomic spellings retain their existing engine position mapping.
- Syllable taps position the caret. Middle edits, unlock boundaries, completion,
  `xi'an` versus `xian`, and `nv/nue/lv/lue` have controller coverage.
- Synchronize Android UTF-16 selection with Rime's UTF-8 preedit positions.
  Reject stale preedit taps; collapse an internal selection to its active end.
  Cancelling from outside the composition restores the logical editor selection.
- Keep cancel separate from committed-text deletion. Raw commits after middle
  correction move the editor caret to the end, without duplicate sentence commits.
- Opening the symbol page during T9 composition no longer commits a candidate
  or switches the engine to ASCII. Preserve the page's own mode for later visits
  after composition ends; non-T9 mode switching keeps its existing path.
- Route composition undo through T9 even with editor shortcut hooks enabled.
  Ctrl+Y is consumed during T9 composition; T9 redo is not implemented.
- Bound pending key repeats and cancel them synchronously on release, touch
  cancellation, disable or detach. A native operation already executing can finish.
- Keep full/double pinyin behavior and API 21 support. Limit native build jobs,
  fix two dependency compiler warnings and the deprecated JNI-library source API.

## Verification Status

- Native controller: **376 checks plus 5 separate-process checks passed** on
  API 35, using the exact final APK library digest above. Evidence:
  `build/t9/fb8bc3451508/identity.json`, `results.txt`, empty `engine.log`.
  This includes raw Return at a middle caret, locks, partial Hanzi, undo,
  ambiguous segmentation, incomplete tails, full/double pinyin and learning.
  The fixture has explicit pronunciation/word entries and isolated user data.
- JVM: **216 tests passed**, zero failures, errors or skips (build 14 reused
  the unchanged successful JVM results from build 11).
  Includes an ABI regression for the delegated `RimeApi` implementation.
- T00 tooling: **26 tests passed**. This is not a performance benchmark.
- Final unhooked `spotlessCheck` passed; `lintDebug` reported **0 errors and
  0 warnings**. Evidence: `build/t02-final-checks-4.log` and the frozen
  `lint-results-debug.xml`. Android-test sources were reanalyzed; unchanged
  production/unit-test analysis was up-to-date. Existing audited dependency
  exceptions remain in force.
- API 35 startup/theme/schema-picker probe: passed on the earlier application
  APK beginning `2ff0e8260b49`, with the build-8 test APK
  (`61409555ae097b52d76ce89f98103a3d0be0012dc7b9937c6002ad3019413c70`).
  3390 main-loop ticks, maximum gap 841 ms. Evidence:
  `build/t02/2ff0e8260b49/api35-startup-1.log`. Not relabelled as a final-APK run.
- T02 real-editor integration: **passed** on API 35, including actual syllable
  view taps, stale-preedit rejection, locks, undo, middle/end/range selection,
  composing and empty symbol-page round trips, exactly-once Hanzi submission,
  cancel versus editor backspace, and middle raw Return. Both a queued repeat
  cancelled on release and a normal hold/release pass; text stays unchanged
  after release. Evidence: `api35-t02-1.log`, `passed=true` and final code `-1`.
- Screenshot `t02-editing.png`: manually inspected at 720 x 1600. The actual
  nine-key grid, Hanzi candidates, raw/current segments and pinyin choices are
  visible without overlap. Candidate/T9 and keyboard regions also pass nonblank
  pixel-variance checks (`screenshot-pixels.json`).
- Current-APK SAF: **21 checks passed** on API 35, including isolated Android
  provider fixtures and Trime's document provider. Evidence: `api35-saf-1.log`.
  This used the same build-14 test APK as T02. Real system-picker persisted grant,
  reboot/revocation and OEM/cloud-provider checks were not repeated on this APK.

## Failed Attempts and Limits

An initial incremental Kotlin build retained the old one-argument delegated
`moveCursorPos` method and threw `AbstractMethodError`. A full, nonincremental
Kotlin rebuild fixed the artifact; the new JVM ABI test prevents accepting it
again. The old APK beginning `9fbb49bd00fc` is failed evidence, not a deliverable.

Test setup now waits for Application preferences and the editor's initial IME
restart before typing. Debug builds perform full deployment at each process
start. Engine startup has its own 300-second deadline before opening the editor.
Build 13 starts the 180-second editing deadline only after the input view and
T9 schema are ready; text assertions still have 10-second deadlines. A separate
480-second guard covers post-engine UI setup, editing and cleanup. These are
functional-test watchdogs, not phone typing-latency measurements.
Earlier deployment timeouts are not counted as editing passes.

The build-9 real-editor run passed lock/caret/unlock/undo and middle edits, then
exposed a real bug: the symbol page's automatic ASCII switch committed the
candidate immediately. Build 11 fixes that path and tests both preservation
and the page's subsequent empty-composition mode. Build 10 was deliberately
interrupted to add the mode-restoration fix before completing verification.
Build 11 then passed the composing symbol-page round trip but its new empty-page
assertion ran before the asynchronously posted layout attachment. The app log
records the subsequent ASCII restoration. Build 12 drains the layout queue
before engine assertions and drains in-flight deletions before repeat-stability
checks; it does not remove or weaken the text/mode/repeat assertions.
Build 12 passed through raw-commit correction, then its setup-inclusive deadline
expired while entering the eight-digit prefix of the repeat fixture, before
any hold/release assertion. Build 13 separates actual editing from UI/schema
initialization instead of increasing individual assertion deadlines.

Build 13 exposed raw Return discarding the suffix when the caret was in the
middle (`64` at caret 1 committed `6`). The new native regression fails against
the old library: `build/t9/20f3f03e959b/`, expected test assertion abort, not an
application crash. Build 14 moves the engine caret to the input end before
delegating raw Return to the schema. Regression cases include unlocked input,
locked input and a previously selected Hanzi prefix. The Android test now waits
for the actual engine caret at position 1 before Return; earlier timing-based
success did not establish this precondition.

API 37 attempts were also disrupted by System UI ANRs, emulator exit and a
system-server crash. Those are retained separately, not relabelled as passes.
API 35 uses the existing `hk_api35` AVD in read-only mode, Android 15 fingerprint
`google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.043/12960925:userdebug/dev-keys`.
ARM64 code runs through the x86_64 native bridge. System dictionary cache files
were matched against the APK's bundled resource hashes; no personal userdb was
seeded or reset. These are warm-system-data UI checks, not cold-start/ranking
benchmarks. Cold and fixed-learning T00 cohorts remain `not_run` for this APK.

Gradle uses one worker, a 1280 MiB heap, 768 MiB Metaspace and two active CPUs.
The emulator uses 2 GiB guest RAM and two cores; it never runs alongside Gradle.
One in-test memory sample reports application PSS 217915 KiB (about 213 MiB),
not a peak-memory guarantee. The app log contains dropped-frame reports during
startup and editing, including 1266 skipped frames during UI initialization.
It contains no `FATAL EXCEPTION`, `Fatal signal` or `AbstractMethodError` entries.
Functionality passing does not establish performance acceptance. The read-only
emulator was shut down after testing; its temporary installation is not persisted.

Four pinned native-dependency CMake configure warnings were present in this
frozen build: old CMake
minimums in snappy, leveldb and OpenCC, and OpenCC's FindPythonInterp/CMP0148
policy. They have not been globally suppressed. The existing 11 narrow,
audited dependency-update exceptions remain documented in [API21.md](API21.md).
The subsequent source fix and its separate verification are documented in
[the CMake follow-up](VALIDATION-t9.5-cmake.md); do not relabel this APK or its
historical logs as a rebuild containing that fix.

Android 5.x runtime, physical-device performance, release signing/R8 and extended
soak tests remain untested. UTF-16 native helper checks do not establish complete
supplementary-character coverage through real JNI/editor round trips. Passing
these regressions is not a guarantee of zero possible bugs or universal SAF
provider compatibility. See [SAF.md](SAF.md) and [the editing rules](../t9/README.md).
