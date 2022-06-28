package com.pranav.common.util

import kotlinx.coroutines.*

object ConcurrentUtil {
    @JvmStatic
    fun execute(runnable: Runnable) = runBlocking {
        launch {
          runnable.run()
        }
    }
    
    @JvmStatic
    fun inParallel(runnable: Runnable) = GlobalScope.launch(Dispatchers.Default) {
        runnable.run()
    }
}
