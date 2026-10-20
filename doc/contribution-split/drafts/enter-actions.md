# fix: show the Return label for the action sent to the editor

An editor can supply a labeled custom actionId while imeOptions names Search. Return currently dispatches that custom ID but can still display the Search label. App-label-only mode can also display the literal null when no label is supplied.

Use the same action resolution policy in the display delegate and the service. Preserve raw Return for TYPE_NULL or NO_ENTER_ACTION, custom-ID precedence, and the theme's four label preferences. App-preferred mode falls back to the matching theme action label; app-only mode falls back to the default instead of null. Multiline editors can still request an explicit action.

**Validation:** Twelve helper tests passed with strict Kotlin compilation. A separate exact-SHA Android application JVM run compiled the actual service/delegate and passed all 313 tests, preserving the 301 baseline cases, without skips. The original exact-SHA four-OS Debug/native CI and Ubuntu Spotless passed.

**Remaining:** Verify actual EditorInfo values, theme rendering and performEditorAction on Android. Application JVM tests cover the shared rule, not real editor dispatch or rendered labels. Lint, Release and device checks remain pending; inherited compiler/tooling diagnostics remain.

Source: `contrib/split-enter-actions` at `bcb47b1a81eb211e7c0ede447c1b3263dcf80746`.
