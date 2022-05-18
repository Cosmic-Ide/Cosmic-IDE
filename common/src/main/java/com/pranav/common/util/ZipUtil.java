package com.pranav.common.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    private static int BUFFER_SIZE = 1024 * 10;

    public static void unzipFromAssets(Context context, String zipFile, String destination) {
        try {
            var stream = context.getAssets().open(zipFile);
            unzip(stream, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void unzip(InputStream stream, String destination) {
        dirChecker(destination, "");
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            var zin = new ZipInputStream(stream);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    dirChecker(destination, ze.getName());
                } else {
                    var f = new File(destination, ze.getName());
                    if (!f.toPath().normalize().startsWith(destination))
                        throw new SecurityException(
                                "Potentially harmful files detected inside zip");
                    if (!f.exists()) {
                        if (!f.createNewFile()) {
                            continue;
                        }
                        var fout = new FileOutputStream(f);
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
            e.printStackTrace();
        }
    }

    private static void dirChecker(String destination, String dir) {
        var f = new File(destination, dir);

        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }
}
