# t9.4 Final Warning and SAF Verification

Date: 2026-09-06. Minimum Android version remains **API 21**. This completes the
[stage-A audit](VALIDATION-t9.4-stage-a.md), including its two follow-up fixes.
Only the user fork's `develop` branch is a push target. This verification does
not publish a Release, move `latest`, or change the stable download links.

## Frozen Build

| Field | Value |
| --- | --- |
| Package | `com.osfans.trime.debug` |
| Display name / signing | Trime; debug signing, not release signing |
| versionName / versionCode | `3.3.13-t9.4` / `20261105` |
| minSdk / targetSdk / ABI | 21 / 37 / `arm64-v8a` |
| Production and test Git SHA | `e386fdc209198c8113ade2121e5fca57f099b62f` |
| APK SHA-256 | `432999ea3da3acedc0657eed0e81708d09427692f357ef856d14891b9a940396` |
| Signer SHA-256 | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| androidTest APK SHA-256 | `05c33bb18b220e8ef1de8d2b98413a941d7c82c49c8eb4f25403f40f1b980902` |
| Nine-key schema | `luna_pinyin_t9` |
| Schema SHA-256 | `9f04415df4bc0342dfa9a11212ef34d57445c60c6b234c9e73835c685308c41f` |
| Bundled Rime resource digest | `05df6f1e0cd576a4955b23452ef18b0bda60095341c3cd149f840dd0a39cc413` |
| Native library SHA-256 | `d0a44d7e4ac7c0656e633f786505905abfeacb813f8feb03e7f4410cbeb2dcf9` |
| T00 identity directory | `build/baseline/432999ea3da3-9c88b6e0fc71/` |

Both APKs were built from a clean worktree with fixed build timestamp
`1788698803410`. Subsequent report-only changes do not change their source SHA.
The unpublished stage-A and final test builds share a version number; use their
different APK hashes and Git SHAs to distinguish them. The frozen application
APK is retained at the identity directory's `inputs/application.apk`.

## Fixes

The stage-A changes harden SAF streaming, read-back verification, replacement,
rollback, incomplete listings, path containment and document-provider operations.
They also update API-21-compatible dependencies, clean up resources/API warnings,
remove static keyboard ownership, and fix clipboard-editor keyboard visibility.
See the stage-A report for the full change list.

Final follow-up changes:

- The schema picker determines emptiness from the actual enabled-schema list.
  Rime's temporary F4 `.default` engine no longer hides that list. Confirming a
  choice first dismisses the internal switcher with Escape, then selects the
  requested schema. Cancelling does not send Escape or change the schema.
- An omitted optional `candidate_border_color` keeps its no-border appearance
  without generating an unknown-color warning. Explicit invalid colors and
  broken fallback references still produce validation errors.
- Regression coverage checks the real Android dialog adapter and item-click
  handler while F4 is active, the resulting engine schema, and the truly empty
  list. Three additional JVM tests cover border-color validation.

## Build Verification

- **215 JVM tests passed**, zero failures, errors or skips.
- **26 baseline-tool tests passed**; these are not performance benchmark runs.
- `assembleDebug`, `assembleDebugAndroidTest` and scoped `spotlessCheck` passed.
  All five final-follow-up Kotlin files report clean formatting.
- Final `lintDebug`: **0 errors, 0 warnings**, with warnings treated as errors.
  There are **11 audited dependency-update exceptions**, not 11 completed
  upgrades. Seven anchored coordinate/version patterns preserve Android 5.x
  compatibility. Changed messages and future versions are not exempt. See
  [API21.md](API21.md); API checks and manifest merger remain enabled.
- The native controller was rerun on API 37 using the library extracted from
  this frozen APK: **185 checks plus 3 separate-process learning checks passed**.
  This covers nine-key locking/editing/undo and actual full/double-pinyin rules
  with an isolated fixture dictionary. Results: `build/t9/3e1bb5829d6c/`.

The native binary and bundled Rime resources are byte-identical to stage A.
This is not a fresh JNI/C++ rebuild. The temporary `app/prebuilt` copy used to
avoid that rebuild was removed after verification. Release/R8 was not executed.

## Device Verification

Both AVDs ran the same ARM64 APK through the x86_64 native bridge, at
720 x 1600 / 280 dpi. Only one emulator ran at a time, with 2 GiB guest RAM and
two virtual CPUs. Gradle used one worker, a 1280 MiB heap and 768 MiB Metaspace,
and never ran alongside an emulator. Both emulators are closed after testing.

