## Pull request

`JEnv` can attach a native Rime notification thread, but currently leaves that attachment alive when the guard returns. Pair each successful attachment with a detach at scope exit; leave already-attached Java threads alone. Disable copying/moving so ownership cannot be duplicated.

The notification callback declares the guard before its local references. Its Java call returns and those references are released before the guard detaches. This follows the [JNI thread lifetime guidance](https://developer.android.com/ndk/guides/jni-tips#threads) without changing the existing attachment/error conventions.

Related: #2170. One header changes; no build, release or fork configuration is included.

### Validation

- [x] Seven production-header JNI-table cases passed; the baseline failed four. Includes failed-attach, borrowing, nested guards, repeated factories and unique ownership.
- [x] An [actual HotSpot 17 comparison with `-Xcheck:jni`](https://github.com/X2M7/trime/blob/5c1a766dd37fe6c9e89f8d75e56f9d04c64e6d19/doc/contribution-split/checks/jni-thread-host-vm/README.md) exercised a native thread and real JNI calls. The baseline remained attached after the guard; the fix returned it to detached. Main-thread borrowing/nesting stayed attached, both VMs shut down normally, and strict C++17 compilation/checked-VM execution emitted no diagnostics. The parameterized reproducer uses the actual pinned headers.
- [x] At `2283878e`, all 301 application JVM cases passed with no failures/errors/skips ([run](https://github.com/X2M7/trime/actions/runs/39423530048)). These JVM tests do not exercise JEnv.
- [ ] New-head [four-platform Debug/native CI](https://github.com/X2M7/trime/actions/runs/39423511285) is still running.
- [ ] Android ART/CheckJNI with actual Rime callbacks.

This is a Draft pending the remaining checks. HotSpot does not replace ART acceptance. Existing build/tool diagnostics remain; Lint, Release and input-performance acceptance are not claimed.

### Contribution checks

- [x] Read CONTRIBUTING; one Conventional Commit with an explanatory body.
- [ ] Manually test: Done.
