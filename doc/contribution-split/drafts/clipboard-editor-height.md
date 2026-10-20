# fix: constrain the clipboard editor above its action buttons

The clipboard editor uses a wrap-content height with existing top and bottom constraints. Without constrainedHeight, a long entry can make the editor exceed those bounds when the window has limited vertical space, covering its action row.

Add layout_constrainedHeight to the existing EditText so measurement respects the declared bounds. Keep its line limits, internal scrolling, margins and both action buttons. The patch is one XML attribute and introduces no activity or persistence changes.

**Validation:** XML parsing, source review, AAPT2 compilation and diff checks passed. Exact-SHA four-OS Debug/native builds and Ubuntu Spotless passed. A separate application JVM run executed all 301 baseline tests successfully, without skips.

**Remaining:** Exercise long entries, small windows and the visible software keyboard. The existing adjustPan policy remains. JVM tests do not measure layout geometry. Lint, Release, APK download auditing and device checks remain pending; inherited upstream diagnostics remain.

Source: `contrib/split-clipboard-editor-height` at `70af58cf2712de59e611b3ca4f2d272175f5707e`.
