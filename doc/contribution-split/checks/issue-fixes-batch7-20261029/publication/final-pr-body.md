Update Spotless from 8.10.2 to 8.10.3 to avoid the deprecated `Configuration.setVisible(false)` call on Gradle 9+. This includes the [dependency fix](https://github.com/diffplug/spotless/pull/3053) for [diffplug/spotless#3099](https://github.com/diffplug/spotless/issues/3099). The plugin ID, target patterns, and explicitly pinned ktlint 1.8.0 stay unchanged.

Local A/B runs executed both formatter and check actions with Gradle 9.8.1 and deprecation tracing. The Spotless-origin warning traces to `GradleProvisioner` in the baseline and disappears after the upgrade. Both runs checked the same 293 tracked Kotlin files with identical paths and contents.

The 52 Android Gradle Plugin and three KSP calls producing the same deprecation remain, so Gradle can still print `setVisible` warnings. The three separate included-build output warnings also remain; #2206 addresses those. This change does not claim that all project warnings are gone.

[All five upstream CI checks passed](https://github.com/osfans/trime/actions/runs/40823791223). [Source-bound validation and retained diagnostics](https://github.com/X2M7/trime/blob/1643097d6807d77e8d88ad8629ffc1abee05de3f/doc/contribution-split/checks/issue-fixes-batch7-20261029/local-validation.json) record the actual tasks and distinguish native compilation from cache reuse. No device or release validation is claimed for this build-tool update.
