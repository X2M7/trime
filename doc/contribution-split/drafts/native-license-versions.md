## Pull request

The bundled license records still report librime 1.16.1 and OpenCC 1.2.0, while the pinned sources report 1.17.0 and 1.4.2. Update the two `artifactVersion` fields so the license metadata matches those sources.

The librime pin's CMake project reports 1.17.0; this does not assert that the pin is the exact release tag. OpenCC is pinned at `ver.1.4.2`. No dependency revision changes.

Related: #2170. The underlying dependency upgrades were merged in #2026 and #2039.

### Validation

This submission at `77774753293cf9b5c7f8c8bf9b85c322efac5b04` has the identical Git tree and patch as the previously tested `f34ee4fca9fd544aa2a20b80cacbc6e3570771bf`. Only the commit message was reconstructed to add the required explanatory body; the original branch is preserved. The [new head's CI](https://github.com/X2M7/trime/actions/runs/39406744329) is running; validation below refers to the tested original head.

- [x] `make style-lint` passed on Ubuntu.
- [x] `make debug` passed on Ubuntu, Windows, ARM macOS and Intel macOS at `f34ee4fca9fd544aa2a20b80cacbc6e3570771bf` ([CI](https://github.com/X2M7/trime/actions/runs/39386296682)).
- [x] All 301 application JVM tests passed, with no failures or skips ([test run](https://github.com/X2M7/trime/actions/runs/39402432371)).
- [x] Both JSON files parse; only the version fields differ; source pins checked.
- [x] The original CI x86_64 APK contains `rime/librime=1.17.0` and `BYVoid/OpenCC=1.4.2` in `res/raw/aboutlibraries.json`. Official artifact ZIP size/SHA-256 matched. APK execution/UI testing was not performed.
- [ ] License-page UI check on Android.

Existing upstream diagnostics remain. Lint and Release acceptance are not claimed. This is a draft pending the UI check.

### Contribution checks

- [x] Read [CONTRIBUTING](https://github.com/osfans/trime/blob/develop/CONTRIBUTING.md).
- [x] Conventional Commit; one commit for one metadata correction.
- [ ] Manually test: Done.
