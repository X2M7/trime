# fix: detach only JNI thread attachments owned by the guard

JEnv can attach a native callback thread but currently never detaches it when the guard leaves scope. Adding unconditional detachment would instead break Java-owned threads and nested environment guards.

Record whether this guard successfully attached a previously detached thread. Detach only that attachment at destruction, and prohibit copying or moving its ownership. Existing borrowed environments remain attached. The existing factory continues to return the guard through mandatory C++17 copy elision.

**Validation:** Seven production-header JNI-table cases passed; the baseline failed four. Strict C++17 compilation and unique-ownership assertions passed, including disabled optional elision. The new pinned-header reproducer also ran successfully. Separate application JVM CI passed all 301 baseline tests without skips. Four-OS Debug/native CI and Ubuntu Spotless passed; actual native tasks and Ninja execution were reviewed, without claiming per-target link-command or binary inspection.

**Remaining:** Exercise real native callbacks and CheckJNI. Neither the mock nor application JVM tests execute ART native callbacks. Failed attachment can still yield a null environment; detach failures remain unhandled. Lint, Release and device acceptance remain pending; inherited diagnostics remain.

Source: `contrib/split-jni-thread-attachment` at `3e990b2dae202d5deed9a1b36ccc5f1048e3524c`.
