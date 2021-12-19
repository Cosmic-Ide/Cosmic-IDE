/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.pranav.ide.dx.cf.attrib.AttRuntimeVisibleAnnotations
import com.pranav.ide.dx.cf.iface.HasAttribute
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.system.exitProcess

/**
 * This is a command line tool used by mainDexClasses script to build a main dex classes list. First
 * argument of the command line is an archive, each class file contained in this archive is used to
 * identify a class that can be used during secondary dex installation, those class files
 * are not opened by this tool only their names matter. Other arguments must be zip files or
 * directories, they constitute in a classpath in with the classes named by the first argument
 * will be searched. Each searched class must be found. On each of this classes are searched for
 * their dependencies to other classes. The tool also browses for classes annotated by runtime
 * visible annotations and adds them to the list/ Finally the tools prints on standard output a list
 * of class files names suitable as content of the file argument --main-dex-list of dx.
 */
class MainDexListBuilder(keepAnnotated: Boolean, rootJar: String, pathString: String?) {
    private val filesToKeep: MutableSet<String?> = HashSet()

    /**
     * Returns a list of classes to keep. This can be passed to dx as a file with --main-dex-list.
     */
    val mainDexList: Set<String?>
        get() = filesToKeep

    /**
     * Keep classes annotated with runtime annotations.
     */
    @Throws(FileNotFoundException::class)
    private fun keepAnnotated(path: Path) {
        for (element in path.getElements()) {
            forClazz@ for (name in element?.list()!!) {
                if (name!!.endsWith(CLASS_EXTENSION)) {
                    val clazz = path.getClass(name)
                    if (hasRuntimeVisibleAnnotation(clazz)) {
                        filesToKeep.add(name)
                    } else {
                        val methods = clazz.methods
                        for (i in 0 until methods.size()) {
                            if (hasRuntimeVisibleAnnotation(methods[i])) {
                                filesToKeep.add(name)
                                continue@forClazz
                            }
                        }
                        val fields = clazz.fields
                        for (i in 0 until fields.size()) {
                            if (hasRuntimeVisibleAnnotation(fields[i])) {
                                filesToKeep.add(name)
                                continue@forClazz
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasRuntimeVisibleAnnotation(element: HasAttribute): Boolean {
        val att = element.attributes.findFirst(
            AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME
        )
        return att != null && (att as AttRuntimeVisibleAnnotations).annotations.size() > 0
    }

    companion object {
        private const val CLASS_EXTENSION = ".class"
        private const val STATUS_ERROR = 1
        private val EOL = System.getProperty("line.separator")
        private val USAGE_MESSAGE = "Usage:" + EOL + EOL +
                "Short version: Don't use this." + EOL + EOL +
                "Slightly longer version: This tool is used by mainDexClasses script to build" + EOL +
                "the main dex list." + EOL

        /**
         * By default we force all classes annotated with runtime annotation to be kept in the
         * main dex list. This option disable the workaround, limiting the index pressure in the main
         * dex but exposing to the Dalvik resolution bug. The resolution bug occurs when accessing
         * annotations of a class that is not in the main dex and one of the annotations as an enum
         * parameter.
         *
         * @see [bug discussion](https://code.google.com/p/android/issues/detail?id=78144)
         */
        private const val DISABLE_ANNOTATION_RESOLUTION_WORKAROUND =
            "--disable-annotation-resolution-workaround"

        @JvmStatic
        fun main(args: Array<String>) {
            var argIndex = 0
            var keepAnnotated = true
            while (argIndex < args.size - 2) {
                if (args[argIndex] == DISABLE_ANNOTATION_RESOLUTION_WORKAROUND) {
                    keepAnnotated = false
                } else {
                    System.err.println("Invalid option " + args[argIndex])
                    printUsage()
                    exitProcess(STATUS_ERROR)
                }
                argIndex++
            }
            if (args.size - argIndex != 2) {
                printUsage()
                exitProcess(STATUS_ERROR)
            }
            try {
                val builder = MainDexListBuilder(
                    keepAnnotated, args[argIndex],
                    args[argIndex + 1]
                )
                val toKeep = builder.mainDexList
                printList(toKeep)
            } catch (e: IOException) {
                System.err.println("A fatal error occured: " + e.message)
                exitProcess(STATUS_ERROR)
            }
        }

        private fun printUsage() {
            System.err.print(USAGE_MESSAGE)
        }

        private fun printList(fileNames: Set<String?>) {
            for (fileName in fileNames) {
                println(fileName)
            }
        }
    }

    init {
        var jarOfRoots: ZipFile? = null
        var path: Path? = null
        try {
            jarOfRoots = try {
                ZipFile(rootJar)
            } catch (e: IOException) {
                throw IOException(
                    "\"" + rootJar + "\" can not be read as a zip archive. ("
                            + e.message + ")", e
                )
            }
            path = Path(pathString!!)
            val mainListBuilder = ClassReferenceListBuilder(path)
            mainListBuilder.addRoots(jarOfRoots!!)
            for (className in mainListBuilder.classNames) {
                filesToKeep.add(className + CLASS_EXTENSION)
            }
            if (keepAnnotated) {
                keepAnnotated(path)
            }
        } finally {
            try {
                jarOfRoots!!.close()
            } catch (e: IOException) {
                // ignore
            }
            if (path != null) {
                for (element in path.elements) {
                    try {
                        element?.close()
                    } catch (e: IOException) {
                        // keep going, lets do our best.
                    }
                }
            }
        }
    }
}