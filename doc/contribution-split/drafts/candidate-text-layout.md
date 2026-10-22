Candidate text can disappear during accessibility-node inspection even though Rime has supplied candidates and Space still commits the expected Hanzi. In the current API21 T9 observation, the spelling controls remain visible but the highlighted and most Hanzi labels are missing. `AutoScaleTextView` keeps private text while the framework TextView text/layout is not synchronized.

Use the framework text as the single value, call its `setText`, and maintain its measure/layout state before applying the existing custom sizing and scaling. Constructor-time calls only update the text and dirty flags; they do not touch uninitialized paint caches. This patch changes one production file, with no JNI, theme, schema, dependency or fork release changes.

Validation for `7a45cf29`:

- The API21 original-APK control displays `你` / `ni` before node creation, then `createAccessibilityNodeInfo()` changes its text to empty. The five-stage normal TextView reference passes. This observation is scoped to node creation, not every ordinary keyboard display.
- The exact-source cloud control actually executed all 301 existing application JVM tests, with zero failures, errors or skips. Those tests do not exercise Android TextView rendering.
- The original `7a45cf29` APK passes the same API21 five-stage production-class regression: construction, `你`, `ni`, null and rebinding. Framework layout, text and node text remain synchronized; original screen glyphs are present. The separate test Activity loads the installed production class without starting Rime or changing the APK signature. This does not substitute for native candidate integration.
- [Upstream Debug/native CI](https://github.com/osfans/trime/actions/runs/39706771464) and [fork CI](https://github.com/X2M7/trime/actions/runs/39705659962) each passed all four OS jobs. The original fork Ubuntu x86_64 APK was audited for exact source, ABI and signature; all four ABI native build tasks ran with a cache miss. Quiet build output does not provide per-object compile/link records.

[The production-class probe sources, original reports and key PNGs](https://github.com/X2M7/trime/tree/83f4dc21/doc/contribution-split/checks/candidate-layout-api21-20261022) are available separately from this one-file patch.

Draft pending API35 display checks. Historical full-fork screen checks support this correction, but are not presented as current-branch acceptance. Existing dependency/toolchain and Debug META-INF/services signature warnings remain. No TalkBack, physical-device, Release or Lint acceptance is claimed here.
