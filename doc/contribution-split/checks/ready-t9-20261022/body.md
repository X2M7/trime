Add Chinese nine-key Pinyin using the existing `luna_pinyin` dictionary and
user dictionary. Enable `luna_pinyin_t9` through the existing scheme selector and
deploy it; both stock themes provide the layout.

Related #1464, #1299, and the spelling-filtering part of #459. This does not close
the broader editing or prediction requests.

The input path provides:

- Digit input, syllable choice/locking, segment focus, unlocking, undo and cancel.
  Native revisions reject stale spelling actions.
- Displayed spelling and caret mapping through native edits, preedit touches and
  editor restarts. Private lock markers stay out of displayed/raw-code commits.
  Hanzi candidates retain their batched API; spelling state adds one snapshot,
  rather than a JNI query per candidate.
- Matching layouts, literal number/symbol pages, long-press digits, regression
  tests and [defined switching behavior](https://github.com/X2M7/trime/blob/9f86bc8f1d11d65d2e22e437f111a965bc2faba5/doc/t9-input.md).

The current head is `9f86bc8f`, including the independent startup correction
`2d2fbce7` (#2188) and candidate TextView correction `7a45cf29` (#2187).
The shared UTF-8/UTF-16 transport work is separately reviewable in sibling PR
#2186; its head `e16ab85f` is not an ancestor of this branch. Upstream JNI ownership,
source-directory changes and Snappy 1.3.1 are integrated through `12ffd16f`.
Prediction, privacy policy, candidate pinning, broad accessibility changes and
fork release configuration remain separate work.

Validation:

- The [exact-source control](https://github.com/X2M7/trime/actions/runs/39723643304)
  passed 330 application JVM tests and 115 separate real-JVM Unicode assertions.
- The [fork push](https://github.com/X2M7/trime/actions/runs/39723620640) built exact
  `9f86bc8f` on four OSes; the upstream PR run built its recorded synthetic merge
  ref on four OSes while binding that head. All eight jobs passed. Native caches
  hit, so this is not a fresh native compile/link claim.
- The original fork Ubuntu artifact's four ABI-specific APKs passed signature,
  ELF, BuildConfig and 20-pin checks. The three audited shared assets are
  `luna_pinyin_t9.schema.yaml`, `trime.yaml` and `tongwenfeng.trime.yaml`.
- [Android 15 current-head evidence](https://github.com/X2M7/trime/blob/3c7da4241e281a43bce6e4a5f923d7690d411dde/doc/contribution-split/checks/deferred-input-view-api35-20261022/provenance.json)
  qualifies seven listed two-theme category/switch/editor groups: literal paths
  retain `a!`/`a！`, pending `64` commits `你` when switching to English, and a fresh
  chat does not inherit the old lock. This head did not rerun all seventeen API35
  cases. Earlier `eeb4804a` fifteen-case coverage is separately attributed prior
  evidence, not current-head coverage; its failed cold tail remains failed.
- [Android 5.0 current-head evidence](https://github.com/X2M7/trime/blob/3c7da4241e281a43bce6e4a5f923d7690d411dde/doc/contribution-split/checks/deferred-input-view-api21-20261022/independent/qualification1.json)
  qualifies all seventeen original cases: the category macro and C1–C8 in both
  themes, with 104 captured frames and strict editor-text oracles. Three Observer
  sessions and the emulator stopped normally. This used the original cloud Debug
  APK in an isolated profile after reinstalling its different Debug certificate
  and preparing three explicit storage/theme preferences; it is not clean-default
  onboarding or an upgrade test. No compiled caches or native libraries were restored.

Existing native,
theme, dependency, tool and Debug signature warnings remain visible. This is not
full-application, Release, physical-device, TalkBack, general candidate-race or
performance acceptance. Source-bound results and original failures are retained
in the [fork-only contribution index](https://github.com/X2M7/trime/tree/contrib/split-index/doc/contribution-split).
