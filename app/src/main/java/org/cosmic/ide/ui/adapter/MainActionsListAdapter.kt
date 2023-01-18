package org.cosmic.ide.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.cosmic.ide.databinding.LayoutMainActionItemBinding
import org.cosmic.ide.ui.model.MainScreenAction

class MainActionsListAdapter
@JvmOverloads
constructor(val actions: List<MainScreenAction> = emptyList()) :
    RecyclerView.Adapter<ActionsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ActionsViewHolder.from(parent)

    override fun onBindViewHolder(holder: ActionsViewHolder, position: Int) {
        val action = getAction(position)
        holder.bind(action)
    }

    override fun getItemCount() = actions.size

    fun getAction(index: Int) = actions[index]
}

class ActionsViewHolder private constructor(private val binding: LayoutMainActionItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(action: MainScreenAction) {
        binding.root.apply {
            setText(action.text)
            setIconResource(action.icon)
            setOnClickListener(action.onClick)
        }
    }

    companion object {
        fun from(parent: ViewGroup) =
            ActionsViewHolder(LayoutMainActionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
}