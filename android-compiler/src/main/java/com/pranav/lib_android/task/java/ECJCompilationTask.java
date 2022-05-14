package com.pranav.lib_android.task.java;

import android.content.Context;
import android.content.SharedPreferences;

import com.pranav.common.util.ConcurrentUtil;
import com.pranav.common.util.FileUtil;
import com.pranav.lib_android.exception.CompilationFailedException;
import com.pranav.lib_android.interfaces.*;

import org.eclipse.jdt.internal.compiler.batch.Main;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

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

        var writer =
                new PrintWriter(
                        new OutputStream() {
                            @Override
                            public void write(int p1) throws IOException {
                                errs.append((char) p1);
                            }
                        });

        var main = new Main(writer, writer, false, null, null);

        var output = new File(FileUtil.getBinDir(), "classes");

        ConcurrentUtil.execute(
                () -> {
                    final var args = new ArrayList<String>();

                    args.add("-log");
                    args.add(FileUtil.getBinDir().concat("debug.xml"));
                    args.add("-g");
                    args.add("-" + prefs.getString("javaVersion", "7.0"));
                    args.add("-d");
                    args.add(output.getAbsolutePath());
                    args.add("-classpath");
                    args.add(FileUtil.getClasspathDir() + "android.jar");
                    var classpath = new StringBuilder();
                    if (prefs.getString("javaVersion", "7.0").equals("8.0")) {
                        classpath.append(FileUtil.getClasspathDir() + "core-lambda-stubs.jar");
                    }
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

                    main.compile(args.toArray(new String[0]));
                });

        if (main.globalErrorsCount > 0 | !output.exists()) {
            throw new CompilationFailedException(errs.toString());
        }
    }
}
