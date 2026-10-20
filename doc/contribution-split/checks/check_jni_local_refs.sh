#!/bin/sh
# SPDX-License-Identifier: GPL-3.0-or-later
set -eu

if [ "$#" -ne 3 ]; then
    echo 'usage: sh check_jni_local_refs.sh PATCHED_REPOSITORY ANDROID_NDK NEW_OUTPUT_DIRECTORY' >&2
    exit 2
fi
patch_root=$(cd "$1" && pwd)
ndk_root=$(cd "$2" && pwd)
test_root=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
base=f5008260dc1e71b05e8e5508c6cfbf8ebf2507a0
fixed=9e296c1e29dd1f896400d71946fa2cd76d101b9a
rime_root="$patch_root/app/src/main/jni/librime"
jni_root="$patch_root/app/src/main/jni/librime_jni"
test "$(git -C "$patch_root" rev-parse HEAD)" = "$fixed"
test "$(git -C "$rime_root" rev-parse HEAD)" = 7bc3fb0005a03aff1c086611e4d52bb7f6444fdd
git -C "$patch_root" diff --exit-code "$fixed" -- app/src/main/jni/librime_jni
git -C "$rime_root" diff --exit-code HEAD -- src/rime_api.h include/utf8 include/utf8.h
mkdir "$3"
out_root=$(cd "$3" && pwd)
mkdir "$out_root/baseline"
cp "$ndk_root/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/jni.h" "$out_root/jni.h"
git -C "$patch_root" show "$base:app/src/main/jni/librime_jni/objconv.h" > "$out_root/baseline/objconv.h"

for mode in baseline fixed; do
    header_root=$jni_root
    if [ "$mode" = baseline ]; then header_root="$out_root/baseline"; fi
    "${CXX:-clang++}" -std=c++17 -O0 -g0 -Wall -Wextra -Werror \
        -Wno-error=pessimizing-move -Wno-error=sign-compare \
        -I "$header_root" -I "$out_root" -I "$jni_root" \
        -I "$rime_root/src" -I "$rime_root/include" \
        "$test_root/jni_local_refs.cc" -o "$out_root/$mode.bin" \
        2> "$out_root/$mode.compile.stderr"
    code=0
    "$out_root/$mode.bin" > "$out_root/$mode.json" || code=$?
    if [ "$mode" = baseline ]; then test "$code" -eq 1; else test "$code" -eq 0; fi
done

python3 - "$out_root" <<'PY'
import json
from pathlib import Path
import sys

out = Path(sys.argv[1])
before = json.loads((out / 'baseline.json').read_text())
after = json.loads((out / 'fixed.json').read_text())
assert before['passed'] is False and before['live_local_refs'] == 8192
assert before['peak_local_refs'] == 8192 and before['DeleteLocalRef'] == 0
assert after['passed'] is True and after['live_local_refs'] == 0
assert after['peak_local_refs'] == 1 and after['DeleteLocalRef'] == 8192
print(json.dumps({'baseline': before, 'fixed': after}, indent=2))
PY
