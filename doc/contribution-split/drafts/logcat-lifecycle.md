# fix: clean up logcat processes during cancellation and restart

Cancelling a log reader does not reliably interrupt its blocking pipe read. The previous lifecycle could leave a child or streams open, while cleanup from an older reader could interfere with a replacement stream.

Give each consumption its own child ownership. Cancellation requests destruction, final cleanup attempts all three stream closures, and read failures remain primary when cleanup also fails. Scope stream state to its owning Job so stop/restart cannot let an older Job clear the current one. Snapshot and clear operations also wait for their owned commands.

**Validation:** Seventeen focused host tests passed, including real JVM child cancellation and combined read/destroy failures. The separate exact-SHA Android application JVM run passed all 318 tests without skips. Four-OS Debug/native CI and Ubuntu Spotless passed; the first Ubuntu dependency-resolution failure and unchanged-SHA retry are preserved.

**Remaining:** Android, Windows and macOS process runtime checks, Lint and Release acceptance remain pending. Existing command flags are unchanged. The one-second exit-poll window does not bound synchronous destroy/close calls. Upstream compiler and dependency diagnostics remain.

Source: `contrib/split-logcat-lifecycle` at `f5909dcb024997cef8eefe437ee810038118f3c1`.

Further pre-submission review found two controlled JVM limits: if the cancellation
watcher throws from destroy while stdout remains blocked, final stream cleanup
cannot run until the read is externally unblocked; a saturated IO dispatcher can
also delay the watcher. These are fixture observations, not an Android app hang
reproduction. The one-second exit polling window does not bound total cancellation.
This topic remains unsubmitted while those boundaries are resolved.
