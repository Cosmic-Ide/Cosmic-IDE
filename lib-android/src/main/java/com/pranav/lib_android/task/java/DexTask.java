package com.pranav.lib_android.task.java;

import com.pranav.lib_android.FileUtil;
import com.pranav.lib_android.interfaces.*;
import com.pranav.ide.dx.command.dexer.Main;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

public class DexTask extends Task {

	private final Builder mBuilder;
	private Exception ex = null;

	public DexTask(Builder builder) {
		this.mBuilder = builder;
	}

	@Override
	public void doFullTask() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				final File f = new File(
						FileUtil.getBinDir()
								+ "classes");
				ArrayList<String> args = new ArrayList<>();
				args.add("--debug");
				args.add("--verbose");
				args.add("--min-sdk-version");
				args.add("21");
				args.add("--output");
				args.add(f.getParent());
				args.add(f.getAbsolutePath());

				Main.clearInternTables();
				Main.Arguments arguments = new Main.Arguments();
				Method parseMethod = Main.Arguments.class
						.getDeclaredMethod("parse", String[].class);
				parseMethod.setAccessible(true);
				parseMethod.invoke(arguments,
						(Object) args.toArray(new String[0]));
				Main.run(arguments);
			} catch (Exception e) {
				ex = e;
			}
			latch.countDown();
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (ex != null) {
			throw ex;
		}
	}

	@Override
	public String getTaskName() {
		return "Dex Task";
	}
}
