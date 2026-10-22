# PR #2183 rebased cloud validation

This directory qualifies source head `1754bb51931407ac916549edc2ae57bba4c073bc`, tree `3c3ecc4764bef17b20bceece6d6c83e4475ec601`, rebased on `19b048494ffa55cae52d5385dfa6d0611461c27d`.

The exact-source control run `39787582756` passed 360 dynamically counted JVM tests in 29 suites with zero failures, errors, or skips. Its fixed JNI host-VM control passed checks 1–115 (`SUMMARY checks=115 destroy=0 exit=0`), while the expected baseline failure remained `SUMMARY checks=17 destroy=0 exit=1`.

Both formal workflows completed successfully:

- Fork Commit run `39789373604`: 5/5 jobs successful at the exact source head.
- Upstream PR run `39789381040`: 5/5 jobs successful after every job checked out synthetic merge `fa7b6215e1b54690fd3151685189239c10cc398f`. That merge has parents `19b04849…` and `1754bb51…`, and its tree equals the source tree.

Every formal platform build log records `armeabi-v7a,arm64-v8a,x86,x86_64`. The PR remained Open Draft; the upstream run actually executed without a pending approval gate. The source-head identity and synthetic-merge identity describe one PR execution context and are not additive test cases.

Temporary run `39787303403` ended `completed/cancelled`. Its check and Ubuntu jobs succeeded; macOS 26, macOS Intel, and Windows have authoritative `cancelled` conclusions. The macOS 26 build and upload steps succeeded and produced three artifacts before cancellation, but that bounded fact is not an all-OS pass. The retained Ubuntu artifact is still exactly bound to the same source head/tree and supplies the four inspected debug APKs.

The warning audit preserves **399 warning-emitting lines across 9 families**:

| Family | Occurrences |
| --- | ---: |
| yaml-cpp C++14 `constexpr` | 168 |
| Java/JNA restricted native access | 72 (18 four-line blocks) |
| Gradle `Configuration.setVisible(boolean)` deprecation | 55 |
| `sdkmanager` CLI deprecation | 45 |
| Gradle deprecated-feature summary | 28 |
| LevelDB CMake minimum deprecation | 14 |
| APK v1/JAR META-INF service-entry warning | 8 |
| librime-lua 32-bit constant conversion | 7 |
| Windows setup-java Gradle-cache save failure | 2 |

The two Windows cache warnings are one per formal run. Git tar could not read busy Gradle lock files in the post-job cache-save step, after the build and artifact uploads had succeeded. They remain CI/action issues and are not erased by the successful job conclusions.

Each of the four downloaded debug APKs verifies with v1 and v2, shares debug certificate SHA-256 `0d50a6c4b5af14741b9307952de134bff81ebf38c81d8030d1528200873f5c38`, and emits two warnings for META-INF service entries excluded from the v1/JAR entry-signature path. The warnings must not be restated as absence of v2 whole-APK integrity.

The warning count deliberately includes repeated emissions across platforms, source-head and PR-merge runs, ABIs, and three partial cancelled-job logs. It counts provenance lines rather than unique defects. Successful CI does not declare any warning harmless.

Primary reports:

- `final-cloud-qualification.json`: bounded overall conclusion.
- `formal-cloud-audit.json`: formal fork/upstream identities, jobs, artifacts, logs, and exact-source control.
- `temp-run-final-audit.json`: final cancellation state and retained Ubuntu binding.
- `warning-records.jsonl`: complete selected warning-line records.
- `warning-summary.json` and `warning-adjudication.json`: counts, emitters, attribution confidence, effects, and follow-up.

This is debug cloud-build evidence. It is not Release acceptance or physical-device acceptance, does not mark Draft PR #2183 Ready, and does not authorize merge. No source, workflow, PR state, Git ref, device, or Gradle execution was changed by this audit.
