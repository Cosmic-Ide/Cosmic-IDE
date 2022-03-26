package com.pranav.lib_android.task.java;

import com.pranav.lib_android.util.FileUtil;
import com.pranav.lib_android.interfaces.*;
import com.android.tools.r8.D8;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

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
				args.add("--release");
				args.add("--lib");
				args.add(FileUtil.getClasspathDir() + "android.jar");
				args.add("--output");
				args.add(f.getParent());
				args.add(f.getAbsolutePath());

				D8.main(args.toArray(new String[0]));
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
		return "D8 Task";
	}
}
