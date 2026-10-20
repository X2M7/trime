# Focused checks for the split branches

These checks support the fork's patch-preparation index. They are not the upstream test suite, a new application test framework, an Android build, or device acceptance. The source branches remain independent of this documentation branch.

## JNI array local references

`jni_local_refs.cc` includes the production `objconv.h`. It mocks the actual NDK `JNINativeInterface` table, not a rewritten converter. It checks empty input, values/order, repeated conversions in one mock JNI environment, live/peak references, and release-before-delete ordering. A real JNI method return clears local references; the regression covers accumulation before that return.

Check out `contrib/split-jni-local-refs` at `9e296c1e29dd1f896400d71946fa2cd76d101b9a` in another directory and initialize its existing librime submodule. With Linux host Clang, Python 3, and an Android NDK installed, run:

```sh
sh check_jni_local_refs.sh /path/to/patched-trime /path/to/android-ndk /path/to/new-output-directory
```

The runner verifies the patch and librime pins, reads the baseline header from Git, and only writes to a new output directory. No APK, JVM, Android VM, or Rime engine is launched. It makes an unmodified copy of NDK `jni.h` to avoid placing the Android system-header directory in a host compiler's normal include path.

The source has existing `pessimizing-move` and `sign-compare` diagnostics in both versions. The focused runner preserves those warnings while allowing compilation; it does not prove warning-free builds. The original stricter invocation and its failed diagnostics were retained locally during preparation. Android CheckJNI, native exception/allocation failure cases, Release builds, and device checks remain pending.

## Layouts and license metadata

Both changed layout files were parsed and compiled separately with AAPT2 36.0.0 (`aapt2 compile <layout.xml> -o <existing-output-directory>`). Both commands exited 0 with empty stdout/stderr. This does not link the application or measure layout geometry.

License JSON parsing and field comparison confirm only `artifactVersion` changed. The existing upstream librime gitlink is `7bc3fb0005a03aff1c086611e4d52bb7f6444fdd`; its CMake project reports version `1.17.0` (not an assertion that this commit is the exact release tag). Its pinned OpenCC is `025f371dc76b598d77384fbdab90c937471844d8`, exactly at `ver.1.4.2`.

## Return action helper and JNI thread ownership

[third-batch-enter-host.json](third-batch-enter-host.json) binds the actual 12-test `EditorEnterActionTest` XML and four final source inputs to `bcb47b1a81eb211e7c0ede447c1b3263dcf80746`. Only the pure helper and its test were compiled in that host harness. `EnterKeyDisplayDelegate` and `TrimeInputMethodService` received formatting/source checks, not Android compilation or editor-action execution. The earlier 25 tests remain independently recorded in [second-batch-host.json](second-batch-host.json).

[jni_thread_attachment.cc](jni_thread_attachment.cc) executes the production `JEnv` and `GlobalRefSingleton.AttachEnv` against a mocked NDK JNI function table. The original commands in [third-batch-jni-thread.json](third-batch-jni-thread.json) cover seven cases: an already attached thread, successful owned attachment, failed attachment, failed `GetEnv`, nested ownership, repeated factory returns, and noncopyable/nonmovable ownership. The baseline fails four runtime cases and its ownership static assertions; the fixed header passes all seven cases and the static assertions. Both ordinary compilations used C++17, `-fno-elide-constructors`, `-Wall -Wextra -Werror` without diagnostics. This is not ART, a Java VM, or a real Rime callback.

The following fork-only reproducer reads the two pinned headers from Git, so it does not require the repository worktree to be at either checkpoint. It creates a new output directory and requires Linux host Clang, Python 3, and an Android NDK:

```sh
sh check_jni_thread_attachment.sh /path/to/trime /path/to/android-ndk /path/to/new-output-directory
```

This new shell reproducer was independently executed successfully after the original run; [jni-thread-reproduction.json](jni-thread-reproduction.json) binds its input hashes and actual result. These are repeated executions of the same seven mock cases, not fourteen distinct cases. The original command outputs remain preserved. A failed attach may still yield a null environment; callers must not dereference it. Detach return status, external manual detach/reattach, Java frames spanning owner destruction, Android CheckJNI, and actual native callback behavior remain outside this mock's coverage.

## CI and issue-search boundaries

