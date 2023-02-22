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

import androidx.annotation.Nullable;

import java.util.List;
import java.util.logging.Level;


/** User provided options. */
public interface JavaCompletionOptions {
    /** Path of the log file. If not set, logs are not written to any file. */
    @Nullable
    String getLogPath();

    /** The minimum log level. Logs with the level and above will be logged. */
    @Nullable
    Level getLogLevel();

    List<String> getIgnorePaths();

    List<String> getTypeIndexFiles();
}