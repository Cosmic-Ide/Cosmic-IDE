package org.cosmic.ide.common.util

import kotlinx.coroutines.*

object CoroutineUtil {

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
