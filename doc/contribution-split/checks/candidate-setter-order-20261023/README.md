# PR #2187: call the framework setter first

Review [4354993843](https://github.com/osfans/trime/pull/2187#discussion_r4354993843) is addressed by `7f909060a1d42198e749eb1f9a66399f76bf8710`. The patch still changes only `AutoScaleTextView.kt` (8 additions / 12 deletions against its original upstream ancestor). The [actual source push receipt](source-push/receipt.json) confirms the original contribution branch advanced normally.

The first executable statement is now `super.setText(charSequence, bufferType)`. Both custom caches are then marked dirty, followed by layout/redraw requests. All five operations are unconditional. No old-text read, equality guard or nullable-to-empty normalization remains in the override. Constructor-time calls touch no uninitialized Paint/Rect caches, and the parent owns null handling and BufferType conversion. `super.onMeasure` stays; the added `super.onLayout` remains removed.

## Investigation

The [independent source review](independent-source/source-review-7f909060.json) traces API21/API35 TextView, View and ViewRootImpl and the current AutoScale consumers. Ordinary relayout/invalidation requests a later traversal. No project text callback that synchronously measures/draws these labels was found, so no current-path need for dirty-before-super was identified. [Fixed official URLs and SHA-256 values](independent-source/official-source-manifest.json), exact source and the consumer search are retained; the full downloaded AOSP files remain local.

This is not a claim that the previous ordering caused an observed bug, or that arbitrary future reentrant client callbacks are safe. Removing the equality optimization may add work on repeated equal assignments; no performance-neutrality or speedup claim is made. Comparing `text` after the parent would already observe its new value and would not reproduce an old-content comparison.

## Current validation and producer identity

[HOST compilation](host-compile1/result.json) uses Kotlin 2.3.21, JVM11 and `-Werror`: actual exit 0, empty stderr. The real compiled bytecode places the parent setter before custom field writes, with no content-comparison branch. This is class compilation, not an APK or device result.

The new fork push workflow did not produce a run. An [explicit dispatch](unit-control-dispatch/receipt.json) returned HTTP422 with `Actions has been disabled for this repository`, while [the read-only permissions API](actions-permissions.json) reported enabled=true. Both observations are retained without asserting a unique configuration cause or changing repository settings. The fork control matrix was pushed with the new source pin, but no new fork JVM execution is claimed. Previous-head 301-test results are historical.

[Upstream check-job qualification](ci-unit/upstream-merge-check/independent-upstream-check-audit1.json) confirms actual successful `:app:check`, `:app:testDebugUnitTest` and Lint tasks. That workflow exports no JUnit XML, so no exact new testcase count is claimed. [Current upstream run39889136561](https://github.com/osfans/trime/actions/runs/39889136561) completes all **5/5 jobs** successfully; [terminal qualification](ci-formal/upstream-ci-qualification1.json) and all five original job logs are retained.

The [original upstream Ubuntu APK audit](ci-apk/audit/result.json) uses producer `osfans/trime`, run `39889136561`, actual synthetic merge **`9ff9572f2ed289d8af198f81ce08bdac66d8a69c` / tree `10f5abd560dc4d365553d8edadc62f47986cb7da`**. The reviewed fork is **`7f909060` / tree `2da497746bf3997515b816629ae92e6d2ffef0fb`**. The whole trees differ in 33 paths; `whole_tree_equivalent` is explicitly false. AutoScale source SHA `c92ded471c62aa2a773510784237d60ac822d831bda9298929a6f99b5d3db8c1` is identical in both. The Kotlin source hash is observed from the Git source, not reverse-inferred from DEX.

All four original ABI APKs pass actual package/BuildConfig/signature/native ELF checks, and the x86_64 class passes the eight focused DEX checks, including parent-before-flags. Selected original APK SHA-256: `1c674f4365ccdf77b52570fa55a16c3c8010e7ed2c737aabdcab2dc5b896b1c9`; actual CI certificate: `2c6035f9931af77a6016a308c5840cf9c3fb9cb153b60fdd053fe567a342e5b6`. It is not modified or re-signed. Actual BuildConfig remains `9ff9572f`, never mislabeled as the fork head.

## API21 scoped runtime

The actual upstream merge original x86_64 APK passes **13 stages / 39 original PNGs**. [Independent phase](independent-runtime/api21-production-phase1.json) and [complete terminal qualification](independent-runtime/api21-production-terminal1.json) verify original report/instrumentation equality, file hashes, pixels, buffer states, LINE actions, strict log bounds/process snapshots and exact restoration. Root actually viewed seven hardware originals ([image review](root-image-review.json)); automated checks cover all39, not a claim of all images manually reviewed.

The single VM, child, wrapper, controller and collector actually exit 0; cleanup is empty. Original IME/display/font and DocumentsUI states are restored, and all10 original SDK files remain byte-identical. Minimum available memory is 6,693,528KiB. The complete original stream retains W1553/E408/F0; no global warning-free claim follows.

The original 13-stage probe is unchanged: construction / Hanzi / Latin / null / rebind, six immediate equal-content buffer transition/repetition sequences, selectable precondition and public LINE NEXT/PREVIOUS selection `0 → 2 → 0`. Text/Layout/node values, hardware/software glyph pixels and all 39 PNG hashes must match their stage oracles. The external Activity loads the actual installed production class; this is not native IME/candidate activation, a TalkBack service, physical-device, Release, upgrade, full-fork/full-merge or performance acceptance. API35 is not rerun for this follow-up; the previous head's system readiness failure stays historical.

The private API21 AVD uses the same strict unique BEGIN/END/process checks and exact IME/DocumentsUI restoration. The legacy runtime `source_head` field denotes the reviewed PR head only; the actual APK build identity is separate in the frozen binding/audit. Hardcoded probe booleans are not process evidence, and unavailable post-probe PID start times are not invented.

## Evidence integrity and warnings

Current artifacts and all original warnings are retained separately from the one-file PR. The upstream check log contains SDK warnings and unexpanded Gradle deprecation summaries; each APK has two META-INF/services signature warnings. This is not zero-warning or all-native-rebuilt acceptance. Actual runtime diagnostics and tool exit values remain visible.

The local `.gitattributes` preserves original line endings and terminal spaces. Publication omits APKs, private backups, binary command streams and signing material. The manifest binds the published original bytes and lossless gzip records. No review comment is posted, thread resolved, review dismissed, PR status changed, merge made or release published by this follow-up.

## PR metadata publication

The [body-only update receipt](pr-body-update1/receipt.json) confirms exact readback of the revised investigation and validation scope. The body links immutable evidence commit `748af2a46a23e01559e9700d5d8c1f8961eecbc0`; source head, title, base, Open/non-Draft state remain unchanged. The [normal evidence push receipt](index-evidence-push1/receipt.json) verifies that commit on the fork. No review comment, resolve or re-request was sent.
