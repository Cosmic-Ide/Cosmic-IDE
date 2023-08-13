/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.project.templates

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import javax.lang.model.element.Modifier
import com.squareup.kotlinpoet.TypeSpec as KotlinTypeSpec

/**
 * Returns the template for a Java class.
 *
 * @param className The name of the class.
 * @param packageName The name of the package that the class should be placed in.
 * @param body Optional body of the main method of the class.
 * @return The template for a Java class.
 */
fun javaClass(className: String, packageName: String, body: String = ""): String {

    val main = MethodSpec.methodBuilder("main").apply {
        addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        returns(Void.TYPE)
        addParameter(Array<String>::class.java, "args")
        if (body.isNotEmpty()) {
            addCode(body)
        }
    }.build()

    val clazz: TypeSpec = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(main)
        .build()

    val javaFile =
        JavaFile.builder(packageName, clazz).indent("\t").skipJavaLangImports(true).build()

    return javaFile.toString()
}

/**
 * Returns the template for a Kotlin class.
 *
 * @param className The name of the class.
 * @param packageName The name of the package that the class should be placed in.
 * @param body Optional body of the main method of the class.
 * @return The template for a Kotlin class.
 */
fun kotlinClass(className: String, packageName: String, body: String = ""): String {
    val clazz: KotlinTypeSpec = KotlinTypeSpec.classBuilder(className)
        .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
        .addFunction(FunSpec.builder("main").apply {
            addModifiers(KModifier.PUBLIC)
            returns(Void.TYPE)
            addParameter("args", Array<String>::class.java)
            if (body.isNotEmpty()) {
                addCode(body)
            }
        }.build())
        .build()

    val file = FileSpec.builder(packageName, className)
        .addType(clazz)
        .indent("\t")
        .addKotlinDefaultImports(true)
        .build()
    return file.toString()
}

