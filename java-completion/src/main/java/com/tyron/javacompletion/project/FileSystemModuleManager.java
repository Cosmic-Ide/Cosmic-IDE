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

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import com.tyron.javacompletion.file.FileManager;
import com.tyron.javacompletion.file.PathUtils;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.options.IndexOptions;
import com.tyron.javacompletion.parser.Parser;
import com.tyron.javacompletion.parser.classfile.ClassModuleBuilder;

public class FileSystemModuleManager implements ModuleManager {
    private static final JLogger logger = JLogger.createForEnclosingClass();
    private static final String JAVA_EXTENSION = ".java";
    private static final String JAR_EXTENSION = ".jar";
    private static final String SRCJAR_EXTENSION = ".srcjar";
    private static final String CLASS_EXTENSION = ".class";

    private final Module projectModule;
    private final Path rootPath;
    private final FileManager fileManager;
    private final Parser parser;

    public FileSystemModuleManager(
            FileManager fileManager, Path rootPath, IndexOptions indexOptions) {
        projectModule = new Module();
        this.rootPath = rootPath;
        this.fileManager = fileManager;
        this.parser = new Parser(fileManager, indexOptions);
    }

    @Override
    public synchronized void initialize() {
        walkDirectory(rootPath);
    }

    @Override
    public synchronized Optional<FileItem> getFileItem(Path path) {
        Deque<Module> queue = new LinkedList<>();
        Set<Module> visitedModules = new HashSet<>();
        queue.addLast(projectModule);
        while (!queue.isEmpty()) {
            Module module = queue.removeFirst();
            Optional<FileScope> fileScope = module.getFileScope(path.toString());
            if (fileScope.isPresent()) {
                return Optional.of(
                        FileItem.newBuilder()
                                .setPath(path)
                                .setModule(module)
                                .setFileScope(fileScope.get())
                                .build());
            }
            for (Module dependency : module.getDependingModules()) {
                if (!visitedModules.contains(module)) {
                    visitedModules.add(module);
                    queue.addLast(module);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized void addOrUpdateFile(Path path, boolean fixContentForParsing) {
        Optional<FileItem> existingFileItem = getFileItem(path);
        Module module =
                existingFileItem.isPresent() ? existingFileItem.get().getModule() : projectModule;
        addOrUpdateFile(module, path, fixContentForParsing);
    }

    private void addOrUpdateFile(Module module, Path path, boolean fixContentForParsing) {
        try {
            Optional<FileScope> fileScope = parser.parseSourceFile(path, fixContentForParsing);
            if (fileScope.isPresent()) {
                module.addOrReplaceFileScope(fileScope.get());
            }
        } catch (Throwable e) {
            logger.warning(e, "Failed to process file %s", path);
        }
    }

    @Override
    public synchronized void addDependingModule(Module module) {
        projectModule.addDependingModule(module);
    }

    @Override
    public synchronized void removeFile(Path path) {
        projectModule.removeFile(path);
    }

    private void walkDirectory(Path rootDir) {
        ImmutableMap<String, Consumer<Path>> handlers =
                ImmutableMap.of(
                        JAVA_EXTENSION,
                        path -> addOrUpdateFile(projectModule, path, /* fixContentForParsing= */ false),
                        JAR_EXTENSION,
                        path -> addJarModule(path),
                        SRCJAR_EXTENSION,
                        path -> addJarModule(path));

        PathUtils.walkDirectory(rootDir, handlers, path -> fileManager.shouldIgnorePath(path));
    }

    private void addJarModule(Path path) {
        logger.fine("Adding JAR module for %s", path);
        try {
            Module jarModule = new Module();
            ClassModuleBuilder classModuleBuilder = new ClassModuleBuilder(jarModule);
            Path rootJarPath = PathUtils.getRootPathForJarFile(path);
            ImmutableMap<String, Consumer<Path>> handlers =
                    ImmutableMap.of(
                            JAVA_EXTENSION,
                            filePath -> addOrUpdateFile(jarModule, filePath, /* fixContentForParsing= */ false),
                            CLASS_EXTENSION,
                            filePath -> {
                                try {
                                    classModuleBuilder.processClassFile(filePath);
                                } catch (Throwable t) {
                                    logger.warning(t, "Failed to process .class file: %s", filePath);
                                }
                            });
            PathUtils.walkDirectory(rootJarPath, handlers, /* ignorePathPredicate= */ filePath -> false);
            projectModule.addDependingModule(jarModule);
        } catch (Throwable t) {
            logger.warning(t, "Failed to create module for JAR file %s", path);
        }
    }
}