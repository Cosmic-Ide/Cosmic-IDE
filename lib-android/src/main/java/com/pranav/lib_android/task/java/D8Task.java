package com.pranav.lib_android.task.java;

import com.pranav.lib_android.util.FileUtil;
import com.pranav.lib_android.util.ConcurrentUtil;
import com.pranav.lib_android.interfaces.*;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class D8Task extends Task {

	private Exception ex = null;

	@Override
	public void doFullTask() throws Exception {
		ConcurrentUtil.execute(() -> {
			try {
			  D8.run(
			    D8Command.builder()
			        .setOutput(Paths.get(FileUtil.getBinDir()), OutputMode.DexIndexed)
			        .addLibraryFiles(Paths.get(FileUtil.getClasspathDir(), "android.jar"))
			        .addProgramFiles(
			            getClassFiles(
			                new File(FileUtil.getBinDir(), "classes")
			            )
			        )
			        .build()
			  );

			} catch (Exception e) {
				ex = e;
			}
		});
		if (ex != null) {
			throw ex;
		}
	}
	
	private List<Path> getClassFiles(File root) {
	  List<Path> paths = new ArrayList<>();
	  
	  File[] files = root.listFiles();
	  if (files != null) {
	    for (File f : files) {
	      if (f.isFile()) {
	        paths.add(f.toPath());
	      } else {
	        paths.addAll(getClassFiles(f));
  	    }
	    }
	  }
	  return paths;
	}
	
	@Override
	public String getTaskName() {
		return "D8 Task";
	}
}
