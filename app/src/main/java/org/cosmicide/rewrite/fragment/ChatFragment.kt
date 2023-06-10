/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pkslow.ai.AIClient
import com.pkslow.ai.GoogleBardClient
import com.pkslow.ai.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.ConversationAdapter
import org.cosmicide.rewrite.chat.ChatProvider
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.databinding.FragmentChatBinding
import java.time.Duration

class ChatFragment : BaseBindingFragment<FragmentChatBinding>() {

    private val conversationAdapter = ConversationAdapter()
    private var model = Models.ORA

    // The client will expire in an hour.
    // If you were thinking of using this key, don't. It's free already, just get your own.
    private val client: AIClient by lazy {
        GoogleBardClient(
            "WwioM6QIAAtOsjpFrWTtle935KZySZOzVDxXGg6IrBezbtYb6RrMzFklQYi2QTJ80bo_Nw.",
            Duration.ofHours(1)
        )
    }

    override fun getViewBinding() = FragmentChatBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = "Chat"
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.model_bard -> {
                    model = Models.BARD
                    true
                }

                R.id.model_vercel -> {
                    model = Models.VERCEL_GPT3_5
                    true
                }

                R.id.model_ora -> {
                    model = Models.ORA
                    true
                }

                R.id.model_bing -> {
                    model = Models.BING_GPT4
                    true
                }

                R.id.model_yqcloud -> {
                    model = Models.YQCLOUD
                    true
                }

                R.id.model_forefront -> {
                    model = Models.FOREFRONT
                    true
                }

                else -> false
            }
        }
        if (model == Models.BARD && Prefs.useBardProxy) {
            // Thanks to us-proxy.org for the free proxy.
            NetworkUtils.setUpProxy("198.199.86.11", "8080")
        }
        setOnClickListeners()
        setupRecyclerView()
    }

    private fun setOnClickListeners() {
        binding.sendMessageButtonIcon.setOnClickListener {
            val message = binding.messageText.text.toString().trim()
            if (message.isEmpty()) {
                return@setOnClickListener
            }
            val conversation = ConversationAdapter.Conversation(message, "User")
            conversationAdapter.add(conversation)
            binding.messageText.setText("")
            lifecycleScope.launch(Dispatchers.IO) {
                val reply: String = when (model) {
                    Models.BARD -> client.ask(message).markdown()
                    Models.VERCEL_GPT3_5 -> ChatProvider.generate(
                        "vercel",
                        conversationAdapter.getConversations()
                    )

                    Models.ORA -> ChatProvider.generate(
                        "ora",
                        conversationAdapter.getConversations()
                    )

                    Models.BING_GPT4 -> ChatProvider.generate(
                        "bing",
                        conversationAdapter.getConversations()
                    )

                    Models.YQCLOUD -> ChatProvider.generate(
                        "yqcloud",
                        conversationAdapter.getConversations()
                    )

                    Models.FOREFRONT -> ChatProvider.generate(
                        "forefront",
                        conversationAdapter.getConversations()
                    )
                }
                val response = ConversationAdapter.Conversation(reply)
                withContext(Dispatchers.Main) {
                    conversationAdapter.add(response)
                    binding.recyclerview.smoothScrollToPosition(conversationAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerview.apply {
            adapter = conversationAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val verticalOffset = 4.dp
                    outRect.top = verticalOffset
                    outRect.bottom = verticalOffset
                }
            })
        }
    }
}

inline val Int.dp: Int
    get() = (Resources.getSystem().displayMetrics.density * this + 0.5f).toInt()

enum class Models {
    BARD,
    BING_GPT4,
    VERCEL_GPT3_5,
    YQCLOUD,
    FOREFRONT,
    ORA
}