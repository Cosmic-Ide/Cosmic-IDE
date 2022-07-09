package com.pranav.android.task.java;

import android.content.Context;
import android.content.SharedPreferences;

import dalvik.system.PathClassLoader;

import com.pranav.android.interfaces.*;
import com.pranav.common.util.FileUtil;
import com.pranav.project.mode.JavaProject;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;

public class ExecuteJavaTask implements Task {

    private final String clazz;
    private Object result;
    private StringBuilder log = new StringBuilder();
    private SharedPreferences prefs;

    public ExecuteJavaTask(SharedPreferences preferences, String claz) {
        this.clazz = claz;
        this.prefs = preferences;
    }

    @Override
    public String getTaskName() {
        return "Execute java Task";
    }

    @Override
    public void doFullTask(JavaProject project) throws Exception {
        var defaultOut = System.out;
        var defaultErr = System.err;
        var dexFile = project.getBinDirPath() + "classes.dex";
        var out =
                new OutputStream() {
                    @Override
                    public void write(int b) {
                        log.append((char) b);
                    }

                    @Override
                    public String toString() {
                        return log.toString();
                    }
                };
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(out));

        var loader = new PathClassLoader(dexFile, this.class.getClassloader());

        var calledClass = loader.loadClass(clazz);

        var method = calledClass.getDeclaredMethod("main", String[].class);

        var args = prefs.getString("program_arguments", "").trim();

        String[] param = args.split("\\s+");

        if (Modifier.isStatic(method.getModifiers())) {
            result = method.invoke(null, new Object[] {param});
        } else if (Modifier.isPublic(method.getModifiers())) {
            var classInstance = calledClass.getConstructor().newInstance();
            result = method.invoke(classInstance, new Object[] {param});
        }
        if (result != null) {
            System.out.println(result.toString());
        }
        System.setOut(defaultOut);
        System.setErr(defaultErr);
    }

    public String getLogs() {
        return log.toString();
    }
}
