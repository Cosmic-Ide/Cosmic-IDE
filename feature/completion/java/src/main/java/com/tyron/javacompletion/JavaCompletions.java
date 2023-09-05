/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.tyron.javacompletion.completion.CompletionResult;
import com.tyron.javacompletion.file.FileManager;
import com.tyron.javacompletion.file.FileManagerImpl;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.options.IndexOptions;
import com.tyron.javacompletion.options.JavaCompletionOptions;
import com.tyron.javacompletion.project.Project;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JavaCompletions {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private final ExecutorService mExecutor;

    private static final String notInit = "Not yet initialized.";

    private boolean mInitialized;
    private FileManager mFileManager;
    private Project mProject;

    public JavaCompletions() {
        mExecutor = Executors.newCachedThreadPool();
    }

    public synchronized void initialize(URI projectRootUri, JavaCompletionOptions options) {
        checkState(!mInitialized, "Already initialized.");
        mInitialized = true;

        List<String> ignorePaths;

        logger.info("Initializing project: %s", projectRootUri);
        logger.info(
                """
                        Options:
                          logPath: %s
                          logLevel: %s
                          ignorePaths: %s
                          typeIndexFiles: %s""",
                options.getLogPath(), options.getLogLevel(), options.getIgnorePaths(), options.getTypeIndexFiles());
        if (options.getLogPath() != null) {
            JLogger.setLogFile(options.getLogPath());
        }
        if (options.getIgnorePaths() != null) {
            ignorePaths = options.getIgnorePaths();
        } else {
            ignorePaths = List.of();
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
        mFileManager.shutdown();
    }

    public synchronized Project getProject() {
        checkState(mInitialized, notInit);
        return checkNotNull(mProject);
    }

    /**
     * Used to inform the infrastructure that the contents of the file
     * has been changed. Useful if code editors are not writing the changes
     * to file immediately
     */
    public synchronized void updateFileContent(Path file, String newContent) {
        checkState(mInitialized, notInit);
        mFileManager.setSnaphotContent(file.toUri(), newContent);
    }

    public synchronized void openFile(Path file, String content) throws IOException {
        checkState(mInitialized, notInit);
        mFileManager.openFileForSnapshot(file.toUri(), content);
    }

    /**
     * Retrieves completions with the file content
     *
     * @param file   Path of file to complete
     * @param line   0 based line of the caret
     * @param column 0 based column of the caret
     */
    public synchronized CompletionResult getCompletions(Path file, int line, int column) {
        checkState(mInitialized, notInit);
        return mProject.getCompletionResult(file, line, column);
    }
}
