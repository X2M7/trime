Fixes #2159.

Some standard-theme palettes do not define the per-key background aliases used by the layouts. Drawable parsing currently turns an unresolved name into a non-null transparent background, preventing `Key` from using its existing fallback. In Google+ and 丹青, the exposed keyboard background matches the text color and makes key labels disappear.

Return no drawable for an invalid value, while keeping explicitly transparent colors valid. Share value classification with the color table; valid custom colors, aliases, fallback chains and images keep their behavior. The patch changes two production files and adds focused regression tests.

Validation on `f27e8425`, rebased onto upstream `7fedca06`:

- [CI](https://github.com/osfans/trime/actions/runs/40698285331): lint/test and all four platform builds passed. Original APK producer source tree equals the PR head; native cache reuse is recorded separately.
- Local application JVM tests: 338 passed, including 7 new regressions using the shipped palettes. These ran before rebase; all three patch files stayed byte-identical, and CI retested the rebased head. Formatting passed; Lint completed with 0 errors and 65 warnings in files unchanged from the upstream base. Existing tool/dependency warnings remain.
- Android 15 emulator A/B using real production theme parsing and public `Keyboard`/`Key` color APIs: 46 palettes, 282 state observations and 16 drawable controls. The old APK reproduced the invalid-background failures; the fixed APK passed every observation and control, including explicit transparency, named/custom colors and images. Existing text colors were unchanged.

[Detailed evidence and limits](https://github.com/X2M7/trime/blob/bdd7bdb3612a0e83a7c9bca98f72fb7033d90338/doc/contribution-split/README.zh-CN.md#第三批标准主题背景回退2026-10-28验收完成) include APK provenance, captured baseline failures, independent review and cleanup. The Android test draws synthetic Canvas swatches; it does not establish full `KeyView`, TalkBack, physical-device or Release acceptance. Diagnostic APK copies share a test certificate and preserve every non-signature payload entry. Runtime and build warnings are retained; this is not a zero-warning claim.
