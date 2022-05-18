package com.pranav.common.util

import kotlinx.coroutines.*

class ConcurrentUtil {
  companion object {
    @JvmStatic
    fun execute(runnable: Runnable) = runBlocking {
      val deferred: Deferred<Unit> = async {
        runnable.run()
      }
      deferred.await()
    }

    @JvmStatic
    fun executeInBackground(runnable: Runnable) = runBlocking {
      launch {
        runnable.run()
      }
    }
  }
}
