package org.cosmic.ide.project;

import org.cosmic.ide.common.util.FileUtil;

import java.io.File;
import java.io.IOException;

public class JavaProject {

    private static final String rootDirPath = FileUtil.getProjectsDir();

    private final File root;

    public JavaProject(File root) {
        this.root = root;
    }

    public static JavaProject newProject(String projectName) throws IOException {
        var projectRoot = new File(getRootDirPath() + projectName);
        if (!projectRoot.exists() && !projectRoot.mkdirs()) {
            throw new IOException("Unable to create directory");
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
        var classTemplate = CodeTemplate.getKotlinClassTemplate(null, "Main", true);
        FileUtil.writeFileFromString(getSrcDirPath() + "Main.kt", classTemplate);
    }

    public void delete() {
        FileUtil.deleteAllInDir(getProjectDirPath());
        FileUtil.delete(getProjectDirPath());
    }

    public File getRootFile() {
        return root;
    }

    public static String getRootDirPath() {
        return rootDirPath;
    }

    public String getProjectName() {
        return getRootFile().getName();
    }

    public String getProjectDirPath() {
        return getRootFile().getAbsolutePath() + File.separator;
    }

    public String getSrcDirPath() {
        return getProjectDirPath() + "src" + File.separator;
    }

    public String getBinDirPath() {
        return getProjectDirPath() + "bin" + File.separator;
    }

    public String getLibDirPath() {
        return getProjectDirPath() + "libs" + File.separator;
    }

    public String getBuildDirPath() {
        return getProjectDirPath() + "build" + File.separator;
    }

    public String getCacheDirPath() {
        return getProjectDirPath() + "cache" + File.separator;
    }
}
