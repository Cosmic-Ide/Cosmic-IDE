package com.pranav.lib_android.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Default

  suspend fun execute(runnable: Runnable) {
    CoroutineScope(Default).launch {
      runnable.run()
    }
  }
  
  fun executeInBackground(runnable: Runnable) {
    CoroutineScope(IO).launch  {
      runnable.run()
    }
  }
