# T05 evidence

## Runtime Audit Follow-Up

The latest local audit is [RUNTIME-AUDIT.md](../../RUNTIME-AUDIT.md), with
[machine-readable results](t9.7-dev.2-runtime-summary.json). It distinguishes
builds 12-16 and preserves failed runs. The menu-resource error is repaired and
verified, but ARM64 native-bridge startup queuing and slow frames remain open;
this is not an all-warnings-cleared result or a published release.

The full local bundle and its checksum/verification sidecars are under
`build/t05-runtime/evidence/`. The historical archives below are unchanged.

Erratum (2026-09-08): the original T05 probe used `tongwenfeng` instead of
`tongwenfeng.trime`, silently falling back to the default theme. The archives
below do not establish T05 coverage of the second theme. See the correction in
[VALIDATION-T05-RISK.md](../../VALIDATION-T05-RISK.md). Historical archives and
summaries are retained verbatim, not relabelled as successful new runs.

The development APK is not a published release. Build identities, interpretation,
failures and remaining limitations are documented in [VALIDATION-T05.md](../../VALIDATION-T05.md).

Archive: `t9.7-dev-20260908-evidence.tar.gz` (about 3.1 MiB).
SHA-256: `8d4204e5f9cf51c16892f6230573bf13fd7048926cf225e34a55d3cdee69329f`.
Its `manifest.json` maps all 109 artifact hashes to the three actual APK identities;
every included artifact checksum has been verified.
The final summary is also available as [summary.json](summary.json).

The evidence archive retains the fixed corpus output, before/after summaries,
native driver assertions and identities, UI instrumentation reports/screenshots,
deployed schema and source snapshots. It excludes APKs, native libraries, personal
user dictionaries and unrelated applications' logcat output. Large local logs
remain under `build/t05/`.

Reproduce each metrics summary with:

```sh
python3 -B script/quality/t05/summarize.py PATH/metrics.json --output NEW-summary.json
```

All recovery figures mean explicitly selecting the suggested canonical syllable
and then finding its representative Hanzi. They are not automatic Hanzi Top-1
accuracy or a representative estimate for everyday typing.

## Risk Follow-Up

The original archive and summary above remain unchanged. The follow-up development
build is `3.3.13-t9.7-dev.1` / `20261110`, APK SHA-256
`0a6c4a24e6426e92d25647ab333b51d72387bd459a89d7dcea6a390a0e934c70`.
See [risk report](../../VALIDATION-T05-RISK.md) and
[risk summary](t9.7-dev.1-risk-summary.json).

Archive: `t9.7-dev.1-risk-20260908-evidence.tar.gz` (804570 bytes).
SHA-256: `e0a0a8c5567726f1bd589c12a147e6d3957b398ba1b00d20432b831109dc6765`.
All 119 manifest entries were verified. Intermediate failed runs and the original
T05 index benchmark library have separate build identities. APKs, native libraries,
personal databases and system-wide logcat are excluded. Runtime startup reused
matching compiled system resources, so it is not a fresh deployment timing claim.
