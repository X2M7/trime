Add Chinese nine-key Pinyin using the existing `luna_pinyin` dictionary and
user dictionary. Enable `luna_pinyin_t9` through the existing scheme selector and
deploy it; both stock themes provide the layout.

Support syllable choice/locking, focus changes, unlock, undo, cancel, literal
digits/symbols and editor-boundary reset. Native revisions reject stale spelling
actions. Candidate responses remain batched, with one additional spelling
snapshot rather than a JNI query per candidate.

Related #1464, #1299 and the spelling-filtering part of #459.

**Draft: maintainer-requested real-device verification is still NOT RUN.**

Patch cleanup:

- Rebased onto upstream `19b04849`; five linear Conventional Commits, no merges.
  All have explanatory bodies and lines within 100 characters.
- Candidate layout and deferred-input-view dependencies are separately reviewable
  in #2187 and #2188; related Unicode work is in #2186.
- The patch contains application code, tests and its behavior document only.
  Fork release/Pages configuration, prediction, privacy policy and candidate
  pinning are outside this PR.
- Regression testing found and fixed remembered T9 restoration after a scheme
  change, then forced email ASCII being overwritten during layout attachment.
  Both original failures are retained.

Verification of current head `c4b613f7`:

- [Fork CI](https://github.com/X2M7/trime/actions/runs/39806420824) and
  [upstream PR CI](https://github.com/osfans/trime/actions/runs/39806428888):
  each 5/5 jobs completed successfully.
- [Exact-source control](https://github.com/X2M7/trime/actions/runs/39806534353):
  360 newly executed application JVM cases and 115 independent real-JVM JNI
  assertions passed.
- The same original x86_64 Debug artifact, without modification or re-signing,
  completed the mapped
  Android 5.0 and Android 15 emulator checks: per system, 12 successful sessions
  covering 14 logical items across both themes. These cover schema/forced-mode
  recovery, syllable selection, Return, literal/symbol switching, fresh-editor
  reset and real-touch Shift behavior. K1 is embedded once in each R4.
  Exact editor text, spelling state, process identity and authenticated stop
  assertions remain intact.
  This is not a rerun of all historical 17 cases or whole-app/device acceptance.
- Full original logs, earlier failures and harness rejections remain traceable.
  Dependency/tool/runtime warnings and two Debug META-INF signature warnings per
  APK remain recorded; there is no zero-warning claim. The Android 15 stream
  retains preinstall phone/SystemUI ANRs and an F-priority kernel IRQ #35 event,
  without claiming a unique cause or zero system errors.

[Current evidence and limits](https://github.com/X2M7/trime/blob/9b8d747227eb6c40f18a36a78dd9bdef6a77a8ce/doc/contribution-split/checks/pr2183-rebase-20261022/README.md).
[Original ARM64 Debug APK identity and physical-device checklist](https://github.com/X2M7/trime/blob/9b8d747227eb6c40f18a36a78dd9bdef6a77a8ce/doc/contribution-split/checks/pr2183-rebase-20261022/device-delivery-c4-reviewed/README.md).
The ARM64 package is prepared and statically checked, not physically verified;
all phone result fields remain NOT_RUN. This PR stays Draft until that work is done.

[Input behavior and switching contract](https://github.com/X2M7/trime/blob/c4b613f78835e7b914e02447955c6b89fe9839ab/doc/t9-input.md).
