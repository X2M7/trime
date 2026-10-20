## Pull request

The setup pager fills its parent while Next, Previous and Skip are placed over the same bottom area. Constrain the pager above a TOP Barrier referencing the visible buttons, so its height follows their visibility. This changes one layout; setup flow and button actions stay the same.

Related: #2170.

### Validation

- [x] The pinned submission `560b5b99` passed all 301 application JVM cases, with no failures/errors/skips ([run](https://github.com/X2M7/trime/actions/runs/39417689930)). Its first attempt stopped before tests because downloading AAPT2 from Google Maven timed out; only that failed job was retried, with unchanged source.
- [x] The exact layout compiled with AAPT2; the patch matches the original split patch.
- [x] A limited API 21 [original-split APK comparison](https://github.com/X2M7/trime/blob/239f5d3103b6c4fd12dbc94c4c69f7d72a399d0b/doc/contribution-split/checks/setup-layout-diagnostic/README.md) checked the unchanged setup sources. On the 480x540 logical display the pager bottom moved from y=492 to the visible footer top y=444. The fixed original APK also reached Enable/Select with Next hidden on Select.
- [ ] New-head [four-platform Debug/native builds](https://github.com/X2M7/trime/actions/runs/39417654338) are still running.
- [ ] Complete layout/touch and large-font acceptance on the new APK.

This is a Draft. The existing fragment is not scrollable; a short pager can still be exceeded by its content. The diagnostic does not establish an overlapping button hit rectangle or glyph clipping, and is not new-head whole-APK acceptance. Lint/Release acceptance and zero warnings are not claimed.

### Contribution checks

- [x] Read CONTRIBUTING; one fix and a Conventional Commit with an explanatory body.
- [ ] Manually test: Done.
