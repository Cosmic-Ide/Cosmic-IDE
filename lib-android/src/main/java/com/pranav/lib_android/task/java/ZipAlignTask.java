package com.pranav.lib_android.task.java;

import com.pranav.lib_android.interfaces.*;
import com.pranav.lib_android.util.BinaryExecutor;
import com.pranav.lib_android.ZipUtil;
import com.pranav.lib_android.FileUtil;
import com.pranav.lib_android.exception.CompilationFailedException;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class ZipAlignTask extends Task {

	private final Builder mBuilder;
	private final BinaryExecutor executor;
	private Exception ex = null;

	public ZipAlignTask(Builder builder) {
		this.mBuilder = builder;
		this.executor = new BinaryExecutor();
	}

	@Override
	public void doFullTask() throws Exception {
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				final String zipalign = mBuilder.getContext().getFilesDir()
						+ "/arm64-v8a";
				if (!new File(zipalign).exists()) {
					ZipUtil.copyFileFromAssets(mBuilder.getContext(),
							"zipalign/arm64-v8a", "arm64-v8a");
				}
				Runtime.getRuntime().exec("chmod 777 " + zipalign);
				final ArrayList<String> args = new ArrayList<>();
				args.add(zipalign);
				args.add("4");
				args.add(FileUtil.getBinDir()
						+ "classes.jar");
				args.add(FileUtil.getBinDir()
						+ "zipAligned.jar");
				executor.execute(args);
			} catch (Exception e) {
				ex = e;
			}
		});
		if (executor.getLogs() != "") {
			throw new CompilationFailedException(executor.getLogs());
		}
		if (ex != null)
			throw ex;
	}

	@Override
	public String getTaskName() {
		return "Zip Align Task";
	}
}
