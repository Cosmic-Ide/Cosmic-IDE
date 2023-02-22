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
package com.tyron.javacompletion;

import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.completion.CompletionResult;
import com.tyron.javacompletion.file.FileManager;
import com.tyron.javacompletion.file.FileManagerImpl;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.options.IndexOptions;
import com.tyron.javacompletion.options.JavaCompletionOptions;
import com.tyron.javacompletion.project.Project;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class JavaCompletions {
    private static final JLogger logger = JLogger.createForEnclosingClass();
    private static final int NUM_THREADS = 10;

    private final ExecutorService mExecutor;

    private boolean mInitialized;
    private FileManager mFileManager;
    private Project mProject;

    public JavaCompletions() {
        mExecutor = Executors.newFixedThreadPool(NUM_THREADS);
    }

    public synchronized void initialize(URI projectRootUri, JavaCompletionOptions options) {
        checkState(!mInitialized, "Already initialized.");
        mInitialized = true;

        List<String> ignorePaths;

        logger.info("Initializing project: %s", projectRootUri);
        logger.info(
                "Options:\n  logPath: %s\n  logLevel: %s\n" + "  ignorePaths: %s\n  typeIndexFiles: %s",
                options.getLogPath(), options.getLogLevel(), options.getIgnorePaths(), options.getTypeIndexFiles());
        if (options.getLogPath() != null) {
            JLogger.setLogFile(options.getLogPath());
        }
        if (options.getIgnorePaths() != null) {
            ignorePaths = options.getIgnorePaths();
        } else {
            ignorePaths = ImmutableList.of();
        }
        mFileManager = new FileManagerImpl(projectRootUri, ignorePaths, mExecutor);
        mProject = new Project(mFileManager, projectRootUri, IndexOptions.FULL_INDEX_BUILDER.build());
        mExecutor.submit(() -> {
            synchronized (JavaCompletions.this) {
                mProject.initialize();
                mProject.loadJdkModule();
                if (options.getTypeIndexFiles() != null) {
                    for (String typeIndexFile : options.getTypeIndexFiles()) {
                        mProject.loadTypeIndexFile(typeIndexFile);
                    }
                }
            }
        });
    }

    public synchronized void shutdown() {
        checkState(mInitialized, "shutdown() called without being initialized.");
        mInitialized = false;
        mExecutor.shutdown();
    }

    public synchronized FileManager getFileManager() {
        checkState(mInitialized, "Not yet initialized.");
        return checkNotNull(mFileManager);
    }

    public synchronized Project getProject() {
        checkState(mInitialized, "Not yet initialized.");
        return checkNotNull(mProject);
    }

    /**
     * Used to inform the infrastructure that the contents of the file
     * has been changed. Useful if code editors are not writing the changes
     * to file immediately
     */
    public synchronized void updateFileContent(Path file, String newContent) {
        checkState(mInitialized, "Not yet initialized.");
        mFileManager.setSnaphotContent(file.toUri(), newContent);
    }

    /**
     * Retrieves completions with the file content
     * @param file Path of file to complete
     * @param line 0 based line of the caret
     * @param column 0 based column of the caret
     */
    public synchronized CompletionResult getCompletions(Path file, int line, int column) {
        checkState(mInitialized, "Not yet initialized.");
        return mProject.getCompletionResult(file, line, column);
    }
}
