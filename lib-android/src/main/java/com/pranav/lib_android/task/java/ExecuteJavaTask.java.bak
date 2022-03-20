package com.pranav.lib_android.task.java;

import com.pranav.lib_android.interfaces.*;
import com.pranav.lib_android.util.FileUtil;
import dalvik.system.PathClassLoader;
import java.io.*;
import java.security.Permission;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class ExecuteJavaTask extends Task {
	
	private final Builder mBuilder;
	private final String clazz;
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
		final PrintStream defaultOut = System.out;
		final PrintStream defaultErr = System.err;
		final String dexFile = FileUtil.getBinDir()
		+ "classes.dex";
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(() -> {
			final OutputStream out = new OutputStream() {
				@Override
				public void write(int b) {
					log.append(String.valueOf((char) b));
				}
				
				@Override
				public String toString() {
					return log.toString();
				}
			};
			System.setOut(new PrintStream(out));
			System.setErr(new PrintStream(out));
			
			PathClassLoader loader = new PathClassLoader(dexFile,
			mBuilder.getClassloader());
			try {

				Class calledClass = loader.loadClass(clazz);
				
				Method method = calledClass.getDeclaredMethod("main", String[].class);
				
				String[] param = {};
				Object result;
				if (Modifier.isStatic(method.getModifiers())) {
					result = method.invoke(null, new Object[] {param});
				} else if (Modifier.isPublic(method.getModifiers())) {
					Object classInstance = calledClass.newInstance();
					result = method.invoke(classInstance, new Object[] {param});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.setSecurityManager(null);
			latch.countDown();
		});
		try {
			latch.await();
		} catch (InterruptedException ignored) {
		}
		System.setOut(defaultOut);
		System.setErr(defaultErr);
	}
	
	public String getLogs() {
		return log.toString();
	}
}
