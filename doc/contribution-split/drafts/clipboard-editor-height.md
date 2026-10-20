## Pull request

The clipboard editor uses `wrap_content` between the parent top and Cancel. When the parent has limited height, its preferred height can cross the action row. Enable `layout_constrainedHeight` so the existing vertical constraints limit the editor. This is a one-attribute change; window behavior remains unchanged.

Related: #2170.

### Reproduction and validation

A focused API 21 [Android probe and raw results](https://github.com/X2M7/trime/blob/239f5d3103b6c4fd12dbc94c4c69f7d72a399d0b/doc/contribution-split/checks/clipboard-height-probe/README.md) inflated the exact baseline/fixed XML with ConstraintLayout 2.2.1/core 1.1.1. At width 360 dp and height AT_MOST 160 dp, long text put the original editor at y=-28..131 while both buttons began at y=102. With the patch it measured y=0..102; button rectangles were unchanged.

- [x] Twelve pairs executed: heights 160/240/600 dp, font scales 1/2, short/long text. Seven baseline geometry checks failed; all fixed cases passed. Text was preserved; four roomy controls were identical.
- [x] All 301 application JVM cases passed at `8344231d`, without failures/errors/skips ([first-attempt results](https://github.com/X2M7/trime/actions/runs/39417689930/attempts/1)).
- [x] New-head [four-platform Debug/native builds](https://github.com/X2M7/trime/actions/runs/39417654398) and Ubuntu style checks passed.
- [ ] Real Activity-window, IME/insets, scrolling and save/cancel touch acceptance.

This is a Draft. The probe measures unattached layouts and uses AndroidX Core 1.3.2 rather than the complete application dependency graph. It does not verify the actual window or change `adjustPan`. Existing build/tool warnings remain; Lint/Release acceptance is not claimed.

### Contribution checks

- [x] Read CONTRIBUTING; one fix and a Conventional Commit with an explanatory body.
- [ ] Manually test: Done.
