# fix: keep setup pages above the navigation buttons

The setup pager currently occupies the full parent height while the navigation buttons occupy the bottom of that same parent. Page content can therefore extend behind the buttons when vertical space is limited.

Constrain the pager between the parent top and a top Barrier referencing Next, Previous and Skip. The Barrier ignores hidden buttons and follows the tallest visible button. The existing three-page visibility policy always leaves at least one referenced button visible. This changes one layout file and preserves the existing setup flow.

**Validation:** XML parsing, button-visibility source review, AAPT2 compilation and diff checks passed. Exact-SHA Debug/native builds passed on four operating systems; Ubuntu Spotless passed. A separate application JVM run executed all 301 baseline tests successfully, without skips.

**Remaining:** Check all three pages on small screens and with enlarged fonts. JVM tests do not measure this layout. Lint, Release and device geometry remain pending; CI APKs were not downloaded/audited for this patch. Existing upstream diagnostics remain.

Source: `contrib/split-setup-footer` at `8e373f34400655a6aa871315531a8b25142d9233`.
