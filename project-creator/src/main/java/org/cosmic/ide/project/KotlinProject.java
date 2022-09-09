package org.cosmic.ide.project;

import org.cosmic.ide.common.util.FileUtil;

import java.io.File;
import java.io.IOException;

public class KotlinProject implements Project {

    private static final String rootDirPath = FileUtil.getProjectsDir();

    private final File root;

    public KotlinProject(File root) {
        this.root = root;
    }

    @Override
    public static Project newProject(String projectName) throws IOException {
        var projectRoot = new File(getRootDirPath() + projectName);
        if (!projectRoot.exists() && !projectRoot.mkdirs()) {
            throw new IOException("Unable to create directory");
        }
        var project = new KotlinProject(projectRoot);
        project.init();
        return project;
    }

    private void init() {
        FileUtil.createOrExistsDir(getProjectDirPath());
        FileUtil.createOrExistsDir(getSrcDirPath());
        FileUtil.createOrExistsDir(getBinDirPath());
        FileUtil.createOrExistsDir(getLibDirPath());
        FileUtil.createOrExistsDir(getBuildDirPath());
        FileUtil.createOrExistsDir(getCacheDirPath());
        var classTemplate = CodeTemplate.getKotlinClassTemplate(null, "Main", true);
        FileUtil.writeFileFromString(getSrcDirPath() + "Main.kt", classTemplate);
    }

    @Override
    public void delete() {
        FileUtil.deleteAllInDir(getProjectDirPath());
        FileUtil.delete(getProjectDirPath());
    }

    @Override
    public File getRootFile() {
        return root;
    }

    @Override
    public static String getRootDirPath() {
        return rootDirPath;
    }

    @Override
    public String getProjectName() {
        return getRootFile().getName();
    }

    @Override
    public String getProjectDirPath() {
        return getRootFile().getAbsolutePath() + File.separator;
    }

    @Override
    public String getSrcDirPath() {
        return getProjectDirPath() + "src" + File.separator;
    }

    @Override
    public String getBinDirPath() {
        return getProjectDirPath() + "bin" + File.separator;
    }

    @Override
    public String getLibDirPath() {
        return getProjectDirPath() + "libs" + File.separator;
    }

    @Override
    public String getBuildDirPath() {
        return getProjectDirPath() + "build" + File.separator;
    }

    @Override
    public String getCacheDirPath() {
        return getProjectDirPath() + "cache" + File.separator;
    }
}
