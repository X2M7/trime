# SPDX-License-Identifier: GPL-3.0-or-later
"""Synthetic gate inputs only; these tests do not install or verify Android APKs."""

import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest

import t14_upgrade as gate


class UpgradeGateTest(unittest.TestCase):
    def test_install_success_accepts_actual_push_and_streamed_line_endings(self):
        for raw in (b"Performing Push Install\n\tpkg: /data/local/tmp/old.apk\r\nSuccess\r\n",
                    b"Performing Streamed Install\nSuccess\n"):
            with self.subTest(raw=raw):
                gate.require_install_success(raw)

    def test_install_success_rejects_missing_ambiguous_or_failed_results(self):
        for raw in (b"", b"NotSuccess\n", b"Success extra\n", b"Success\r\nSuccess\n",
                    b"Success\nFailure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]\r\n"):
            with self.subTest(raw=raw), self.assertRaises(RuntimeError):
                gate.require_install_success(raw)

    def test_preference_seed_preserves_unrelated_values_and_types(self):
        original = b'<map><string name="empty"/><set name="choices"><string>b</string><string>a</string></set><long name="count" value="17"/><boolean name="candidate_show_comments" value="true"/></map>'
        before = gate.preference_values(original)
        after = gate.preference_values(gate.seeded_preferences(original))
        for key in ("empty", "choices", "count"):
            self.assertEqual(before[key], after[key])
        for key, value in gate.PREFERENCE_FIXTURE.items():
            self.assertEqual(value, after[key])

    def test_duplicate_preference_and_unsupported_type_are_rejected(self):
        for raw in (b'<map><int name="x" value="1"/><string name="x">different</string></map>',
                    b'<map><unknown name="x" value="1"/></map>'):
            with self.subTest(raw=raw), self.assertRaises(RuntimeError):
                gate.preference_values(raw)

    def artifact_case(self, mutate=lambda rows: None, old_kind="release"):
        # The files are explicitly synthetic byte fixtures, never called real APKs.
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        rows = {}
        for index, kind in enumerate(("old", "new", "test")):
            path = root / (kind + ".synthetic")
            path.write_bytes(("not an Android APK: " + kind).encode())
            rows[kind] = dict(gate.bound(path), package=gate.PACKAGE + (".test" if kind == "test" else ""),
                              version_name="synthetic", version_code=10 + index,
                              debuggable=kind == "test", certificate_sha256="a" * 64,
                              min_sdk=21, abis=["x86_64"], instrumentation_target=gate.PACKAGE)
        mutate(rows)
        record = root / "record.json"
        record.write_text(json.dumps(dict(schema_version=1, task="T14", candidate_commit="b" * 40, apks=rows)))
        args = SimpleNamespace(artifact_record=record, artifact_record_sha256=gate.bound(record)["sha256"],
                               target_sha="b" * 40, api=21, old_kind=old_kind,
                               old_apk=Path(rows["old"]["path"]), new_apk=Path(rows["new"]["path"]),
                               test_apk=Path(rows["test"]["path"]))
        return args

    def test_monotonic_matching_record_is_accepted(self):
        record, inputs = gate.validate_artifacts(self.artifact_case())
        self.assertEqual("T14", record["task"])
        self.assertEqual(4, len(inputs))

    def test_same_or_lower_version_is_rejected(self):
        for code in (9, 10):
            with self.subTest(code=code), self.assertRaises(RuntimeError):
                gate.validate_artifacts(self.artifact_case(lambda rows: rows["new"].update(version_code=code)))

    def test_changed_certificate_or_package_is_rejected(self):
        for field, value in (("certificate_sha256", "c" * 64), ("package", "com.osfans.trime")):
            with self.subTest(field=field), self.assertRaises(RuntimeError):
                gate.validate_artifacts(self.artifact_case(lambda rows: rows["new"].update({field: value})))

    def test_debug_new_release_and_wrong_old_kind_are_rejected(self):
        with self.assertRaises(RuntimeError):
            gate.validate_artifacts(self.artifact_case(lambda rows: rows["new"].update(debuggable=True)))
        with self.assertRaises(RuntimeError):
            gate.validate_artifacts(self.artifact_case(old_kind="debug"))

    def test_wrong_test_target_or_changed_bytes_are_rejected(self):
        with self.assertRaises(RuntimeError):
            gate.validate_artifacts(self.artifact_case(lambda rows: rows["test"].update(instrumentation_target="other")))
        args = self.artifact_case()
        args.new_apk.write_bytes(b"changed after inspection")
        with self.assertRaises(RuntimeError):
            gate.validate_artifacts(args)


if __name__ == "__main__":
    unittest.main()
