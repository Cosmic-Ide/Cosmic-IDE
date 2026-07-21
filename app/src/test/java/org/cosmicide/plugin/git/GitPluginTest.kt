package org.cosmicide.plugin.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitPluginTest {
    @Test
    fun derivesProjectNameFromHttpsAndSshUrls() {
        assertEquals("cosmic-ide", repositoryName("https://example.com/team/cosmic-ide.git"))
        assertEquals("cosmic-ide", repositoryName("git@example.com:team/cosmic-ide.git"))
        assertEquals("cosmic-ide", repositoryName("https://example.com/team/cosmic-ide/"))
        assertEquals("repository", repositoryName(" "))
    }

    @Test
    fun validatesRefsPassedToGit() {
        assertTrue(isSafeRef("feature/plugin-api"))
        assertFalse(isSafeRef("--upload-pack=bad"))
        assertFalse(isSafeRef("feature..bad"))
        assertFalse(isSafeRef("branch@{1}"))
        assertFalse(isSafeRef("feature/"))
        assertFalse(isSafeRef("feature branch"))
        assertFalse(isSafeRef(""))
    }

    @Test
    fun extractsLatestProgressFromChunk() {
        assertEquals(0.72f, gitProgress("Receiving objects: 40%\rResolving deltas: 72%"))
        assertEquals(1f, gitProgress("Receiving objects: 100%"))
        assertNull(gitProgress("Already up to date."))
    }
}
