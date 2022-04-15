package com.pranav.lib_android.util

import kotlinx.coroutines.*

fun execute(runnable: Runnable) = runBlocking {
  val deferred: Deferred = async {
    runnable.run()
  }
  deferred.await()
}

fun executeInBackground(runnable: Runnable) = runBlocking {
  launch {
    runnable.run()
  }
}
