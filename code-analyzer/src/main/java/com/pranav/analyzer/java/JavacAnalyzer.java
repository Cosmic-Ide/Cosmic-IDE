package com.pranav.analyzer.java;

import android.content.Context;
import android.content.SharedPreferences;

import com.pranav.common.util.FileUtil;
import com.pranav.project.mode.JavaProject;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;

import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

public class JavacAnalyzer {

    private final SharedPreferences prefs;
    private DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    private boolean isFirstUse = true;
    private final String currentFile;
    private final JavaProject project;

    public JavacAnalyzer(Context context, String file, JavaProject project) {
        prefs = context.getSharedPreferences("compiler_settings", Context.MODE_PRIVATE);
        this.project = project;
        currentFile = file;
    }

    public void analyze() throws IOException {
        final var output = new File(project.getBinDirPath(), "classes");
        output.mkdirs();
        final var version = prefs.getString("version", "7");
        final var files = getSourceFiles(new File(project.getSrcDirPath()));

        final var javaFileObjects = new ArrayList<JavaFileObject>();
        for (var file : files) {
        javaFileObjects.add(
                new SimpleJavaFileObject(
                        file.toURI(), JavaFileObject.Kind.SOURCE) {
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
        standardJavaFileManager.setLocation(
                StandardLocation.SOURCE_PATH, files);

        final var args = new ArrayList<String>();

        args.add("-proc:none");
        args.add("-source");
        args.add(version);
        args.add("-target");
        args.add(version);
        if (Integer.valueOf(version) >= 9) {
            args.add("--system");
            args.add(FileUtil.getDataDir() + "compiler-modules");
        }

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
//            if (!it.getCode().startsWith("compiler.err.cant.resolve")) {
//                 problems.add(new DiagnosticRegion((int) it.getStartPosition(), (int) it.getEndPosition(), severity));
//             }
            short severity =
                    (it.getKind() == Diagnostic.Kind.ERROR)
                            ? DiagnosticRegion.SEVERITY_ERROR
                            : DiagnosticRegion.SEVERITY_WARNING;
            problems.add(
                    new DiagnosticRegion(
                            (int) it.getStartPosition(), (int) it.getEndPosition(), severity));
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
        classpath.add(project.getBinDir() + "classes");
        return classpath;
    }

    private ArrayList<File> getPlatformClasspath() {
        final var classpath = new ArrayList<File>();
        classpath.add(new File(FileUtil.getClasspathDir(), "android.jar"));
        classpath.add(new File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar"));
        classpath.add(new File(FileUtil.getClasspathDir(), "kotlin-stdlib-1.7.10.jar"));
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
