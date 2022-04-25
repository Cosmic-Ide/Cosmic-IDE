package com.pranav.lib_android.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileUtil {

    private static Context mContext;

    public static void initializeContext(Context context) {
        mContext = context;
    }

    public static boolean createDirectory(String path) {
        File file = new File(path);
        return file.mkdir();
    }

    public static void createAndWriteToFile(String path, String content) throws IOException {
        File file = new File(path);

        if (file.exists()) return;

        if (file.createNewFile()) {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(mContext.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(content);
            outputStreamWriter.close();
        }
    }

    public static String readFile(File file) {
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            return "Error Reading File";
        }

        return text.toString();
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
