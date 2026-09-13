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

    @Test
    fun parsesGitStatusPorcelain() {
        val output = """
            ## main...origin/main [ahead 1, behind 2]
            M  staged-modified.kt
            A  staged-added.kt
            D  staged-deleted.kt
             M unstaged-modified.kt
            ?? untracked.kt
        """.trimIndent()

        val status = parseGitStatus(output)
        assertEquals("main", status.branch)
        assertEquals("origin/main", status.tracking)
        assertEquals(1, status.ahead)
        assertEquals(2, status.behind)
        assertFalse(status.isClean)
        assertEquals(5, status.totalChangedCount)

        assertEquals(3, status.staged.size)
        assertEquals("staged-modified.kt", status.staged[0].path)
        assertTrue(status.staged[0].isModified)
        assertEquals("staged-added.kt", status.staged[1].path)
        assertTrue(status.staged[1].isAdded)
        assertEquals("staged-deleted.kt", status.staged[2].path)
        assertTrue(status.staged[2].isDeleted)

        assertEquals(1, status.unstaged.size)
        assertEquals("unstaged-modified.kt", status.unstaged[0].path)
        assertTrue(status.unstaged[0].isModified)

        assertEquals(1, status.untracked.size)
        assertEquals("untracked.kt", status.untracked[0].path)
        assertTrue(status.untracked[0].isUntracked)
    }

    @Test
    fun parsesCleanGitStatus() {
        val output = "## main...origin/main\n"
        val status = parseGitStatus(output)
        assertEquals("main", status.branch)
        assertEquals("origin/main", status.tracking)
        assertEquals(0, status.ahead)
        assertEquals(0, status.behind)
        assertTrue(status.isClean)
        assertEquals(0, status.totalChangedCount)
    }

    @Test
    fun parsesGitBranches() {
        val output = """
            * main                1a2b3c4 [origin/main] Initial commit
              feature/auth        5d6e7f8 Add authentication
              remotes/origin/main 1a2b3c4 Initial commit
        """.trimIndent()

        val branches = parseGitBranches(output)
        assertEquals(3, branches.size)

        val current = branches[0]
        assertEquals("main", current.name)
        assertTrue(current.isCurrent)
        assertFalse(current.isRemote)
        assertEquals("1a2b3c4", current.hash)
        assertEquals("origin/main", current.upstream)

        val feature = branches[1]
        assertEquals("feature/auth", feature.name)
        assertFalse(feature.isCurrent)
        assertFalse(feature.isRemote)

        val remote = branches[2]
        assertEquals("remotes/origin/main", remote.name)
        assertEquals("origin/main", remote.displayName)
        assertTrue(remote.isRemote)
    }

    @Test
    fun parsesGitLog() {
        val output =
            "a1b2c3d4e5f6\u0000a1b2c3d\u0000Jane Doe\u00002 hours ago\u0000Fix login crash\n" +
                    "b2c3d4e5f6a1\u0000b2c3d4e\u0000John Smith\u0000Yesterday\u0000Initial commit"

        val commits = parseGitLog(output)
        assertEquals(2, commits.size)

        assertEquals("a1b2c3d4e5f6", commits[0].hash)
        assertEquals("a1b2c3d", commits[0].shortHash)
        assertEquals("Jane Doe", commits[0].author)
        assertEquals("2 hours ago", commits[0].date)
        assertEquals("Fix login crash", commits[0].message)

        assertEquals("b2c3d4e5f6a1", commits[1].hash)
        assertEquals("Initial commit", commits[1].message)
    }

    @Test
    fun parsesGitRemotes() {
        val output = """
            origin	https://github.com/owner/cosmic-ide.git (fetch)
            origin	https://github.com/owner/cosmic-ide.git (push)
            upstream	https://github.com/upstream/cosmic-ide.git (fetch)
            upstream	https://github.com/upstream/cosmic-ide.git (push)
        """.trimIndent()

        val remotes = parseGitRemotes(output)
        assertEquals(2, remotes.size)

        assertEquals("origin", remotes[0].name)
        assertEquals("https://github.com/owner/cosmic-ide.git", remotes[0].fetchUrl)
        assertEquals("https://github.com/owner/cosmic-ide.git", remotes[0].pushUrl)

        assertEquals("upstream", remotes[1].name)
        assertEquals("https://github.com/upstream/cosmic-ide.git", remotes[1].fetchUrl)
    }
}
