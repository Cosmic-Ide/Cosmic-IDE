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
