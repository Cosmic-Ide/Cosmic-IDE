package org.cosmicide.rewrite.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.transition.MaterialSharedAxis

/**
 * A base Fragment class that provides a convenient way to use ViewBinding with the Fragment.
 */
abstract class BaseBindingFragment<T : ViewBinding> : Fragment() {

    private var _binding: T? = null

    protected val binding: T
        get() = _binding ?: throw IllegalStateException("View binding is not initialized")

    abstract fun getViewBinding(): T

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTransitions()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = getViewBinding()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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