/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.adapter

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import org.eclipse.jgit.revwalk.RevCommit

class GitAdapter : RecyclerView.Adapter<GitAdapter.ViewHolder>() {

    private val commits = mutableListOf<RevCommit>()

    fun updateCommits(commits: List<RevCommit>) {
        this.commits.clear()
        this.commits.addAll(commits)
        notifyItemRangeChanged(0, commits.size)
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
        holder.bind(commits[position])
    }

    override fun getItemCount() = commits.size

    inner class ViewHolder(
        val textView: TextView
    ) : RecyclerView.ViewHolder(textView) {

        fun bind(commit: RevCommit) {
            textView.textSize = 16f
            textView.text = commit.authorIdent.name + " - " + commit.shortMessage
        }
    }
}
