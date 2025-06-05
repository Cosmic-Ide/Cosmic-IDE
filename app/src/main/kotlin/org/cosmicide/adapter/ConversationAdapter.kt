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
import com.google.genai.ResponseStream
import com.google.genai.types.GenerateContentResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.databinding.ConversationItemReceivedBinding
import org.cosmicide.databinding.ConversationItemSentBinding
import org.cosmicide.util.CommonUtils
import java.util.concurrent.CompletableFuture

class ConversationAdapter :
    RecyclerView.Adapter<BindableViewHolder<ConversationAdapter.Conversation, *>>() {

    private val conversations = mutableListOf<Conversation>()

    data class Conversation(
        var text: String = "",
        val author: String = "assistant",
        val stream: CompletableFuture<ResponseStream<GenerateContentResponse>>? = null,
        var finished: Boolean = false
    )

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    fun add(conversation: Conversation) {
        if (conversations.isNotEmpty())
            conversations.last().finished = true
        conversations += conversation
        notifyItemInserted(conversations.lastIndex)
    }

    fun getConversations(): List<Pair<String, String>> {
        return conversations.map { Pair(it.author, it.text) }
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
            // Display existing text immediately
            if (data.text.isNotEmpty()) {
                binding.message.apply {
                    CommonUtils.getMarkwon().setMarkdown(this, data.text)
                }
            }

            // Only process the stream if it's not finished yet
            if (!data.finished && data.stream != null) {
                processStream(data)
            }
        }

        private fun processStream(data: Conversation) {
            // Create a dedicated coroutine to handle the stream processing
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Wait for the CompletableFuture to complete
                    val responseStream = data.stream?.get() ?: return@launch

                    var accumulatedText = data.text

                    // Process each response in the stream
                    for (response in responseStream) {
                        if (data.finished) break

                        val newText = response.text() ?: ""
                        if (newText.isNotEmpty()) {
                            accumulatedText += newText

                            // Update UI on the main thread
                            withContext(Dispatchers.Main) {
                                data.text = accumulatedText
                                binding.message.apply {
                                    CommonUtils.getMarkwon().setMarkdown(this, accumulatedText)
                                }
                            }
                        }
                    }

                    // Mark as finished once complete
                    withContext(Dispatchers.Main) {
                        data.finished = true
                    }
                } catch (e: Exception) {
                    // Handle errors on the main thread
                    withContext(Dispatchers.Main) {
                        val errorMsg = "Error: ${e.message}"
                        data.text += errorMsg
                        data.finished = true
                        binding.message.apply {
                            CommonUtils.getMarkwon().setMarkdown(this, data.text)
                        }
                    }
                }
            }
        }
    }
}
