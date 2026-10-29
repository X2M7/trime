Exclude the included convention build's outputs from the root Kotlin formatter. Spotless's default string-target exclusions cover the root and ordinary subprojects, but not this included build, producing three cross-build output dependency deprecations.

Local A/B checks with Gradle 9.8.1 and Spotless 8.10.2 executed both formatter and check actions without cache reuse. Both checked the same 293 tracked Kotlin files; the three warnings became zero, and all 47 producer output files retained identical paths, sizes and hashes. The existing target patterns and formatter rules are unchanged.

[All five upstream CI checks passed](https://github.com/osfans/trime/actions/runs/40820471270). [Source-bound A/B evidence](https://github.com/X2M7/trime/blob/fa153b8d852aa22ac48e72889873df59c10da5df/doc/contribution-split/checks/issue-fixes-batch6-20261029/local-validation.json) records the initial configuration failure and retry separately. The `Configuration.setVisible(boolean)` deprecation and other tool/dependency warnings remain recorded.

Follow-up to warnings observed during #2205 validation; no matching issue was found for this specific formatter warning.
