# PR #2187 review follow-up — 2026-10-23

Application head: `92b13711162c5fa56c701267c3a06e5fbceeb6ce`; tree: `64afb38768dee019ce136d4673996ccbe36c3784`. The upstream patch remains one production file (`AutoScaleTextView.kt`, 10 additions / 7 deletions against `12ffd16f`). Evidence and test tooling are kept on the fork contribution-index branch, outside the application PR.

## Changes and review rationale

Our measurement/transform flags are set before the parent setter, and explicit `requestLayout` / `invalidate` calls run only for changed content or null. The original nullable value and requested BufferType always reach `super.setText`; equal content may still need a real buffer conversion, and TextView calls the override during its constructor. Null is not pre-normalized to an empty string. The added `super.onLayout` forwarding is removed. `super.onMeasure` stays, with a comment explaining the framework Layout requirement. Custom measurement, scaling and drawing algorithms are unchanged.

See [source review](independent-static-review1.json), [scoped patch](application-review-fix.diff) and [actual source push](application-push1/receipt.json).

[Root scoped verification summary](root-qualified-scope.json) binds the exact current results and remaining API35 gap.

## Verification of this head

The [exact-source control](https://github.com/X2M7/trime/actions/runs/39872481596) newly executes all **301 existing application JVM tests** (19 XML files, zero failures/errors/skips). They do not exercise Android TextView rendering. [Root XML/log audit](ci-unit1/root-audit1.json) and [original reports](unit-tests) preserve actual execution and all 20 initialized recursive submodule pins.

[Fork CI](https://github.com/X2M7/trime/actions/runs/39872383227) completes **4/4 jobs**, and [upstream PR CI](https://github.com/osfans/trime/actions/runs/39872386788) completes **5/5**. [Terminal qualification](formal-ci/formal-ci-qualification1.json), all nine original job logs (lossless gzip), raw run/jobs JSON and warning rows are retained. Upstream uses its recorded synthetic merge; it is not substituted for the exact fork APK producer. No all-native-objects-freshly-compiled claim is made.

The [original Ubuntu artifact audit](ci-apk1/audit/result.json) checks all four ABI APKs, certificates, ELF ABI, BuildConfig and focused DEX contracts. Selected original x86_64 APK SHA-256: `36ede385eac30511f3c600dbc059a196e1ac462f173b154643a54f79b3af7d3a`; its actual shared CI Debug certificate is `3a9003f52ba2aa9045d5ba320df1f2e04277c001420e05b8b8f0c69183b18458`. The tested APK is never modified or re-signed.

| Emulator | Original installed APK class | Separate no-super-measure source control | Literal equality-guard setter source control |
| --- | --- | --- | --- |
| API21 / Android 5.0, round4 | 13 stages / 39 PNGs pass | First 11 framework Layout oracles fail with null Layout; matching reference passes. The two later selectable/granularity stages pass. | Initial measurement aborts in `BoringLayout.isBoring`; no result/PNG directory exported, no buffer-stage result claimed. |
| API35 / Android 15, round5 | NOT RUN: safe diagnostic aborted before any stage because the 8-second combined window-focus / shown-view / positive-width precondition was not met. | NOT RUN | NOT RUN |

[API21 complete child qualification](independent-runtime1/api21-round4-child-terminal2.json) and [fixture/terminal qualification](independent-runtime1/api21-round4-fixture-terminal3.json) are the current references; earlier partial counts and parser refusals remain historical.

The API21 13 positive stages cover construction; Hanzi/Latin/null/rebind; six same-content NORMAL/SPANNABLE/EDITABLE transition/repetition sequences; selectable-text precondition; and public LINE NEXT/PREVIOUS actions with selection `0 → 2 → 0`. Buffer types are observed immediately after each setter, before node creation can itself promote NORMAL. Hardware capture happens before software drawing; text, Layout, node text, glyph pixels and file hashes are checked against the same-stage framework TextView reference.

Root actually viewed seven original API21 hardware screenshots, and the API35 baseline/restored failure screenshots; the remaining pixel/hash checks are automated independent checks, not claimed as manual image inspection.

The three source controls are **separately compiled non-production diagnostic classes**: safe is namespace-only, literal setter replaces exactly that setter plus namespace, and no-measure removes exactly one parent measurement call plus namespace. They share a diagnostic toolchain with each other, not the production PR toolchain (diagnostic AGP 9.4.1 vs PR AGP 9.3.0). [Toolchain correction](diagnostic-build/toolchain-correction1.json), [host qualification](diagnostic-build/build3/host-qualification2.json), exact source/diff bindings and failed earlier builder records remain separate. An aborted control is `CONTROL_ABORTED_UNQUALIFIED`, not an invented exported negative pass.

## Runtime scope and retained limitations

The production-class probe uses an external Activity context and loads the installed original class. It does not validate the native IME, candidate activation, TalkBack service/binder-node routing, physical devices, Release, upgrade or performance. LINE movement is a public View action with an explicitly selectable diagnostic precondition. Static screenshots cannot establish the selection movement; actual JSON action returns and positions do.

Class-only isolation temporarily hides DocumentsUI on API21 and disables it on API35 in disposable private AVDs. Actual package path, installed/hidden/enabled/stopped state and original IME settings must be restored exactly. Only unique real BEGIN/END log boundaries plus phase process snapshots qualify the absence of Trime target processes. Hardcoded probe booleans are not process evidence, and observed self PIDs with start time `NOT_OBSERVED` are not assigned invented identities. Full logs, outside-interval starts, collector exit values and native diagnostic crashes remain visible.

API21 complete closed log counts are W1568 / E526 / F0; one E-level `FATAL EXCEPTION` belongs to the literal diagnostic control. It is not a production-class crash, and it does not permit a global no-fatal claim. Earlier host counting omitted two space-containing `WAP PUSH` tags and 34 debug rows; [the correction](independent-runtime1/log-priority-parser-note1.json) preserves all omitted original rows and old reports. API35 round5 has a preceding SystemUI ANR and repeated phone ANRs. The final restored screenshot visibly shows the System UI not responding dialog. No contemporaneous focus-window dump was captured before shutdown, so a unique causal explanation is not asserted. The three readiness values are not individually recorded, so failure of window focus alone is not established. The safe diagnostic exports a real failure with zero stages; the child then fails on its missing source identity field. No original-APK rendering or negative-control result is inferred from that failure. The END-following fatal exception of com.android.phone PID2301 is also retained; it is not a product or negative-control crash. The round ends with normal VM exit 0 and collector exit 255, exact settings/DocumentsUI restoration and all 25 original SDK files unchanged. See the [independent failed-round audit](independent-runtime1/api35-round5-failed-terminal1.json) and full raw logs.

[Observed CI warning inventory](formal-ci/warning-row-inventory1.json) retains fork/upstream **301/57 mechanical matched rows**, not distinct defects or JVM test counts. Actual native dependency warnings include yaml-cpp dragonbox, librime-lua integer conversion and leveldb CMake deprecation. SDK/Gradle/JDK/cache diagnostics and two META-INF/services signature warnings per APK also remain. They are neither silently removed nor declared harmless or fixed by this focused patch.

Earlier API21 rounds remain failed/unqualified: round1 stopped after the literal constructor crash; round2 observed a background `RimeDataProvider` start and refused class-only qualification; round3 refused the unavailable restore command before mutation. API35 round4 refused an extra permission-section `User 0` header before mutation/BEGIN/child, and its collector actually exited 255. Plan5 changes only package-state-line parsing, keeps duplicate actual states a failure, and does not rerun or rewrite API21 round4. Empty permission headings are not package state.

The local `.gitattributes` disables text newline conversion for these original artifacts, preserving CRLF and terminal spaces byte-for-byte; artifact whitespace is not normalized. Application source formatting checks are separate and passed.

[Runtime publication map](runtime-publication-map.json) binds original bytes to copies and lossless gzip; binary command streams, APKs, private backups and signing material are omitted from Git. [Formal CI manifest](formal-ci/manifest.json) independently binds all original logs. The complete local originals and all refused attempts are retained. No review thread is resolved, review dismissed, merge performed or release published by this evidence update.
