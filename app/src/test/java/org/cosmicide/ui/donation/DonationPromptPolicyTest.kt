package org.cosmicide.ui.donation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DonationPromptPolicyTest {
    @Test
    fun `first prompt requires five launches and three projects`() {
        assertNull(donationPromptMilestone(launchCount = 4, projectCount = 3, 0))
        assertNull(donationPromptMilestone(launchCount = 5, projectCount = 2, 0))
        assertEquals(5, donationPromptMilestone(launchCount = 5, projectCount = 3, 0))
    }

    @Test
    fun `first eligible prompt can occur after launch five`() {
        assertEquals(5, donationPromptMilestone(launchCount = 18, projectCount = 3, 0))
    }

    @Test
    fun `recurring prompts use twenty five launch milestones`() {
        assertNull(donationPromptMilestone(launchCount = 24, projectCount = 3, 5))
        assertEquals(25, donationPromptMilestone(launchCount = 25, projectCount = 3, 5))
        assertEquals(50, donationPromptMilestone(launchCount = 63, projectCount = 3, 25))
        assertEquals(75, donationPromptMilestone(launchCount = 75, projectCount = 3, 50))
    }

    @Test
    fun `a claimed milestone is not repeated`() {
        assertNull(donationPromptMilestone(launchCount = 25, projectCount = 3, 25))
        assertNull(donationPromptMilestone(launchCount = 74, projectCount = 3, 50))
    }

    @Test
    fun `late eligibility claims only the latest milestone`() {
        assertEquals(75, donationPromptMilestone(launchCount = 88, projectCount = 3, 0))
    }
}
