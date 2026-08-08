package org.cosmicide.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher

object AppDispatchers {
    val IO = MemoryAwareDispatchers.IO.asCoroutineDispatcher()
    val Main = Dispatchers.Main
}
