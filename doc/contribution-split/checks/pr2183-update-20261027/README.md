# PR #2183 current validation evidence

Source `8a1ee0ea` is rebased on `a047f747`. Original Debug APK producer `0ef0eb55` has the same complete Git tree; commits, build metadata and APK bytes are not interchangeable.

See [current qualified scope](root-current-qualified-scope.json). Upstream CI completed 5/5 jobs. Fresh host checks passed 743 T9, 115 CheckJNI Unicode and 46 UTF-16 assertions with recorded engine reuse. Android 5.0 and Android 15 r2 completed the targeted 12 sessions/14 logical checks each. Earlier API35 r1 failed before business with real system boot ANRs and guest LMK; its records remain included. Runtime, tool/dependency and Debug-signature warnings remain documented. This is not zero-warning, Release, performance, physical-device or whole-app acceptance. All 44 physical-device fields remain NOT_RUN, and PR #2183 stays Draft.

`SELECTED-MANIFEST.json` maps exact original text files and hashes. `SHA256SUMS` covers all files here except itself. Execution journals retain their original false/PENDING flags; separate record, actual-image and terminal qualifications describe the observed scope without rewriting those journals. Historical reports retain their original dates and scope.

This is a selected evidence subset. APKs, binaries, disks, large logs and PNGs remain local with exact references and hashes; see `LOCAL-ONLY-DEPENDENCIES.json`. Original absolute paths are preserved. The phone delivery README and checksums refer to the complete original delivery folder, not this subset. The Actions artifact may expire or require access. No source branch, review state or logging configuration is changed by this evidence index.

The local `.gitattributes` preserves exact evidence bytes. Only listed immutable originals retain captured trailing whitespace, CRLF or blank EOF lines; their content and text diffs remain visible. This does not disable application, build or runtime warning checks.
