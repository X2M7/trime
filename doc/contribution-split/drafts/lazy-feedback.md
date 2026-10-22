# perf: initialize input feedback resources on demand

Starting the IME currently constructs TextToSpeech and SoundPool even when their feedback is disabled. Retain the application context and create each backend when enabled feedback needs it. Wait for TTS initialization and custom sample loading before playback, release resources on destruction, and ignore callbacks from previous resources. Add the TTS service visibility query described in the [Android API documentation](https://developer.android.com/reference/android/speech/tts/TextToSpeech).

Speech commits can arrive from Rime worker jobs. Dispatch both speech entry points to main before reading preferences or touching pending/resource state, and invalidate queued requests when input starts, ends, or the manager is initialized/destroyed. Initialization failures are retried after destroy/init rather than on every key; custom key sounds received before loading completes are not replayed.

Validation:

- 14 focused resource/worker-dispatch tests passed; strict helper/test Kotlin compilation and focused Spotless passed.
- The exact-source [complete application JVM run](https://github.com/X2M7/trime/actions/runs/39435068756) passed all 315 cases (301 baseline + 14 new), with no failures/errors/skips. The new test compiler diagnostic is gone; existing build-logic/tool warnings remain.
- [Fork four-platform Debug/native builds](https://github.com/X2M7/trime/actions/runs/39435037065) and [upstream PR builds](https://github.com/osfans/trime/actions/runs/39436199322) passed all eight OS jobs; Ubuntu Spotless passed. Existing dependency/toolchain diagnostics remain. These builds do not establish Android playback acceptance.

- On API21, six real Android manager/Looper check groups passed: disabled requests allocate no backend, worker requests queued before finish/destroy are rejected, muted/system sounds allocate no custom pool, and a real WAV load callback populates/clears the custom sound cache. Pico TTS initialized and reported the initial utterance start/done; finish/destroy cleared pending state/resources. These are separate from the 14 host cases.
- The original round then recorded a system Pico native crash during shutdown; that failed full-round record is retained. Two independent self-target standard TTS controls, without Trime classes or bindings, reproduced the same `libttspico.so` shutdown SIGSEGV with fresh engine PIDs: two explicit stops plus one shutdown, and zero explicit stops plus one shutdown. This establishes the matching API21 x86_64 system-engine cause; it does not fix Pico or make all-PID Fatal counts zero.
- On API35, the same six real Android manager/Looper check groups passed, including the real SoundPool WAV load callback, cache retention across finishInput and release on destroy. No TTS engine was installed: the actual initialization-failure path released resources, cleared pending speech and avoided per-key retries. This proves failure handling, not successful speech or utterance callbacks. The six feedback preferences matched the host before/after values, and the owned sound fixtures were removed.
- The API35 feedback interval retained 33 warning/error lines, including `TextToSpeech: shutdown failed: not bound to TTS engine`; that warning is preserved rather than hidden. No Fatal/ANR/OOM was found in that interval. The combined setup/feedback capture also retained one system calendar low-memory kill and its original logs; it is not a Trime crash or a zero-warning run.

Original CI APK payloads were preserved under a temporary diagnostic signature: all 784 non-signature ZIP payloads and the installed APK readback were verified for each target. This is not official signing or upgrade acceptance. Actual keyboard gestures, observed late native callback delivery, physical audible output and the SoundPool stream ID remain unverified. The manager invoked sound playback after loading; no successful audio stream or audible output is inferred from that Unit-returning call. API35 speech callbacks remain untested because no engine was available. Lint, Release validation and measured memory savings remain unverified; existing system/tool diagnostics and all earlier failures remain. These are scoped resource checks, not whole-application or whole-system acceptance.

[API21 Android manager evidence](https://github.com/X2M7/trime/blob/contrib/split-index/doc/contribution-split/checks/lazy-feedback-api21-20261022.json), [independent system TTS controls](https://github.com/X2M7/trime/blob/contrib/split-index/doc/contribution-split/checks/lazy-feedback-pico-control-20261022.json), and [API35 qualification and original feedback log](https://github.com/X2M7/trime/tree/contrib/split-index/doc/contribution-split/checks/ui-api35-20261022).

[Source and validation scope](https://github.com/X2M7/trime/blob/60aac317e22d640526c0fc1f65b7a4fd3ae21f89/doc/contribution-split/checks/lazy-feedback-submission-source.json).

Source: `contrib/pr-feedback-resources` at `e7e63932683dfcc985cc88601f995968b8fc50b4`.

Submitted as [#2179](https://github.com/osfans/trime/pull/2179).