| Final APK check | Android 15 / API 35 | Android 17 / API 37 |
| --- | --- | --- |
| 21 SAF fixture/Trime-provider assertions | Passed on stable-system retry | Passed |
| Startup, themes, option cache, portrait/landscape T9 | Passed | Passed |
| F4 schema picker and empty-list regression | Passed | Passed |
| Focused clipboard editor, real keyboard bounds and nonblank pixels | Passed | Passed |
| Real system-provider create/replace/read-back/cleanup | Not repeated on final APK | Passed |
| Persisted grant after app process restart | Not repeated on final APK | Passed |
| Persisted grant after emulator restart | Not run | Passed |
| Actual denial after releasing persisted grant | Not repeated on final APK | Passed |
| App picker/import/deploy plus imported-file digest | Not run | Passed on retry |

API 35 real-system-provider create/replace/restart/revocation passed on the
separate frozen stage-A APK. Its SAF implementation is unchanged in the final
follow-up; those results are not relabelled as final-APK device tests.

System fingerprints:

- `hk_api35`, read-only session:
  `google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.043/12960925:userdebug/dev-keys`.
- `trime_saf_api37`, dedicated test AVD:
  `google/sdk_gphone64_x86_64/emu64xa:17/CE2A.260420.019/15611780:userdebug/dev-keys`.

Startup probe: API 35 recorded 3745 main-loop ticks and a largest gap of 711 ms;
API 37 recorded 3288 ticks and a largest gap of 911 ms. Both are below the
4000 ms failure threshold, not evidence of phone typing latency. Keyboard
visibility probes reported `[0,1079][720,1516]` on both devices; their captured
screenshots show a real full-pinyin keyboard below the focused editor.

The API 37 system picker granted only
`Documents/trime-saf-47b7f7ca-6984-47e4-b251-cdd551198168`, through
`com.android.externalstorage.documents`. A normal text fixture imported through
the app had identical source/destination SHA-256:
`4df71700ee235c5b76f28f92342d6b94b35626858947abdf4010ff2b21f38a56`.
The loading dialog closed, the selected directory/storage mode remained, and
Rime reported `DeployMessage(state=Success)`. Releasing that directory's grant
then caused a real system access denial. The dedicated AVD was returned to
app-specific storage after the test; no personal dictionary was cleared.

## Evidence and Limitations

The final identity directory contains **106 hash-verified artifacts**, including
raw results, screenshots and sidecars, build/Lint logs, test XML, fixture hashes
and native results. Regression archive: `saf-regression/`; working evidence:
`build/saf-warning-final/`. These generated files remain local. T00 captures
`capture-a42cbe2890c0` (API 35) and `capture-bd0513d3ddf0` (API 37) verify the
installed APK hash. Their uncontrolled UI/memory snapshots are not benchmarks.
The stage-A archive remains separate and unchanged.

Failed and interrupted attempts are retained:

- API 35's initial instrumentation was killed by ActivityManager for
  `timeout publishing content providers` while loading the application/test
  packages. No SAF cases had reported results. The crash buffer contained no
  Java/native exception trace; a stable-system retry passed. This is not proof
  that all possible first-launch failures have been fixed.
- API 37's emulator exited normally during the first import/deploy attempt;
  the cause was not established. That attempt is not a pass. After restarting,
  its persisted grant remained usable, and a separate picker/import/deploy
  retry completed with the text-fixture digest check above.
- Boot-time System UI/phone-service ANRs are recorded separately. Final crash
  buffers are empty. Logs still contain intentional fault-injection/missing-theme
  diagnostics and slow background-query/native-bridge notices. Zero warnings
  refers to the specified build/Lint checks, not removal of diagnostic logging.
- Preliminary formatting with relative hook paths and an invalid app-scoped
  Spotless task invocation were not accepted as verification. Correct absolute
  paths and root tasks passed later; their separate logs are retained.

See [SAF.md](SAF.md) for repeatable probes and provider limits. Safe replacement
requires rename support; incomplete listings fail closed, and unknown metadata
is streamed/read back rather than assumed unchanged. These tests do not certify
every OEM/cloud provider. Android 5.x runtime, release signing/R8, physical-device
performance and long-duration use remain untested. The T00 cold/learned cohorts
for this APK remain `not_run`; system-cache UI checks are not cold-start or
ranking benchmarks. Passing this regression set is not a claim of zero possible
bugs.
