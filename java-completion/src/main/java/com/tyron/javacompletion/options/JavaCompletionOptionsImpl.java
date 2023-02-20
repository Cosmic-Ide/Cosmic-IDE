package com.tyron.javacompletion.options;

import com.google.common.collect.ImmutableList;

import androidx.annotation.Nullable;
import java.util.List;
import java.util.logging.Level;

public class JavaCompletionOptionsImpl implements JavaCompletionOptions {

    private final String logPath;
    private final Level logLevel;
    private final List<String> ignoredPaths;
    private final List<String> indexFiles;

    public JavaCompletionOptionsImpl() {
        this(null, Level.OFF, ImmutableList.of(), ImmutableList.of());
    }

    public JavaCompletionOptionsImpl(String logPath,
                                     Level logLevel,
                                     List<String> ignoredPaths,
                                     List<String> indexFiles) {
        this.logPath = logPath;
        this.logLevel = logLevel;
        this.ignoredPaths = ignoredPaths;
        this.indexFiles = indexFiles;
    }

    @Nullable
    @Override
    public String getLogPath() {
        return logPath;
    }

    @Nullable
    @Override
    public Level getLogLevel() {
        return logLevel;
    }

    @Override
    public List<String> getIgnorePaths() {
        return ignoredPaths;
    }

    @Override
    public List<String> getTypeIndexFiles() {
        return indexFiles;
    }
}
