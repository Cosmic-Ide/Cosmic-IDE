package com.pranav.android.task.java;

import android.content.Context;
import android.content.SharedPreferences;

import com.pranav.android.exception.CompilationFailedException;
import com.pranav.android.interfaces.*;
import com.pranav.common.util.FileUtil;
import com.pranav.common.Indexer;

import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.eclipse.jdt.internal.compiler.tool.EclipseFileManager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

public class ECJCompilationTask extends Task {

    private final StringBuilder errs = new StringBuilder();
    private final SharedPreferences prefs;

    public ECJCompilationTask(Builder builder) {
        prefs =
                builder.getContext()
                        .getSharedPreferences("compiler_settings", Context.MODE_PRIVATE);
    }

    @Override
    public String getTaskName() {
        return "ECJ Compilation Task";
    }

    @Override
    public void doFullTask() throws Exception {
        final var diagnostics = new DiagnosticCollector<JavaFileObject>();

        var compiler = new EclipseCompiler();
        var fileManager = (EclipseFileManager) compiler.getStandardFileManager(diagnostics, null, null);
        var filesToCompile = getSourceFilesToCompile(new File(FileUtil.getJavaDir()), new Indexer("editor").getLong("lastBuildTime"));

        var output = new File(FileUtil.getBinDir(), "classes");

        final var args = new ArrayList<String>();

        args.add("-log");
        args.add(FileUtil.getBinDir().concat("debug.xml"));
        args.add("-g");
        args.add("-" + prefs.getString("version", "7"));
        args.add("-d");
        args.add(output.getAbsolutePath());
        args.add("-classpath");
        args.add(FileUtil.getClasspathDir() + "android.jar");
        var classpath = new StringBuilder();
        classpath.append(FileUtil.getClasspathDir() + "core-lambda-stubs.jar");
        var clspath = prefs.getString("classpath", "");
        if (!clspath.isEmpty() && classpath.length() > 0) {
            classpath.append(":");
            classpath.append(clspath);
        }
        if (classpath.length() > 0) {
            args.add("-cp");
            args.add(classpath.toString());
        }
        args.add("-proc:none");
        args.add("-sourcepath");
        args.add(" ");
        args.add(FileUtil.getJavaDir());

        
        var task = compiler.getTask(null, fileManager, diagnostics, args, null, fileManager.getJavaFileObjectsFromFiles(filesToCompile.iterator()));
        
        if(!task.call()) {
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
                                  errs.append(message.toString());
                                  errs.append("\n");
                                  break;
                              case NOTE:
                              case WARNING:
                              case MANDATORY_WARNING:
                                  warns.append(message.toString());
                                  warns.append("\n");
                                  break;
                              default:
                                  warns.append(message.toString());
                          }
                      }
                      var errors = errs.toString();
                      var warnings = warns.toString();
          
                      throw new CompilationFailedException(warnings + "\n" + errors);
                  }
                  new Indexer("editor").put("lastBuildTime", System.currentTimeMillis());
    }

    public ArrayList<File> getSourceFilesToCompile(File path, long lastCompileTime) {
        var sourceFiles = new ArrayList<File>();
        var files = path.listFiles();
        if (files == null) {
            return new ArrayList<File>();
        }
        for (var file : files) {
            if (file.isFile()) {
                if (file.getName().endsWith(".java") && file.lastModified() > lastCompileTime) {
                    sourceFiles.add(file);
                }
            } else {
                sourceFiles.addAll(getSourceFilesToCompile(file, lastCompileTime));
            }
        }
        return sourceFiles;
    }
}
