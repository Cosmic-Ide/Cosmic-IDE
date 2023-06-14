/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
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
package com.tyron.javacompletion.logging;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Wrapper around Java logger.
 *
 * <p>This wrapper provide convenient methods for creating loggers and logging with formatting
 * strings.
 */
public class JLogger {
    private static volatile boolean hasFileHandler = false;
    private final Logger javaLogger;

    private JLogger(String enclosingClassName) {
        javaLogger = Logger.getLogger(enclosingClassName);
    }

    public static synchronized void setLogFile(String filePath) {
        Logger rootLogger = Logger.getLogger("");

        if (hasFileHandler) {
            rootLogger.warning("Log file has already been set.");
            return;
        }
        hasFileHandler = true;

        try {
            FileHandler fileHandler = new FileHandler(filePath);
            fileHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(fileHandler);
        } catch (Exception ignored) {
        }
    }

    /**
     * Creates a {@link JLogger} and sets its tag to the name of the class that calls it.
     */
    public static JLogger createForEnclosingClass() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        // The top of the stack trace is this method. The one belows it is the caller class.
        String enclosingClassName = stackTrace[1].getClassName();
        return new JLogger(enclosingClassName);
    }

    private static StackTraceElement findCallerStackTraceElement() {
        Throwable throwable = new Throwable();
        StackTraceElement[] stackTrace = throwable.getStackTrace();

        for (StackTraceElement stackTraceElement : stackTrace) {
            String className = stackTraceElement.getClassName();
            if (!JLogger.class.getCanonicalName().equals(className)) {
                // We've skipped all JLogger. This is the caller.
                return stackTraceElement;
            }
        }
        return null;
    }

    /**
     * Logs a message at severe level with formatting parameters.
     *
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args   arguments to be filled into {@code msgfmt}
     */
    public void severe(String msgfmt, Object... args) {
        log(Level.SEVERE, String.format(msgfmt, args), null /* thrown */);
    }

    /**
     * Logs a message at severe level with formatting parameters and associated Throwable information.
     *
     * @param thrown Throwable associated with the log message
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args   arguments to be filled into {@code msgfmt}
     */
    public void severe(Throwable thrown, String msgfmt, Object... args) {
        log(Level.SEVERE, String.format(msgfmt, args), thrown);
    }

    /**
     * Logs a message at warning level.
     */
    public void warning(String msg) {
        log(Level.WARNING, msg, null /* thrown */);
    }

    /**
     * Logs a message at warning level with formatting parameters.
     *
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args   arguments to be filled into {@code msgfmt}
     */
    public void warning(String msgfmt, Object... args) {
        log(Level.WARNING, String.format(msgfmt, args), null /* thrown */);
    }

    /**
     * Logs a message at warning level with formatting parameters and associated Throwable
     * information.
     *
     * @param thrown Throwable associated with the log message
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args   arguments to be filled into {@code msgfmt}
     */
    public void warning(Throwable thrown, String msgfmt, Object... args) {
        log(Level.WARNING, String.format(msgfmt, args), thrown);
    }

    /**
     * Logs a message at info level.
     */
    public void info(String msg) {
        log(Level.INFO, msg, null /* thrown */);
    }

    /**
     * Logs a message at info level with formatting parameters.
     *
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args   arguments to be filled into {@code msgfmt}
     */
    public void info(String msgfmt, Object... args) {
        log(Level.INFO, String.format(msgfmt, args), null /* thrown */);
    }

    /**
     * Logs a message at fine level with formatting parameters.
     *
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args   arguments to be filled into {@code msgfmt}
     */
    public void fine(String msgfmt, Object... args) {
        log(Level.FINE, String.format(msgfmt, args), null /* thrown */);
    }

    private void log(Level level, String msg, Throwable thrown) {
        LogRecord logRecord = new LogRecord(level, msg);
        if (thrown != null) {
            logRecord.setThrown(thrown);
        }
        StackTraceElement callerStackTraceElement = findCallerStackTraceElement();
        if (callerStackTraceElement != null) {
            logRecord.setSourceClassName(callerStackTraceElement.getClassName());
            logRecord.setSourceMethodName(callerStackTraceElement.getMethodName());
        }
        javaLogger.log(logRecord);
    }
}