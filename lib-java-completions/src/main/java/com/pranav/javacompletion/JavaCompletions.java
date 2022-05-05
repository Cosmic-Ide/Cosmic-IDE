package com.pranav.javacompletion;

import com.google.common.collect.ImmutableList;
import com.pranav.javacompletion.file.FileManager;
import com.pranav.javacompletion.file.FileManagerImpl;
import com.pranav.javacompletion.logging.JLogger;
import com.pranav.javacompletion.options.IndexOptions;
import com.pranav.javacompletion.options.JavaCompletionOptions;
import com.pranav.javacompletion.project.Project;

import java.net.URI;
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
}
