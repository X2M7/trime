Add native Chinese nine-key Pinyin as one usable input path, using the existing
`luna_pinyin` dictionary and user dictionary. Enable `luna_pinyin_t9` through the
existing scheme selector and deploy it; both stock themes provide the layout.

#### Issue tracker

Related #1464, #1299, and the spelling-filtering part of #459. This does not close
the broader editing or prediction requests. Earlier #2081 was closed by its author
without merging or review and provides related design history.

#### Feature

- Enter Pinyin with 2–9, choose and lock a syllable, change segment focus, unlock,
  undo or cancel. Native revisions reject stale spelling actions.
- Carry displayed spelling and its caret mapping through native edits, preedit
  touches and editor restarts. Keep private lock markers out of displayed text and
  raw-code commits. Retain the batched Hanzi candidate API and read one additional
  snapshot of the current spelling state.
- Supply matching layouts, literal number/symbol pages and long-press digits.
  Keep existing user configuration and describe switching behavior in
  [doc/t9-input.md](https://github.com/X2M7/trime/blob/593c23002343d5c7e6db5bf37e2bdfa0ba05f8ee/doc/t9-input.md).

The feature includes its native/JVM tests and usage notes. Prediction models,
privacy policy, candidate pinning, broad accessibility/viewport changes and fork
release configuration are outside this patch.

#### Unicode correctness

Carry standard UTF-8 across JNI through UTF-16. The previous Modified UTF-8
bridge corrupted supplementary Hanzi and emoji; a confirmed T9 prefix `𠮷 426`
arrived with different text and length. The same string helper serves candidates,
commits, configuration and OpenCC. Reject failed input conversion before native
side effects and retain the original Java exception. Remove signedness, return-move
and aggregate-initializer diagnostics without changing pinned dependencies.

#### Current upstream integration

Current head `593c23002343d5c7e6db5bf37e2bdfa0ba05f8ee` normally merges the
independent TextView state correction (#2187) into `11746801`, which already
integrated upstream `12ffd16f` (JNI ownership/source directories and Snappy 1.3.1).
The rendering merge changes only AutoScaleTextView; T9 native/protocol/assets and
all 20 recursive pins remain identical to `11746801`. The first API21 run of
`11746801` failed its display precondition under actual accessibility-node
inspection, so its 16 planned input cases were not executed or accepted. New
Android checks use the repaired source; earlier build evidence retains its SHA.

#### Validation

- [x] Current `593c2300`: [complete exact-source JVM/JNI task](https://github.com/X2M7/trime/actions/runs/39706284345)
  actually passed 328 application cases / 25 suites and 115 real-JVM Unicode
  assertions. XMLs, original streams, source and 20 pins were verified.
- [ ] Current-source cloud APK platform jobs and two-theme Android checks are
  still being completed; earlier source results below are not relabeled as new runs.

- [x] Parent `11746801` (same native inputs): eight JNI translation units compile for NDK28,
  x86_64/API21 under strict warnings as errors, with zero diagnostics.
- [x] Parent `11746801` (same native inputs): real JDK17 checked-JNI VM passes 146 assertions:
  115 Unicode checks plus 31 borrowed/owned/nested attachment and unwind checks.
  This is separate from application unit tests and Android runtime acceptance.
- [x] Parent `5b6d1df4`: [exact-source application JVM and JNI task](https://github.com/X2M7/trime/actions/runs/39526955308)
  passed 328 JUnit cases / 25 suites and 115 separate real-JDK25 JNI assertions.
  Parent-source pinned-engine tests executed 743 assertions in four processes.
- [x] Parent `11746801` [complete JVM/JNI task](https://github.com/X2M7/trime/actions/runs/39634407030):
  328 application cases / 25 suites and 115 separate real-JDK25 JNI assertions
  actually passed, with zero failures/errors/skips; exact source and 20 pins verified.
- [x] Parent `11746801` Debug/native APK builds:
  [fork](https://github.com/X2M7/trime/actions/runs/39634189669),
  [upstream PR](https://github.com/osfans/trime/actions/runs/39634199167) — all four OS jobs
  in each pipeline succeeded. Fork Ubuntu/Windows retain their first-attempt results;
  only the failed/capacity-interrupted Mac jobs were rerun. Original APK identity,
  ABI and signature were audited; known dependency/Debug signature warnings remain.
- [ ] Two-theme Android input, spelling locks/undo, literal pages, Unicode caret,
  editor restart/application switching and stale-action smoke checks.

The PR remains Draft. Host checks do not replace production native linking,
Android ART/InputConnection/UI or device acceptance. Existing dependency/tool
warnings, general native OOM handling, Lint, Release and device performance are
not claimed resolved. Previous evidence remains in the
[fork-only contribution index](https://github.com/X2M7/trime/tree/contrib/split-index/doc/contribution-split).
