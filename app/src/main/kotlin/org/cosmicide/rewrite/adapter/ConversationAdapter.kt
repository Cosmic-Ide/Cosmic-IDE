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
import com.pkslow.ai.domain.Answer
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin
import org.cosmicide.rewrite.databinding.ConversationItemReceivedBinding
import org.cosmicide.rewrite.databinding.ConversationItemSentBinding


class ConversationAdapter :
    RecyclerView.Adapter<BindableViewHolder<ConversationAdapter.Conversation, *>>() {

    private val conversations = mutableListOf<Conversation>()

    data class Conversation(val text: String, val author: String = "Bot")

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    fun add(conversation: Conversation) {
        conversations += conversation
        notifyItemInserted(conversations.lastIndex)
    }

    fun add(answer: Answer) {
        add(Conversation(answer.markdown()))
    }

    fun getConversations(): List<Map<String, String>> {
        return conversations.map { mapOf("text" to it.text, "author" to it.author) }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BindableViewHolder<Conversation, *> {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> SentViewHolder(
                ConversationItemSentBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            VIEW_TYPE_RECEIVED -> ReceivedViewHolder(
                ConversationItemReceivedBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BindableViewHolder<Conversation, *>, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size

    override fun getItemViewType(position: Int): Int {
        val conversation = conversations[position]
        return if (conversation.author == "User") {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    fun clear() {
        conversations.clear()
        notifyDataSetChanged()
    }

    inner class SentViewHolder(itemBinding: ConversationItemSentBinding) :
        BindableViewHolder<Conversation, ConversationItemSentBinding>(itemBinding) {

        private val markwon: Markwon by lazy {
            Markwon.builder(binding.message.context)
                .usePlugin(LinkifyPlugin.create())
                .build()
        }

        override fun bind(data: Conversation) {
            binding.message.apply {
                movementMethod = LinkMovementMethod.getInstance()
                markwon.setMarkdown(this, data.text)
            }
        }
    }

    inner class ReceivedViewHolder(itemBinding: ConversationItemReceivedBinding) :
        BindableViewHolder<Conversation, ConversationItemReceivedBinding>(itemBinding) {

        private val markwon: Markwon by lazy {
            Markwon.builder(binding.message.context)
                .usePlugin(LinkifyPlugin.create())
                .build()
        }

        override fun bind(data: Conversation) {
            binding.message.apply {
                movementMethod = LinkMovementMethod.getInstance()
                markwon.setMarkdown(this, data.text)
            }
        }
    }
}