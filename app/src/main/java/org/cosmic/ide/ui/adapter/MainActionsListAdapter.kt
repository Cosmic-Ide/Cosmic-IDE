package org.cosmic.ide.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.cosmic.ide.databinding.LayoutMainActionItemBinding
import org.cosmic.ide.ui.model.MainScreenAction

class MainActionsListAdapter
@JvmOverloads
constructor(val actions: List<MainScreenAction> = emptyList()) :
    RecyclerView.Adapter<MainActionsListAdapter.ViewHolder>() {
    class ViewHolder(val binding: LayoutMainActionItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutMainActionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val action = getAction(position)
        val binding = holder.binding

        binding.root.apply {
            setText(action.text)
            setIconResource(action.icon)
            setOnClickListener(action.onClick)
        }
    }

    override fun getItemCount(): Int = actions.size

    fun getAction(index: Int) = actions[index]
}