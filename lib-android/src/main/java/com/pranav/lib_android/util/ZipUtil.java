package com.pranav.lib_android.util;

import android.content.Context;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {
	private static final int BUFFER_SIZE = 1024 * 10;
	private static final String TAG = "ZipUtil";

	public static void unzipFromAssets(Context context, String zipFile,
	String destination) {
		try {
			InputStream stream = context.getAssets().open(zipFile);
			unzip(stream, destination);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void unzip(InputStream stream, String destination) {
		dirChecker(destination, "");
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			ZipInputStream zin = new ZipInputStream(stream);
			ZipEntry ze = null;

			while ((ze = zin.getNextEntry()) != null) {
				Log.v(TAG, "Unzipping " + ze.getName());

				if (ze.isDirectory()) {
					dirChecker(destination, ze.getName());
				} else {
					File f = new File(destination, ze.getName());
					if (!f.toPath().normalize().startsWith(destination))
						throw new SecurityException("Potentially harmful files detected inside zip");
					if (!f.exists()) {
						boolean success = f.createNewFile();
						if (!success) {
							Log.w(TAG, "Failed to create file " + f.getName());
							continue;
						}
						FileOutputStream fout = new FileOutputStream(f);
						int count;
						while ((count = zin.read(buffer)) != -1) {
							fout.write(buffer, 0, count);
						}
						zin.closeEntry();
						fout.close();
					}
				}
			}
			zin.close();
		} catch (IOException e) {
			Log.e(TAG, "unzip", e);
		}
	}

	private static void dirChecker(String destination, String dir) {
		File f = new File(destination, dir);

		if (!f.isDirectory() && !f.mkdirs()) {
				Log.w(TAG, "Failed to create folder " + f.getName());
		}
	}

	public static void copyFileFromAssets(Context context, String inputFile, String fileName) throws IOException {
		final InputStream in = context.getAssets().open(inputFile);
		final String outputPath = context.getFilesDir() + "/" + fileName;
		final File out = new File(outputPath);
		Files.write(ByteStreams.toByteArray(in), out);
	}
}
