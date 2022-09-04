// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.com.intellij.openapi.diagnostic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.kotlin.com.intellij.util.ExceptionUtil;
import org.jetbrains.kotlin.org.apache.log4j.Level;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultLogger extends Logger {
  private static boolean ourMirrorToStderr = true;

  @SuppressWarnings("UnusedParameters")
  public DefaultLogger(String category) { }

  @Override
  public boolean isDebugEnabled() {
    return false;
  }

  @Override
  public void debug(String message) { }

  @Override
  public void debug(Throwable t) { }

  @Override
  public void debug(@NonNull String message, Throwable t) { }

  @Override
  public void info(String message) { }

  @Override
  public void info(String message, Throwable t) { }

  @Override
  @SuppressWarnings({"UseOfSystemOutOrSystemErr", "HardCodedStringLiteral"})
  public void warn(@NonNull String message, @Nullable Throwable t) {
    t = checkException(t);
    System.err.println("WARN: " + message);
    if (t != null) t.printStackTrace(System.err);
  }

  @Override
  public void error(String message, @Nullable Throwable t, @NonNull String... details) {
    t = checkException(t);
    message += attachmentsToString(t);
    dumpExceptionsToStderr(message, t, details);

    throw new AssertionError(message, t);
  }

  @SuppressWarnings({"UseOfSystemOutOrSystemErr", "HardCodedStringLiteral"})
  public static void dumpExceptionsToStderr(String message,
                                            @Nullable Throwable t,
                                            @NonNull String... details) {
    if (shouldDumpExceptionToStderr()) {
      System.err.println("ERROR: " + message);
      if (t != null) t.printStackTrace(System.err);
      if (details.length > 0) {
        System.err.println("details: ");
        for (String detail : details) {
          System.err.println(detail);
        }
      }
    }
  }

  @Override
  public void setLevel(Level level) { }

  public static @NonNls String attachmentsToString(@Nullable Throwable t) {
    if (t != null) {
        return "\n\nAttachments:\n" + StringUtil.join(ExceptionUtil.getThrowableText(t), ATTACHMENT_TO_STRING::apply, "\n----\n");
    }
    return "";
  }

  public static boolean shouldDumpExceptionToStderr() {
    return ourMirrorToStderr;
  }

  public static void disableStderrDumping(@NonNull Disposable parentDisposable) {
    final boolean prev = ourMirrorToStderr;
    ourMirrorToStderr = false;
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        ourMirrorToStderr = prev;
      }
    });
  }
}