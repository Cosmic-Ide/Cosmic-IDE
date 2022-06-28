package com.pranav.common.util;

import com.itsaky.androidide.utils.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtil {

    private static String privateDataDirectory;
    private static String javaDir;

    public static void setDataDirectory(String directory) {
        privateDataDirectory = directory + "/";
        javaDir = privateDataDirectory + "java/";
        Environment.init(new File(privateDataDirectory, "compiler-modules"));
    }

    public static void setJavaDirectory(String dir) {
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        try {
            var path = Paths.get(dir).normalize();
            if (Files.isRegularFile(path)) {
                Files.delete(path);
            }
            Files.createDirectories(path);
            javaDir = path.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean createDirectory(String path) {
        return new File(path).mkdir();
    }

    public static void writeFile(InputStream in, String path) throws IOException {
        var filePath = Paths.get(path).normalize();
        Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void writeFile(String path, String content) throws IOException {
        var file = new File(path);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), content.getBytes());
    }

    public static void writeFile(String path, byte[] content) throws IOException {
        var file = new File(path);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), content);
    }

    public static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    public static void deleteFile(String p) {
        try {
            var path = Paths.get(p);
            if (Files.isRegularFile(path)) {
                Files.delete(path);
                return;
            }

            Files.walkFileTree(
                    path,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException e)
                                throws IOException {
                            if (e != null) {
                                e.printStackTrace();
                            }
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getFileName(String path) {
        var splited = path.split("/");
        return splited[splited.length - 1].replace(".java", "");
    }

    public static String getDataDir() {
        return privateDataDirectory;
    }

    public static String getJavaDir() {
        return javaDir;
    }

    public static String getBinDir() {
        return getDataDir() + "bin/";
    }

    public static String getCacheDir() {
        // write caches to external storage because we don't want android system
        // to delete index files
        return getDataDir() + "cache/";
    }

    public static String getClasspathDir() {
        return getDataDir() + "classpath/";
    }
}
