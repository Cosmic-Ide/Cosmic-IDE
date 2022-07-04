package com.pranav.common.util

import kotlinx.coroutines.*

object ConcurrentUtil {

    private val scope = CoroutineScope(Dispatchers.IO)

    @JvmStatic
    fun execute(runnable: Runnable) = runBlocking {
        launch {
          runnable.run()
        }
    }
    
    @JvmStatic
    fun inParallel(runnable: Runnable) = scope.launch {
        runnable.run()
    }
}
