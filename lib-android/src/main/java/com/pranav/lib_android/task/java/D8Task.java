package com.pranav.lib_android.task.java;

import com.pranav.lib_android.util.FileUtil;
import com.pranav.lib_android.interfaces.*;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class D8Task extends Task {

	private final Builder mBuilder;
	private Exception ex = null;

	public D8Task(Builder builder) {
	  this.mBuilder = builder;
	}

	@Override
	public void doFullTask() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				final File f = new File(
						FileUtil.getBinDir()
								+ "classes.jar");
				D8Command command = D8Command.builder();
									.setMinApiLevel(21)
									.addLibraryFiles(new File(FileUtil.getClasspathDir() + "android.jar").toPath());
									.addProgramFiles(f.toPath());
									.setOutput(new File(FileUtil.getBinDir() + "classes.dex").toPath(), OutputMode.DexIndexed);
									.build();
				D8.run(command);

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
