Typing `a` can expose the supplementary Hanzi `𠼞` in the stock Pinyin candidates (the pinned luna-pinyin dictionary maps that exact word to `a`). On an API21 x86_64 emulator with CheckJNI, returning that candidate aborts in `NewStringUTF`: Rime supplies standard UTF-8, while that JNI API expects Modified UTF-8.

Convert native strings explicitly to UTF-16 with `NewString`, and Java UTF-16 strings to owned standard UTF-8. Use the same UTF-16 code units for preedit length, cursor and selection positions. Preserve the original pending exception and validate converted input before Rime/OpenCC side effects; discard incomplete string arrays. Malformed text is replaced with U+FFFD. Native C-string consumers retain their existing embedded-NUL termination behavior.

This is a standalone correction based on `12ffd16f`: eight production JNI files plus four native test/documentation files. It includes the signedness, return-move and aggregate-initialization fixes needed for the strict native checks. It does not change T9 layouts/schemas, Kotlin/UI behavior, submodule pins or fork release configuration. Existing `JEnv`/`GlobalRef` ownership code is unchanged.

Validation so far:

- The original API21 CheckJNI abort and tombstone are retained.
- 115 real checked-JVM Unicode assertions and 46 real-JVM composition-position assertions pass. The same two tests against the upstream headers fail on their expected supplementary-character checks.
- All six upstream JNI translation units compile for NDK28/API21 x86_64 with `-Wall -Wextra -Werror`.
- [Fresh exact-source cloud checks](https://github.com/X2M7/trime/actions/runs/39701967075) passed all 301 application JVM cases and the 115 + 46 checked-JVM assertions with both expected upstream negative controls. Original XMLs/streams, source and all 20 recursive pins were verified.
- [Fork Debug/native build](https://github.com/X2M7/trime/actions/runs/39701935883) and [upstream PR build](https://github.com/osfans/trime/actions/runs/39704066636) each passed all four OS jobs; the original four ABI APKs were checked for package/source, ELF ABI and valid signatures. On API21 with CheckJNI already enabled, the original `e16ab85f` APK survives real stock `a` candidates including `𠼞` in the same PID; real `ni` + Space commits `你`. Compact and expanded candidate rows render blank, so visible selection and whole-app acceptance are explicitly not passed. API35 checks are pending.

Draft until the remaining device/build checks finish. The host checks do not initialize a real Rime session or Android editor. This does not claim arbitrary native allocation-exception handling, other-ABI runtime acceptance, Release/Lint validation, or zero dependency/toolchain warnings.

JNI string contracts: [Modified UTF-8](https://docs.oracle.com/en/java/javase/25/docs/specs/jni/types.html#modified-utf-8-strings), [NewString](https://docs.oracle.com/en/java/javase/25/docs/specs/jni/functions.html#newstring).
