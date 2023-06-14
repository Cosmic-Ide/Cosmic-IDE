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
 * Returns the template for a Java interface.
 *
 * @param interfaceName The name of the interface.
 * @param packageName The name of the package that the interface should be placed in.
 * @return The template for a Java interface.
 */
fun javaInterface(interfaceName: String, packageName: String): String {
    val clazz: TypeSpec = TypeSpec.interfaceBuilder(interfaceName)
        .addModifiers(Modifier.PUBLIC)
        .build()

    val javaFile = JavaFile.builder(packageName, clazz).indent("\t").build()
    return javaFile.toString()
}

/**
 * Returns the template for a Java enum.
 *
 * @param enumName The name of the enum.
 * @param packageName The name of the package that the enum should be placed in.
 * @return The template for a Java enum.
 */
fun javaEnum(enumName: String, packageName: String): String {
    val clazz: TypeSpec = TypeSpec.enumBuilder(enumName)
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("EXAMPLE")
        .build()

    val javaFile = JavaFile.builder(packageName, clazz).indent("\t").build()
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

/**
 * Returns the template for a Kotlin interface.
 *
 * @param interfaceName The name of the interface.
 * @param packageName The name of the package that the interface should be placed in.
 * @return The template for a Kotlin interface.
 */
fun kotlinInterface(interfaceName: String, packageName: String): String {
    val clazz: KotlinTypeSpec = KotlinTypeSpec.interfaceBuilder(interfaceName)
        .addModifiers(KModifier.PUBLIC)
        .build()
    val file = FileSpec.builder(packageName, interfaceName)
        .addType(clazz)
        .indent("\t")
        .build()
    return file.toString()
}

/**
 * Returns the template for a Kotlin object.
 *
 * @param objectName The name of the object.
 * @param packageName The name of the package that the object should be placed in.
 * @return The template for a Kotlin object.
 */
fun kotlinObject(objectName: String, packageName: String): String {
    val clazz: KotlinTypeSpec = KotlinTypeSpec.objectBuilder(objectName)
        .addModifiers(KModifier.PUBLIC)
        .build()
    val file = FileSpec.builder(packageName, objectName)
        .addType(clazz)
        .indent("\t")
        .build()
    return file.toString()
}

/**
 * Returns the template for a Kotlin enum.
 *
 * @param enumName The name of the enum.
 * @param packageName The name of the package that the enum should be placed in.
 * @return The template for a Kotlin enum.
 */
fun kotlinEnum(enumName: String, packageName: String): String {
    val clazz: KotlinTypeSpec = KotlinTypeSpec.enumBuilder(enumName)
        .addModifiers(KModifier.PUBLIC)
        .build()
    val file = FileSpec.builder(packageName, enumName)
        .addType(clazz)
        .indent("\t")
        .build()
    return file.toString()
}

/**
 * Returns the template for a Kotlin annotation.
 *
 * @param annotationName The name of the annotation.
 * @param packageName The name of the package that the annotation should be placed in.
 * @return The template for a Kotlin annotation.
 */
fun kotlinAnnotation(annotationName: String, packageName: String): String {
    val clazz: KotlinTypeSpec = KotlinTypeSpec.annotationBuilder(annotationName)
        .addModifiers(KModifier.PUBLIC)
        .build()
    val file = FileSpec.builder(packageName, annotationName)
        .addType(clazz)
        .indent("\t")
        .build()
    return file.toString()
}

/**
 * Returns the template for a Kotlin data class.
 *
 * @param dataClassName The name of the data class.
 * @param packageName The name of the package that the data class should be placed in.
 * @return The template for a Kotlin data class.
 */
fun kotlinDataClass(dataClassName: String, packageName: String): String {
    val clazz: KotlinTypeSpec = KotlinTypeSpec.classBuilder(dataClassName)
        .addModifiers(KModifier.PUBLIC, KModifier.DATA)
        .build()
    val file = FileSpec.builder(packageName, dataClassName)
        .addType(clazz)
        .indent("\t")
        .build()
    return file.toString()
}

/**
 * Returns the template for a Kotlin sealed class.
 *
 * @param sealedClassName The name of the sealed class.
 * @param packageName The name of the package that the sealed class should be placed in.
 * @return The template for a Kotlin sealed class.
 */
fun kotlinSealedClass(sealedClassName: String, packageName: String): String {
    val clazz: KotlinTypeSpec = KotlinTypeSpec.classBuilder(sealedClassName)
        .addModifiers(KModifier.PUBLIC, KModifier.SEALED)
        .build()
    val file = FileSpec.builder(packageName, sealedClassName)
        .addType(clazz)
        .indent("\t")
        .build()
    return file.toString()
}