[second-batch-ci-pending.json](second-batch-ci-pending.json) records the known first-attempt Ubuntu failure before formatter execution: Gradle could not resolve two Kotlin 2.4.10 artifacts. A later local HTTP 200 probe does not establish availability on a new GitHub runner. That historical failure record does not assert a retry outcome; the final same-SHA successful retry and all remaining jobs are in [second-batch-ci.json](second-batch-ci.json).

[issue-match.json](issue-match.json) preserves conservative issue/PR matching context. Nearby or historical discussions are not assigned `Fixes`/`Closes`; keyword search does not prove the absence of an issue. The fork-only validation control branch is listed separately in [branches.json](../branches.json), not as an application module or a passed test result.

[patch-composition.json](patch-composition.json) records successful independent `git apply --cached --check` checks for all eight patches and their ordered application to a temporary index. The composed tree was not compiled or run. A corrected audit path rule accepts the two license JSON files; that correction did not change any source patch or real worktree.

## Complete application JVM tests

[First matrix](application-jvm-first.json) and [second matrix](application-jvm-second.json) bind the fixed source heads, actual XML test counts, uncached application test task, and original evidence hashes. Every source branch preserves all 301 upstream baseline cases. Feedback adds 8, logcat adds 17, and Return adds 12. No test failed, errored or skipped. The same pre-existing SHOW_IMPLICIT compiler warning remains in all scopes; these results do not include Lint, Release, or device acceptance.

## Upstream submissions

[All original source builds](all-split-builds.json) verify 32/32 successful OS slots.
The original creation receipts are historical snapshots. [The first two PRs
were merged](upstream-merges-20261020.json); [two layout Drafts were submitted](layout-upstream-submissions.json)
and their [eight fork platform jobs subsequently passed](layout-pr-current-check.json).
Their manual/runtime limits remain; no zero-warning, Lint or Release conclusion follows.

[The next JNI attachment source](jni-attachment-submission-source.json) is a one-header
submission based on `144f6111`. Its [complete application JVM run](jni-attachment-application-jvm.json)
passed the same 301 cases; this does not execute JEnv. [Real HotSpot tests and a
parameterized reproducer](jni-thread-host-vm/README.md) separately exercise actual
attachment, nested borrowing and destruction, including `-Xcheck:jni`. They do not
execute Android ART or Rime callbacks. Baseline and fixed outputs remain distinct.

`remote-preservation.json` and `upstream-submissions.json` retain their original
capture-time state. Current status is in the main contribution index and later
receipts linked above; old snapshots are not rewritten as present outcomes.

## Feedback resource submission

[lazy-feedback-focused-host.json](lazy-feedback-focused-host.json) records 14
production-helper cases (the same cases rerun after the nullability correction),
strict helper/test Kotlin compilation and focused formatting. The Manager is only
style-checked in that fixture. [The final exact-source application run](lazy-feedback-application-jvm-final.json)
separately compiles the application and passes all 315 JVM cases at `e7e63932`.
Its 301 baseline cases are preserved and the 14 added labels match source.
The original [315-test run with one new compiler diagnostic](lazy-feedback-application-jvm-initial.json)
is retained as a blocked submission attempt; passing tests alone was insufficient.
The final new diagnostic is gone. Existing build-logic/tool warnings remain.
Neither helper nor full JVM tests establish actual Android TTS/SoundPool playback.

## Return labels and cached keyboard redraw

[enter-actions-focused-host.json](enter-actions-focused-host.json) binds the 12
policy cases executed in the strict Kotlin host fixture; the four other changed
files were format-checked only there. [The exact-source application run](enter-actions-application-jvm.json)
separately passes all 317 JVM cases at `5879c1eb`: all 301 baseline cases, the 12
policy cases and four real Theme/Kodein/display-delegate/InputBroadcaster cases.
The latter exercise state-before-broadcast and repeated/changed metadata; they
do not construct Android EditorInfo or KeyboardWindow views, perform service
commits, or prove on-screen redraw. New source diagnostics are absent in that
run; existing build-logic/toolchain warnings remain.

[The previous feedback module's completed platform review](lazy-feedback-platform-ci-final.json)
now closes all eight fork/upstream OS jobs without new feedback source diagnostics.
Its original pending snapshots and creation receipt retain their capture-time
state. [The validation-only PR body refresh](lazy-feedback-validation-refresh.json)
does not promote the Draft or add playback/device acceptance.
