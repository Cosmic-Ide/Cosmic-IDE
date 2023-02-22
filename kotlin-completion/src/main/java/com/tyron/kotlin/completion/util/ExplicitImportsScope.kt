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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.BaseImportingScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class ExplicitImportsScope(private val descriptors: Collection<DeclarationDescriptor>) : BaseImportingScope(null) {
    override fun getContributedClassifier(name: Name, location: LookupLocation) =
        descriptors.filter { it.name == name }.firstIsInstanceOrNull<ClassifierDescriptor>()

    override fun getContributedPackage(name: Name) = descriptors.filter { it.name == name }.firstIsInstanceOrNull<PackageViewDescriptor>()

    override fun getContributedVariables(name: Name, location: LookupLocation) =
        descriptors.filter { it.name == name }.filterIsInstance<VariableDescriptor>()

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        descriptors.filter { it.name == name }.filterIsInstance<FunctionDescriptor>()

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ) = descriptors

    override fun computeImportedNames() = descriptors.mapTo(hashSetOf()) { it.name }

    override fun printStructure(p: Printer) {
        p.println(this::class.java.name)
    }
}