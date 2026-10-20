# build: use mutable JNI source directories API

The native convention calls the deprecated `AndroidSourceDirectorySet.srcDirs` overload, which emits a Kotlin warning while compiling build logic. Append `prebuilt` through the supported mutable `directories` API instead.

This one-line change keeps the default and custom JNI directories. It leaves the existing prebuilt-directory condition, CMake fallback and ABI settings unchanged, without updating AGP.

Validation:

- The exact-source [complete application JVM run](https://github.com/X2M7/trime/actions/runs/39455598482) passed all 301 cases with no failures, errors or skips. Convention compilation actually ran; the prior structured `srcDirs` warning is absent, with no new source diagnostics.
- A real AGP 9.3 configuration comparison passed 21 assertions across two scenarios. Both append paths retain default/custom/prebuilt directories and sentinel files, including paths with spaces; the actual plugin is checked before variant finalization.
- [Fork four-platform Debug builds](https://github.com/X2M7/trime/actions/runs/39455570865) are in progress.

Existing dependency and toolchain warnings remain; this addresses only the project's deprecated source-directory call. APK packaging with prebuilt libraries, Lint and Release checks have not been verified in this branch.

[Source and validation scope](https://github.com/X2M7/trime/blob/714b3dfa492cbcb221f512f41ecdfb69b089a2e1/doc/contribution-split/checks/jni-source-directories-submission-source.json); [reproducible AGP configuration comparison](https://github.com/X2M7/trime/tree/714b3dfa492cbcb221f512f41ecdfb69b089a2e1/doc/contribution-split/checks/jni-source-directories-configuration).

Source: `contrib/pr-prebuilt-jni-directories` at `dfcc87dfc9e0efa6650b29baf83852e57224020e`.

Submitted as [#2181 Draft](https://github.com/osfans/trime/pull/2181).
