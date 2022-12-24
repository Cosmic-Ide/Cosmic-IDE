package org.cosmic.ide.android.task.java;

import android.content.SharedPreferences;
import android.util.Log;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;

import org.cosmic.ide.CompilerUtil;
import org.cosmic.ide.android.exception.CompilationFailedException;
import org.cosmic.ide.android.interfaces.*;
import org.cosmic.ide.common.util.FileUtil;
import org.cosmic.ide.project.Project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

public class JavaCompiler implements Task {

    private final SharedPreferences prefs;
    private final String TAG = "JavaCompiler";
    private final JavacTool tool;

    public JavaCompiler(SharedPreferences preferences) {
        tool = JavacTool.create();
        prefs = preferences;
    }

    /*
     * Compile the java files into classes.
     *
     * @param project the project to compile.
     */
    @Override
    public void doFullTask(Project project) throws Exception {

        final var output = new File(project.getBinDirPath(), "classes");

        final var version = prefs.getString("java_version", "7");

        Log.d(TAG, "Current Java Version: " + version);

        final var diagnostics = new DiagnosticCollector<JavaFileObject>();

        if (!output.exists()) {
            output.mkdirs();
        }
        final var javaFileObjects = new ArrayList<JavaFileObject>();
        final var javaFiles = getSourceFiles(new File(project.getSrcDirPath()));
        for (var file : javaFiles) {
            var path = file.getAbsolutePath();
            new File(output, path.replaceFirst(project.getSrcDirPath(), "")).delete();
            javaFileObjects.add(
                    new SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
                        @Override
                        public CharSequence getCharContent(boolean ignoreEncodingErrors)
                                throws IOException {
                            return FileUtil.readFile(file);
                        }
                    });
        }

        if (javaFileObjects.isEmpty()) {
            return;
        }

        final var standardJavaFileManager =
                tool.getStandardFileManager(
                        diagnostics, Locale.getDefault(), Charset.defaultCharset());
        standardJavaFileManager.setLocation(
                StandardLocation.CLASS_OUTPUT, Collections.singletonList(output));
        standardJavaFileManager.setLocation(
                StandardLocation.PLATFORM_CLASS_PATH, CompilerUtil.getPlatformClasspath());
        standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, getClasspath(project));
        standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, javaFiles);

        final var args = new ArrayList<String>();

        args.add("-proc:none");
        args.add("-Xlint:-options");
        args.add("-source");
        args.add(version);
        args.add("-target");
        args.add(version);

        final var task =
                (JavacTask)
                        tool.getTask(
                                null,
                                standardJavaFileManager,
                                diagnostics,
                                args,
                                null,
                                javaFileObjects);

        if (!task.call()) {
            var errs = new StringBuilder();
            var warns = new StringBuilder();
            for (var diagnostic : diagnostics.getDiagnostics()) {
                var message = new StringBuilder();
                if (diagnostic.getSource() != null) {
                    message.append(diagnostic.getSource().getName());
                    message.append(":");
                    message.append(diagnostic.getLineNumber());
                    message.append(": ");
                }
                message.append(diagnostic.getKind().name());
                message.append(": ");
                message.append(diagnostic.getMessage(Locale.getDefault()));

                switch (diagnostic.getKind()) {
                    case ERROR:
                    case OTHER:
                        errs.append(message);
                        errs.append("\n");
                        break;
                    case NOTE:
                    case WARNING:
                    case MANDATORY_WARNING:
                        warns.append(message);
                        warns.append("\n");
                        break;
                    default:
                        warns.append(message);
                        break;
                }
            }
            var errors = errs.toString();
            var warnings = warns.toString();

            throw new CompilationFailedException(warnings + "\n" + errors);
        }
    }

    public ArrayList<File> getSourceFiles(File path) {
        var sourceFiles = new ArrayList<File>();
        var files = path.listFiles();
        if (files == null) {
            return new ArrayList<File>();
        }
        for (var file : files) {
            if (file.isFile()) {
                if (file.getName().endsWith(".java")) {
                    sourceFiles.add(file);
                }
            } else {
                sourceFiles.addAll(getSourceFiles(file));
            }
        }
        return sourceFiles;
    }

    public ArrayList<File> getClasspath(Project project) {
        var classpath = new ArrayList<File>();
        var clspath = prefs.getString("key_classpath", "");

        if (!clspath.isEmpty()) {
            for (var clas : clspath.split(":")) {
                classpath.add(new File(clas));
            }
        }
        classpath.add(new File(project.getBinDirPath(), "classes"));
        var libs = new File(project.getLibDirPath()).listFiles();
        if (libs != null) {
            Collections.addAll(classpath, libs);
        }
        return classpath;
    }
}
