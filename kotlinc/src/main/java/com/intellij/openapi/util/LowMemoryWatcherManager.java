// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.util;

import com.intellij.openapi.Disposable;
import androidx.annotation.NonNull;
import java.util.concurrent.ExecutorService;

public final class LowMemoryWatcherManager implements Disposable {
  public LowMemoryWatcherManager(@NonNull ExecutorService backendExecutorService) {
    
  }

  @Override
  public void dispose() {
    
  }
}