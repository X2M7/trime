# Popup grid evidence selection

Only selected text records and the external helper source are copied here. Original APKs, artifact ZIPs, device PNGs, emulator disks and complete logs remain in the referenced local evidence directory; this selection does not include them. Their hashes and original locations are preserved in the reports.

The baseline driver executed with SHA-256 0494270478fdcb294bad6ecd72e031778ad88f40e733f92f88935f6789135da7. Before fixed execution, image retrieval was moved ahead of behavioral gates to preserve originals on unexpected failure; the fixed driver SHA-256 is e6295bb93f435c3df9cf1f12ff5702be938f7961a46754526b1f835024d8789f. Both versions are retained. No scenario oracle was relaxed.

The first cleanup/finalizer review found a process-scan prefix defect and missing prerequisite assertions in the external reporting script. Those findings and the separate correction review remain. They were corrected before final execution; no production change or failed device record was rewritten.

The local helper-only build runner copied historical main-APK/JUnit outputs. helper-build-qualification.json explicitly excludes those copies from new application-build/test claims. The fixed runtime candidate instead comes from the original final CI artifact with a complete producer/head tree proof.

See runtime/grid-validation.json for scope limits. There is no full IME long-press, physical device, Release, update-install or zero-warning acceptance claim.
