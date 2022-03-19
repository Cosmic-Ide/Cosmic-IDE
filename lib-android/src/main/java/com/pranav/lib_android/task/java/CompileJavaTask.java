package com.pranav.lib_android.task.java;

import android.content.Context;
import android.content.SharedPreferences;
import com.pranav.lib_android.FileUtil;
import com.pranav.lib_android.exception.CompilationFailedException;
import com.pranav.lib_android.interfaces.*;
import org.eclipse.jdt.internal.compiler.batch.Main;
import java.io.*;
import java.util.ArrayList;

public class CompileJavaTask extends Task {

	private final Builder mBuilder;
	private final StringBuilder errs = new StringBuilder();
	private final SharedPreferences prefs;

	public CompileJavaTask(Builder builder) {
		this.mBuilder = builder;
		prefs = mBuilder.getContext().getSharedPreferences("compiler_settings",
				Context.MODE_PRIVATE);
	}

	@Override
	public String getTaskName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void doFullTask() throws Exception {

        final File output = new File(FileUtil.getBinDir() + "classes");
		final ArrayList<String> args = new ArrayList<>();

		args.add("-warn:all");
		args.add("-log");
		args.add(FileUtil.getBinDir()
				.concat("debug.xml"));
		args.add("-g");
		args.add("-" + prefs.getString("javaVersion", "1.7"));
		args.add("-d");
		args.add(output.getAbsolutePath());
		args.add("-bootclasspath");
		args.add(FileUtil.getClasspathDir()
				+ "android.jar");
		StringBuilder classpath = new StringBuilder();
		if (prefs.getString("javaVersion", "1.7").equals("1.8")) {
			classpath.append(FileUtil.getClasspathDir()
					+ "core-lambda-stubs.jar");
		}
		final String clspath = prefs.getString("classpath", "");
		if (clspath != "") {
			if (classpath.toString() != "") classpath.append(":");
			classpath.append(clspath);
		}
		if (classpath.toString() != "") {
			args.add("-cp");
			args.add(classpath.toString());
		}
		args.add("-proc:none");
		args.add("-sourcepath");
		args.add(" ");
		args.add(FileUtil.getJavaDir());

		PrintWriter writer = new PrintWriter((p1) -> errs.append((char) p1););

		Main main = new Main(writer, writer, false, null, null);

		main.compile(args.toArray(new String[0]));

		if (main.globalErrorsCount > 0 | !output.exists()) {
			throw new CompilationFailedException(errs.toString());
		}
	}
}
