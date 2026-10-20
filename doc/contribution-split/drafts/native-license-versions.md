# fix: align native license metadata with the pinned sources

The bundled license records identify librime as 1.16.1 and OpenCC as 1.2.0, although the existing source pins describe newer versions. The licenses page can consequently report versions that do not match the dependencies being built.

Update only the two artifactVersion fields to 1.17.0 and 1.4.2. The pinned librime CMake project reports 1.17.0; this does not assert that its commit is the exact release tag. Its OpenCC gitlink is exactly at ver.1.4.2. Dependency pins, license identifiers and descriptions remain unchanged.

**Validation:** Both JSON files parse; only the version fields changed, and recursive source pins were checked. Exact-SHA four-OS Debug/native builds and Ubuntu Spotless passed. A separate application JVM run executed all 301 baseline tests successfully, without skips.

**Remaining:** Verify the displayed licenses page. Lint, Release and device checks remain pending; APK contents were not audited in this check. Existing upstream compiler, dependency and tooling diagnostics remain.

Source: `contrib/split-native-license-versions` at `f34ee4fca9fd544aa2a20b80cacbc6e3570771bf`.
