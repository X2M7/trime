Setup pages currently extend beneath the bottom navigation buttons. Constrain the pager above the top of all visible navigation buttons, including Prev and Skip when Next is gone. Keep those buttons fixed while page content can scroll in a short viewport. A fill-viewport ScrollView preserves the centered page when its content fits; the existing page controls, storage behavior and navigation remain unchanged.

The original footer-only patch reduced the page height without making overflowing content reachable. A paired API21 check at 480×540 and font scale 1 found the completed-storage hint clipped from 16 visible pixels to 4. That failed revision is retained in history. This update wraps the page content for scrolling and still needs the corresponding Android retest; successful navigation alone is not treated as proof that all content is visible.

Validation:

- Previous exact-source application tests and four-platform Debug builds passed for `560b5b99`; those results do not validate the new scrolling layout.
- The paired original/fixed Activity check exercised five real touch and navigation steps for each layout. Footer button bounds stayed identical and the pager stopped at the visible footer, but the content-clipping regression above prevents acceptance.
- The revised XML parses and compiles with AAPT2 without diagnostics. Existing child controls, IDs and constraints are preserved. Source review verified that SetupFragment returns the binding root as View without a ConstraintLayout cast.
- Fresh full application unit tests and Debug/native builds are in progress for `a219a075`. Actual scrolling, focus, large-font visibility and navigation still need targeted Android validation.

This remains Draft. The earlier Activity comparison used exact CI setup code with a temporary diagnostic signature; the historical baseline APK has source-equivalent setup classes/layouts rather than the complete baseline tree. The probe does not establish a complete onboarding, upgrade, Release or Lint result. Existing dependency and tool warnings remain.

This branch also merges accepted upstream develop at `12ffd16f`, including JNI attachment ownership, the JNI source-directory API and Snappy. Relative to current develop, the feature changes only the two setup layouts.

Source: `contrib/pr-setup-footer` at `a219a07545ba26d7cfda287305ceedee34ea824c`.
