package com.pranav.javacompletion.options;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.logging.Level;


/** User provided options. */
public interface JavaCompletionOptions {
    /** Path of the log file. If not set, logs are not written to any file. */
    @Nullable
    public String getLogPath();

    /** The minimum log level. Logs with the level and above will be logged. */
    @Nullable
    public Level getLogLevel();

    public List<String> getIgnorePaths();

    public List<String> getTypeIndexFiles();
}