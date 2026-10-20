# perf: create speech and custom sound resources on demand

Starting the input method currently constructs TextToSpeech and SoundPool even when both feedback features are disabled. It can also attempt playback before an asynchronously initialized resource is ready.

Keep init(context), retain only the application context, and create speech or custom sound resources when enabled feedback needs them. Use them after readiness, release them on destruction, and reject callbacks belonging to a previous resource. Add the TTS service visibility query required on newer Android versions. Failed creation also stops repeated sound-path work on every key.

**Validation:** Eight focused production-helper tests passed with strict Kotlin compilation. The separate exact-SHA Android application JVM run compiled the manager and passed all 309 tests, including the 301 upstream cases, without skips. The original exact-SHA four-OS Commit CI and Ubuntu Spotless passed.

**Remaining:** Exercise the Android manager and actual TTS/SoundPool playback. Initialization failures retry after destroy/init; pre-load key sounds are not replayed. Memory gains, Lint, Release and device acceptance remain unverified. Existing upstream compiler/tooling diagnostics remain.

Source: `contrib/split-lazy-feedback` at `3912fb23aced49da94cee6e3c59b3a9585da779b`.
