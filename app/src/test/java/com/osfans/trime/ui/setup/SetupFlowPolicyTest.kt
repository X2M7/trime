/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ui.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupFlowPolicyTest {
    @Test fun upgradeDoesNotRequireRepeatingNewWizardSteps() {
        assertFalse(needsInputSetup(false, false, false, false, false))
    }

    @Test fun freshInstallStillNeedsInputChoiceAfterSystemImeStepsAreFinished() {
        assertTrue(needsInputSetup(false, true, false, false, false))
    }

    @Test fun interruptedSetupResumesItsUnfinishedTrial() {
        assertTrue(needsInputSetup(true, false, true, false, false))
        assertFalse(needsInputSetup(true, false, true, true, false))
    }

    @Test fun explicitlyDeferringTrialDoesNotOpenTheWizardAgain() {
        assertFalse(needsInputSetup(true, true, false, false, true))
        assertFalse(needsInputSetup(true, false, true, false, true))
    }
}
