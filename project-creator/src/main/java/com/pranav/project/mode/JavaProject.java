package com.pranav.project.mode;

import com.pranav.common.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List; 

public class JavaProject {

    private static final String rootDirPath = FileUtil.getProjectsDir();

    private static final String srcDirName = "src";
    private static final String binDirName = "bin";
    private static final String libDirName = "lib";
    private static final String buildDirName = "build";
    private static final String cacheDirName = "cache";

    private final File root;

    private String projectName;
    private String projectDirPath;

    public JavaProject(File root) {
        this.root = root;
        this.projectName = root.getName();
        this.projectDirPath = getRootDirPath() + projectName;
    }

    public static JavaProject newProject(String projectName) throws IOException {
        var projectRoot = new File(getRootDirPath() + projectName);
        if (!projectRoot.exists()) {
            if (!projectRoot.mkdirs()) {
                throw new IOException("Unable to create directory");
            }
        }
        var project = new JavaProject(projectRoot);
        project.init();
        return project;
    }

    public void init() {
        FileUtil.createOrExistsDir(getProjectDirPath());
        FileUtil.createOrExistsDir(getSrcDirPath());
        FileUtil.createOrExistsDir(getBinDirPath());
        FileUtil.createOrExistsDir(getLibDirPath());
        FileUtil.createOrExistsDir(getBuildDirPath());
        FileUtil.createOrExistsDir(getCacheDirPath());
        var classTemplate = JavaTemplate.getKotlinClassTemplate(null, "Main", true);
        FileUtil.writeFileFromString(getSrcDirPath() + "Main.kt", classTemplate);
    }

    public void delete() {
        FileUtil.deleteAllInDir(projectDirPath);
        FileUtil.delete(projectDirPath);
    }

    public File getRootFile() {
        return root;
    }

    public static String getRootDirPath() {
        return rootDirPath;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectDirPath() {
        return projectDirPath + "/";
    }

    public String getSrcDirPath() {
        return getProjectDirPath() + srcDirName + "/";
    }

    public String getBinDirPath() {
        return getProjectDirPath() + binDirName + "/";
    }

    public String getLibDirPath() {
        return getProjectDirPath + libDirName + "/";
    }

    public String getBuildDirPath() {
        return getProjectDirPath() + buildDirName + "/";
    }

    public String getCacheDirPath() {
        return getProjectDirPath() + cacheDirName + "/";
    }
}