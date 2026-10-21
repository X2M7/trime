# feat: add nine-key Pinyin input with syllable selection

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
  [doc/t9-input.md](https://github.com/X2M7/trime/blob/5b6d1df4dc1a553fbe513c729aade7e9b3e4cdc7/doc/t9-input.md).

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

#### Validation

- [x] Eight JNI translation units compile with NDK 28 for x86_64/API21 under
  `-Wall -Wextra -Werror -Wno-unused-parameter`, with zero diagnostics.
- [x] Real JDK 17 VM with checked JNI: 115 assertions pass, including supplementary
  characters, malformed input, null/array failure and exception identity. The
  original-source failure control fails on supplementary Hanzi as expected.
- [x] Final clean source runs four pinned-engine native processes: 743 assertion
  executions, including two process-restart user-dictionary checks. The unchanged
  pinned host engine library is reused; it was not rebuilt this round.
- [x] clang-format 22, patch whitespace and Conventional Commit checks.
- [x] [Final exact-source complete application JVM and checked-JNI task](https://github.com/X2M7/trime/actions/runs/39526955308):
  328 tests / 25 suites, zero failures/errors/skips, plus 115 separate JNI
  assertions on JDK 25. The original-source Unicode failure control is retained.
  Official artifact digest, raw XML, executed tasks, source and 20 pins verified.
- [ ] New four-platform Debug/native builds: [fork](https://github.com/X2M7/trime/actions/runs/39526931659), [upstream PR](https://github.com/osfans/trime/actions/runs/39526937528).
- [ ] Android manual input/editor/layout acceptance, Lint and Release.

[Current evidence and exact input bindings](https://github.com/X2M7/trime/blob/contrib/split-index/doc/contribution-split/checks/t9-input-unicode-warning-repair.json).
The zero-diagnostic statement is limited to the recorded JNI checks. Existing
build-tool/dependency warnings, general native OOM handling, Android ART/UI and
device performance remain outside those results. Keep this PR Draft.

Source: `5b6d1df4dc1a553fbe513c729aade7e9b3e4cdc7` (three commits), based on
`osfans:develop` at `144f6111d0498da85e0ffa250245f18955179ed3`.
