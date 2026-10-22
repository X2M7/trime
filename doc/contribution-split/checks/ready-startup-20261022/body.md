When the framework requests the IME before theme initialization, `onCreateInputView()`
returns null. The framework can retain that empty input frame after the theme
becomes available, leaving a blank keyboard until the IME is switched away and back.

Complete that deferred request after the first theme scope is ready. Create only
missing input/candidate trees and replay an active virtual view's start with the
latest `EditorInfo`. Finishing clears the pending start; destruction clears both
flags. Completion does not call `showWindow`. This is one service-file fix directly based on `develop` at `12ffd16f`.

Validation at `2d2fbce7`:

- Single-file ktlint, diff checking and independent lifecycle/source review passed;
  all 301 exact-source application JVM tests passed.
- The fork push built exact `2d2fbce7` on four OSes. The upstream PR run built its
  recorded synthetic merge ref on four OSes while binding that head. All eight
  jobs passed. The original fork Ubuntu artifact's four ABI-specific APKs passed
  signature, ELF, BuildConfig and 20-pin checks. Native evidence establishes
  CMake/Ninja tasks, not object-by-object recompilation.
- The fix is included in combined T9 head `9f86bc8f`; eight reviewed startup
  fragments match byte-for-byte. The standalone startup APK is not device-tested.
  API21 naturally deferred, then constructed its keyboard in the same process
  without another IME toggle or SHOW. After actual maintenance success, real
  T9 touches selected and committed `你好`; its first Observer stopped normally.
  API35 supports delayed creation, the latest numeric editor and literal `2`
  after an explicit SHOW; continuously visible automatic recovery was not established.

The [original failure and diagnostic control](https://github.com/X2M7/trime/blob/06dd2ac672aeb96d94a88a40cd6b56abaf140080/doc/contribution-split/checks/deferred-input-view-negative-20261022/provenance.json),
[cloud audit](https://github.com/X2M7/trime/blob/06dd2ac672aeb96d94a88a40cd6b56abaf140080/doc/contribution-split/checks/deferred-input-view-cloud-final-20261022/final-cloud-supplement-summary.json), [API21 startup scope](https://github.com/X2M7/trime/blob/06dd2ac672aeb96d94a88a40cd6b56abaf140080/doc/contribution-split/checks/deferred-input-view-api21-startup-20261022/startup-lifecycle-qualification1.json) and
[API35 scope](https://github.com/X2M7/trime/blob/06dd2ac672aeb96d94a88a40cd6b56abaf140080/doc/contribution-split/checks/deferred-input-view-api35-20261022/independent/g1-independent-limited-qualification.json) retain original files, rejected attempts and warnings.
Device checks used controlled test profiles, including three explicit preferences
and an isolated Debug reinstall. Hide-before-ready and physical-keyboard behavior,
Release, real-user upgrades, hardware, TalkBack and performance remain unqualified.
