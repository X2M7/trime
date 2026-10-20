# fix: keep Return labels consistent with editor actions

An editor can provide a labeled custom action ID while `imeOptions` names Search. Return already sends the custom ID, but the key can still display the Search label. App-label-only mode can also display the literal `null`. Share the existing dispatch decision with the display delegate, following the [EditorInfo action contract](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#actionId).

Keep raw Return for TYPE_NULL or NO_ENTER_ACTION and retain custom-ID precedence and the theme's four label modes. App-preferred mode now falls back to the theme label for the resolved standard action when the app label is absent; app-only mode falls back to the theme default. Handle label-change broadcasts by invalidating keys in every cached keyboard, so a same-layout editor restart has a repaint path.

Validation:

- Twelve focused policy tests passed with strict helper/test Kotlin compilation; focused formatting passed for all six changed files.
- The exact-source [complete application JVM run](https://github.com/X2M7/trime/actions/runs/39448942546) passed all 317 cases: the 301 baseline, 12 policy cases and 4 real delegate/broadcast cases. No failures/errors/skips or new source compiler diagnostics were found.
- [Four-platform Debug/native builds](https://github.com/X2M7/trime/actions/runs/39448874294) are in progress; the Ubuntu full-project style check passed.

This remains Draft for actual Android EditorInfo/InputConnection dispatch and on-screen repaint after restartInput or switching cached layouts. The four delegate tests use the real Theme, Kodein, display delegate and broadcaster, but do not construct Android views or exercise the service. Lint and Release checks remain unverified; existing dependency/toolchain warnings are retained.

[Source and validation scope](https://github.com/X2M7/trime/blob/e4e32680d3c7a4417bd509168f2f706d89c98c9d/doc/contribution-split/checks/enter-actions-submission-source.json).

Source: `contrib/pr-enter-actions` at `5879c1eb5a80011a1b1a9eaf3db16750cf01205d`.

Submitted as [#2180 Draft](https://github.com/osfans/trime/pull/2180).
