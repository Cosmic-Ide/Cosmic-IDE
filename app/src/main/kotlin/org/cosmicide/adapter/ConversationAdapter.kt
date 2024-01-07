/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */
package org.cosmicide.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.databinding.ConversationItemReceivedBinding
import org.cosmicide.databinding.ConversationItemSentBinding
import org.cosmicide.util.CommonUtils

class ConversationAdapter :
    RecyclerView.Adapter<BindableViewHolder<ConversationAdapter.Conversation, *>>() {

    private val conversations = mutableListOf<Conversation>()

    data class Conversation(
        var text: String = "",
        val author: String = "assistant",
        val flow: Flow<GenerateContentResponse>? = null,
        var finished: Boolean = false
    ) {
        init {
            if (flow != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    flow.collect {
                        if (finished) return@collect
                        text = it.text!!
                    }
                }
            }
        }
    }

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    fun add(conversation: Conversation) {
        if (conversations.size != 0)
            conversations.last().finished = true
        conversations += conversation
        notifyItemInserted(conversations.lastIndex)
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
        return if (conversation.author == "user") {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    fun clear() {
        conversations.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    inner class SentViewHolder(itemBinding: ConversationItemSentBinding) :
        BindableViewHolder<Conversation, ConversationItemSentBinding>(itemBinding) {

        override fun bind(data: Conversation) {
            binding.message.apply {
                CommonUtils.getMarkwon().setMarkdown(this, data.text)
            }
        }
    }

    inner class ReceivedViewHolder(itemBinding: ConversationItemReceivedBinding) :
        BindableViewHolder<Conversation, ConversationItemReceivedBinding>(itemBinding) {

        override fun bind(data: Conversation) {
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                if (data.text.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        stream(data.text)
                    }
                }
                data.flow!!.collect {
                    if (data.finished) return@collect

                    withContext(Dispatchers.Main) {
                        stream(it.text!!)
                    }
                }
            }
        }

        fun stream(text: String) {
            binding.message.apply {
                CommonUtils.getMarkwon().setMarkdown(this, binding.message.text.toString() + text)
            }
        }
    }
}
