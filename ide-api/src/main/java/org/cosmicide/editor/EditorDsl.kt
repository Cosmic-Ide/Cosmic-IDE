/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.editor

import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmicide.plugin.api.ConfigurableExtension
import org.cosmicide.plugin.api.Disposable
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.register
import org.cosmicide.project.Project
import java.io.File

/** Context provided when the user long-clicks or right-clicks in the editor. */
interface EditorContextMenuContext {
    val editor: CodeEditor
    val project: Project
    val file: File
    val selectedText: String
    val line: Int
    val column: Int
}

data class EditorContextMenuItem(
    val id: String,
    val label: String,
    val icon: String? = null,
    val onClick: (EditorContextMenuContext) -> Unit
)

interface EditorContextMenuProvider : ConfigurableExtension {
    fun menuItems(context: EditorContextMenuContext): List<EditorContextMenuItem>
}

/** Context provided to custom floating text action items (selection window). */
interface EditorTextActionContext {
    val editor: CodeEditor
    val selectedText: String
    val project: Project
    val file: File
}

data class EditorTextActionItem(
    val id: String,
    val label: String,
    val onClick: (EditorTextActionContext) -> Unit
)

interface EditorTextActionProvider : ConfigurableExtension {
    fun textActions(context: EditorTextActionContext): List<EditorTextActionItem>
}

/** Allows plugins to subscribe to Sora Editor events when an editor instance is created or opened. */
interface EditorEventSubscriber : ConfigurableExtension {
    fun onEditorCreated(editor: CodeEditor, project: Project, file: File, disposable: Disposable)
}

/**
 * Registers an editor toolbar action in 1 line.
 */
fun PluginContext.registerEditorAction(
    id: String,
    label: String,
    icon: String? = null,
    description: String = "",
    priority: Int = 350,
    onClick: (EditorActionContext) -> Unit
): Disposable {
    val provider = object : EditorActionProvider {
        override val id: String = id
        override val displayName: String = label

        override fun actions(project: Project, file: File?): List<EditorAction> {
            return listOf(
                EditorAction(
                    id = id,
                    label = label,
                    description = description,
                    icon = icon,
                    onClick = onClick
                )
            )
        }
    }
    return register(EditorExtensionPoints.EDITOR_ACTION_PROVIDER, provider, priority)
}

/**
 * Registers a context menu (long-click/right-click) action in 1 line.
 */
fun PluginContext.registerContextMenuAction(
    id: String,
    label: String,
    icon: String? = null,
    priority: Int = 350,
    onClick: (EditorContextMenuContext) -> Unit
): Disposable {
    val provider = object : EditorContextMenuProvider {
        override val id: String = id
        override val displayName: String = label

        override fun menuItems(context: EditorContextMenuContext): List<EditorContextMenuItem> {
            return listOf(
                EditorContextMenuItem(
                    id = id,
                    label = label,
                    icon = icon,
                    onClick = onClick
                )
            )
        }
    }
    return register(EditorExtensionPoints.CONTEXT_MENU_PROVIDER, provider, priority)
}

/**
 * Registers a floating text action (selection window) in 1 line.
 */
fun PluginContext.registerTextAction(
    id: String,
    label: String,
    priority: Int = 350,
    onClick: (EditorTextActionContext) -> Unit
): Disposable {
    val provider = object : EditorTextActionProvider {
        override val id: String = id
        override val displayName: String = label

        override fun textActions(context: EditorTextActionContext): List<EditorTextActionItem> {
            return listOf(
                EditorTextActionItem(
                    id = id,
                    label = label,
                    onClick = onClick
                )
            )
        }
    }
    return register(EditorExtensionPoints.TEXT_ACTION_PROVIDER, provider, priority)
}

/**
 * Subscribes to Sora Editor events when an editor is created or opened.
 */
fun PluginContext.subscribeEditorEvents(
    id: String = "${descriptor.id}.editorEvents",
    priority: Int = 350,
    onCreated: (editor: CodeEditor, project: Project, file: File, disposable: Disposable) -> Unit
): Disposable {
    val subscriber = object : EditorEventSubscriber {
        override val id: String = id
        override val displayName: String = "Editor Event Subscriber"

        override fun onEditorCreated(
            editor: CodeEditor,
            project: Project,
            file: File,
            disposable: Disposable
        ) {
            onCreated(editor, project, file, disposable)
        }
    }
    return register(EditorExtensionPoints.EVENT_SUBSCRIBER, subscriber, priority)
}
