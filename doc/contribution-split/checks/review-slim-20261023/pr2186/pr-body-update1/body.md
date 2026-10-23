Rime supplies standard UTF-8, while JNI `NewStringUTF` expects Modified UTF-8. The stock `a` candidate `𠼞` can therefore abort CheckJNI. Convert strings explicitly at the UTF-8/UTF-16 boundary and use UTF-16 code units for composition positions.

Following the review feedback, `2ad1865f` removes checks after non-throwing length queries and the duplicate check after a failed key description. Valid UTF-8 is converted directly once; only malformed input enters replacement handling, with partial output cleared before retry. Checks that stop Rime/OpenCC side effects after a failed conversion, release acquired characters, or preserve an existing JNI exception remain.

Validation for this revision:

- 115 real checked-JVM Unicode assertions and 46 UTF-16 position assertions pass. Both upstream controls compile and fail at their expected supplementary-character assertions.
- All six JNI translation units compile for NDK28/API21 x86_64 with `-Wall -Wextra -Werror` and the existing unused-parameter exclusion; diagnostic logs are empty.
- [Current upstream PR CI](https://github.com/osfans/trime/actions/runs/39897889043): 5/5 jobs passed; application unit-test and Lint tasks executed. Its recorded upstream merge is qualified separately from the fork head; no new full-application testcase count is inferred.

[Source, original streams and scope](https://github.com/X2M7/trime/blob/599b26cfbbec22f753ce421fe1d7abf895cf7d00/doc/contribution-split/checks/review-slim-20261023/README.md) are outside the patch. This revision has no new Android runtime acceptance. Earlier CheckJNI smoke evidence remains historical; the API21 candidate-rendering gap was separate and whole-app/display/Release acceptance is not claimed.

The native CI build retains warnings from pinned yaml-cpp, librime-lua and LevelDB dependencies; those libraries are unchanged by this PR.
