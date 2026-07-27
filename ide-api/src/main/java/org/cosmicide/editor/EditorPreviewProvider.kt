/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, version 3 or later.
 */

package org.cosmicide.editor

import android.content.Context
import android.view.View
import org.cosmicide.plugin.api.ConfigurableExtension
import org.cosmicide.project.Project
import java.io.File

enum class EditorPreviewPresentation {
    CODE_AND_PREVIEW,
    PREVIEW_ONLY
}

data class EditorPreviewMatchRequest(
    val project: Project,
    val file: File
)

data class EditorPreviewRenderRequest(
    val context: Context,
    val project: Project,
    val file: File,
    val content: String?,
    val backgroundColor: Int,
    val contentColor: Int
)

interface EditorPreviewProvider : ConfigurableExtension {
    val priority: Int
        get() = 0

    val presentation: EditorPreviewPresentation
        get() = EditorPreviewPresentation.CODE_AND_PREVIEW

    fun supports(request: EditorPreviewMatchRequest): Boolean

    fun createView(request: EditorPreviewRenderRequest): View

    fun updateView(view: View, request: EditorPreviewRenderRequest) = Unit

    fun releaseView(view: View) = Unit
}
