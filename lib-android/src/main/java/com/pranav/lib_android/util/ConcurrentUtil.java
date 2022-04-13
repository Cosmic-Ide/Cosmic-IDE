package com.pranav.lib_android.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ConcurrentUtil {

  public static ExecutorService createPool() {
    return Executors.newCachedThreadPool();
  }

  public static void execute(Runnable runnable) {
    ExecutorService service = createPool();
    service.execute(runnable);
    service.shutdown();
    try {
      service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  public static void executeInBackground(Runnable runnable) {
    ExecutorService service = createPool();
    service.execute(runnable);
  }
}