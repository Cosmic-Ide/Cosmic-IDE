/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.common

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.transition.MaterialSharedAxis

/**
 * A base Fragment class that provides a convenient way to use ViewBinding with the Fragment.
 */
abstract class BaseBindingFragment<T : ViewBinding> : Fragment() {
    protected open lateinit var binding: T
    open var isBackHandled = false

    abstract fun getViewBinding(): T

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTransitions()

        if (isBackHandled.not()) {
            requireActivity().onBackPressedDispatcher.addCallback(this) {
                if (parentFragmentManager.backStackEntryCount > 0) {
                    parentFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    startActivity(
                        Intent(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addCategory(Intent.CATEGORY_HOME)
                    )
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = getViewBinding()
        return binding.root
    }

    /**
     * Applies MaterialSharedAxis transitions to the Fragment's enter, return, exit, and reenter transitions.
     */
    protected open fun applyTransitions() {
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }
}
