/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * An abstract class for creating view holders that can bind data to their views.
 *
 * @param T the type of data that the view holder can bind to its views
 * @param B the type of view binding used by the view holder
 */
abstract class BindableViewHolder<T, B : ViewBinding>(protected val binding: B) : RecyclerView.ViewHolder(binding.root) {
    /**
     * Binds the given data to the views in the view holder.
     *
     * @param data the data to bind
     */
    abstract fun bind(data: T)
}