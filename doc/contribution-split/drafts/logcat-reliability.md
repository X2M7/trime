# fix: make logcat collection cancellable across supported Android versions

Stopping or reopening the log screen can leave a child process or a stale stream slot behind. Coroutine cancellation does not reliably interrupt a blocked pipe read, and the current PID/clear command options are unavailable on Android 5/6.

Own snapshot, streaming and clear processes through creation, cancellation and stream cleanup. Use a separate IO cleanup view so a blocked reader cannot consume the sole normal IO slot; retain cleanup diagnostics on the original cancellation cause. Subscribe before starting the reader, stop the viewer's own binding on replacement/detach, and let old jobs finish without clearing a newer stream. Deployment clear/read commands run on IO, with cooperative waits and a diagnostic fallback that preserves deployment notifications after collection errors.

On API21–23, use short options and filter the numeric PID field in threadtime output. Preserve LF record framing so a payload's bare CR cannot become a forged PID header. From API24 use native --pid filtering. The viewer recognizes both formats and handles short marker lines safely.

Validation:

- The 32 focused Process/Logcat cases passed, including real JVM child cancellation, a separate JVM with IO parallelism limited to one, destruction/close failures, restart identity, legacy PID filtering, bare-CR payloads and subscriber startup. Strict Kotlin compilation and formatting of all six changed files passed.
- The exact-source [complete application JVM run](https://github.com/X2M7/trime/actions/runs/39461202289) passed 333 cases: all 301 baseline cases and the 32 new cases. Official artifacts, 20 recursive submodule pins and original JUnit reports were verified; no new source diagnostics were found.
- [Fork four-platform Debug builds](https://github.com/X2M7/trime/actions/runs/39461172912) are in progress.

The change spans four implementation/caller files and two test files. Existing dependency/toolchain warnings remain. Actual Android UI/logcat, Lint and Release checks have not been verified here, so this remains Draft. Synchronous platform destroy/close calls do not have a universal hard deadline; stderr is not drained concurrently. Detach stops collection; this does not add automatic replay/resume when the same view is reattached.

[Source and validation scope](https://github.com/X2M7/trime/blob/f3c5b9c0483b9ce117082cd2dcf91870a30ec40a/doc/contribution-split/checks/logcat-reliability-submission-source.json); [focused checks and limits](https://github.com/X2M7/trime/blob/f3c5b9c0483b9ce117082cd2dcf91870a30ec40a/doc/contribution-split/checks/logcat-reliability-focused-jvm.json).

Source: `contrib/pr-logcat-reliability` at `6e4f2e59acce6cff0d097ff47a8d635940a6aa69`.

Submitted as [#2182 Draft](https://github.com/osfans/trime/pull/2182).
