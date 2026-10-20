# Small PR descriptions and drafts

The original eight source branches remain independent and based on `f5008260`.
The first two submissions, [licenses #2174](https://github.com/osfans/trime/pull/2174)
and [JNI references #2175](https://github.com/osfans/trime/pull/2175), were merged
by the maintainer after upstream CI passed.

The next two independent submissions are based directly on `144f6111`:
[setup footer #2176](https://github.com/osfans/trime/pull/2176) and
[clipboard editor height #2177](https://github.com/osfans/trime/pull/2177).
Their descriptions below match the actual Draft submissions. Each layout head
passed 301 application JVM cases, and all fork/upstream platform jobs for #2176,
#2177 and JNI ownership [#2178](https://github.com/osfans/trime/pull/2178) have now
passed. Android/manual acceptance remains separate.

The next feedback submission, [#2179 Draft](https://github.com/osfans/trime/pull/2179),
is based directly on `144f6111`, at `e7e63932` on
`contrib/pr-feedback-resources`. It fixes worker-thread speech ownership before
submission. Its 315 complete application JVM cases and 14 focused helper cases
passed; the initial new test diagnostic is fixed and its historical run retained.
The new four-platform builds and actual Android playback remain pending. The
logcat and Return topics remain unsubmitted; logcat cancellation limits are
recorded in its draft. Original source refs and test records remain intact.

- [setup-footer](setup-footer.md)
- [clipboard-editor-height](clipboard-editor-height.md)
- [native-license-versions](native-license-versions.md)
- [jni-local-refs](jni-local-refs.md)
- [lazy-feedback](lazy-feedback.md)
- [logcat-lifecycle](logcat-lifecycle.md)
- [enter-actions](enter-actions.md)
- [jni-thread-attachment](jni-thread-attachment.md)
