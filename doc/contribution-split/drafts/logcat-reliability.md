Stopping or reopening the log screen can leave a child process or a stale stream slot behind. Coroutine cancellation does not reliably interrupt a blocked pipe read, and the current PID/clear command options are unavailable on Android 5/6.

Own snapshot, streaming and clear processes through creation, cancellation and stream cleanup. Use a separate IO cleanup view so a blocked reader cannot consume the sole normal IO slot; retain cleanup diagnostics on the original cancellation cause. Subscribe before starting the reader, stop the viewer's own binding on replacement/detach, and let old jobs finish without clearing a newer stream. Deployment clear/read commands run on IO, with cooperative waits and a diagnostic fallback that preserves deployment notifications after collection errors.

On API21–23, use short options and filter the numeric PID field in threadtime output. Preserve LF record framing so a payload's bare CR cannot become a forged PID header. From API24 use native --pid filtering. The viewer recognizes both formats and handles short marker lines safely.

The default SAF export filename now replaces timestamp colons with hyphens. The unchanged ISO name was actually rejected by the API21 VFAT provider with EINVAL; the corrected default has now been saved without editing on API21. The shared datetime display format is unchanged.

A separate first-maintenance run exposed another defect in this same viewer: the adapter retained every log row and styled/notified for each line. The app stayed alive, but retained Dalvik allocations reached about 242 MiB and repeated GCs left very little free heap, so that run was stopped with a normal Back rather than accepted or allowed to crash. Both display and pending windows now retain at most 10,000 rows and 2 Mi UTF-16 units each. Updates are combined per frame, styling is applied to visible rows, and the UI/export explicitly reports omitted old records. This is a log-retention bound, not a total application heap guarantee.

Validation for `e8e4db83`:

- The 32 focused Process/Logcat cases cover real JVM child cancellation, IO parallelism one, destruction/close failures, restart identity, legacy PID filtering, bare-CR payloads and subscriber startup.
- [Fresh complete application tests](https://github.com/X2M7/trime/actions/runs/39705023492) actually executed all 349 cases (including 16 new buffer-budget cases) with zero failures, errors or skips. Original XMLs, exact source and 20 recursive pins were verified.
- [Upstream Debug/native CI](https://github.com/osfans/trime/actions/runs/39704822026) passed all four OS jobs; the [fork run](https://github.com/X2M7/trime/actions/runs/39704817884) still has a platform job running. The original Ubuntu APK signature, BuildConfig source, x86_64 ELF and official artifact digest were verified; native bytes were reused from the matching cache, not newly compiled in that job.
- The branch normally merges accepted upstream `12ffd16f`. The changes remain one logging behavior: collection ownership and compatibility, the export filename, and bounded display/export retention; 14 changed paths in total.

The previous `d7c0d441` APK passed API21 three open/Back cycles (one reader while open, zero after Back), actual same-UID negative-PID exclusion, and an untouched default-name SAF export. All 5,073 parsed exported records belonged to the original app PID. Its separate cold run failed on log retention as described above. The exact `e8e4db83` cold-memory/lifecycle/export rerun and API35 checks are pending, so this PR stays Draft.

An earlier APK predating #2178 failed on missing JNI detach. A different attempt failed on the independent standard-UTF8/Modified-UTF8 candidate bridge, now tracked in #2186. The old unexplained stream failure and all original failed attempts are retained; a later steady-state pass is not presented as proof of their root cause.
Existing dependency/toolchain and Debug META-INF/services signature warnings remain. This is not Release/Lint, physical-device or general input-stability acceptance.

Source: `contrib/pr-logcat-reliability` at `e8e4db837d16e00bdbc3d5ab266561ac5a99114c`.
