package com.tyron.javacompletion.logging;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
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
        } catch (Exception e) {
        }
    }

    public static synchronized void setLogLevel(Level level) {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(level);
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(level);
        }
    }

    private JLogger(String enclosingClassName) {
        javaLogger = Logger.getLogger(enclosingClassName);
    }

    /** Creates a {@link JLogger} and sets its tag to the name of the class that calls it. */
    public static JLogger createForEnclosingClass() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        // The top of the stack trace is this method. The one belows it is the caller class.
        String enclosingClassName = stackTrace[1].getClassName();
        return new JLogger(enclosingClassName);
    }

    /** Logs a message at severe level. */
    public void severe(String msg) {
        log(Level.SEVERE, msg, null /* thrown */);
    }

    /**
     * Logs a message at severe level with formatting parameters.
     *
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args arguments to be filled into {@code msgfmt}
     */
    public void severe(String msgfmt, Object... args) {
        log(Level.SEVERE, String.format(msgfmt, args), null /* thrown */);
    }

    /**
     * Logs a message at severe level with formatting parameters and associated Throwable information.
     *
     * @param thrown Throwable associated with the log message
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args arguments to be filled into {@code msgfmt}
     */
    public void severe(Throwable thrown, String msgfmt, Object... args) {
        log(Level.SEVERE, String.format(msgfmt, args), thrown);
    }

    /** Logs a message at warning level. */
    public void warning(String msg) {
        log(Level.WARNING, msg, null /* thrown */);
    }

    /**
     * Logs a message at warning level with formatting parameters.
     *
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args arguments to be filled into {@code msgfmt}
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
     * @param args arguments to be filled into {@code msgfmt}
     */
    public void warning(Throwable thrown, String msgfmt, Object... args) {
        log(Level.WARNING, String.format(msgfmt, args), thrown);
    }

    /** Logs a message at info level. */
    public void info(String msg) {
        log(Level.INFO, msg, null /* thrown */);
    }

    /**
     * Logs a message at info level with formatting parameters.
     *
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args arguments to be filled into {@code msgfmt}
     */
    public void info(String msgfmt, Object... args) {
        log(Level.INFO, String.format(msgfmt, args), null /* thrown */);
    }

    /** Logs a message at fine level. */
    public void fine(String msg) {
        log(Level.FINE, msg, null /* thrown */);
    }

    /**
     * Logs a message at fine level with formatting parameters.
     *
     * @param msgfmt the message format string that can be accepted by {@link String#format}
     * @param args arguments to be filled into {@code msgfmt}
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

    private static StackTraceElement findCallerStackTraceElement() {
        Throwable throwable = new Throwable();
        StackTraceElement[] stackTrace = throwable.getStackTrace();

        boolean loggerStackTraceFound = true;
        for (StackTraceElement stackTraceElement : stackTrace) {
            String className = stackTraceElement.getClassName();
            if (JLogger.class.getCanonicalName().equals(className)) {
                loggerStackTraceFound = true;
            } else {
                if (loggerStackTraceFound) {
                    // We've skipped all JLogger. This is the caller.
                    return stackTraceElement;
                }
            }
        }
        return null;
    }
}