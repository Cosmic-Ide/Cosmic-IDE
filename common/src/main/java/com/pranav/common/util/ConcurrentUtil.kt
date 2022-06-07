package com.pranav.common.util

import kotlinx.coroutines.*

class ConcurrentUtil {
  companion object {
    @JvmStatic
    fun execute(runnable: Runnable) = runBlocking {
      launch {
        runnable.run()
      }
      deferred.await()
    }
  }
}
