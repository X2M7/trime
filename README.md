<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# Trime

Rime IME for Android

![build](https://github.com/X2M7/trime/actions/workflows/commit-ci.yml/badge.svg?branch=develop)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub release](https://img.shields.io/github/release/X2M7/trime.svg)](https://github.com/X2M7/trime/releases)
[![F-Droid release](https://img.shields.io/f-droid/v/com.osfans.trime.svg)](https://f-droid.org/packages/com.osfans.trime)
[![Project page](https://img.shields.io/badge/GitHub%20Pages-Trime%20T9-2f6f5e)](https://x2m7.github.io/trime/)

English | [简体中文](README_sc.md) | [繁體中文](README_tc.md)

## Current status: v3.3.13-t9.11 T08 acceptance record

**Acceptance completed; publication status follows the GitHub tag.** This version contains T08 candidate snapshot guards, bounded paging and optional numeric latency diagnostics. Input delivery remains ordered and the existing aggregate JNI response is retained. T09 mixed-input changes are outside this tag.

The frozen APKs are non-debuggable Release/R8 builds with performance tracing disabled, Android 5.0+ support, and ARM64 / x86_64 variants. Package `com.osfans.trime.debug` and the fork's previous signing certificate are retained for upgrades. R8 optimizes and shrinks code/resources while the current rules preserve class names; no new production signing key or ARM64 device testing is claimed.

Compiled source `2e0ab64a3bd590376c1d15805235241c36e83d33` is frozen in `build/t08/stable-candidate-4`; all 536 selected input hashes match before/after the build. Debug took 542.599 seconds and Release 653.891 seconds. The 598 application JVM tests actually ran and passed; 9 build-logic reports have zero failures/errors/skips, with that task UP-TO-DATE. Both online Lint reports contain zero issues; the 174 host checks passed in this build's scope.

Fresh API21/API35 x86_64 emulator acceptance passed: 320 native FIFO keys, a 49-candidate 48+1 paging fixture, stale snapshot/editor rejection, and 100 show/hide cycles per API. Debug covers both shipped themes' native candidate Unicode, accessibility and actual screen glyphs. Normal Release separately completed 6 actual taps and 2 exact Hanzi commits in one current theme per API, 17/23 lifecycle checkpoints, a 36-tap diagnostic trace, and restoration of the exact trace-disabled normal APK. API35 also ran T02 and the full 360-width T03 case. Debug and normal API35 stages have separately recorded boot identities.

The prior candidate2 was rejected for actual blank candidate glyphs; this version restores the TextView text/measurement/layout contract while retaining scaling. Candidate3 failed its API35 T03 geometry check. Candidate4 corrects only that test's own-scroll normalization and uses entirely fresh build/device evidence. Earlier candidates and T07 results remain historical.

Physical-device candidate P95, Java/native/graphics/total PSS, accuracy comparison and first deployment remain unmeasured. The T08 performance budget is not declared passed. Each build phase retains 55 upstream AGP/KSP deprecations (52/3), each normal APK retains two META-INF service-signature warnings, and full device warnings remain recorded. No warning-free or smoothness claim is made.

The release asset names are the ARM64 APK, x86_64 APK and a source archive with submodules. Actual upload, downloads and Latest are determined by GitHub; see the [9.11 acceptance record](doc/releases/v3.3.13-t9.11.md) and [T08 validation report](script/quality/VALIDATION-T08.md).

The first API35 Debug boot retains three approximately 4-second popup buffer destruction stalls as explicit findings. Additional normal Release checks use the separately verified 3 GiB boot: both themes complete three full-pinyin rounds with actual fixture IMM hide/show taps, owned popup visibility 1/0/1, retained preedit and one exact Space submission. Within each theme, all three rounds use one unchanged owned IME process. Those extra scopes show no matching stall; this is not a physical latency budget result.

## About

Trime is originally a frontend of open-source [Android Traditional Chinese IME], based on [RIME] input method framework and written in Java/Kotlin with JNI. It is designed to protect the native language of various local dialects of Chinese and is a universal shape-based and phonetic-based input method platform.

[Documentation](https://github.com/osfans/trime/wiki)

## T9 Pinyin

This fork adds dictionary-backed `Luna Pinyin T9` / `luna_pinyin_t9` to Trime, with a 3x3 main key area in both bundled themes. Digits 2–9 enter pinyin; syllable locking, middle editing, unlocking and undo preserve the original numeric input. Selecting pinyin does not commit Hanzi. The layout supports a pinyin sidebar or horizontal list, one-handed modes, adjustable key height, literal digits on long press and Space cursor sliding.

[Project page](https://x2m7.github.io/trime/) | [Download T9 stable APKs](https://github.com/X2M7/trime/releases/latest)

<details>
<summary>Historical T07 / v3.3.13-t9.10 and earlier candidate records</summary>

These retained records describe their original time, build and test scope. Their “current”, “pending” and “Latest” wording, APK hashes and debuggable-build descriptions are historical; they do not describe the 9.11 candidate or supply its acceptance.

Stable release target [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) has completed C37 build/static, API21/API35 whole matrices, real SAF, Direct Boot pressure, natural PIN and three cold-start acceptance. The accepted build is `a5309c0256e325a0f042d2d9062ddbd4f1326c28`, frozen at `build/t07-privacy/stable-candidate37-final1`, timestamp1791464154404 (2026-10-08T12:55:54.404Z). The full708.260124-second build passed582 application JVM tests,9 build-logic tests, Spotless and Lint0, with zero compiler/build warning lines and527 unchanged clean source inputs.

C37 pins each listed descriptor with an owned duplicate; full fstat device/inode identity comes first. Only real EACCES may use bounded metadata after both sound fixtures calibrate, and only a unique strict inode differing from every fixture proves a nonfixture. Acquisition-only direct-cause EBADF records vanished slots; all post-pin errors fail. The original200 scans,0→1→2→1→0 control,9 unrelated descriptors, privacy/TTS/SoundPool/playback/release and30-second budget completed on both matrices. API21 has8 probes and used no fdinfo fallback; API35 has11 probes and verified owned-copy metadata. Actual APK audit retains19 appDEX/6 testDEX,21 probe classes/148 bodies and catch tables/603 references,0 unresolved or above21;56 negatives rejected, two fresh replays had31 byte-equal outputs with51 files/553 dependencies. ARM64 remains build/static only.

QA3 drawing-state canonicalization and exact old-PID INFO clean-exit classification retain their focused112 evidence and strict raw/PID/context guards. QA4 adds only the trusted Toybox regular-empty alias for expected regular files with parsed size0; its actual61 focused cases retain all eight fields, integer/inode/permission/mtime predicates, raw kind and stat commands, without a numeric S_IFREG claim. QA5 isolates a lazy parent-owned four-core DeviceRun subclass whose complete Matrix startup method differs only in the core2→4 literal; boot/storage/readiness/cleanup behavior and the byte-immutable Matrix driver remain intact. Its focused32 cases and independent source peers passed. QA5 live98+139+56=293 passed once, `build/t07-privacy/reviews/candidate37-QA-epoch5-host-final1/final-review.json` SHA `cc3c12a760ff6c5637403d1ec3165c8ac0f690df743c19604e359774d622ff48`; current affected publication23 passed once, host END2026-10-08T16:49:26.873240Z, `build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final3/final-review.json` SHA `b21fbe762ac2804864ee2d552e3829bec2baada0c45665dadac701d9aa7894b0`. QA3 release68+23 remains historical once-only evidence, END2026-10-08T14:58:52.115495Z, `build/t07-privacy/reviews/candidate37-release-QAepoch3-host-final1/final-review.json` SHA `89fbbdf892e9e21303d826c43d3fe1a620e7949bbadf3eb453408f14145db3c9`. The unchanged original adjudicator68 and actual matrix-only adjudication final2 retain their own evidence; neither was rerun for QA5. Each host run binds its own QA13 and dependency15/17/10 closures; older global QA epochs remain historical snapshots. Current applicability peer `4faeddee008fb439bd7b0de07845d0d12084ab51b22bfbd87e0c509c682b52ee` preserves actual matrix/SAF/byte-audit execution scope. All527 candidate sources and three APKs remain the accepted C37 build. QA6 retains the one inherited post-handoff user-state read as full raw stdout/stderr with its exact command index. Replay requires this unique adjacent read, rc0, empty stderr and independently RUNNING_UNLOCKED before normal input; all original actions and deadlines remain intact. Current live Natural56 passed once, `build/t07-privacy/reviews/candidate37-QA-epoch6-host-final1/final-review.json` SHA `fe53a24b017ee6137010e858847800657e117871f0d845f901f7934bd7b3bec8`, and affected publication23 passed once. The parent and Matrix remain byte-immutable; the applicability peer also preserves pressure final5 through its separate15-input execution closure. QA5 host evidence and older global QA epochs retain their own historical source scopes.

Separate finished reports and independent peers accept both matrices, the three real SAF phases, fresh pressure final5, the real natural PIN path and three independent cold starts. Pressure keeps the original7s/1.25s/6500ms/≤60Hz/DOWN→START+EOF→UP/UID2000/PID-boot-binding/one Delete/resource-cleanup protocol. Natural uses no controlled rebind or stress injection. Matrix-only adjudication passed with0 validation errors and0 unexplained blockers, while retaining1558 scoped warning records and480 known project diagnostics, warning_free=false; this report does not adjudicate the separate SAF/pressure/Natural/cold gates. Independent gate peers: `7982a8d4d1b67d06e61577e1e1e1838a4c7bc3c7b03ad59539b7c6aa29cdf5e2`, `ffab0aba9a6d2bdcc9e584580dd2a8687e767ff51a561f4ceba32380789ffdbf`, `ff0e8cf843c0db09e2c06278bb2abb6c29a081557c49a552d5be65ad225b0eb0`, `964802c914e28a6589f1a819b1cb85b143553062550ed36077fe566acc3d85c2`, `5cd94ae1f2ac6817d64a73f6234d13351fa415cd5d2ab28d5b28f02def6199c5`, `89aa05b31f0a7cd76528a29b7b81689cb3e8435a17d5a7e74e30a2d2c8a186b5`. Fresh pressure final5 and Natural final2 acceptance applies to the explicitly recorded4-vCPU/2048-MiB host-GPU profile; both matrices and cold-start acceptance keep their original2-vCPU/2048-MiB scope. No seven-second guarantee on a two-core environment or CPU-specific product fix is claimed. External-resource byte proofs use a scoped privileged read-only lease after UID2000 input actions, followed by restoration to UID2000; they do not establish direct AppUID access.

Application APKs: ARM64 `d36241f2e7e61145d7f01148732b44632c46149cc1f571bb3c5aeab66689e17c` (22,442,517 bytes), x86_64 `2a6b7375d1c80595c736c0b64e9e9f34272d768f06fb78352767a5a97c10b05e` (22,539,554 bytes); testAPK `c1b41ba4149585d275c01f3c1b7be2289a71e6ce8fd024f6f5306188b60f28db` (1,480,837 bytes). Both appAPKs retain two META-INF v1 service warnings, test zero, min21/target37/versionCode20261115 and the existing debug package/certificate. API21 retains105 AudioTrack FAST warnings with successful SoundPool behavior. Physical ARM/16KiB-page/OEM coverage and universal bug or warning elimination are not claimed. Historical C37 failures remain unchanged: P1 whole FAIL `be5d3c579b3e63498f0e6829c01836ae35947f321646ee899e7c72d1c40b45f7`; P2 whole FAIL `e1c9ca93eab763ee79a11a8ce02429081eec697fb24de96e4ae9f8469445424d`; P3 whole FAIL `92105ccfe5d210ff16804525cf35221e5e3fb20ae0ac62a4241e8b184899d9c7`; P4 two-core whole FAIL `29740315c373a5173f6a945295652912ff7348e04fb5586758b3baf131432d71`; first matrix adjudication FAIL17 `b468ea8f97cc2d4f74cc11805843268c00fd7e048db0ea8d23d630142b0bdd21`. Their partial handoff/Delete facts are not whole or resource acceptance; P2 resource-audit body never started. Publication targets the authorized stable tag/link after the six-document commit, recursive source archive, APK staging and eleven-evidence verification; no earlier published timestamp or receipt is asserted. P3 inventoried87 files and completed81 four-step proofs, then stopped at file82 LOCK stat-before; its cat/device-hash/stat-after and remaining5 files were not reached. P4 exhausted the unchanged seven-second observation deadline: a replacement fork near the boundary did not supply complete in-budget process or ready-input evidence. Its separate timing/source diagnoses `939b16394b5bc4a727390ba834c9b36dd7038d7bbaf4d609d30715fde2cbf2f6`/`c8b9b4de1a2c8efa25318a36610d9997537aa23a12bb2ce8bc20e612206488a7` establish no supported product latency fix or fatal/OOM explanation for LatinIME deliberate self-kill. Both failures remain historical; current whole acceptance comes only from fresh pressure final5 on its stated four-core profile. Natural1 whole FAIL `ace1a169196e44828639a99da52476a948af0207ed5401386fdb596f817bc73c` remains unchanged: its bounded handoff and Delete/UID2000 input proof completed, but fixed-read span bookkeeping failed before the resource audit. Independent diagnosis `e020a11ed125e8cd759706f59b1b01022ddfc4901f5528e09efdf260128a1c93` and diagnostic replay `0cddccef95d8df463433b6913e615958e00c887ad9301479ee2dcfe05ee1d050` do not reclassify that run; current natural acceptance comes only from fresh Natural final2. The first publication23 fixture attempt failed before its audit derivation because the locked-log input was missing, `build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final1/failure-summary.json` SHA `43fe8d2516df7b13e12f10875ffccaeb24babbf5f57d8300388d7fdd7174424a`; that failed test run remains historical. The second attempt passed22 methods and failed one old counterexample whose reordered rows lacked resealed command indices, `build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final2/failure-summary.json` SHA `c863cc1d87827e145cedd8c91d1148bdd409937c6ad69f2c1ad81ffd6aac3367`; it also remains a failed host attempt. Final23 acceptance uses the corrected fixture/counterexample and its own finished source-bound report.

### Historical C36/C35 record — retained without borrowing acceptance

The following paragraphs preserve their recorded C36/C35 scope; C37 status is the pending record above.

Planned stable release [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) is **candidate36: the build/static checks and complete Android 5.0 matrix passed, but the complete Android 15 matrix failed; stable publication is stopped**. Commit `97d38863a4353fbdde95eca3592a60e4a653887e` changes only AndroidTest `InputFeedbackProbe.kt`, with no production change. The frozen directory is `build/t07-privacy/stable-candidate36-final1`, timestamp `1791460043820` (2026-10-08T11:47:23.820Z). The 458.793-second build passed 582 application JVM tests, 9 build-logic tests, Spotless and Lint 0, with no compiler/build warning lines and unchanged 527 source inputs. New C36 APK identity/signature/ABI/48-resource/16 KiB ZIP/ELF checks passed; each app retains two META-INF v1 service warnings (test zero), min21/target37 and the existing debug certificate/package. The 415 device-QA host checks and independent source review passed, with eleven frozen QA hashes. Android 5.0 completed eight probes, thirteen routes, seventeen lifecycle checkpoints, 100 visibility cycles, 52 glyphs/four dot commits and unavailable-input/retry; independent review passed 925 checks. Android 15 then failed at PID5442's first FD snapshot when `Os.stat` returned EACCES. The numeric FD slot was not recorded, and its precise cause is not established or fixed. The real owned-clipboard UI and preceding five probes are partial passes only. Targeted bytecode, final publication-tool binding and other gates still require full new evidence: C36 overall acceptance FAIL: API21 whole PASS, API35 whole FAIL; remaining gates NOT RUN and stable publication stopped. The next corrected candidate must rebuild and complete every gate itself; C36 API21 and earlier C35 passes cannot supply its acceptance. Nothing has been pushed, tagged or released; Latest remains `v3.3.13-t9.9`.

The C35 Android 5.0 final2 matrix under current QA failed overall: after 200 synchronous reads, `InputFeedbackProbe.kt:103` asserted that the total process FD increase exceeded eight. Actual before/after counts and FD targets were not recorded, so the origin remains unknown; neither a sound-reader leak nor a false positive has been established. Later SoundPool/melody/playback checks were not reached, and the sequential formal Android 15 matrix did not start. C36 scopes the test to each FD slot with the sound fixtures' `(st_dev, st_ino)` identities while retaining process totals as diagnostics. Host review covers opening the same fixture twice (1→2→1→0) and holding nine unrelated descriptors; API21 calibration passed; API35 failed during its first snapshot with EACCES, so the next candidate must supply fresh calibration and complete gates. The original 200 reads, 30-second budget, privacy/TTS/SoundPool/playback/cleanup checks remain, with no GC, added waits, raised tolerance or malformed-YAML warning injection.

C35 historical record (superseded by the C36 status above): the candidate35 build passed but complete device acceptance was not achieved. Unified privacy and 30 upstream commits through `e9b21960` remain included. C35 fixes the normal-process WorkManager initialization race with the official WorkManager 2.10.5 Configuration.Provider, a stable lazy configuration and one getInstance path; actual SDK21 guards and DPS/old-DE/replacement-state checks prevent initialization in unsafe process states. Implementation commit `dc69d73baa0c06e5a103000d1cd13f106f280afd` binds 527 source inputs and timestamp `1791449483471` (2026-10-08T08:51:23.471Z). The frozen build is `stable-candidate35-final1`; the earlier interrupted build did not produce an identity chain. The full build passed in 519.403 seconds with 582 application JVM tests, 9 build-logic tests, Spotless, Lint 0 and no compiler/build warning lines. APK identity, static checks and targeted API21 DEX review passed. Each app APK retains two META-INF v1 warnings (test APK 0). The earlier C35 Android 5.0 matrix and independent review genuinely passed under the previous QA, but QA phase4 changed the Matrix/binding tools; the fresh API21 final2 replay subsequently failed overall at the process-FD assertion. The unchanged real SAF phases and process-scoped review remain accepted after current-verifier revalidation; all other current device gates and final adjudication are pending. The first SAF attempt stopped before any picker action because preparation left Latin selected and Main redirected to Setup3; it proves neither a product defect nor successful SAF acceptance. The corrected preparation restores and verifies Trime before the gate without SAF tree/preferences/grant edits; its independent review and the subsequent real DocumentsUI roundtrip/restart/revoke gates passed; C35 complete runtime acceptance was not achieved; API21 final2 failed overall (history). The first C35 Android 15 matrix failed overall: the real owned-clipboard UI and nine probes passed, but ClipEditorProbe could not establish a stable keyboard; later navigation/lifecycle/fallback stages did not run. The failed evidence is retained; the subsequent parser diagnosis does not reclassify this matrix. C35 pressure final1 also failed overall: the original 7-second handoff and full 6501-ms gesture passed, but stage 10 required an IME before its planned single focus, so no real Delete or resource audit completed. Its evidence is retained; the pressure QA ordering correction passed host and independent peer checks. Pressure final2 then failed mutation-3 at the original 1.25-second command limit; its completed 6503-ms gesture is partial evidence only. The epoch3 full-window diagnostic remains an overall FAIL. Read-only replay found 75 Latin frames: its first transitional frame still fails current binding guards, while 74 fully shown frames have a live current connection and `mRestartOnNextWindowFocus=true`. Android 15 defines this as a future window-focus restart request, so requiring false incorrectly rejected those otherwise valid frames. Phase4 has applied a strict unique true|false parser while retaining all current PID, sequence, live-connection, method, EditorInfo and visibility checks; the exact future flag remains recorded outside stable binding identity. Independent review also found malformed duplicate-field gaps (empty values, whitespace before or after equals, or a bare key) in the initial /tmp normal-input proposals; it was corrected before any live application, and both normal-input suites now reject those duplicates. The fresh phase4 short API35 clipboard diagnostic passed with the complete original probe and its assertions after one Latin-to-fresh-Trime sequence; independent runtime review is pending, and this short diagnostic is not a complete matrix or release acceptance. All 415 checks of the exact applied host tools passed; host/source review proves the parser correction only; it does not turn the old diagnostic into PASS or supply a new device result. Current API21/API35 matrices, pressure and natural PIN, cold starts and final adjudication remain pending fresh evidence. C31–C34 results are historical only: C34's successful natural handoff and one real Delete are partial evidence within an overall failed gate. The original 7-second, 1.25-second command, 6500-ms and 60-Hz requirements remain. Privileged read-only file auditing is identified separately and does not prove app-UID file access. The debug certificate/package and historical C31 6,280-ms SAF deployment queue wait remain documented. The current C35 SAF deployment also records one non-injected 5,434-ms Rime queue wait before instrumentation; repeated snapshots show the same event, not three waits. This measured performance limit remains. The final process-scoped SAF review passed and retains one actual native SIGSEGV from the root system input.Input command, identified by PID; the complete device logs are not fatal-free. ARM64 has build/static checks only, with no physical ARM64 or native ARM image run. Nothing has been pushed or published; Latest remains `v3.3.13-t9.9`. See the [release draft](doc/releases/v3.3.13-t9.10.md) and [validation report](script/quality/VALIDATION-T07-PRIVACY.md).

ARM64 and x86_64 APKs retain Android 5.0+ support, package `com.osfans.trime.debug`, and the previous fork signing certificate for in-place upgrades. The default and non-Chinese display name is `Trime`; Simplified Chinese uses `同文输入法`, while Hong Kong and Taiwan Traditional Chinese use `同文輸入法`. These are debug-signed, debuggable builds; production signing and R8 are not enabled. Back up configuration and dictionaries before upgrading. Custom copies of the T9 schema or themes may need the updated rules/layout and redeployment; retain personal dictionaries.

[T04](script/quality/t04/README.md) provides fixed ranking and learning benchmarks in isolated data directories. Luna remains the default; no large dictionary or grammar model is enabled. [T05](script/quality/t05/README.md) separates six optional fuzzy-pinyin rules from three bounded numeric-key repair rules. All are off by default; suggestions display their source. Optimization-plan T08–T10 remain follow-up work: this release does not claim the required physical-device candidate-latency/PSS budget, fixed mixed-input switch-count acceptance, or expanded personal-dictionary and candidate-management acceptance.

Each C35 application APK has two required coroutine service descriptors with v1 META-INF signature-coverage warnings (test APK zero). The C36 static checks independently confirmed the same two v1 META-INF warnings per app APK (test APK zero); these required resources remain, and this draft makes no warning-free claim. Native phone performance, OEM editor coverage, and physical 16 KB-page devices are outside this emulator acceptance. The release has three manual assets: two application APKs and one recursive source archive; GitHub also provides its automatic source links. See also the [key contracts](script/quality/T03.md), [T9 design](script/t9/README.md), and [upstream merge record](script/t9/UPSTREAM-t9.3.zh-CN.md).

</details>

## Download

- T9 Stable [Download](https://github.com/X2M7/trime/releases/latest)

- Upstream Stable Channel <br>
  [<img alt='Get it on F-Droid' src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' height='80px'/>](https://f-droid.org/packages/com.osfans.trime)
  [<img alt='Google Play Download Now' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height='80px'/>](https://play.google.com/store/apps/details?id=com.osfans.trime)

- Nightly Channel [Download](https://github.com/osfans/trime/releases/tag/nightly)

- Canary Channel [Download](https://github.com/osfans/trime/actions)

- Configurations [rimerc](https://github.com/Bambooin/rimerc)

## History

TRIME is the abbreviation of _Tongwen RIME_ or _ThaeRv Input Method_.

From the beginning, TRIME was written for TaeRv Pinyin, and named _TaeRv Input Method (泰如输入法)_.

Then, we created an input method platform with some code tables, such as Wu dialect (吴语). We renamed it to _Chinese Character Dialect Input Method (汉字方言输入法)_.

Later, it supports Wubi and Liangbi and other shape-based input method, we branded it [_Tongwen Input Method Platform 2.0 (同文输入法平台 2.0)_](https://github.com/osfans/trime-legacy), which implies that the phonetic-based and shape-based input method on one platform, while dialects and Mandrain share one kind of characters.

Benefit from the [librime](https://github.com/rime/librime) project by JNI, we are now in version 3.0 of TRIME aka _Tongwen Input Method (同文输入法)_.

Your are now welcome to [contribution](CONTRIBUTING.md) ~ !

## Getting Started for developer

### Prepare

#### Requirements:

- Android SDK and Android NDK
  * If you are new to Android development, please install [Android Studio](https://developer.android.com/studio).

- JDK (OpenJDK) 17
- Python 3 (required by OpenCC to generate dictionary text files)

#### Prerequisites for Windows

Symbolic links will be created according to current build configurations, developers need:

- Enable [Developer Mode](https://learn.microsoft.com/en-us/windows/apps/get-started/enable-your-device-for-development) so that symlinks can be created without administrator privilege.

- Enable symlink support for `git`:

  ```powershell
  git config --global core.symlinks true
  ```

If you cannot or wouldn't like to enable anything, it doesn't matter. Copying will be used instead when error on creating symbolic links.

### Build

#### 1. Clone this project and fetch all submodules:

```sh
git clone git@github.com:X2M7/trime.git
cd trime
git submodule update --init --recursive
```

#### 2. Debug version without signature:

```sh
# On Linux or macOS
make debug

# On Windows
.\gradlew assembleDebug
```

#### 3. Release version with signture:

Create `keystore.properties` file which contains following contents for [signing information](https://developer.android.com/studio/publish/app-signing.html):

```gradle.properties
storePassword=myStorePassword
keyPassword=mykeyPassword
keyAlias=myKeyAlias
storeFile=myStoreFileLocation
```

Then, you may run:

```sh
# On Linux or macOS
make release

# On Windows
.\gradlew assembleRelease
```

### Troubleshooting

```
Target "boost_log_setup" links to target "Boost::coroutine" but the target was not found.
```

Run `make clean` on Linux or macOS, or run `.\gradlew clean` on Windows.

Other issues:

1. Try `make clean`
2. Make sure your repo is up-to-date. If one or more submodules are modified, also make sure they are compatible with the current version.
3. If the problem still exists(very unlikely), try to make a new clone.
4. Check if this is there is an issue/PR related to your problem. If yes, try their solutions.
5. If none of them works, you may make an issue to ask for help.(optional)

## Acknowledgments

- Developer: [osfans](https://github.com/osfans)
- Contributors: [boboIqiqi](https://github.com/boboIqiqi)、[Bambooin](https://github.com/Bambooin)、[senchi96](https://github.com/senchi96)、[heiher](https://github.com/heiher)、[abay](https://github.com/a342191555)、[iovxw](https://github.com/iovxw)、[huyz-git](https://github.com/huyz-git)、[tumuyan](https://github.com/tumuyan)、[WhiredPlanck](https://github.com/WhiredPlanck)、[nopdan](https://github.com/nopdan)...
- [Wiki Editors](https://github.com/osfans/trime/wiki): [xiaoqun2016](https://github.com/xiaoqun2016)、[boboIqiqi](https://github.com/boboIqiqi)...
- Translators: 天真可爱的满满 (Chinese Traditional), 点解 (English) ...
- Keyboard Designers: 天真可爱的满满、皛筱晓小笨鱼、吴琛 11、熊猫阿 Bo、默默ㄇㄛ ˋ...
- Donations: See QR Code in [Releases](https://github.com/osfans/trime/releases)
- Community: Netizens who feedback in [Issues](https://github.com/osfans/trime/issues)、[QQ Group (811142286)](https://jq.qq.com/?_wv=1027&k=AXdR80HN)、[QQ Group (224230445)](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=pg_q7UVumWYLq1Rk8kIAqkK1xGt64VnX&authKey=04m9l7OBO5H5vgrEL8IbpsmtnptWM60xy%2FUwYCfyvw9VcRhe8zRzAS1ezoemZdFr&noverify=0&group_code=224230445)、[Tieba](http://tieba.baidu.com/f?kw=rime)、[Google Play](https://play.google.com/store/apps/details?id=com.osfans.trime)、[Telegram](https://t.me/trime_dev)...
- Projects: [RIME]、[OpenCC]、[Android Traditional Chinese IME] and so on.

## Third Party Libraries

- [Boost C++ Libraries](https://www.boost.org/) (Boost Software License)
- [darts-clone](https://github.com/s-yata/darts-clone) (New BSD License)
- [LevelDB](https://github.com/google/leveldb) (New BSD License)
- [libiconv](https://www.gnu.org/software/libiconv/) (LGPL License)
- [marisa-trie](https://github.com/s-yata/marisa-trie) (BSD License)
- [glog](https://github.com/google/glog) (New BSD License)
- [OpenCC](https://github.com/BYVoid/OpenCC) (Apache License 2.0)
- [RIME](https://rime.im) (BSD License)
- [snappy](https://github.com/google/snappy)(BSD License)
- [utfcpp](https://github.com/nemtrif/utfcpp) (Boost Software License)
- [yaml-cpp](https://github.com/jbeder/yaml-cpp) (MIT License)
- [Android Traditional Chinese IME](https://code.google.com/p/android-traditional-chinese-ime/) (Apache License 2.0)

[Android Traditional Chinese IME]: https://code.google.com/p/android-traditional-chinese-ime/
[RIME]: http://rime.im
[OpenCC]: https://github.com/BYVoid/OpenCC
