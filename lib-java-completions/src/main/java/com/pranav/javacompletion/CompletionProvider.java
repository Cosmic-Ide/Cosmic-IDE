package com.pranav.autocompletion;

import com.pranav.javacompletion.api.MethodItem;
import com.pranav.lib_android.util.FileUtil;
import dalvik.system.DexClassLoader;

import java.util.ArrayList;

public class CompletionProvider {

  public static ArrayList<MethodItem> getMethods() {
    ArrayList<MethodItem> items = new ArrayList<>();
    String libPath = FileUtil.getClasspathDir();

    File libs = new File(libPath);
    for (File lib : libs.listFiles()) {
      if (lib.getName().endsWith(".dex")) {
        PathClassLoader loader = new PathClassLoader(lib.getAbsolutePath(), ClassLoader.getSystemClassloader());
        try {
          Class dynamicClass = loader.loadClass("java.lang.Class");
          for (Method m : dynamicClass.getDeclaredMethods()) {
            items.add(new MethodItem(m));
          }
        } catch (Throwable e) {
          // ignore if class is not present
        }
      }
    }
    return items;
  }
}