# Non-production source comparison

This APK contains three independently namespaced copies of `AutoScaleTextView`.
It does not load, replace, or prove a production APK, and it never starts Trime or
Rime. The original app source is retained byte for byte with a SHA-256 binding.

- `safe`: only the Kotlin package differs from the frozen new app source.
- `equal_return`: the exact setter from review comment `4350112633`, with all
  other app lines unchanged apart from the package.
- `no_super_measure`: exactly one `super.onMeasure(...)` line is removed from
  `safe`; other app lines are unchanged apart from the package.

The thirteen-stage probe uses the original render, equal-content buffer, and
public framework line movement oracles. The diagnostic wrapper changes only its
package, source class loader, variant selection, provenance, and output scope.
It does not weaken assertions for either negative control. A control may fail;
preserve its real return code, JSON, images, and exception. A failure is not an
app acceptance result.

`build_diagnostic.py` uses Kotlin/compiler and stdlib 2.3.21, compile-only
core-ktx 1.16.0, JDK 17, SDK build tools 36.0.0, and D8 9.4.24 embedded in an
independently selected host AGP 9.4.1 builder dependency. The PR itself declares
AGP 9.3.0; this diagnostic does not use the same AGP toolchain as the PR.
The host D8 selection only addresses the observed Kotlin metadata parsing issue
and provides one common toolchain for all three source variants. It is not a
production build or production acceptance result. The SDK 36 built-in D8 8.10.9 was rejected
after it reported Kotlin metadata warnings; that original attempt is retained.
Kotlin and D8 heap limits are
384 MiB; javac/signing limits are 128 MiB. It runs no Gradle, ADB, or emulator.
The existing diagnostic signing key is used, never the production signing key.

Run each variant separately and serially on the controlled emulator:

```sh
am instrument -w -r -e variant safe org.x2m7.trime.readiness.candidatediagnostic/.CandidateProbe
am instrument -w -r -e variant equal_return org.x2m7.trime.readiness.candidatediagnostic/.CandidateProbe
am instrument -w -r -e variant no_super_measure org.x2m7.trime.readiness.candidatediagnostic/.CandidateProbe
```

Each run reports its own installed APK hash and the selected Kotlin source hash.
The source binding is also stored in the APK asset
`diagnostic-source-binding.json`. Keep all original failure attempts. After this
comparison, run the original cloud-built production APK through the separate
production probe; source-copy diagnostic results cannot replace that check.
