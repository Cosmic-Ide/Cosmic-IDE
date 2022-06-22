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
import com.pranav.analyzer.JavaSourceFromString;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

public class JavacAnalyzer {

    private final SharedPreferences prefs;
    private DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    private boolean isFirstUse = true;

    public JavacAnalyzer(Context context) {
        prefs = context.getSharedPreferences("compiler_settings", Context.MODE_PRIVATE);
    }

    public void analyze(String name, String code) throws IOException {
        final var output = new File(FileUtil.getBinDir(), "classes");
        output.mkdirs();
        final var version = prefs.getString("version", "7");

        final var javaFileObjects = new ArrayList<JavaFileObject>();
        javaFileObjects.add(
                new JavaSourceFromString(name, code)
        );

        final var tool = JavacTool.create();

        final var standardJavaFileManager =
                tool.getStandardFileManager(
                        diagnostics, Locale.getDefault(), Charset.defaultCharset());
        standardJavaFileManager.setLocation(
                StandardLocation.PLATFORM_CLASS_PATH, getPlatformClasspath());
        standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, getClasspath());

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
        diagnostics.clear();
    }

    public ArrayList<DiagnosticRegion> getDiagnostics() {
        final var problems = new ArrayList<DiagnosticRegion>();
        for (var it : diagnostics.getDiagnostics()) {
            if (it.getSource() == null) continue;
            // since we're not compiling the whole project, there might be some errors
            // from files that we skipped, so it should mostly be safe to ignore these
            short flag = it.getKind() == Diagnostic.Kind.ERROR ? DiagnosticRegion.SEVERITY_ERROR : DiagnosticRegion.SEVERITY_WARNING;
            if (!it.getCode().startsWith("compiler.err.cant.resolve")) {
                problems.add(new DiagnosticRegion(it.getStartPosition(), it.getEndPosition(), severity));
            }
        }
        return problems;
    }

    private ArrayList<File> getClasspath() {
        var classpath = new ArrayList<File>();
        var clspath = prefs.getString("classpath", "");

        if (!clspath.isEmpty()) {
            for (var clas : clspath.split(":")) {
                classpath.add(new File(clas));
            }
        }
        return classpath;
    }

    private ArrayList<File> getPlatformClasspath() {
        var classpath = new ArrayList<File>();
        classpath.add(new File(FileUtil.getClasspathDir(), "android.jar"));
        classpath.add(new File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar"));
        return classpath;
    }
}
