# T04 evidence, 2026-09-08

- `comparison.md/json`: four completed profiles; metrics recalculated from candidate/selection traces.
- `t9.6-20260908-evidence.tar.gz`: raw phase JSON/logs, before/after user-dictionary TSVs,
  corpus, run identities/checksums, five deployed schemas, the original probe source,
  dictionary provenance/attribution/licenses and APK resource audit.
- Archive SHA-256: `b462287889c46711bd403d2380016e05392789a0314cf8fe0b75d54a3d23699f`.
- All 36 successful phase payloads and five deployed schema hashes were checked against
  their run reports. The archive also retains compact's failed training and diagnostic export.

`e40563859022` is the complete luna/common/extended/balanced matrix.
`0f4032b73320` is the compact experiment: cold measurements completed, training failed.
Its `postmortem-export.tsv` is diagnostic evidence, NOT a successful learned cohort.

This is a compact evidence archive, not a complete replay environment: it excludes APK/native
binaries, full shared dictionaries and compiled/user databases. Re-running `summarize.py` with
its full input-tree validation requires the original local run directory, not just this archive.
The dictionaries can be regenerated from the pinned sources; the APK is identified in each report.

The first matrix used packs-v2 and compact used packs-v4. The included packs-v5 provenance
adds OpenCC license/tool metadata and enforced source checks; every measured YAML file was
verified byte-identical to packs-v5. Current probe source fixes a redundant cost field and adds
a clearer training error; historical metrics use hashed step traces, not that old cost field.

See [the validation report](../../VALIDATION-T04.md) for conclusions and limitations.
