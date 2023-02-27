/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tyron.kotlin.completion.resolve.lazy

import org.jetbrains.kotlin.resolve.BindingTraceFilter

enum class BodyResolveMode(val bindingTraceFilter: BindingTraceFilter, val doControlFlowAnalysis: Boolean, val resolveAdditionals: Boolean = true) {
    // All body statements are analyzed, diagnostics included
    FULL(BindingTraceFilter.ACCEPT_ALL, doControlFlowAnalysis = true),

    // Analyzes only dependent statements, including all declaration statements (difference from PARTIAL_WITH_CFA)
    PARTIAL_FOR_COMPLETION(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = true),

    // Analyzes only dependent statements, diagnostics included
    PARTIAL_WITH_DIAGNOSTICS(BindingTraceFilter.ACCEPT_ALL, doControlFlowAnalysis = true),

    // Analyzes only dependent statements, performs control flow analysis (mostly needed for isUsedAsExpression / AsStatement)
    PARTIAL_WITH_CFA(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = true),

    // Analyzes only dependent statements, including only used declaration statements, does not perform control flow analysis
    PARTIAL(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = false),

    // Resolve mode to resolve only the element itself without the additional elements (annotation resolve would not lead to function resolve or default parameters)
    PARTIAL_NO_ADDITIONAL(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = false, resolveAdditionals = false);

    fun doesNotLessThan(other: BodyResolveMode): Boolean {
        return this <= other && this.bindingTraceFilter.includesEverythingIn(other.bindingTraceFilter)
    }
}