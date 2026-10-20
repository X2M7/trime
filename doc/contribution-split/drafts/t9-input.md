# feat: add nine-key Pinyin input with native spelling selection

Provide one usable nine-key input path: a scheme, layouts in both stock themes, native spelling disambiguation, spelling controls, mapped preedit and ordinary Hanzi candidate submission. Enable `luna_pinyin_t9` through the existing scheme selector and deploy it; the scheme shares the existing `luna_pinyin` dictionary and user dictionary. Existing scheme selections and custom configuration are preserved. Custom themes still need the matching keyboard or a `trime/keyboard` mapping.

Press 2–9 to compose, choose a syllable to lock its spelling, and use segment focus, unlock, undo or cancel to revise it. Native revisions reject stale spelling actions. Keep private lock markers out of displayed preedit and literal commits, and retain the existing batched Hanzi candidate API. Read one additional snapshot of the current spelling state on the Rime dispatcher rather than querying syllables for each candidate.

Carry the displayed spelling and its cursor mapping through the editor path. Convert a touched UTF-16 offset to UTF-8 once, retain the pressed text identity, and cancel queued cursor work when the composing state resets. A new or restarting editor clears the previous spelling locks. Long-pressing a digit uses the complete raw-input Return path before inserting the literal digit; number and symbol pages use literal mode. Switching to the English layout follows the existing layout-switch commit behavior.

Related: [#1464](https://github.com/osfans/trime/issues/1464), [#1299](https://github.com/osfans/trime/issues/1299), and the spelling-filtering part of [#459](https://github.com/osfans/trime/issues/459). The broader editing and prediction requests are not fully addressed. Earlier [#2081](https://github.com/osfans/trime/pull/2081) was closed by its author without merging or review; it is design history, not an active or merged duplicate.

This is a single feature rather than the previous aggregate patch. It excludes next-word prediction, privacy mode, candidate pinning, optional fuzzy-spelling preferences, general candidate/editor-generation infrastructure, setup/import changes, broad accessibility/viewport changes and fork release configuration. Usage and switching behavior are described in `doc/t9-input.md` in the feature branch.

Local application JVM checks passed 328 tests in 25 suites, with no failures, errors
or skips; Kotlin/Java compilation and SpotlessCheck passed. The task began on the
initial commit with the color-name correction uncommitted; all tested source bytes
match the final commit, but generated BuildConfig identified the initial commit.
It used an empty ignored prebuilt directory and is not an APK or JNI test.

The exact-source [application JVM run](https://github.com/X2M7/trime/actions/runs/39467124333)
and [four-platform Debug builds](https://github.com/X2M7/trime/actions/runs/39467034017)
are pending. Native host runtime acceptance is also pending. Preserve the existing
JNI signedness/copy-elision and build-tool diagnostics; do not claim zero warnings.
Android spelling controls, Hanzi submission, Unicode transport, literal input,
editor transitions, TalkBack, Lint and Release remain unverified. Retain Draft.

Source: `X2M7:contrib/pr-t9-input` at `649c07559cfbc1e494835465e71277fe8dc8051c`;
base: `osfans:develop` at `144f6111d0498da85e0ffa250245f18955179ed3`.
This file is a proposal draft; no upstream PR has been created for this module.
