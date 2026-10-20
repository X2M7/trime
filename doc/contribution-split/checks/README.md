# Focused checks for the split branches

These checks support the fork's patch-preparation index. They are not the upstream test suite, a new application test framework, an Android build, or device acceptance. The four source branches remain independent of this documentation branch.

## JNI array local references

`jni_local_refs.cc` includes the production `objconv.h`. It mocks the actual NDK `JNINativeInterface` table, not a rewritten converter. It checks empty input, values/order, repeated conversions in one mock JNI environment, live/peak references, and release-before-delete ordering. A real JNI method return clears local references; the regression covers accumulation before that return.

Check out `contrib/split-jni-local-refs` at `9e296c1e29dd1f896400d71946fa2cd76d101b9a` in another directory and initialize its existing librime submodule. With Linux host Clang, Python 3, and an Android NDK installed, run:

```sh
sh check_jni_local_refs.sh /path/to/patched-trime /path/to/android-ndk /path/to/new-output-directory
```

The runner verifies the patch and librime pins, reads the baseline header from Git, and only writes to a new output directory. No APK, JVM, Android VM, or Rime engine is launched. It makes an unmodified copy of NDK `jni.h` to avoid placing the Android system-header directory in a host compiler's normal include path.

The source has existing `pessimizing-move` and `sign-compare` diagnostics in both versions. The focused runner preserves those warnings while allowing compilation; it does not prove warning-free builds. The original stricter invocation and its failed diagnostics were retained locally during preparation. Android CheckJNI, native exception/allocation failure cases, full application builds, and device checks remain pending.

## Layouts and license metadata

Both changed layout files were parsed and compiled separately with AAPT2 36.0.0 (`aapt2 compile <layout.xml> -o <existing-output-directory>`). Both commands exited 0 with empty stdout/stderr. This does not link the application or measure layout geometry.

License JSON parsing and field comparison confirm only `artifactVersion` changed. The existing upstream librime gitlink is `7bc3fb0005a03aff1c086611e4d52bb7f6444fdd`; its CMake project reports version `1.17.0` (not an assertion that this commit is the exact release tag). Its pinned OpenCC is `025f371dc76b598d77384fbdab90c937471844d8`, exactly at `ver.1.4.2`.
