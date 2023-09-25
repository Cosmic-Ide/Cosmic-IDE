/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.pkslow.ai.AIClient
import com.pkslow.ai.GoogleBardClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.cosmicide.R
import org.cosmicide.adapter.ConversationAdapter
import org.cosmicide.chat.ChatProvider
import org.cosmicide.databinding.FragmentChatBinding
import org.cosmicide.extension.getDip
import org.cosmicide.rewrite.common.BaseBindingFragment
import java.time.Duration

class ChatFragment : BaseBindingFragment<FragmentChatBinding>() {

    private val conversationAdapter = ConversationAdapter()
    private var model = Models.FALCON_40B

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
        setupUI(view.context)
        setOnClickListeners()
        setupRecyclerView()
        binding.messageText.requestFocus()
    }

    private fun setupUI(context: Context) {
        initToolbar()
        initBackground(context)
        binding.toolbar.title = model.name
    }

    private fun initToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.setOnMenuItemClickListener {

            if (it.itemId == R.id.clear) {
                conversationAdapter.clear()
                binding.recyclerview.invalidate()
                return@setOnMenuItemClickListener false
            }
            val modelName = when (it.itemId) {
                R.id.model_bard -> {
                    model = Models.BARD
                    "Google Bard"
                }

                R.id.model_aiservice -> {
                    model = Models.AISERVICE
                    "AI Service"
                }

                R.id.model_wewordle -> {
                    model = Models.WEWORDLE
                    "WeWordle"
                }

                R.id.model_falcon_40b -> {
                    model = Models.FALCON_40B
                    "falcon-40b"
                }

                R.id.model_falcon_7b -> {
                    model = Models.FALCON_7B
                    "falcon-7b"
                }

                R.id.model_llama_13b -> {
                    model = Models.LLAMA_13B
                    "llama-13b"
                }

                R.id.model_theb -> {
                    model = Models.THEB
                    "TheB"
                }

                else -> return@setOnMenuItemClickListener false
            }

            binding.toolbar.title = modelName
            true
        }
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
            lifecycleScope.async {
                val reply = when (model) {
                    Models.BARD -> client.ask(message).markdown()

                    Models.AISERVICE -> ChatProvider.generate(
                        "aiservice",
                        conversationAdapter.getConversations()
                    )

                    Models.WEWORDLE -> ChatProvider.generate(
                        "wewordle",
                        conversationAdapter.getConversations()
                    )

                    Models.FALCON_40B -> ChatProvider.generate(
                        "h2o/falcon-40b",
                        conversationAdapter.getConversations()
                    )

                    Models.FALCON_7B -> ChatProvider.generate(
                        "h2o/falcon-7b",
                        conversationAdapter.getConversations()
                    )

                    Models.LLAMA_13B -> ChatProvider.generate(
                        "h2o/llama-13b",
                        conversationAdapter.getConversations()
                    )

                    Models.THEB -> ChatProvider.generate(
                        "theb",
                        conversationAdapter.getConversations()
                    )
                }
                val response = ConversationAdapter.Conversation(reply)
                withContext(Dispatchers.Main) {
                    conversationAdapter.add(response)
                    binding.recyclerview.scrollToPosition(conversationAdapter.itemCount - 1)
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
                    val verticalOffset = 8.dp
                    outRect.top = verticalOffset
                    outRect.bottom = verticalOffset
                }
            })
        }
    }

    private fun initBackground(context: Context) {
        val shapeAppearance = ShapeAppearanceModel.builder().setAllCornerSizes(context.getDip(24f)).build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearance).apply {
            initializeElevationOverlay(context)
            fillColor = ColorStateList.valueOf(
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorSurface,
                    0
                )
            )
            elevation = 6f
        }
        binding.chatLayout.background = shapeDrawable
        binding.toolbar.background = shapeDrawable
    }
}

enum class Models {
    BARD,
    AISERVICE,
    WEWORDLE,
    FALCON_40B,
    FALCON_7B,
    LLAMA_13B,
    THEB
}

private val Int.dp: Int
    get() = (Resources.getSystem().displayMetrics.density * this + 0.5f).toInt()