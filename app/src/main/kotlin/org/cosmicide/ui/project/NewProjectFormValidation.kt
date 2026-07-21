package org.cosmicide.ui.project

import java.io.File

internal data class NewProjectFormValidation(
    val normalizedName: String,
    val normalizedPackageName: String,
    val isNameInvalid: Boolean,
    val projectAlreadyExists: Boolean,
    val isPackageInvalid: Boolean
) {
    val isValid: Boolean
        get() = normalizedName.isNotEmpty() &&
                normalizedPackageName.isNotEmpty() &&
                !isNameInvalid &&
                !projectAlreadyExists &&
                !isPackageInvalid
}

internal fun validateNewProjectForm(
    state: NewProjectFormState,
    projectsDirectory: File
): NewProjectFormValidation {
    val normalizedName = state.name.trim()
    val normalizedPackageName = state.packageName.trim()
    val isNameInvalid = state.name.isNotEmpty() && !PROJECT_NAME.matches(normalizedName)
    val projectAlreadyExists = !isNameInvalid && normalizedName.isNotEmpty() &&
            projectsDirectory.resolve(normalizedName).exists()
    val isPackageInvalid = state.packageName.isNotEmpty() &&
            !PACKAGE_NAME.matches(normalizedPackageName)

    return NewProjectFormValidation(
        normalizedName = normalizedName,
        normalizedPackageName = normalizedPackageName,
        isNameInvalid = isNameInvalid,
        projectAlreadyExists = projectAlreadyExists,
        isPackageInvalid = isPackageInvalid
    )
}

private val PROJECT_NAME = Regex("[A-Za-z][A-Za-z0-9._-]*")
private val PACKAGE_NAME = Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*")
