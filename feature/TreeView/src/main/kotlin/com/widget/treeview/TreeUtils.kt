/*
 * Copyright © 2022 Github Lzhiyong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.widget.treeview

import java.io.File

data class Node<T>(
    var value: T,
    var parent: Node<T>? = null,
    var children: MutableList<Node<T>> = mutableListOf(),
    var isExpanded: Boolean = false,
    var depth: Int = 0
) {
    override fun hashCode(): Int {
        return value.hashCode()
    }
}

object TreeUtils {

    fun File.toNodeList(): MutableList<Node<File>> {
        val files = listFiles()?.toMutableList() ?: return mutableListOf()
        val dirs = files.filter { it.isDirectory }.sortedBy { it.name }
        val remainingFiles = (files - dirs.toSet()).sortedBy { it.name }
        return (dirs + remainingFiles).map { Node(it) }.toMutableList()
    }

    fun <T> addChildren(
        parent: Node<T>,
        children: List<Node<T>>
    ) { // Removed default value for slight performance gain
        if (children.isNotEmpty()) {
            parent.isExpanded = true
            parent.children.addAll(children)
            children.forEach {
                it.parent = parent
                it.depth = parent.depth + 1
            }
        }
    }

    fun <T> removeChildren(parent: Node<T>) {
        if (parent.children.isNotEmpty()) {
            parent.isExpanded = false
            parent.children.forEach { child ->
                child.parent = null
                child.depth = 0
                if (child.isExpanded) {
                    child.isExpanded = false
                    removeChildren(child)
                }
            }
            parent.children.clear()
        }
    }

    fun <T> getDescendants(parent: Node<T>): List<Node<T>> {
        val descendants = mutableListOf<Node<T>>()
        getDescendantsRecursive(parent, descendants)
        return descendants
    }

    private fun <T> getDescendantsRecursive(parent: Node<T>, descendants: MutableList<Node<T>>) {
        descendants.addAll(parent.children)
        parent.children.forEach {
            if (it.isExpanded) {
                getDescendantsRecursive(it, descendants)
            }
        }
    }
}
