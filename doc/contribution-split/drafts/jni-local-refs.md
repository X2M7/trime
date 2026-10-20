## Pull request

`stringArrayToStringVector` retains every local reference created by `GetObjectArrayElement` until the native method returns. Large arrays therefore increase local-reference pressure within that JNI frame.

Wrap each element in the existing `JRef`. `CString` releases the UTF characters first, then `JRef` deletes the element reference at the end of the iteration. Values and ordering stay unchanged. This fixes accumulation before a native return, not a permanent leak across JNI calls.

Related: #2170. This patch changes only the two lines needed for element ownership.

### Validation

This submission at `a83583b89c4f37755ce542f8b41c891fa46c1275` has the identical Git tree and patch as the previously tested `9e296c1e29dd1f896400d71946fa2cd76d101b9a`. Only the commit message was reconstructed to add the required explanatory body; the original branch is preserved. The [new head's CI](https://github.com/X2M7/trime/actions/runs/39406744423) is running; validation below refers to the tested original head.

- [x] `make style-lint` passed on Ubuntu.
- [x] Four-OS Debug/native builds passed at `9e296c1e29dd1f896400d71946fa2cd76d101b9a` ([CI](https://github.com/X2M7/trime/actions/runs/39386297302)).
- [x] All 301 application JVM tests passed without failures/skips ([test run](https://github.com/X2M7/trime/actions/runs/39402432371)).
- [x] A [production-header JNI-table mock](https://github.com/X2M7/trime/blob/2b3672168976af602605790dd90a10b76d4405ec/doc/contribution-split/checks/jni_local_refs.cc) converted 2,048 elements four times: baseline retained 8,192 refs; fixed retained zero, peak one. Values and cleanup order passed.
- [ ] Android CheckJNI/runtime validation. The JVM suite does not execute this converter.

The host runner retains the same pre-existing `pessimizing-move`/`sign-compare` diagnostics in both versions; those two categories were allowed during the focused compile. Native allocation/exception behavior is unchanged. This is a draft pending Android validation.

### Contribution checks

- [x] Read [CONTRIBUTING](https://github.com/osfans/trime/blob/develop/CONTRIBUTING.md).
- [x] Conventional Commit; no new imports or unrelated code.
- [ ] Manually test: Done.
