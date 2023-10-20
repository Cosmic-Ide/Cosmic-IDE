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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.R
import org.cosmicide.adapter.ConversationAdapter
import org.cosmicide.chat.ChatProvider
import org.cosmicide.databinding.FragmentChatBinding
import org.cosmicide.extension.getDip
import org.cosmicide.rewrite.common.BaseBindingFragment

class ChatFragment : BaseBindingFragment<FragmentChatBinding>() {

    private val conversationAdapter = ConversationAdapter()
    private var model = Models.GPT_35_TURBO

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
                R.id.model_gpt35turbo -> {
                    model = Models.GPT_35_TURBO
                    "GPT-3.5 Turbo"
                }

                R.id.model_gpt3516k -> {
                    model = Models.GPT_3516K
                    "GPT-3.5 16k"
                }

                R.id.model_gpt35turbo0613 -> {
                    model = Models.GPT_35_TURBO_0613
                    "GPT-3.5 Turbo 0613"
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
            val conversation = ConversationAdapter.Conversation(message, "user")
            conversationAdapter.add(conversation)
            binding.messageText.setText("")
            lifecycleScope.launch(Dispatchers.IO) {
                val reply = when (model) {
                    Models.GPT_35_TURBO_0613 -> ChatProvider.generate(
                        "gpt-3.5-turbo-0613",
                        conversationAdapter.getConversations()
                    )

                    Models.GPT_35_TURBO -> ChatProvider.generate(
                        "gpt-3.5-turbo",
                        conversationAdapter.getConversations()
                    )

                    Models.GPT_3516K -> ChatProvider.generate(
                        "gpt-3.5-16k",
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
    GPT_35_TURBO,
    GPT_3516K,
    GPT_35_TURBO_0613,
}

private val Int.dp: Int
    get() = (Resources.getSystem().displayMetrics.density * this + 0.5f).toInt()
