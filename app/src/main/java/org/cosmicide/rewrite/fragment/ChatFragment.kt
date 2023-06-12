/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

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
        setupUI(view.context)
        setOnClickListeners()
        setupRecyclerView()
    }

    private fun setupUI(context: Context) {
        initToolbar()
        initBackground(context)
        if (model == Models.BARD && Prefs.useBardProxy) {
            // Thanks to us-proxy.org for the free proxy.
            NetworkUtils.setUpProxy("198.199.86.11", "8080")
        }
    }

    private fun initToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_ios_24)
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.setOnMenuItemClickListener {
            val modelName = when (it.itemId) {
                R.id.model_bard -> {
                    model = Models.BARD
                    "Google Bard"
                }

                R.id.model_vercel -> {
                    model = Models.VERCEL_GPT3_5
                    "Vercel GPT-3.5"
                }

                R.id.model_ora -> {
                    model = Models.ORA
                    "Ora"
                }

                R.id.model_yqcloud -> {
                    model = Models.YQCLOUD
                    "YqCloud"
                }


                R.id.model_phind -> {
                    model = Models.PHIND
                    "Phind"
                }


                else -> return@setOnMenuItemClickListener false
            }

            if (it.itemId == R.id.clear) {
                conversationAdapter.clear()
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
            lifecycleScope.launch(Dispatchers.IO) {
                val reply = when (model) {
                    Models.BARD -> client.ask(message).markdown()
                    Models.VERCEL_GPT3_5 -> ChatProvider.generate(
                        "vercel",
                        conversationAdapter.getConversations()
                    )

                    Models.ORA -> ChatProvider.generate(
                        "ora",
                        conversationAdapter.getConversations()
                    )

                    Models.YQCLOUD -> ChatProvider.generate(
                        "yqcloud",
                        conversationAdapter.getConversations()
                    )

                    Models.PHIND -> ChatProvider.generate(
                        "phind",
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
                    val verticalOffset = 6.dp
                    outRect.top = verticalOffset
                    outRect.bottom = verticalOffset
                }
            })
        }
    }

    private fun initBackground(context: Context) {
        val shapeAppearance = ShapeAppearanceModel.builder().setAllCornerSizes(24f).build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearance).apply {
            initializeElevationOverlay(context)
            fillColor = ColorStateList.valueOf(
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorSurface,
                    0
                )
            )
        }
        binding.chatLayout.background = shapeDrawable
        binding.toolbar.background = shapeDrawable
    }
}

enum class Models {
    BARD,
    VERCEL_GPT3_5,
    YQCLOUD,
    ORA,
    PHIND,
}

private val Int.dp: Int
    get() = (Resources.getSystem().displayMetrics.density * this + 0.5f).toInt()