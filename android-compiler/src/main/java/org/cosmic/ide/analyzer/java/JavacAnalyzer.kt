package org.cosmic.ide.analyzer.java

import android.content.Context
import android.content.SharedPreferences
import com.sun.source.util.JavacTask
import com.sun.tools.javac.api.JavacTool
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.project.JavaProject
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation

class JavacAnalyzer(context: Context, file: String, javaProject: JavaProject) {

    private val prefs: SharedPreferences
    private var diagnostics = DiagnosticCollector<JavaFileObject>()
    private var isFirstUse = true
    private val currentFile: String
    private val project: JavaProject

    init {
        prefs = context.getSharedPreferences("compiler_settings", Context.MODE_PRIVATE)
        project = javaProject
        currentFile = file
    }

    @Throws(IOException::class)
    fun analyze() {
        val output = File(project.getBinDirPath(), "classes")
        output.mkdirs()
        val version = prefs.getString("version", "7")
        val files = getSourceFiles(File(project.getSrcDirPath()))

        val javaFileObjects = arrayListOf<JavaFileObject>()
        for (file in files) {
            javaFileObjects.add(
                object : SimpleJavaFileObject(
                    file.toURI(), JavaFileObject.Kind.SOURCE
                ) {
                    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                        return FileUtil.readFile(file)
                    }
                })
        }

        val tool = JavacTool.create()

        val standardJavaFileManager =
            tool.getStandardFileManager(
                diagnostics, Locale.getDefault(), Charset.defaultCharset()
            )
        standardJavaFileManager.apply {
            setLocation(
                StandardLocation.PLATFORM_CLASS_PATH, getPlatformClasspath()
            )
            setLocation(StandardLocation.CLASS_PATH, getClasspath())
            setLocation(
                StandardLocation.SOURCE_PATH, files
            )
        }

        val args = arrayListOf<String>()

        args.add("-proc:none")
        args.add("-source")
        args.add(version!!)
        args.add("-target")
        args.add(version)
        if (version.toInt() >= 9) {
            args.add("--system")
            args.add(FileUtil.getDataDir() + "compiler-modules")
        }

        val task =
            tool.getTask(
                null,
                standardJavaFileManager,
                diagnostics,
                args,
                null,
                javaFileObjects
            ) as JavacTask

        task.parse()
        task.analyze()
        standardJavaFileManager.close()
        isFirstUse = false
    }

    fun isFirstRun() = isFirstUse

    fun reset() {
        diagnostics = DiagnosticCollector<JavaFileObject>()
    }

    fun getDiagnostics(): ArrayList<DiagnosticRegion> {
        val problems = arrayListOf<DiagnosticRegion>()
        for (it in diagnostics.getDiagnostics()) {
            if (it.getSource() == null) continue
            val severity = if (it.getKind() == Diagnostic.Kind.ERROR) DiagnosticRegion.SEVERITY_ERROR else DiagnosticRegion.SEVERITY_WARNING
            problems.add(
                DiagnosticRegion(
                    it.getStartPosition().toInt(), it.getEndPosition().toInt(), severity
                )
            )
        }
        return problems
    }

    private fun getClasspath(): ArrayList<File> {
        val classpath = arrayListOf<File>()
        val clspath = prefs.getString("classpath", "")

        if (!clspath!!.isEmpty()) {
            for (clas in clspath.split(":")) {
                classpath.add(File(clas))
            }
        }
        classpath.add(File(project.getBinDirPath() + "classes"))
        val libs = File(project.getLibDirPath()).listFiles()
        for (lib in libs!!) {
            classpath.add(lib)
        }

        return classpath
    }

    private fun getPlatformClasspath(): ArrayList<File> {
        val classpath = arrayListOf<File>()
        classpath.add(File(FileUtil.getClasspathDir(), "android.jar"))
        classpath.add(File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar"))
        classpath.add(File(FileUtil.getClasspathDir(), "kotlin-stdlib-1.7.10.jar"))
        return classpath
    }

    private fun getSourceFiles(path: File): ArrayList<File> {
        val sourceFiles = arrayListOf<File>()
        val files = path.listFiles()
        if (files == null) {
            return arrayListOf<File>()
        }
        for (file in files) {
            if (file.isFile()) {
                if (file.name.endsWith(".java")) {
                    sourceFiles.add(file)
                }
            } else {
                sourceFiles.addAll(getSourceFiles(file))
            }
        }
        return sourceFiles
    }
}
