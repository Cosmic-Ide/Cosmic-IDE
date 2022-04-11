package com.pranav.lib_android.task.java;

import com.pranav.lib_android.util.FileUtil;
import com.pranav.lib_android.interfaces.*;
import com.android.tools.r8.D8;
import java.util.ArrayList;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class D8Task extends Task {

	private Exception ex = null;

	@Override
	public void doFullTask() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				ArrayList<String> args = new ArrayList<>();
				args.add("--output");
				args.add(FileUtil.getBinDir());
				args.add("--lib");
				args.add(FileUtil.getClasspathDir() + "android.jar");
				args.add(FileUtil.getBinDir() + "classes.jar");

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
