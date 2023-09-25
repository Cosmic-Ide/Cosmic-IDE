/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.adapter

import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.eclipse.jgit.api.Status

class StagingAdapter(val path: String) : RecyclerView.Adapter<StagingAdapter.ViewHolder>() {

    val files = mutableSetOf<File>()

    fun updateStatus(status: Status) {
        files.clear()
        status.apply {
            files.addAll(added.map { File(it, FileStatus.ADDED) })
            files.addAll(changed.map { File(it, FileStatus.CHANGED) })
            files.addAll(removed.map { File(it, FileStatus.REMOVED) })
            files.addAll(missing.map { File(it, FileStatus.MISSING) })
            files.addAll(modified.map { File(it, FileStatus.MODIFIED) })
            files.addAll(conflicting.map { File(it, FileStatus.CONFLICTING) })
            files.addAll(untracked.map { File(it, FileStatus.UNTRACKED) })
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            TextView(parent.context)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files.elementAt(position))
    }

    override fun getItemCount() = files.size

    inner class ViewHolder(
        val textView: TextView
    ) : RecyclerView.ViewHolder(textView) {

        fun bind(commit: File) {
            setColor(commit.status)
            textView.textSize = 12f
            textView.text = commit.path.substringAfter(path)
        }

        fun setColor(status: FileStatus) {
            textView.setTextColor(
                when (status) {
                    FileStatus.ADDED -> Color.GREEN
                    FileStatus.CHANGED, FileStatus.MODIFIED -> Color.YELLOW
                    FileStatus.REMOVED, FileStatus.MISSING, FileStatus.CONFLICTING -> Color.RED
                    FileStatus.UNTRACKED -> Color.WHITE
                }
            )
        }
    }

    data class File(
        val path: String,
        val status: FileStatus
    )

    enum class FileStatus {
        ADDED, CHANGED, REMOVED, MISSING, MODIFIED, CONFLICTING, UNTRACKED
    }
}
