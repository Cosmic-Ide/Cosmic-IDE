package com.pranav.lib_android.task.java;

import dalvik.system.PathClassLoader;

import com.pranav.common.util.ConcurrentUtil;
import com.pranav.common.util.FileUtil;
import com.pranav.lib_android.interfaces.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;

public class ExecuteJavaTask extends Task {

    private final Builder mBuilder;
    private final String clazz;
    private Object result;
    public StringBuilder log = new StringBuilder();

    public ExecuteJavaTask(Builder builder, String claz) {
        this.mBuilder = builder;
        this.clazz = claz;
    }

    @Override
    public String getTaskName() {
        return "Execute java Task";
    }

    @Override
    public void doFullTask() throws Exception {
        var defaultOut = System.out;
        var defaultErr = System.err;
        var dexFile = FileUtil.getBinDir() + "classes.dex";
        ConcurrentUtil.execute(
                () -> {
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

                    var loader = new PathClassLoader(dexFile, mBuilder.getClassloader());

                        var calledClass = loader.loadClass(clazz);

                        var method = calledClass.getDeclaredMethod("main", String[].class);

                        String[] param = {};

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
                });
    }

    public String getLogs() {
        return log.toString();
    }
}
