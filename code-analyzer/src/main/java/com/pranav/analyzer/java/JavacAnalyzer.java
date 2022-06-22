package com.pranav.analyzer.java;

import android.content.Context;
import android.content.SharedPreferences;

import com.pranav.common.util.DiagnosticWrapper;
import com.pranav.common.util.FileUtil;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;

import javax.tools.DiagnosticCollector;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

public class JavacAnalyzer {

    private final SharedPreferences prefs;
    private DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    private boolean isFirstUse = true;

    public JavacAnalyzer(Context context) {
        prefs = context.getSharedPreferences("compiler_settings", Context.MODE_PRIVATE);
    }

    // TODO: check and return diagnostics from only the current file being edited
    public void analyze() throws IOException {
        final var output = new File(FileUtil.getBinDir(), "classes");
        output.mkdirs();
        final var version = prefs.getString("version", "7");

        final var javaFileObjects = new ArrayList<JavaFileObject>();
        final var javaFiles = getSourceFiles(new File(FileUtil.getJavaDir()));
        for (var file : javaFiles) {
            javaFileObjects.add(
                    new SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
                        @Override
                        public CharSequence getCharContent(boolean ignoreEncodingErrors)
                                throws IOException {
                            return FileUtil.readFile(file);
                        }
                    });
        }

        final var tool = JavacTool.create();

        final var standardJavaFileManager =
                tool.getStandardFileManager(
                        diagnostics, Locale.getDefault(), Charset.defaultCharset());
        standardJavaFileManager.setLocation(
                StandardLocation.PLATFORM_CLASS_PATH, getPlatformClasspath());
        standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, getClasspath());
        standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, javaFiles);

        final var args = new ArrayList<String>();

        args.add("-proc:none");
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

        task.parse();
        task.analyze();
        standardJavaFileManager.close();
        isFirstUse = false;
    }

    public boolean isFirstRun() {
        return isFirstUse;
    }

    public void reset() {
        diagnostics = new DiagnosticCollector<>();
    }

    public ArrayList<DiagnosticRegion> getDiagnostics() {
        final var problems = new ArrayList<DiagnosticRegion>();
        for (var it : diagnostics.getDiagnostics()) {
            if (it.getSource() == null) continue;
            short severity = it.getKind() == Diagnostic.Kind.ERROR ? DiagnosticRegion.SEVERITY_ERROR : DiagnosticRegion.SEVERITY_WARNING;
            problems.add(new DiagnosticRegion((int) it.getStartPosition(), (int) it.getEndPosition(), severity));
        }
        return problems;
    }

    private ArrayList<File> getClasspath() {
        final var classpath = new ArrayList<File>();
        final var clspath = prefs.getString("classpath", "");

        if (!clspath.isEmpty()) {
            for (var clas : clspath.split(":")) {
                classpath.add(new File(clas));
            }
        }
        return classpath;
    }

    private ArrayList<File> getPlatformClasspath() {
        final var classpath = new ArrayList<File>();
        classpath.add(new File(FileUtil.getClasspathDir(), "android.jar"));
        classpath.add(new File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar"));
        return classpath;
    }

    private ArrayList<File> getSourceFiles(File path) {
        final var sourceFiles = new ArrayList<File>();
        final var files = path.listFiles();
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
}
