Fixes #1443.

When a keyboard layout omits `width`, clickable keys get zero width even when `style.key_width` is configured. Use the style value as the fallback for the layout default in both layout passes. A positive layout width, explicit per-key widths and spacers keep their existing behavior.

Validation:

- Debug build, 331 application JVM tests and Spotless passed. Lint has no errors; its 65 existing warnings match the previous check on the same base.
- Android 15 emulator: seven parsed-YAML scenarios, 39 real `Key` objects. The original APK reproduces two failing scenarios with 21 zero-width keys; the fixed APK passes all geometry and unique-center `isInside` checks. Controls cover layout/per-key overrides, spacers, fractional widths, wrapping, column limits and edge rounding.
- [All five upstream CI checks passed](https://github.com/osfans/trime/actions/runs/40769057800). [Bounded A/B results and artifact identity](https://github.com/X2M7/trime/blob/43eb6f0eabc6e961047c906ff329a58b4c4e4482/doc/contribution-split/checks/issue-fixes-batch5-20261029/runtime/key-width-validation.json) are recorded separately from CI.

The Android checks exercise production geometry directly, without rendering a full IME or dispatching system touch. The CI APK payload is preserved in a diagnostic copy signed with a common test certificate. Existing tool/dependency and runtime warnings remain recorded; this is not a Release or physical-device acceptance claim.
