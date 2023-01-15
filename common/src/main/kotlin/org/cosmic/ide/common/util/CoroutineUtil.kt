package org.cosmic.ide.common.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object CoroutineUtil {

    private val scope = CoroutineScope(Dispatchers.IO)

    @JvmStatic
    fun execute(action: Runnable) = runBlocking {
        launch {
            action.run()
        }
    }

    @JvmStatic
    fun inParallel(action: Runnable) = scope.launch {
        action.run()
    }
}
