package com.pranav.lib_android.util

import kotlinx.coroutines.*

class ConcurrentUtil {
  // make these utility methods static for java interoperability
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
