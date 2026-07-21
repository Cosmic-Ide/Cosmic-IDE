package org.cosmicide.ui.project

import org.cosmicide.project.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class GradleProjectCreatorTest {
    @Test
    fun `builds deterministic Gradle init arguments`() {
        val arguments = gradleInitArguments(
            GradleProjectCreationRequest(
                language = Language.Kotlin,
                name = "sample",
                packageName = "com.example.sample",
                dslType = DslType.KOTLIN,
                splitProject = false,
                testFramework = TestFramework.JUNIT
            )
        )

        assertEquals("init", arguments.first())
        assertTrue("--type=kotlin-application" in arguments)
        assertTrue("--dsl=kotlin" in arguments)
        assertTrue("--no-split-project" in arguments)
        assertTrue("--test-framework=junit-jupiter" in arguments)
    }

    @Test
    fun `validates normalized project and package names`() {
        val root = Files.createTempDirectory("cosmic-projects").toFile()
        try {
            val validation = validateNewProjectForm(
                state = NewProjectFormState(
                    name = " Sample-App ",
                    packageName = " com.example.app "
                ),
                projectsDirectory = root
            )

            assertEquals("Sample-App", validation.normalizedName)
            assertEquals("com.example.app", validation.normalizedPackageName)
            assertTrue(validation.isValid)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `rejects invalid and duplicate project forms`() {
        val root = Files.createTempDirectory("cosmic-projects").toFile()
        try {
            root.resolve("Existing").mkdir()
            assertFalse(
                validateNewProjectForm(
                    NewProjectFormState(name = "../bad", packageName = "Bad.Package"),
                    root
                ).isValid
            )
            assertFalse(
                validateNewProjectForm(
                    NewProjectFormState(name = "Existing", packageName = "com.example"),
                    root
                ).isValid
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `language selection resets the test framework to a compatible default`() {
        val javaForm = NewProjectFormState(
            language = Language.Java,
            testFramework = TestFramework.TESTNG
        )

        val scalaForm = javaForm.selectLanguage(Language.Scala)

        assertEquals(Language.Scala, scalaForm.language)
        assertEquals(TestFramework.SCALATEST, scalaForm.testFramework)
        assertEquals(listOf(TestFramework.SCALATEST), scalaForm.availableTestFrameworks)
    }

    @Test
    fun `creation request uses normalized values and selected options`() {
        val form = NewProjectFormState(
            name = " Sample ",
            packageName = " com.example.sample ",
            language = Language.Java,
            dslType = DslType.GROOVY,
            splitProject = true,
            testFramework = TestFramework.TESTNG
        )
        val validation = NewProjectFormValidation(
            normalizedName = "Sample",
            normalizedPackageName = "com.example.sample",
            isNameInvalid = false,
            projectAlreadyExists = false,
            isPackageInvalid = false
        )

        val request = form.toCreationRequest(validation)

        assertEquals("Sample", request.name)
        assertEquals("com.example.sample", request.packageName)
        assertEquals(DslType.GROOVY, request.dslType)
        assertTrue(request.splitProject)
        assertEquals(TestFramework.TESTNG, request.testFramework)
    }
}
