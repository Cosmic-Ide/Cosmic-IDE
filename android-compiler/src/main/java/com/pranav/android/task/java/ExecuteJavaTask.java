package com.pranav.android.task.java;

import android.content.Context;
import android.content.SharedPreferences;

import dalvik.system.PathClassLoader;

import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;

import com.pranav.android.interfaces.*;
import com.pranav.common.util.FileUtil;
import com.pranav.common.util.MultipleDexClassLoader;
import com.pranav.project.mode.JavaProject;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.File;
import java.nio.file.Paths;
import java.lang.reflect.Modifier;

public class ExecuteJavaTask implements Task {

    private final String clazz;
    private Object result;
    private final StringBuilder log = new StringBuilder();
    private final SharedPreferences prefs;

    public ExecuteJavaTask(SharedPreferences preferences, String claz) {
        this.clazz = claz;
        this.prefs = preferences;
    }

    @Override
    public String getTaskName() {
        return "Execute java Task";
    }

    /*
     * Runs the main method pf the program by loading it through
     * PathClassLoader
     */
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

        // Load the dex file into a ClassLoader
        var dexLoader = new MultipleDexClassLoader();

        dexLoader.loadDex(dexFile);

        var libs = new File(project.getLibDirPath()).listFiles();
        if (libs != null) {
            for (var lib : libs) {
                var out = project.getBuildDirPath() + lib.getName().replace(".jar", ".dex");

                if (!new File(out).exists()) {
                    D8.run(
                        D8Command.builder()
                                .setOutput(Paths.get(project.getBuildDirPath()), OutputMode.DexIndexed)
                                .addLibraryFiles(Paths.get(FileUtil.getClasspathDir(), "android.jar"))
                                .addProgramFiles(lib.toPath())
                                .build()
                    );
                }
                dexLoader.loadDex(out);
            }
        }

        final var loader = dexLoader.loadDex(FileUtil.getClasspathDir() + "kotlin-stdlib-1.7.10.jar");

        var calledClass = loader.loadClass(clazz);

        var method = calledClass.getDeclaredMethod("main", String[].class);

        var args = prefs.getString("program_arguments", "").trim();

        // Split argument into an array
        String[] param = args.split("\\s+");

        if (Modifier.isStatic(method.getModifiers())) {
            // If the method is static, directly call it
            result = method.invoke(null, new Object[] { param });
        } else if (Modifier.isPublic(method.getModifiers())) {
            // If the method is public, create an instance of the class,
            // and then call it on the instance
            var classInstance = calledClass.getConstructor().newInstance();
            result = method.invoke(classInstance, new Object[] { param });
        }
        if (result != null) {
            log.append(result.toString());
        }
        System.setOut(defaultOut);
        System.setErr(defaultErr);
    }

    /*
     * Returns all the system logs recorded while executing the method
     */
    public String getLogs() {
        return log.toString();
    }
}
