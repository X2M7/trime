# SPDX-License-Identifier: GPL-3.0-or-later
import unittest

from run_runtime import api21_editor_bound


READY = """Current Input Method Manager state:
  mCurMethodId=com.android.inputmethod.latin/.LatinIME
  mCurClient=ClientState{abcdef uid 10053 pid 3123} mCurSeq=5
  mInputShown=true
Input method service state for com.android.inputmethod.latin.LatinIME@30aa:
  mWindowVisible=true mWindowWasVisible=true
  mInputStarted=true mInputViewStarted=true mCandidatesViewStarted=false
  mInputEditorInfo:
    inputType=0x1 imeOptions=0x4 privateImeOptions=null
    packageName=com.osfans.trime.debug fieldId=1 fieldName=null
"""


class RuntimeBindingTest(unittest.TestCase):
    def test_ready_editor(self):
        self.assertTrue(api21_editor_bound(READY, 3123))

    def test_previous_client_is_not_ready(self):
        self.assertFalse(api21_editor_bound(READY, 2173))
        self.assertFalse(api21_editor_bound(READY.replace("pid 3123}", "pid 31230}"), 3123))

    def test_launcher_or_initial_activity_field_is_not_ready(self):
        self.assertFalse(api21_editor_bound(READY.replace("com.osfans.trime.debug field", "com.android.launcher field"), 3123))
        self.assertFalse(api21_editor_bound(READY.replace("imeOptions=0x4", "imeOptions=0x0"), 3123))

    def test_window_focus_is_not_sufficient(self):
        for condition in ("mInputShown=true", "mWindowVisible=true", "mInputViewStarted=true"):
            with self.subTest(condition=condition):
                self.assertFalse(api21_editor_bound(READY.replace(condition, condition.replace("true", "false")), 3123))

    def test_wrong_or_missing_service_is_not_ready(self):
        self.assertFalse(api21_editor_bound(READY.replace("com.android.inputmethod.latin/.LatinIME", "com.osfans.trime.debug/.IME"), 3123))
        self.assertFalse(api21_editor_bound("", 3123))
        self.assertFalse(api21_editor_bound(READY + READY, 3123))


if __name__ == "__main__":
    unittest.main()
