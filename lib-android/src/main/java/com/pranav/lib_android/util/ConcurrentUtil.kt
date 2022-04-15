package com.pranav.lib_android.util

import kotlinx.coroutines.Async
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Default

  suspend fun execute(runnable: Runnable) {
    val task = Async(Default) {
      runnable.run()
    }
    task.await()
  }
  
  fun executeInBackground(runnable: Runnable) {
    launch(IO)  {
      runnable.run()
    }
  }
