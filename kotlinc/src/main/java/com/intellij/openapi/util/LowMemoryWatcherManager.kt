// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.util

import com.intellij.openapi.Disposable
import java.util.concurrent.ExecutorService

class LowMemoryWatcherManager(@Suppress("UNUSED_PARAMETER") service: ExecutorService) : Disposable {

  override fun dispose() {}
}
