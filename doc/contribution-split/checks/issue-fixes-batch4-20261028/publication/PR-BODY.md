Fixes #2033.

Popup action labels are resolved correctly, then reduced to their first Unicode code point. Thus `popup: [TrimeWiki, Settings]` displays `W` and `设` instead of the declared `Wiki` and `设置`.

Keep the complete resolved label and let the existing proportional `AutoScaleTextView` fit it to the key. This one-file change also preserves complete combining and emoji sequences; the original action tokens, ASCII shortcut and icon path are unchanged.

Validation for `492d8d60` on upstream `7fedca06`:

- [CI](https://github.com/osfans/trime/actions/runs/40704522250): all five checks passed. Ubuntu compiled four native ABIs; the other three builds used JNI caches. The original tested APK's producer source tree matches the PR head.
- Local APK, 331 application JVM tests and formatting passed. Lint completed with 0 errors and 65 warnings in unchanged upstream files; existing native/tool warnings remain.
- Android 15 production-popup A/B with dedicated real Rime sessions: 11 observations across default, narrow, large-font and explicit control scenarios. The original APK had 9 expected label failures; the fixed APK passed all observations. Public focus/trigger calls retained every action token, and all eight original rendered PNGs were inspected.

[Evidence and limits](https://github.com/X2M7/trime/blob/63443d39141efabee6bc8bdb951def9c0ea4fb58/doc/contribution-split/checks/issue-fixes-batch4-20261028/runtime/popup-validation.json) include the first helper initialization-race failure, its correction, APK provenance, independent review, existing theme diagnostics and system boot ANRs. These are unattached View rendering tests, not real long-press, full IME, Chinese input, TalkBack or physical-device acceptance. Test-certificate APK copies preserve every non-signature payload entry; no Release or zero-warning claim is made.
