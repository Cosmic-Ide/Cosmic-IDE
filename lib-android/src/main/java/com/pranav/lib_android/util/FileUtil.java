package com.pranav.lib_android.util;

import android.content.Context;

import java.io.File;

public class FileUtil {

	private static Context mContext;

	public static void initializeContext(Context context) {
		mContext = context;
	}

	public static void deleteFile(String path) {
		File file = new File(path);

		if (!file.exists())
			return;

		if (file.isFile()) {
			file.delete();
			return;
		}

		File[] fileArr = file.listFiles();

		if (fileArr != null) {
			for (File subFile : fileArr) {
				if (subFile.isDirectory()) {
					deleteFile(subFile.getAbsolutePath());
				}

				if (subFile.isFile()) {
					subFile.delete();
				}
			}
		}

		file.delete();
	}

	private static String getDataDir() {
		return mContext.getExternalFilesDir(null).getAbsolutePath();
	}

	public static String getJavaDir() {
		return getDataDir() + "/java/";
	}

	public static String getBinDir() {
		return getDataDir() + "/bin/";
	}

	public static String getClasspathDir() {
		return getDataDir() + "/classpath/";
	}
}
