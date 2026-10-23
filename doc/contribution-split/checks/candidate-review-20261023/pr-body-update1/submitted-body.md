Candidate labels can disappear during accessibility-node inspection when custom drawing uses private text while the framework TextView still has an empty text/layout. Keep framework text as the single value and synchronize its Layout before the existing custom sizing/scaling. This PR changes one production file.

Review follow-up in `92b13711`:

- Set our dirty flags before the parent setter; restrict our explicit layout/redraw requests to changed content or null. Always forward the original nullable text and BufferType to `super.setText`: the parent constructor calls this override, and equal text can still need NORMAL/SPANNABLE/EDITABLE conversion.
- Remove the added `super.onLayout` call.
- Retain `super.onMeasure` with a comment explaining its framework Layout/accessibility role. In the separately compiled API21 source controls, removing it leaves the first 11 Layout checks failing while their reference passes; the later two selectable/granularity checks pass. Putting the entire setter inside the suggested equality guard instead aborts during initial `BoringLayout.isBoring` measurement, before buffer tests, with no exported result.

Validation of this head:

- [Exact-source control](https://github.com/X2M7/trime/actions/runs/39872481596): 301 existing application JVM tests actually executed, zero failures/errors/skips. [Fork CI](https://github.com/X2M7/trime/actions/runs/39872383227): 4/4 jobs; [upstream PR CI](https://github.com/osfans/trime/actions/runs/39872386788): 5/5. Four original Ubuntu ABI APKs are audited; upstream synthetic-merge artifacts are kept separate.
- The unchanged, un-re-signed original x86_64 APK class passes 13 API21 stages / 39 PNGs: construction/Hanzi/Latin/null/rebind, six equal-content buffer sequences, and selectable/public LINE NEXT/PREVIOUS actions (`0 → 2 → 0`). Text, Layout, nodes, immediate buffer types and glyphs are checked against the same-stage reference.
- API35 is **not qualified**: the safe diagnostic runs zero stages because its 8-second combined window-focus / shown-view / positive-width precondition fails; individual values were not recorded. SystemUI ANR precedes the probe and the restored screen shows its dialog, without proving a unique cause. Both negative controls and the original-APK phase remain unrun.

[Sources, controls, original reports/screenshots and scoped qualification](https://github.com/X2M7/trime/tree/9eb3810a0a578b786e4d9cb92835a2c186f9073c/doc/contribution-split/checks/candidate-review-20261023) are outside this one-file patch. The external Activity loads the original installed class; this is not native IME/candidate activation, TalkBack-service, physical-device, Release, upgrade or performance acceptance. Source controls use their own diagnostic toolchain. Observed native dependency, SDK/Gradle/JDK and Debug META-INF/services signature warnings remain; no zero-warning or all-native-objects-rebuilt claim.
