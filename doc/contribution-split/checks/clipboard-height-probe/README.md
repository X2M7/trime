# Clipboard height: focused Android measurement

This fork-only probe is separate from the one-attribute upstream patch.
`ProbeActivity.java` is the actual executed source. Its generated `R` belongs to
`com.osfans.trime.clipheightprobe`; the two layout files are byte-identical to the
production layouts at `144f6111` and `8344231d`, apart from their resource filenames.
Use the normal `clip_edit_text_hint` string and Android's button strings when
including these layouts in an Android fixture.

It ran on API 21 with ConstraintLayout 2.2.1, constraintlayout-core 1.1.1 and
AndroidX Core 1.3.2. This is not the complete application's dependency graph.
The original resource/input hashes and actual APK/result binding are in
`inputs.json` and `review.json`. D8 emitted six missing unused dependency class
warnings; those warnings are preserved in the local original build records.
Do not describe this fixture build as warning-free.

For each layout the activity runs `LayoutInflater.inflate`, then two actual
Android `measure`/`layout` passes: width EXACTLY 360 dp, height AT_MOST
160/240/600 dp; fontScale 1/2; short and 40-line synthetic content. It does not
attach the layouts to a window. Check that the editor starts at or below zero,
ends above both buttons, both buttons remain inside the parent, and all text
is unchanged. The original CRLF output is `result-api21.json` (stored without Git text normalization); `review.json`
independently recomputes the geometric checks from the recorded coordinates.

Twelve pairs executed: seven baseline geometry failures; all twelve fixed
cases pass. At 160 dp with normal-font long text the original editor spans
y=-28..131 and the buttons begin at y=102; fixed spans y=0..102 with identical
button rectangles. The four roomy 600 dp controls are unchanged.

This does not test real Activity-window sizing, IME/insets, scrolling, touch,
save/cancel persistence, Lint or Release acceptance.
