/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tyron.javacompletion.project;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.Optional;
import com.tyron.javacompletion.completion.CompletionResult;
import com.tyron.javacompletion.completion.Completor;
import com.tyron.javacompletion.file.FileChangeListener;
import com.tyron.javacompletion.file.FileManager;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.options.IndexOptions;
import com.tyron.javacompletion.storage.IndexStore;
import org.cosmic.ide.common.util.FileUtil;

public class Project {

    private static final JLogger logger = JLogger.createForEnclosingClass();

    private static final String JAVA_EXTENSION = ".java";

    private final FileManager fileManager;
    private final Completor completor;
    private final ModuleManager moduleManager;
    private Path lastCompletedFile = null;

    private boolean initialized;

    public Project(FileManager fileManager, URI rootUri, IndexOptions indexOptions) {
        this(new FileSystemModuleManager(fileManager, Paths.get(rootUri), indexOptions), fileManager);
    }

    public Project(ModuleManager moduleManager, FileManager fileManager) {
        completor = new Completor(fileManager);
        this.fileManager = fileManager;
        this.moduleManager = moduleManager;
    }

    public synchronized void initialize() {
        if (initialized) {
            logger.warning("Project has already been initalized.");
            return;
        }
        initialized = true;

        fileManager.setFileChangeListener(new ProjectFileChangeListener());
        moduleManager.initialize();
    }

    private synchronized void addOrUpdateFile(Path filePath) {
        // Only fix content for files that are under completion.
        boolean fixContentForParsing = lastCompletedFile != null && lastCompletedFile.equals(filePath);
        moduleManager.addOrUpdateFile(filePath, fixContentForParsing);
    }

    public synchronized void loadJdkModule() {
        logger.info("Loading JDK module");
        try (BufferedReader reader =
                     Files.newBufferedReader(Paths.get(FileUtil.getDataDir(), "index.json"))) {
            moduleManager.addDependingModule(new IndexStore().readModule(reader));
            logger.info("JDK module loaded");
        } catch (Throwable t) {
            logger.warning(t, "Unable to load JDK module");
        }
    }

    public synchronized void loadTypeIndexFile(String typeIndexFile) {
        logger.info("Loading type index file %s", typeIndexFile);
        IndexStore indexStore = new IndexStore();
        try {
            Module module =
                    indexStore.readModuleFromFile(
                            fileManager.getProjectRootPath().resolve(Paths.get(typeIndexFile)));
            moduleManager.addDependingModule(module);
            logger.info("Loaded type index file %s", typeIndexFile);
        } catch (NoSuchFileException nsfe) {
            logger.warning("Unable to load type index file %s: file doesn't exist", typeIndexFile);
        } catch (Throwable t) {
            logger.warning(t, "Unable to load type index file %s", typeIndexFile);
        }
    }

    /**
     * @param filePath the path of the file beging completed
     * @param line 0-based line number
     * @param column 0-based character offset of the line
     */
    public synchronized CompletionResult getCompletionResult(Path filePath, int line, int column) {
        if (!filePath.equals(lastCompletedFile)) {
            lastCompletedFile = filePath;
            addOrUpdateFile(filePath);
        }
        return completor.getCompletionResult(moduleManager, filePath, line, column);
    }

    private static boolean isJavaFile(Path filePath) {
        // We don't check if file is regular file here because the file may be new in editor and not
        // saved to the file system.
        return filePath.toString().endsWith(JAVA_EXTENSION) && !Files.isDirectory(filePath);
    }

    private class ProjectFileChangeListener implements FileChangeListener {
        @Override
        public void onFileChange(Path filePath, WatchEvent.Kind<?> changeKind) {
            logger.fine("onFileChange(%s): %s", changeKind, filePath);
            if (changeKind == StandardWatchEventKinds.ENTRY_CREATE
                    || changeKind == StandardWatchEventKinds.ENTRY_MODIFY) {
                if (isJavaFile(filePath)) {
                    addOrUpdateFile(filePath);
                }
            } else if (changeKind == StandardWatchEventKinds.ENTRY_DELETE) {
                // Do not check if the file is a java source file here. Deleted file is not a regular file.
                // The module handles nonexistence file correctly.
                moduleManager.removeFile(filePath);
            }
        }
    }
}
