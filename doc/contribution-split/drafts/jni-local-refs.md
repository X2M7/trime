# fix: release local references during JNI string-array conversion

Each GetObjectArrayElement call creates a local reference. stringArrayToStringVector releases UTF characters but currently retains every element reference until the native method returns, so a large array increases local-reference pressure within that call.

Wrap each element in the existing JRef. CString is destroyed first, releasing its UTF characters before JRef deletes the reference. Converted values and ordering stay unchanged. This addresses accumulation inside a JNI frame, not a permanent leak across native-method returns.

**Validation:** The production-header JNI mock converted 2,048 elements four times: baseline retained 8,192 references; the patch retained zero with peak one, preserving values and release order. Four-OS Debug/native CI and Ubuntu Spotless passed. A separate application JVM run passed all 301 baseline tests, without skips.

**Remaining:** Android CheckJNI, allocation-failure boundaries, Lint and Release verification remain pending. JVM tests do not execute this converter. Both host versions retain the same pessimizing-move/sign-compare warnings; only those warnings were allowed by the focused runner.

Source: `contrib/split-jni-local-refs` at `9e296c1e29dd1f896400d71946fa2cd76d101b9a`.
