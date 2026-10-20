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
  [doc/t9-input.md](https://github.com/X2M7/trime/blob/649c07559cfbc1e494835465e71277fe8dc8051c/doc/t9-input.md).

The feature includes its native/JVM tests and usage notes. Prediction models,
privacy policy, candidate pinning, broad accessibility/viewport changes and fork
release configuration are outside this patch.

#### Validation

- [x] [Exact-source complete application JVM task](https://github.com/X2M7/trime/actions/runs/39467124333):
  328 tests in 25 suites, zero failures/errors/skips. Official artifacts, source
  identity and all 20 recursive pins independently checked. The existing 301 cases
  remain after one explicit theme-test title rename; 27 new cases match the source.
- [x] [Pinned-engine native host checks](https://github.com/X2M7/trime/blob/a0f95841/doc/contribution-split/checks/t9-input-native-host.json):
  freshly build unmodified librime; four native processes execute 743 assertion
  checks, including two process-restart learning checks. These are separate from
  the JVM test count. New T9 sources also compile with strict warnings as errors.
- [x] SpotlessCheck, clang-format 22 checks and Conventional Commits.
- [ ] `make debug`: [four-platform source CI](https://github.com/X2M7/trime/actions/runs/39467034017)
  is still running.
- [ ] Android manual input/editor/layout acceptance, Lint and Release.

No new application source diagnostics were found in the exact-source JVM build.
Existing build-tool warnings and six baseline JNI signedness/copy-elision
diagnostics remain. Host UTF-16 offset checks do not validate Android string
transport; the existing Modified UTF-8 bridge remains a limitation. Production
ranking, touch/TalkBack behavior and device performance are unverified.

Keep this PR Draft while build and Android acceptance remain incomplete.
Source: `649c07559cfbc1e494835465e71277fe8dc8051c` (two commits, 52 files), based on
`osfans:develop` at `144f6111d0498da85e0ffa250245f18955179ed3`.
