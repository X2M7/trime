# Small PR descriptions and drafts

The original eight source branches remain independent and based on `f5008260`.
The first two submissions, [licenses #2174](https://github.com/osfans/trime/pull/2174)
and [JNI references #2175](https://github.com/osfans/trime/pull/2175), were merged
by the maintainer after upstream CI passed.

The next two independent submissions are based directly on `144f6111`:
[setup footer #2176](https://github.com/osfans/trime/pull/2176) and
[clipboard editor height #2177](https://github.com/osfans/trime/pull/2177).
Their descriptions below match the actual Draft submissions. Each new head
passed 301 application JVM cases and all four fork platform builds; #2177 has
also passed all upstream platform jobs. #2176 still has one upstream job running.
The next independent submission is [JNI ownership #2178](https://github.com/osfans/trime/pull/2178),
based on `144f6111` at `2283878e`; its 301 application JVM cases and focused
mock/checked HotSpot tests passed, while its new platform builds and Android
ART acceptance remain pending. The other three topics have not been submitted.
Original source refs and test records remain intact.

- [setup-footer](setup-footer.md)
- [clipboard-editor-height](clipboard-editor-height.md)
- [native-license-versions](native-license-versions.md)
- [jni-local-refs](jni-local-refs.md)
- [lazy-feedback](lazy-feedback.md)
- [logcat-lifecycle](logcat-lifecycle.md)
- [enter-actions](enter-actions.md)
- [jni-thread-attachment](jni-thread-attachment.md)
