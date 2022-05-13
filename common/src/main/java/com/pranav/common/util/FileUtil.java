package com.pranav.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtil {

    private static String privateDataDirectory;

    public static void setDataDirectory(String directory) {
        privateDataDirectory = directory;
    }

    public static boolean createDirectory(String path) {
        return new File(path).mkdir();
    }

    public static void writeFile(InputStream in, String path) throws IOException {
        File file = new File(path);
        Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void writeFile(String path, String content) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        file.delete();
        Files.write(file.toPath(), content.getBytes());
    }

    public static void writeFile(String path, byte[] content) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        file.delete();
        Files.write(file.toPath(), content);
    }

    public static String readFile(File file) throws IOException {
        return new String(readBytes(file));
    }

    public static byte[] readBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    public static void deleteFile(String path) {
        File file = new File(path);

        if (!file.exists()) return;

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

    public static String getFileName(String path) {
        String[] splited = path.split("/");
        return splited[splited.length - 1];
    }

    private static String getDataDir() {
        return privateDataDirectory;
    }

    public static String getJavaDir() {
        return getDataDir() + "/java/";
    }

    public static String getBinDir() {
        return getDataDir() + "/bin/";
    }

    public static String getCacheDir() {
        // write caches to external storage because we don't want android system
        // to delete index files
        return getDataDir() + "/cache/";
    }

    public static String getClasspathDir() {
        return getDataDir() + "/classpath/";
    }
}
