package org.cosmicide.ui.project

import org.cosmicide.project.Language

internal enum class DslType(val gradleValue: String) {
    KOTLIN("kotlin"),
    GROOVY("groovy")
}

internal enum class TestFramework(val gradleValue: String, val displayName: String) {
    JUNIT("junit-jupiter", "JUnit"),
    TESTNG("testng", "TestNG"),
    KotlinTest("kotlintest", "KotlinTest"),
    SCALATEST("scalatest", "ScalaTest")
}

internal data class NewProjectFormState(
    val name: String = "",
    val packageName: String = "",
    val language: Language = Language.Java,
    val dslType: DslType = DslType.GROOVY,
    val splitProject: Boolean = false,
    val testFramework: TestFramework = defaultTestFramework(language)
) {
    val availableTestFrameworks: List<TestFramework>
        get() = testFrameworksFor(language)

    fun selectLanguage(language: Language): NewProjectFormState = copy(
        language = language,
        testFramework = defaultTestFramework(language)
    )

}

internal fun testFrameworksFor(language: Language): List<TestFramework> = when (language) {
    Language.Java -> listOf(TestFramework.JUNIT, TestFramework.TESTNG)
    Language.Kotlin -> listOf(TestFramework.JUNIT, TestFramework.KotlinTest)
    Language.Scala -> listOf(TestFramework.SCALATEST)
    is Language.Custom -> emptyList()
}

private fun defaultTestFramework(language: Language): TestFramework =
    testFrameworksFor(language).firstOrNull()
        ?: error("${language.name} projects are created by their installed plugin")
