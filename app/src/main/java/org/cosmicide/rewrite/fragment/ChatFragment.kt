/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pkslow.ai.AIClient
import com.pkslow.ai.GoogleBardClient
import com.pkslow.ai.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.rewrite.adapter.ConversationAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.databinding.FragmentChatBinding

class ChatFragment : BaseBindingFragment<FragmentChatBinding>() {
    private val conversationAdapter = ConversationAdapter()
    val client: AIClient =
        GoogleBardClient(
            "WwioM6QIAAtOsjpFrWTtle935KZySZOzVDxXGg6IrBezbtYb6RrMzFklQYi2QTJ80bo_Nw.",
            java.time.Duration.ofHours(1)
        ) // i hope you dont talk to it for more than an hour lmao
    // hi, if you were thinking of using this key, don't. It's free already, just get your own.

    override fun getViewBinding() = FragmentChatBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Prefs.useBardProxy) {
            NetworkUtils.setUpProxy(
                "198.199.86.11",
                "8080"
            ) // thanks to us-proxy.org for the free proxy
        }
        binding.recyclerview.apply {
            adapter = conversationAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        }
    }

    override fun onResume() {
        super.onResume()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.send.setOnClickListener {
            conversationAdapter.add(
                ConversationAdapter.Conversation(
                    binding.textinput.text.toString(),
                    "User"
                )
            )
            val text = binding.textinput.text.toString()
            binding.textinput.setText("")
            lifecycleScope.launch(Dispatchers.IO) {
                val answer = client.ask(text)
                lifecycleScope.launch(Dispatchers.Main) {
                    conversationAdapter.add(answer)
                }
            }
            binding.recyclerview.smoothScrollToPosition(conversationAdapter.itemCount - 1)
        }
    }
}

