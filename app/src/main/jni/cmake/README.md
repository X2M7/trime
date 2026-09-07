<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

Please format CMake file with https://github.com/cheshirekow/cmake_format
```
make cmake-format
```

## Pinned Dependency Adaptations

`ThirdPartyCmake.cmake` creates private build-tree copies of snappy, leveldb and
OpenCC. Only four CMake declarations are changed; submodule files and commits
stay untouched. The copies retain upstream licenses and are refreshed on
reconfiguration, including when source files are added or removed.

- Preserve each dependency's minimum CMake version, but declare policy
  compatibility through 3.10 with a version range. This resolves CMake 3.31's
  deprecated pre-3.10 policy compatibility without disabling diagnostics.
- Replace OpenCC data generation's `FindPythonInterp` call with
  `FindPython3 COMPONENTS Interpreter`. Keep the local `PYTHON_EXECUTABLE`
  variable expected by its existing commands. Only a host interpreter is needed;
  no Python runtime or development libraries are linked into Android.

Each expected declaration must occur exactly once. If a pinned dependency is
updated and the declaration no longer matches, configuration fails with a
re-audit message instead of silently dropping the adaptation. Re-evaluate or
remove the relevant adaptation after such an update.

The current toolchain is CMake 3.31.6. `CMAKE_POLICY_VERSION_MINIMUM` cannot be
used here because it was added in CMake 4.0. See the official documentation for
[policy version ranges](https://cmake.org/cmake/help/v3.31/command/cmake_minimum_required.html),
[the external policy floor](https://cmake.org/cmake/help/latest/variable/CMAKE_POLICY_VERSION_MINIMUM.html),
and [the Python module migration](https://cmake.org/cmake/help/latest/policy/CMP0148.html).

Run the isolated regression tests with the chosen CMake executable:

```sh
CMAKE_EXECUTABLE="$ANDROID_HOME/cmake/3.31.6/bin/cmake" \
  python3 -B -m unittest discover -s script/quality -p test_third_party_cmake.py -v
```

The tests promote developer and deprecation warnings to errors and cover all
four adaptations, host Python execution, unchanged originals, refresh/deletion,
and rejection of changed or ambiguous patch targets.
