package com.pranav.common.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    public static void unzipFromAssets(Context context, String zipFile, String destination) {
        try {
            InputStream stream = context.getAssets().open(zipFile);
            unzip(stream, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void unzip(InputStream stream, String destination) {
        dirChecker(destination, "");
        try {
            ZipInputStream zin = new ZipInputStream(stream);
            ZipEntry ze = null;

            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    dirChecker(destination, ze.getName());
                } else {
                    File f = new File(destination, ze.getName());
                    if (!f.toPath().normalize().startsWith(destination))
                        throw new SecurityException(
                                "Potentially harmful files detected inside zip");
                    if (!f.exists()) {
                        if (!f.createNewFile()) {
                            continue;
                        }
                        byte[] data = new byte[zin.available()];
                        zin.read(data);
                        FileUtil.writeFile(f.getAbsolutePath(), data);
                        zin.closeEntry();
                    }
                }
            }
            zin.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dirChecker(String destination, String dir) {
        File f = new File(destination, dir);

        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }
}
