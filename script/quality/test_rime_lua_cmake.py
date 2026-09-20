# SPDX-License-Identifier: GPL-3.0-or-later
"""Verify Android 21 Rime Lua compatibility without editing the submodule."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
NATIVE = ROOT / "app/src/main/jni"
CMAKE = os.environ.get("CMAKE_EXECUTABLE", "cmake")


class RimeLuaCmakeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        plugin = self.root / "librime-lua"
        source = plugin / "src/types.cc"
        source.parent.mkdir(parents=True)
        shutil.copyfile(NATIVE / "librime-lua/src/types.cc", source)
        (plugin / "CMakeLists.txt").write_text(
            "add_library(rime-lua-objs OBJECT src/types.cc)\n"
        )
        (self.root / "CMakeLists.txt").write_text(
            "cmake_minimum_required(VERSION 3.18...3.31)\n"
            "project(probe CXX)\n"
            f'include("{NATIVE}/cmake/ThirdPartyCmake.cmake")\n'
            "add_subdirectory(librime-lua)\n"
            f'include("{NATIVE}/cmake/RimeLuaCompat.cmake")\n'
            "trime_adapt_rime_lua()\n"
            "get_target_property(adapted rime-lua-objs SOURCES)\n"
            "get_target_property(definitions rime-lua-objs COMPILE_DEFINITIONS)\n"
            "get_target_property(includes rime-lua-objs INCLUDE_DIRECTORIES)\n"
            'file(WRITE "${CMAKE_BINARY_DIR}/sources.txt" "${adapted}")\n'
            'file(WRITE "${CMAKE_BINARY_DIR}/definitions.txt" "${definitions}")\n'
            'file(WRITE "${CMAKE_BINARY_DIR}/includes.txt" "${includes}")\n'
        )

    def configure(self, abi="armeabi-v7a", platform=21, build="build"):
        return subprocess.run(
            [
                CMAKE,
                "-S",
                str(self.root),
                "-B",
                str(self.root / build),
                "-DANDROID=TRUE",
                f"-DANDROID_ABI={abi}",
                f"-DANDROID_PLATFORM_LEVEL={platform}",
                "-Werror=dev",
                "-Werror=deprecated",
            ],
            capture_output=True,
            text=True,
            timeout=60,
        )

    def test_overlay_is_stable_and_uses_platform_width(self):
        original = self.root / "librime-lua/src/types.cc"
        before = original.read_bytes()
        timestamp = None
        for _ in range(2):
            result = self.configure()
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertNotIn("Warning", result.stdout + result.stderr)
            overlay = self.root / "build/trime-rime-lua/types.cc"
            current = overlay.stat().st_mtime_ns
            if timestamp is not None:
                self.assertEqual(timestamp, current)
            timestamp = current
        self.assertEqual(before, original.read_bytes())
        sources = (self.root / "build/sources.txt").read_text().split(";")
        self.assertEqual(len(sources), 1)
        self.assertIn("/trime-rime-lua/types.cc", sources[0])
        content = (self.root / "build/trime-rime-lua/types.cc").read_text()
        self.assertIn("#include <limits>", content)
        self.assertIn("std::numeric_limits<size_t>::max()", content)
        self.assertIn(
            "/librime-lua/src", (self.root / "build/includes.txt").read_text()
        )
        self.assertEqual(
            (self.root / "build/definitions.txt").read_text(),
            "_FILE_OFFSET_BITS=32",
        )

    def test_file_offset_fallback_is_limited_to_legacy_32_bit_android(self):
        for abi, platform, expected in [
            ("x86", 21, "_FILE_OFFSET_BITS=32"),
            ("arm64-v8a", 21, "definitions-NOTFOUND"),
            ("armeabi-v7a", 24, "definitions-NOTFOUND"),
        ]:
            build = f"build-{abi}-{platform}"
            result = self.configure(abi, platform, build)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertEqual(
                (self.root / build / "definitions.txt").read_text(), expected
            )

    def test_upstream_drift_is_not_silently_ignored(self):
        source = self.root / "librime-lua/src/types.cc"
        source.write_text(
            source.read_text().replace(
                "limit = limit == 0 ? 0xffffffffffffffff : limit;",
                "limit = limit == 0 ? 0xffffffff : limit;",
            )
        )
        result = self.configure()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Re-audit", result.stderr)


if __name__ == "__main__":
    unittest.main()
