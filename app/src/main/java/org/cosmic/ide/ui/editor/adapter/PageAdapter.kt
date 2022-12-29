package org.cosmic.ide.ui.editor.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.cosmic.ide.fragment.CodeEditorFragment.Companion.newInstance
import java.io.File

class PageAdapter(fm: FragmentManager?, lifecycle: Lifecycle?) : FragmentStateAdapter(
    fm!!, lifecycle!!
) {
    private val data: MutableList<File> = ArrayList()
    fun submitList(files: List<File>) {
        val result = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return data.size
                }

                override fun getNewListSize(): Int {
                    return files.size
                }

                override fun areItemsTheSame(
                    oldItemPosition: Int, newItemPosition: Int
                ): Boolean {
                    return data[oldItemPosition] == files[newItemPosition]
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int, newItemPosition: Int
                ): Boolean {
                    return data[oldItemPosition] == files[newItemPosition]
                }
            })
        data.clear()
        data.addAll(files)
        result.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun createFragment(p1: Int): Fragment {
        return newInstance(data[p1])
    }

    override fun getItemId(position: Int): Long {
        return if (data.isEmpty() || position > data.size) {
            -1
        } else data[position].absolutePath.hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        for (file in data) {
            if (file.absolutePath.hashCode().toLong() == itemId) {
                return true
            }
        }
        return false
    }
}