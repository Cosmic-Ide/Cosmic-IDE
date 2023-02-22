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
package com.tyron.kotlin.completion

import com.tyron.kotlin.completion.resolve.ResolutionFacade
import com.tyron.kotlin.completion.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForProject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext

@FrontendInternals
class KotlinResolutionFacade(
    override val project: Project, private val componentProvider: ComponentProvider,
    override val moduleDescriptor: ModuleDescriptor
) :
    ResolutionFacade {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getFrontendService(serviceClass: Class<T>) =
        componentProvider.resolve(serviceClass)?.getValue() as T

    override fun analyze(
        elements: Collection<KtElement>,
        bodyResolveMode: BodyResolveMode
    ): BindingContext = TODO("not implemented")

    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext =
        TODO("not implemented")

    override fun analyzeWithAllCompilerChecks(elements: Collection<KtElement>): AnalysisResult =
        TODO("not implemented")

    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        TODO()
    }

    override fun <T : Any> getFrontendService(
        moduleDescriptor: ModuleDescriptor,
        serviceClass: Class<T>
    ): T = TODO("not implemented")

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return componentProvider.resolve(serviceClass)!!.getValue() as T
    }

    override fun resolveToDescriptor(
        declaration: KtDeclaration,
        bodyResolveMode: BodyResolveMode
    ): DeclarationDescriptor = TODO("not implemented")

    override fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T =
        TODO("not implemented")

    override fun getResolverForProject(): ResolverForProject<out ModuleInfo> {
        TODO("Not yet implemented")
    }
}