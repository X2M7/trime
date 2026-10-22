Setup pages currently extend beneath the bottom navigation buttons. Constrain the pager above all visible navigation buttons, including Prev and Skip when Next is gone, and let overflowing page content scroll while the footer stays fixed. A fill-viewport ScrollView retains the centered layout when the content fits; existing page controls, storage behavior and navigation remain unchanged.

The earlier footer-only revision shortened the page without making overflowing content reachable. A paired API21 check at 480×540 and font scale 1 found the completed-storage hint clipped from 16 visible pixels to 4. That failed revision and its original images remain in the audit record; the current scrolling revision addresses that regression.

Validation for `a219a075`:

- [301 application JVM cases](https://github.com/X2M7/trime/actions/runs/39699212750) actually executed and passed with no failures, errors or skips. Source, test XMLs and 20 recursive submodule pins were verified.
- [Upstream Debug/native CI](https://github.com/osfans/trime/actions/runs/39699200200) passed all four OS jobs. The separate fork run still had two jobs running at the recorded snapshot; no eight-job success or full native rebuild claim is made.
- The actual SetupActivity passed the five touch/navigation steps at both system font scales 1 and 2 on API21, 480×540/dpi160. Each run preserved seven original frames; real vertical swipes made all body controls/text and the complete Done text/icon reachable. Footer bounds stayed fixed, and the pager stopped above Prev/Skip when Next was gone.
- The font2 run read back both the actual ActivityManager configuration and target Resources (48px step text versus 24px at font1). Font configuration, its original unset settings key, screen size/density and Latin IME were restored. Key original PNGs and complete/visible rectangles were independently reviewed.

The PR is still Draft; this update records scoped Android evidence rather than a status change. The diagnostic target differs from the original exact-source CI APK only in its temporary signature: all 784 non-signature ZIP payloads and the installed APK readback were verified. It does not establish official upgrade/signature acceptance, complete onboarding, API35, physical-device, Release or Lint results. System/graphics/audio and existing dependency/tool warnings remain. Earlier modal, configuration and fixed-distance probe failures are preserved separately.

This branch also merges accepted upstream develop at `12ffd16f`, including JNI attachment ownership, the JNI source-directory API and Snappy. Relative to current develop, the feature changes only the two setup layouts.

[Runtime evidence and scope](https://github.com/X2M7/trime/blob/contrib/split-index/doc/contribution-split/checks/setup-footer-api21-20261022.json).

Source: `contrib/pr-setup-footer` at `a219a07545ba26d7cfda287305ceedee34ea824c`.
