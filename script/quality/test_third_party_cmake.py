# SPDX-License-Identifier: GPL-3.0-or-later
"""Exercise build-tree adaptations without modifying real dependency sources."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
CMAKE = os.environ.get("CMAKE_EXECUTABLE", "cmake")
ADAPTER = ROOT / "app/src/main/jni/cmake/ThirdPartyCmake.cmake"


class ThirdPartyCmakeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / "source"
        self.build = self.root / "build"
        self.source.mkdir()
        (self.source / "CMakeLists.txt").write_text(
            'cmake_minimum_required(VERSION 3.18)\nproject(probe NONE)\n'
            f'include("{ADAPTER.as_posix()}")\n'
            'trime_add_dependency(vendor/snappy snappy 3.1)\n'
            'trime_add_dependency(vendor/leveldb librime/deps/leveldb 3.9)\n'
            'trime_add_dependency(vendor/OpenCC OpenCC 3.5)\n', encoding="utf-8")
        for name, version in (("snappy", "3.1"), ("leveldb", "3.9"), ("OpenCC", "3.5")):
            directory = self.source / "vendor" / name
            directory.mkdir(parents=True)
            (directory / "CMakeLists.txt").write_text(
                f'cmake_minimum_required(VERSION {version})\nproject({name} NONE)\n'
                + ('add_subdirectory(data)\n' if name == "OpenCC" else ''), encoding="utf-8")
        data = self.source / "vendor/OpenCC/data"
        data.mkdir()
        (data / "CMakeLists.txt").write_text(
            'find_package(PythonInterp REQUIRED)\n'
            'execute_process(COMMAND "${PYTHON_EXECUTABLE}" -c '
            '"import sys; assert sys.version_info.major == 3" RESULT_VARIABLE result)\n'
            'if(NOT result EQUAL 0)\nmessage(FATAL_ERROR "Host Python failed")\nendif()\n',
            encoding="utf-8")

    def configure(self, success=True):
        result = subprocess.run(
            [CMAKE, "-S", str(self.source), "-B", str(self.build),
             "-Werror=dev", "-Werror=deprecated"],
            capture_output=True, text=True, timeout=60)
        if success:
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertNotIn("Warning", result.stdout + result.stderr)
        else:
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("Re-audit", result.stderr)
        return result

    def test_all_four_adaptations_preserve_sources_and_refresh(self):
        before = {p.relative_to(self.source): p.read_bytes()
                  for p in self.source.rglob("*") if p.is_file()}
        self.configure()
        self.assertEqual(before, {p.relative_to(self.source): p.read_bytes()
                                  for p in self.source.rglob("*") if p.is_file()})
        for name in ("snappy", "librime/deps/leveldb", "OpenCC"):
            self.assertIn("...3.10)", (self.build / "trime-deps" / name / "CMakeLists.txt").read_text())
        data = self.build / "trime-deps/OpenCC/data/CMakeLists.txt"
        self.assertNotIn("find_package(PythonInterp", data.read_text())
        self.assertIn("find_package(Python3 REQUIRED COMPONENTS Interpreter)", data.read_text())
        added = self.source / "vendor/snappy/added.txt"
        added.write_text("new input", encoding="utf-8")
        self.configure()
        copy = self.build / "trime-deps/snappy/added.txt"
        self.assertEqual(copy.read_text(), "new input")
        added.unlink()
        self.configure()
        self.assertFalse(copy.exists())

    def test_minimum_version_drift_requires_reaudit(self):
        path = self.source / "vendor/snappy/CMakeLists.txt"
        path.write_text(path.read_text().replace("VERSION 3.1", "VERSION 3.12"), encoding="utf-8")
        self.configure(success=False)

    def test_python_find_drift_requires_reaudit(self):
        path = self.source / "vendor/OpenCC/data/CMakeLists.txt"
        path.write_text(path.read_text().replace("PythonInterp", "Python"), encoding="utf-8")
        self.configure(success=False)

    def test_duplicate_patch_target_requires_reaudit(self):
        path = self.source / "vendor/snappy/CMakeLists.txt"
        path.write_text(path.read_text() * 2, encoding="utf-8")
        self.configure(success=False)


if __name__ == "__main__":
    unittest.main()
