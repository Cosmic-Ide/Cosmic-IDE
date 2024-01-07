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
import androidx.recyclerview.widget.RecyclerView
import com.unnamed.b.atv.R
import java.io.File

interface OnItemClickListener {
    fun onItemClick(v: View, position: Int)

    fun onItemLongClick(v: View, position: Int)
}

class TreeViewAdapter(
    val context: Context,
    var data: MutableList<Node<File>>
) : RecyclerView.Adapter<TreeViewAdapter.ViewHolder>() {

    private val icFile = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.outline_insert_drive_file_24,
        context.theme
    )
    private val icFolder = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.outline_folder_24,
        context.theme
    )
    private val icChevronRight = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.round_chevron_right_24,
        context.theme
    )
    private val icExpandMore = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.round_expand_more_24,
        context.theme
    )

    private var listener: OnItemClickListener? = null

    companion object {
        fun merge(root: File): MutableList<Node<File>> {
            // child files
            val list = root.listFiles()?.toMutableList() ?: return mutableListOf()
            // dir with sorted
            val dirs = list.filter { it.isDirectory }.sortedBy { it.name }
            // file with sorted
            val files = (list - dirs.toSet()).sortedBy { it.name }
            // file to node
            return (dirs + files).map { Node(it) }.toMutableList()
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val node = data[position]

        // set itemView margin
        holder.itemView.setPaddingRelative(node.level * 35, 0, 0, 0)

        if (node.value.isDirectory) {
            if (!node.isExpand) {
                holder.expandView.setImageDrawable(icChevronRight)
            } else {
                holder.expandView.setImageDrawable(icExpandMore)
            }

            holder.fileView.setPadding(0, 0, 0, 0)
            holder.fileView.setImageDrawable(icFolder)
        } else {
            // non-directory not show the expand icon
            holder.expandView.setImageDrawable(null)
            // padding
            holder.fileView.setPadding(icChevronRight!!.intrinsicWidth, 0, 0, 0)
            holder.fileView.setImageDrawable(icFile)
        }

        holder.textView.text = node.value.name

        holder.itemView.setOnClickListener {
            if (node.value.isDirectory) {
                var parent = node
                var child: List<Node<File>>
                // expand and collapsed
                if (!node.isExpand) {
                    var index = position
                    var count = 0
                    // only one child directory
                    do {
                        child = merge(parent.value)
                        data.addAll(index + 1, child)
                        TreeView.add(parent, child)

                        if (child.isNotEmpty()) {
                            parent = child[0]
                            count += child.size
                            index++
                        }
                    } while (child.size == 1 && child[0].value.isDirectory)
                    // refresh data
                    notifyItemRangeInserted(position + 1, count)
                } else {
                    child = TreeView.getChildren(parent)
                    data.removeAll(child.toSet())
                    TreeView.remove(parent, parent.child)
                    // refresh data
                    notifyItemRangeRemoved(position + 1, child.size)
                }

                // refresh data at position
                notifyItemChanged(position)
            }

            // callback
            listener?.onItemClick(it, position)
        }

        holder.itemView.setOnLongClickListener {
            // callback
            listener?.onItemLongClick(it, position)
            return@setOnLongClickListener true
        }
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemCount() = data.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val expandView: ImageView = v.findViewById(R.id.expand)
        val fileView: ImageView = v.findViewById(R.id.file_view)
        val textView: TextView = v.findViewById(R.id.text_view)
    }
}

