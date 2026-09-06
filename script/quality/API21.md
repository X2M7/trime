# Android 5.x Compatibility Constraints

The application must retain `minSdk = 21`. Do not use `tools:overrideLibrary`
to force incompatible Android libraries into the APK. Compile/target SDK upgrades
do not authorize changing this minimum.

Audited on 2026-09-06 against the published AAR manifests and POM dependencies:

| Dependency | Selected | Incompatible update | Reason |
| --- | --- | --- | --- |
| Activity | 1.11.0 | 1.12.0 / 1.13.0 | minSdk 23 |
| Core | 1.17.0 | 1.18.0 / 1.19.0 | minSdk 23 |
| AppCompat | 1.7.1 | 1.8.0 | minSdk 23 |
| Navigation | 2.9.8 | 2.10.0 | runtime-android minSdk 24 |
| Room | 2.7.2 | 2.8.4 | runtime-android minSdk 23 |
| Paging | 3.3.6 | 3.5.1 | runtime minSdk 23 |
| WorkManager | 2.10.5 | 2.11.x | minSdk 23 |

The version catalog uses strict versions at these boundaries. Manifest merger
and Lint API checks remain enabled: transitive dependencies must also be checked.
Re-audit the constraint before accepting an update, including its transitive
dependencies. Version-notification exceptions must match the audited coordinates
and versions only, never disable all dependency checks or create a general baseline.

`app/lint.xml` exempts exactly 11 update notices (seven narrowly anchored
patterns). Room's build-time compiler is kept aligned with its API-21-compatible
runtime; the compiler itself is not an Android runtime library. A change to a
selected version or suggested update stops matching the exception. Lint warnings
are treated as errors, and release-build checking is no longer disabled.

Iconics 5.6.0 and its Compose runtime 1.9.3 dependency both declare minSdk 21.
Do not infer a library's minimum solely from its release date or the general
AndroidX policy; inspect the actual AAR and resolved dependency graph.

Primary references:

- [AndroidX minimum SDK policy](https://developer.android.com/jetpack/androidx/versions)
- [Room releases](https://developer.android.com/jetpack/androidx/releases/room)
- [Paging releases](https://developer.android.com/jetpack/androidx/releases/paging)
- [WorkManager releases](https://developer.android.com/jetpack/androidx/releases/work)
- [Core 1.17 AAR](https://dl.google.com/dl/android/maven2/androidx/core/core/1.17.0/core-1.17.0.aar)
- [Core 1.18 AAR](https://dl.google.com/dl/android/maven2/androidx/core/core/1.18.0/core-1.18.0.aar)
- [Activity 1.11 AAR](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.11.0/activity-1.11.0.aar)
- [Activity 1.12 AAR](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.12.0/activity-1.12.0.aar)
- [Activity 1.13 AAR](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.13.0/activity-1.13.0.aar)
- [Core 1.19 AAR](https://dl.google.com/dl/android/maven2/androidx/core/core/1.19.0/core-1.19.0.aar)
- [Navigation 2.10 runtime AAR](https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-runtime-android/2.10.0/navigation-runtime-android-2.10.0.aar)
- [Iconics 5.6 dependency POM](https://repo.maven.apache.org/maven2/com/mikepenz/iconics-core/5.6.0/iconics-core-5.6.0.pom)

Runtime test coverage, APK fingerprints and any remaining limitations belong in
the build-specific regression report. An API 21 manifest alone is not evidence
of a successful Android 5.x device test.
