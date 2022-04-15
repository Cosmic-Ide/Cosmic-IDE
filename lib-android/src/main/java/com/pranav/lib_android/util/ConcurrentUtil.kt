package com.pranav.lib_android.util

import kotlinx.coroutines.*

class ConcurrentUtil {

  init {
    // empty
  }

  suspend fun execute(runnable: Runnable) {
    val deferred: Deferred<Unit> = async {
      runnable.run()
    }
    deferred.await()
  }

  suspend fun executeInBackground(runnable: Runnable) {
    runnable.run()
  }
}
