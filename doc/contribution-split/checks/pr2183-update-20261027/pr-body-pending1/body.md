Add Chinese nine-key Pinyin using the existing `luna_pinyin` dictionary and
user dictionary. Enable `luna_pinyin_t9` through the existing scheme selector;
both stock themes provide the layout.

Support syllable choice/locking, focus changes, unlock, undo, cancel, literal
digits/symbols and editor-boundary reset. Native revisions reject stale spelling
actions. Candidate responses remain batched, with one bounded spelling snapshot
rather than a JNI query per candidate. Related #1464, #1299 and #459.

**Draft: the new rebase is awaiting CI/emulator verification, and the
maintainer-requested physical-device verification remains NOT RUN.**

Current head `8a1ee0ea` is rebased onto upstream `a047f747`, with four linear
Conventional Commits. The obsolete candidate dependency has been removed:
#2187 is already merged upstream, and its accepted implementation is retained.
The included Unicode and deferred-view dependencies now match the reviewed
changes in #2186 and #2188. The scheme-restoration and editor-required ASCII
fixes remain. All six intervening upstream fixes, including symbol-only keys,
toolchain and glog changes, are preserved.

The patch contains application code, tests and its behavior document. Fork
release/Pages configuration, prediction, privacy policy and candidate pinning
remain outside this PR.

Verification is being repeated for this source. The old `c4b613f7` CI and
Android 5.0/15 emulator results remain historical; they do not qualify the new
rebase. New results will be recorded here when completed. No zero-warning,
physical-device or Release acceptance is claimed. There is currently no phone
available for the required real-device test, so this PR remains Draft.

[Input behavior and switching contract](https://github.com/X2M7/trime/blob/8a1ee0ea6538e220fcddf31ed6fd491f5bc3cca7/doc/t9-input.md).
