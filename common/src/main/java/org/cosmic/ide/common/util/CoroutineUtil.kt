package org.cosmic.ide.common.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
