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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.unnamed.b.atv.R
import com.widget.treeview.TreeUtils.toNodeList
import java.io.File

interface OnTreeItemClickListener {
    fun onItemClick(view: View, position: Int)
    fun onItemLongClick(view: View, position: Int)
}

class TreeViewAdapter(
    context: Context,
    private var nodes: MutableList<Node<File>>
) : RecyclerView.Adapter<TreeViewAdapter.ViewHolder>() {

    private val fileIcon = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.outline_insert_drive_file_24,
        context.theme
    )
    private val folderIcon = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.outline_folder_24,
        context.theme
    )
    private val chevronRightIcon = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.round_chevron_right_24,
        context.theme
    )!!
    private val expandMoreIcon = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.round_expand_more_24,
        context.theme
    )!!

    private var listener: OnTreeItemClickListener? = null

    fun setOnItemClickListener(listener: OnTreeItemClickListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val node = nodes[position]

        val indentation = node.depth * 36
        holder.itemView.setPaddingRelative(indentation, 0, 0, 0)

        if (node.value.isDirectory) {
            holder.expandView.setImageDrawable(if (!node.isExpanded) chevronRightIcon else expandMoreIcon)
            holder.fileView.setPadding(0)
            holder.fileView.setImageDrawable(folderIcon)
        } else {
            holder.expandView.setImageDrawable(null)
            holder.fileView.setPaddingRelative(chevronRightIcon.intrinsicWidth, 0, 0, 0)
            holder.fileView.setImageDrawable(fileIcon)
        }

        holder.textView.text = node.value.name

        holder.itemView.setOnClickListener {
            if (node.value.isDirectory) {
                toggleDirectory(node, position)
            }
            listener?.onItemClick(it, position)
        }

        holder.itemView.setOnLongClickListener {
            listener?.onItemLongClick(it, position)
            true
        }
    }

    private fun toggleDirectory(node: Node<File>, position: Int) {
        if (!node.isExpanded) {
            expandDirectory(node, position)
        } else {
            collapseDirectory(node, position)
        }
        notifyItemChanged(position)
    }

    private fun expandDirectory(node: Node<File>, position: Int) {
        var parent = node
        var children: List<Node<File>>
        var index = position
        var count = 0
        do {
            children = parent.value.toNodeList()
            nodes.addAll(index + 1, children)
            TreeUtils.addChildren(parent, children)

            if (children.isNotEmpty()) {
                parent = children[0]
                count += children.size
                index++
            }
        } while (children.size == 1 && children[0].value.isDirectory)
        notifyItemRangeInserted(position + 1, count)
    }

    private fun collapseDirectory(node: Node<File>, position: Int) {
        val descendants = TreeUtils.getDescendants(node)
        nodes.removeAll(descendants.toSet())
        TreeUtils.removeChildren(node)
        notifyItemRangeRemoved(position + 1, descendants.size)
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemCount() = nodes.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val expandView: ImageView = view.findViewById(R.id.expand)
        val fileView: ImageView = view.findViewById(R.id.file_view)
        val textView: TextView = view.findViewById(R.id.text_view)
    }
}
