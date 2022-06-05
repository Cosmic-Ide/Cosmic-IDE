package com.pranav.android.analyzer;

import android.content.Context;
import android.content.SharedPreferences;

import com.pranav.common.util.FileUtil;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

public class JavacAnalyzer {

    private final SharedPreferences prefs;
    private DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    private boolean isFirstTime = true;

    public JavacAnalyzer(Context context) {
        prefs = context.getSharedPreferences("compiler_settings", Context.MODE_PRIVATE);
    }

    public void analyze() throws Exception {

        var output = new File(FileUtil.getBinDir(), "classes");
        output.mkdirs();
        var version = prefs.getString("version", "7");

        var javaFileObjects = new ArrayList<JavaFileObject>();
        var javaFiles = getSourceFiles(new File(FileUtil.getJavaDir()));
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

        var tool = JavacTool.create();

        var standardJavaFileManager =
                tool.getStandardFileManager(
                        diagnostics, Locale.getDefault(), Charset.defaultCharset());
        try {
            standardJavaFileManager.setLocation(
                    StandardLocation.PLATFORM_CLASS_PATH, getPlatformClasspath());
            standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, getClasspath());
            standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, javaFiles);
        } catch (IOException e) {
            // ignored
        }

        var args = new ArrayList<String>();

        args.add("-proc:none");
        args.add("-source");
        args.add(version);
        args.add("-target");
        args.add(version);

        var task =
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
        isFirstTime = false;
    }

    public boolean isFirstRun() {
      return this.isFirstTime;
    }

    public void reset() {
        diagnostics = new DiagnosticCollector<>();
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return diagnostics.getDiagnostics();
    }

    private ArrayList<File> getSourceFiles(File path) {
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
