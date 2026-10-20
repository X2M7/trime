# Setup footer: original split APK comparison

This is an API 21 diagnostic of original split APKs, not acceptance of the
current `560b5b99` whole APK. The baseline is `f34ee4fc`, fixed is `8e373f34`;
the relevant setup sources match the respective new baseline and patch.
The current layout is byte-identical to the fixed original layout.
Both original artifacts and the unchanged manifest/setup source files were
checked; these Debug APKs have different CI signing keys and were installed
separately in a new isolated AVD, not as an upgrade or data-preservation test.

`setup-baseline-small.png` / `setup-fixed-small.png` are unedited captures.
Logical display is 480x540 at 160 dpi; the physical screenshot is 480x800
and contains 130-pixel black bars at top and bottom. Do not compare those
physical coordinates directly with UIAutomator's logical coordinates.

The logical baseline pager is y=25..492 and Next is y=444..492. With the
patch the pager ends at y=444. The first page's action button moves from
y=415..463 to y=391..439. Its horizontal range does not intersect Next,
so this is evidence of occupying the footer's vertical band, not a
reproduced overlapping click rectangle or misclick.

The fixed original APK was also navigated through storage choice, Enable
and Select; the pager continues ending at the visible row when Next is
hidden. The existing non-scrollable page can still exceed a short pager:
in the storage-selected state the Done node ends at y=450, beyond y=444.
Its actual glyph clipping was not established by the screenshot, and
that state has no baseline counterpart here. This remains a limitation.
A font-scale settings write did not change the observed layout; large-font
acceptance is explicitly excluded. See `root-diagnostic-review.json` for
independent image/XML review and hashes. Submit as Draft, not Ready.
