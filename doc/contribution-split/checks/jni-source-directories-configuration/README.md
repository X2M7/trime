# Prebuilt JNI source-directory configuration comparison

Fork-only evidence; these files are not included in the upstream patch.

With JDK 17 and Gradle 9.8.1, run from this directory:

```sh
gradle --no-daemon --max-workers=1 --console=plain verifyDirectories
```

This uses the pinned AGP 9.3.0 implementation and Gradle ProjectBuilder. Two scenarios contain 21 assertions: a real directory-set comparison and an actual Android plugin applied before variant finalization. They compare the old and new literal `prebuilt` append paths, preserve the default/custom directories and sentinel files, and check relative paths with spaces. The real plugin has zero late-addition delegates at this phase.

Sentinel `.so` files are arbitrary bytes, not usable native libraries. This does not test APK packaging, native ABI/signature or late source changes after variant finalization. No Provider scenario is accepted as verified.

The isolated harness calls ProjectBuilder teardown in `finally`, then explicitly exits its JVM after all assertions and both teardown calls succeed, because AGP/ProjectBuilder global threads outlive the project. Assertions or teardown failures cannot reach the successful exit. Earlier offline-resolution, Provider-setup and controlled-stop attempts remain recorded in the result; their printed assertions are not counted as successful runs.
