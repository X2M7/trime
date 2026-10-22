# API 35 / 9f86 T9 bounded business qualification

This HOST-only review supports the explicitly executed API 35 delta on source `9f86bc8f1d11d65d2e22e437f111a965bc2faba5`, APK SHA-256 `9cdb36ab38a5a4d76ab619d54b0b5b484505472cdd7253be3cf0a34502a4706b`.

Supported new-source scope:

- Trime: `64 -> ni -> cancel`, literal `2 -> default/64 -> cancel`, C7 ASCII/literal-symbol/default plus pending-composition switch, and C8 old/new editor boundary.
- Standard: category punctuation selection and number restore, C7 ASCII/fullwidth-symbol/default plus pending-composition switch, and C8 old/new editor boundary.

The strict final editor values are `""`, `"2"`, `"a!"`, `"你"`, `""` for the Trime checks and `"、"`, `"、2"`, `"a！"` (U+FF01), `"你"`, `""` for the Standard checks. The C8 old and fresh editors have distinct fixture PIDs and activity instances in both themes.

Both acquisition journals remain unchanged at `passed=false`, `checks=[]`; every capture also retains `image_reviewed=false` and `accepted_business_case=false`. This file and `qualification1.json` are separate HOST adjudication, not late completion of either raw journal.

Structural review covered 2,426 commands, 89 declared actions, 45 captures, and 42 before/after oracle pairs. All command stdout/stderr hashes match. Every declared action maps in order and parameters to 79 `input` commands plus 10 independent fixture launches; there is no unrecorded input command. No journaled `input text`/`keyevent`, package install/clear/uninstall, settings/content mutation, or target-private-file write was found. All 177 `/proc/2235/stat` checks retain starttime `35778`, and all 177 cmdline checks read `com.osfans.trime.debug`.

The two observer sessions stopped normally and independently: 25 captures plus stop sequence 26, then 20 captures plus stop sequence 21. Ready, finished, and actual-stop identities match within each session; both have zero capture failures. The first session's final Standard-theme oracle and the second session's first category oracle continue the same fixture PID/activity instance with sequences 1-2 then 3-4.

All 42 oracle pairs decode exactly from their raw base64 records. Within each screenshot sandwich, the pair retains the same text, selection, fixture PID, activity instance, focus and lifecycle state; only nonce, sequence, and query timing fields differ. Three dialog/menu captures are UI-only and are not described as oracle-backed. The unpaired `trime-T9-ready-empty` preflight has `has_window_focus=false`, is absent from `report.captures`, and is explicitly excluded in favor of the later paired focused retry.

Root's four visual-progress records attest 28 viewed original PNGs and 46 decoded oracle files, with no referenced hash mismatch. The business subset is 23 selected PNGs and 42 oracle files; five PNGs and four oracle files belong to the separate G1 cold-start scope. The two contact sheets here are derived review aids and do not replace the original PNG evidence.

The parent controller ended normally with emulator exit 0, collector disconnect exit 255, no cleanup errors, and 981 memory samples with a 2,268,976 KiB minimum. The final 110,463,480-byte log hashes to `f1b6bde1321f878c8d3ea0a968ddb797b80bfe3d0fb7e8c60c06ecd1da727198`. It preserves full-boot SystemUI and Launcher ANRs, a kernel IRQ 35 fatal record, and target warnings/errors. The business window has no target fatal exception, ANR, or process death, but it includes 97 target error-level lines; this is not a clean-log claim.

Old API 35 evidence remains separately attributed to source `eeb4804a3dcfbb48cdb5b4c9638aad550a0283e0`, APK `6ebfb90c9aa4d6723f23eff46ac8604538e67779816775721b27a5d1f7700181`, boot `46a3645f-f8cf-40f8-85ec-fb37e603ea7d`, and PID/starttime `13449/399690`. It supports Standard C1-C8 plus category and Trime C1-C6 only. Trime C7/C8 were unstarted there. Those 15 results cannot be relabelled as 9f86 execution or combined into a homogeneous 17-case new-head rerun.

The authoritative machine-readable review is `qualification1.json`. CI, G1 cold-start adjudication, API 21, overall PR readiness, and release policy are outside this report.
