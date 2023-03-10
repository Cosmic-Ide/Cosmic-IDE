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
package com.tyron.kotlin.completion.util

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

fun FunctionDescriptor.shouldNotConvertToProperty(notProperties: Set<FqNameUnsafe>): Boolean {
    if (fqNameUnsafe in notProperties) return true
    return this.overriddenTreeUniqueAsSequence(false).any { fqNameUnsafe in notProperties }
}

fun SyntheticJavaPropertyDescriptor.suppressedByNotPropertyList(set: Set<FqNameUnsafe>) =
    getMethod.shouldNotConvertToProperty(set) || setMethod?.shouldNotConvertToProperty(set) ?: false