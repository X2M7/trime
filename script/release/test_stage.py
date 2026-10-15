#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""The raw APK audit uses certificate arrays, including for one signer."""
import copy
import unittest

import stage


CERTIFICATE = "c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2"


class PublishedHistoryTests(unittest.TestCase):
    def setUp(self):
        self.meta = {"applicationId": "com.osfans.trime.debug", "previousVersionCode": 20261121,
                     "certificateSha256": CERTIFICATE}
        # The package, versions and one-element signer arrays match the actual
        # published t9.10/t9.16 APK audit. Extra original audit fields are allowed.
        self.history = [
            {"asset_id": 1, "package_name": "com.osfans.trime.debug", "version_code": 20261115,
             "signer_certificate_sha256": [CERTIFICATE], "debuggable": True, "passed": True},
            {"asset_id": 2, "package_name": "com.osfans.trime.debug", "version_code": 20261121,
             "signer_certificate_sha256": [CERTIFICATE], "debuggable": False, "passed": True},
        ]

    def test_original_single_signer_arrays_pass_without_mutation(self):
        original = copy.deepcopy(self.history)
        stage.validate_apk_history(self.history, self.meta)
        self.assertEqual(self.history, original)

    def test_empty_multiple_string_or_wrong_signer_is_rejected(self):
        for certificates in ([], [CERTIFICATE, CERTIFICATE], CERTIFICATE, ["a" * 64], None):
            with self.subTest(certificates=certificates):
                history = copy.deepcopy(self.history)
                history[0]["signer_certificate_sha256"] = certificates
                with self.assertRaises(ValueError):
                    stage.validate_apk_history(history, self.meta)

    def test_failed_or_truthy_non_boolean_audit_is_rejected(self):
        for passed in (False, None, 1, "true", "false"):
            with self.subTest(passed=passed):
                history = copy.deepcopy(self.history)
                history[0]["passed"] = passed
                with self.assertRaises(ValueError):
                    stage.validate_apk_history(history, self.meta)

    def test_changed_maximum_cannot_reuse_previous_version_claim(self):
        for maximum in (20261120, 20261122):
            with self.subTest(maximum=maximum):
                history = copy.deepcopy(self.history)
                history[-1]["version_code"] = maximum
                with self.assertRaises(ValueError):
                    stage.validate_apk_history(history, self.meta)

    def test_invalid_version_type_or_range_is_rejected(self):
        for version in ("20261121", True, 0, 2100000001):
            with self.subTest(version=version):
                history = copy.deepcopy(self.history)
                history[-1]["version_code"] = version
                with self.assertRaises(ValueError):
                    stage.validate_apk_history(history, self.meta)

    def test_empty_history_or_missing_installation_identity_is_rejected(self):
        foreign = copy.deepcopy(self.history)
        for row in foreign:
            row["package_name"] = "com.osfans.trime"
        for history in ([], None, {}, foreign):
            with self.subTest(history=history):
                with self.assertRaises(ValueError):
                    stage.validate_apk_history(history, self.meta)

    def test_failed_other_package_audit_cannot_be_silently_ignored(self):
        self.history.append({"package_name": "com.osfans.trime", "passed": False})
        with self.assertRaises(ValueError):
            stage.validate_apk_history(self.history, self.meta)


if __name__ == "__main__":
    unittest.main()
