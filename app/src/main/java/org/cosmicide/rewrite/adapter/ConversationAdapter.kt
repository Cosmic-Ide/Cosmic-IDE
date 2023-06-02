/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.adapter

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.pkslow.ai.domain.Answer
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin
import org.cosmicide.rewrite.databinding.ConversationItemBinding

class ConversationAdapter :
    RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    private val conversations = mutableListOf<Conversation>()

    data class Conversation(
        val text: String,
        val author: String = "Bot",
    )

    fun add(conversation: Conversation) {
        this.conversations += conversation
        notifyItemInserted(conversations.size - 1)
    }

    fun add(answer: Answer) {
        add(Conversation(answer.markdown()))
    }

    fun getConversations(): List<Map<String, String>> {
        val convos = mutableListOf<Map<String, String>>()
        conversations.forEach {
            convos.add(mapOf("text" to it.text, "author" to it.author))
        }
        return convos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ConversationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size

    class ViewHolder(
        private val binding: ConversationItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(convo: Conversation) {
            binding.textview.apply {
                val markwon = Markwon.builder(context)
                    .usePlugin(LinkifyPlugin.create())
                    .build()
                movementMethod = LinkMovementMethod.getInstance()
                text = markwon.toMarkdown(convo.text)
            }
            binding.root.apply {
                if (convo.author == "User") {
                    setBackgroundColor(
                        MaterialColors.getColor(
                            this,
                            com.google.android.material.R.attr.colorSurface
                        )
                    )
                }
                /*setOnLongClickListener {
                    PopupMenu(context, this).apply {
                        inflate(R.menu.bard_response_menu)
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.view_draft_1 -> {

                                    true
                                }
                                R.id.view_draft_2 -> {
                                    binding.textview.text.toString().copyToClipboard(context)
                                    true
                                }
                                R.id.view_draft_3 -> {
                                    binding.textview.text.toString().copyToClipboard(context)
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show(
                    true
                }*/
            }
        }
    }
}