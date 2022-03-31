package com.pranav.lib_android.task.java;

import com.pranav.lib_android.util.FileUtil;
import com.pranav.lib_android.interfaces.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarTask extends Task {

	@Override
	public void doFullTask() throws Exception {

		//input file
		File classesFolder = new File(FileUtil.getBinDir() + "classes");
		
		// Open archive file
		FileOutputStream stream = new FileOutputStream(new File(FileUtil.getBinDir() + "classes.jar"));
		
		Manifest manifest = buildManifest();
		
		//Create the jar file
		JarOutputStream out = new JarOutputStream(stream, manifest);
		
		//Add the files..
		if (classesFolder.listFiles() != null) {
			for (File clazz : classesFolder.listFiles()) {
				add(classesFolder.getPath(), clazz, out);
			}
		}
		
		out.close();
		stream.close();
	}

	@Override
	public String getTaskName() {
		return "JarTask";
	}

	private Manifest buildManifest() {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		return manifest;
	}
	
	private void add(String parentPath, File source, JarOutputStream target) throws IOException {
		String name = source.getPath().substring(parentPath.length() + 1);
		
		BufferedInputStream in = null;
		try {
			if (source.isDirectory()) {
				if (!name.isEmpty()) {
					if (!name.endsWith("/"))
					    name += "/";
					
					//Add the Entry
					JarEntry entry = new JarEntry(name);
					entry.setTime(source.lastModified());
					target.putNextEntry(entry);
					target.closeEntry();
				}
				
				for (File nestedFile : source.listFiles()) {
					add(parentPath, nestedFile, target);
				}
				return;
			}
			
			JarEntry entry = new JarEntry(name);
			entry.setTime(source.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(source));
			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1)
				break;
				target.write(buffer, 0, count);
			}
			target.closeEntry();
			
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
}
