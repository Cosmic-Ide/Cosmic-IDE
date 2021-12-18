/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pranav.ide.multidex

import com.pranav.ide.dx.cf.direct.DirectClassFile
import com.pranav.ide.dx.rop.cst.CstBaseMethodRef
import com.pranav.ide.dx.rop.cst.CstFieldRef
import com.pranav.ide.dx.rop.cst.CstType
import com.pranav.ide.dx.rop.type.Prototype
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipFile

/**
 * Tool to find direct class references to other classes.
 */
class ClassReferenceListBuilder(private val path: Path) {
    val classNames: MutableSet<String> = HashSet()

    /**
     * @param jarOfRoots Archive containing the class files resulting of the tracing, typically
     * this is the result of running ProGuard.
     */
    @Throws(IOException::class)
    fun addRoots(jarOfRoots: ZipFile) {

        // keep roots
        run {
            val entries = jarOfRoots.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (name.endsWith(CLASS_EXTENSION)) {
                    classNames.add(name.substring(0, name.length - CLASS_EXTENSION.length))
                }
            }
        }

        // keep direct references of roots (+ direct references hierarchy)
        val entries = jarOfRoots.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val name = entry.name
            if (name.endsWith(CLASS_EXTENSION)) {
                val classFile: DirectClassFile = try {
                    path.getClass(name)
                } catch (e: FileNotFoundException) {
                    throw IOException(
                        "Class " + name +
                                " is missing form original class path " + path, e
                    )
                }
                addDependencies(classFile)
            }
        }
    }

    private fun addDependencies(classFile: DirectClassFile) {
        for (constant in classFile.constantPool.entries) {
            when (constant) {
                is CstType -> {
                    checkDescriptor(constant.classType.descriptor)
                }
                is CstFieldRef -> {
                    checkDescriptor(constant.type.descriptor)
                }
                is CstBaseMethodRef -> {
                    checkPrototype(constant.prototype)
                }
            }
        }
        val fields = classFile.fields
        val nbField = fields.size()
        for (i in 0 until nbField) {
            checkDescriptor(fields[i].descriptor.string)
        }
        val methods = classFile.methods
        val nbMethods = methods.size()
        for (i in 0 until nbMethods) {
            checkPrototype(Prototype.intern(methods[i].descriptor.string))
        }
    }

    private fun checkPrototype(proto: Prototype) {
        checkDescriptor(proto.returnType.descriptor)
        val args = proto.parameterTypes
        for (i in 0 until args.size()) {
            checkDescriptor(args[i].descriptor)
        }
    }

    private fun checkDescriptor(typeDescriptor: String) {
        if (typeDescriptor.endsWith(";")) {
            val lastBrace = typeDescriptor.lastIndexOf('[')
            if (lastBrace < 0) {
                addClassWithHierachy(typeDescriptor.substring(1, typeDescriptor.length - 1))
            } else {
                assert(
                    typeDescriptor.length > lastBrace + 3
                            && typeDescriptor[lastBrace + 1] == 'L'
                )
                addClassWithHierachy(
                    typeDescriptor.substring(
                        lastBrace + 2,
                        typeDescriptor.length - 1
                    )
                )
            }
        }
    }

    private fun addClassWithHierachy(classBinaryName: String) {
        if (classNames.contains(classBinaryName)) {
            return
        }
        try {
            val classFile = path.getClass(classBinaryName + CLASS_EXTENSION)
            classNames.add(classBinaryName)
            val superClass = classFile.superclass
            if (superClass != null) {
                addClassWithHierachy(superClass.classType.className)
            }
            val interfaceList = classFile.interfaces
            val interfaceNumber = interfaceList.size()
            for (i in 0 until interfaceNumber) {
                addClassWithHierachy(interfaceList.getType(i).className)
            }
        } catch (e: FileNotFoundException) {
            // Ignore: The referenced type is not in the path it must be part of the libraries.
        }
    }

    companion object {
        private const val CLASS_EXTENSION = ".class"

        /**
         * Kept for compatibility with the gradle integration, this method just forwards to
         * [com.pranav.ide.multidex.MainDexListBuilder.main].
         */
        @Deprecated(
            "use {@link com.pranav.ide.multidex.MainDexListBuilder#main(String[])} instead.",
            ReplaceWith("MainDexListBuilder.main(args)")
        )
        @JvmStatic
        fun main(args: Array<String>) {
            MainDexListBuilder.main(args)
        }
    }
}