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
package com.tyron.kotlin.completion.resolve

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory

/**
 * Helper methods for commonly used frontend components.
 * Use them to avoid explicit opt-ins.
 * Before adding a new helper method please make sure component doesn't have fragile invariants that can be violated by external use.
 */

@OptIn(FrontendInternals::class)
fun ResolutionFacade.getLanguageVersionSettings(): LanguageVersionSettings =
    frontendService()

@OptIn(FrontendInternals::class)
fun ResolutionFacade.getDataFlowValueFactory(): DataFlowValueFactory =
    frontendService()