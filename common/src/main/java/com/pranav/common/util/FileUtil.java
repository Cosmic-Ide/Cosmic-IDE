package com.pranav.common.util;

import com.itsaky.androidide.utils.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
    private static String projectsDir;

    public static void setDataDirectory(String directory) {
        privateDataDirectory = directory + "/";
        projectsDir = privateDataDirectory + "projects";
        Environment.init(new File(privateDataDirectory, "compiler-modules"));
    }

    public static void setProjectsDirectory(String dir) {
        if(!dir.endsWith("/")) {
            dir += "/";
        }
        try {
            var path = Paths.get(dir).normalize();
            if(Files.isRegularFile(path)) {
                Files.delete(path);
            }
            Files.createDirectory(path);
            projectsDir = path.toString(); 
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

    public static String getProjectsDir() {
        return projectsDir;
    }

    public static String getClasspathDir() {
        return getDataDir() + "classpath/";
    }

    public static boolean createOrExistsDir(final String dirPath) {
        return createOrExistsDir(getFileByPath(dirPath));
    }

    public static boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    public static boolean createOrExistsFile(final String filePath) {
        return createOrExistsFile(getFileByPath(filePath));
    }

    public static boolean createOrExistsFile(final File file) {
        if (file == null) return false;
        if (file.exists()) return file.isFile();
        if (!createOrExistsDir(file.getParentFile())) return false;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean writeFileFromString(final String filePath, final String content) {
        return writeFileFromString(FileUtil.getFileByPath(filePath), content, false);
    }

    public static boolean writeFileFromString(final File file, final String content) {
        return writeFileFromString(file, content, false);
    }

    public static boolean writeFileFromString(final String filePath,
                                              final String content,
                                              final boolean append) {
        return writeFileFromString(FileUtil.getFileByPath(filePath), content, append);
    }

    public static boolean writeFileFromString(final File file,
                                              final String content,
                                              final boolean append) {
        if (file == null || content == null) return false;
        if (!FileUtil.createOrExistsFile(file)) {
            return false;
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file, append));
            bw.write(content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File getFileByPath(final String filePath) {
        return FileUtil.isSpace(filePath) ? null : new File(filePath);
    }

    public static boolean isSpace(final String s) {
        if (s == null) return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
