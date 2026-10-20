#!/bin/sh
# SPDX-License-Identifier: GPL-3.0-or-later
set -eu

if [ "$#" -ne 3 ]; then
    echo 'usage: sh check_jni_thread_attachment.sh REPOSITORY ANDROID_NDK NEW_OUTPUT_DIRECTORY' >&2
    exit 2
fi
source_root=$(cd "$1" && pwd)
ndk_root=$(cd "$2" && pwd)
check_root=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
base=f5008260dc1e71b05e8e5508c6cfbf8ebf2507a0
fixed=3e990b2dae202d5deed9a1b36ccc5f1048e3524c
mkdir "$3"
out_root=$(cd "$3" && pwd)
cp "$ndk_root/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/jni.h" "$out_root/jni.h"

for mode in baseline fixed; do
    revision=$base
    if [ "$mode" = fixed ]; then revision=$fixed; fi
    mkdir "$out_root/$mode"
    git -C "$source_root" show "$revision:app/src/main/jni/librime_jni/jni-utils.h" > "$out_root/$mode/jni-utils.h"
    "${CXX:-clang++}" -std=c++17 -fno-elide-constructors -O0 -g0 -Wall -Wextra -Werror \
        -I "$out_root" -I "$out_root/$mode" \
        "$check_root/jni_thread_attachment.cc" -o "$out_root/$mode.bin" \
        2> "$out_root/$mode.compile.stderr"
    code=0
    "$out_root/$mode.bin" > "$out_root/$mode.jsonl" || code=$?
    if [ "$mode" = baseline ]; then test "$code" -eq 1; else test "$code" -eq 0; fi
    code=0
    "${CXX:-clang++}" -std=c++17 -fno-elide-constructors -O0 -g0 -Wall -Wextra -Werror \
        -DREQUIRE_UNIQUE_OWNER -I "$out_root" -I "$out_root/$mode" \
        "$check_root/jni_thread_attachment.cc" -o "$out_root/$mode-ownership.bin" \
        2> "$out_root/$mode-ownership.compile.stderr" || code=$?
    if [ "$mode" = baseline ]; then test "$code" -eq 1; else test "$code" -eq 0; fi
done

python3 - "$out_root" <<'PY'
import json
from pathlib import Path
import sys

out = Path(sys.argv[1])
results = {}
for mode in ("baseline", "fixed"):
    lines = [json.loads(line) for line in (out / (mode + ".jsonl")).read_text().splitlines()]
    cases, summary = lines[:-1], lines[-1]
    assert summary["cases"] == len(cases)
    assert summary["failed_cases"] == sum(not case["passed"] for case in cases)
    results[mode] = {"cases": cases, "summary": summary}
assert results["baseline"]["summary"]["passed"] is False
assert results["fixed"]["summary"]["passed"] is True
assert all(case["passed"] for case in results["fixed"]["cases"])
assert [case["case"] for case in results["baseline"]["cases"]] == [case["case"] for case in results["fixed"]["cases"]]
print(json.dumps(results, indent=2))
PY
