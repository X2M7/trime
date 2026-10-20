# perf: initialize input feedback resources on demand

Starting the IME currently constructs TextToSpeech and SoundPool even when their feedback is disabled. Retain the application context and create each backend when enabled feedback needs it. Wait for TTS initialization and custom sample loading before playback, release resources on destruction, and ignore callbacks from previous resources. Add the TTS service visibility query described in the [Android API documentation](https://developer.android.com/reference/android/speech/tts/TextToSpeech).

Speech commits can arrive from Rime worker jobs. Dispatch both speech entry points to main before reading preferences or touching pending/resource state, and invalidate queued requests when input starts, ends, or the manager is initialized/destroyed. Initialization failures are retried after destroy/init rather than on every key; custom key sounds received before loading completes are not replayed.

Validation:

- 14 focused resource/worker-dispatch tests passed; strict helper/test Kotlin compilation and focused Spotless passed.
- The exact-source [complete application JVM run](https://github.com/X2M7/trime/actions/runs/39435068756) passed all 315 cases (301 baseline + 14 new), with no failures/errors/skips. The new test compiler diagnostic is gone; existing build-logic/tool warnings remain.
- [Four-platform Debug/native builds](https://github.com/X2M7/trime/actions/runs/39435037065) are in progress.

This remains Draft for Android playback/lifecycle checks: toggling feedback, initialization completing after finish/destroy, and loading custom samples. The pure helper tests do not exercise Android playback. Lint, Release validation and measured memory savings remain unverified; existing toolchain diagnostics are retained.

[Source and validation scope](https://github.com/X2M7/trime/blob/60aac317e22d640526c0fc1f65b7a4fd3ae21f89/doc/contribution-split/checks/lazy-feedback-submission-source.json).

Source: `contrib/pr-feedback-resources` at `e7e63932683dfcc985cc88601f995968b8fc50b4`.

Submitted as [#2179 Draft](https://github.com/osfans/trime/pull/2179).
