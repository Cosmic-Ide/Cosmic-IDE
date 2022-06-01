package com.pranav.common.util;

import com.pranav.common.Indexer;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtil {

    private static String privateDataDirectory;
    private static String javaDir;
    private Indexer indexer;

    public static void setDataDirectory(String directory) {
        privateDataDirectory = directory;
        javaDir = directory + "/java/";
    }
    
    public static void setJavaDirectory(String dir) {
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        javaDir = dir;
        try {
            new Indexer("editor").put("java_path", dir);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static boolean createDirectory(String path) {
        return new File(path).mkdir();
    }

    public static void writeFile(InputStream in, String path) throws IOException {
        var file = new File(path);
        Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void writeFile(String path, String content) throws IOException {
        var file = new File(path);
        file.getParentFile().mkdirs();
        file.delete();
        Files.write(file.toPath(), content.getBytes());
    }

    public static void writeFile(String path, byte[] content) throws IOException {
        var file = new File(path);
        file.getParentFile().mkdirs();
        file.delete();
        Files.write(file.toPath(), content);
    }

    public static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    public static void deleteFile(String path) {
        var file = new File(path);

        if (!file.exists()) return;

        if (file.isFile()) {
            file.delete();
            return;
        }

        var fileArr = file.listFiles();

        if (fileArr != null) {
            for (var subFile : fileArr) {
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
        var splited = path.split("/");
        return splited[splited.length - 1];
    }

    public static String getDataDir() {
        return privateDataDirectory;
    }

    public static String getJavaDir() {
        return javaDir;
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
