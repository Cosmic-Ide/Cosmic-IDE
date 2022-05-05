package com.pranav.lib_android.task.java;

import android.content.Context;
import android.content.SharedPreferences;

import com.pranav.lib_android.exception.CompilationFailedException;
import com.pranav.lib_android.interfaces.*;
import com.pranav.lib_android.util.FileUtil;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

public class JavacCompilationTask extends Task {

    private final SharedPreferences prefs;

    public JavacCompilationTask(Builder builder) {
        prefs =
                builder.getContext()
                        .getSharedPreferences("compiler_settings", Context.MODE_PRIVATE);
    }

    @Override
    public String getTaskName() {
        return "Javac Compilation Task";
    }

    @Override
    public void doFullTask() throws Exception {

        final File output = new File(FileUtil.getBinDir(), "classes");
        output.mkdirs();
        final String version = prefs.getString("javaVersion", "7");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        List<JavaFileObject> javaFileObjects = new ArrayList<>();
        List<File> javaFiles = getSourceFiles(new File(FileUtil.getJavaDir()));
        for (File file : javaFiles) {
            javaFileObjects.add(
                    new SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
                        @Override
                        public CharSequence getCharContent(boolean ignoreEncodingErrors)
                                throws IOException {
                            return FileUtil.readFile(file);
                        }
                    });
        }

        JavaCompiler tool = JavacTool.create();

        StandardJavaFileManager standardJavaFileManager =
                tool.getStandardFileManager(
                        diagnostics, Locale.getDefault(), Charset.defaultCharset());
        try {
            standardJavaFileManager.setLocation(
                    StandardLocation.CLASS_OUTPUT, Collections.singletonList(output));
            standardJavaFileManager.setLocation(
                    StandardLocation.PLATFORM_CLASS_PATH, getPlatformClasspath(version));
            standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, getClasspath());
            standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, javaFiles);
        } catch (IOException e) {
            throw new CompilationFailedException(e);
        }

        final ArrayList<String> args = new ArrayList<>();

        args.add("-proc:none");
        args.add("-source");
        args.add(version);
        args.add("-target");
        args.add(version);

        JavacTask task =
                (JavacTask)
                        tool.getTask(
                                null,
                                standardJavaFileManager,
                                diagnostics,
                                args,
                                null,
                                javaFileObjects);

        if (!task.call()) {
            StringBuilder errs = new StringBuilder();
            StringBuilder warns = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                StringBuilder message = new StringBuilder();
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
                        errs.append(message.toString());
                        errs.append("\n");
                        break;
                    case NOTE:
                    case WARNING:
                    case MANDATORY_WARNING:
                        warns.append(message.toString());
                        warns.append("\n");
                        break;
                }
            }
            String errors = errs.toString();
            String warnings = warns.toString();

            throw new CompilationFailedException(warnings + "\n" + errors);
        }
    }

    public ArrayList<File> getSourceFiles(File path) {
        ArrayList<File> sourceFiles = new ArrayList<>();
        File[] files = path.listFiles();
        if (files == null) {
            return new ArrayList<File>();
        }
        for (File file : files) {
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

    public List<File> getClasspath() {
        List<File> classpath = new ArrayList<>();
        final String clspath = prefs.getString("classpath", "");

        if (!clspath.isEmpty()) {
            for (String clas : clspath.toString().split(":")) {
                classpath.add(new File(clas));
            }
        }
        return classpath;
    }

    public List<File> getPlatformClasspath(String version) {
        List<File> classpath = new ArrayList<>();
        classpath.add(new File(FileUtil.getClasspathDir(), "android.jar"));
        if (version.equals("8.0")) {
            classpath.add(new File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar"));
        }
        return classpath;
    }
}
