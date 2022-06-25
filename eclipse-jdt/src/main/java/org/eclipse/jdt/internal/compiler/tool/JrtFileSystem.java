/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.tool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import javax.tools.JavaFileObject;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.tool.ModuleLocationHandler.ModuleLocationWrapper;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;

public class JrtFileSystem extends Archive {

	static final String BOOT_MODULE = "jrt-fs.jar"; //$NON-NLS-1$

	public HashMap<String, Path> modulePathMap;
	Path modules;
	private java.nio.file.FileSystem jrtfs;

	public JrtFileSystem(File file) throws ZipException, IOException {
		this.file = file;
		initialize();
	}

	public void initialize() throws IOException {
		// initialize packages
		this.modulePathMap = new HashMap<>();
		URL jrtPath = null;

		if (this.file.exists()) {
			jrtPath = Paths.get(this.file.toPath().toString(), "lib", JRTUtil.JRT_FS_JAR).toUri().toURL(); //$NON-NLS-1$
			try (URLClassLoader loader = new URLClassLoader(new URL[] { jrtPath })) {
				HashMap<String, ?> env = new HashMap<>();
				this.jrtfs = FileSystems.newFileSystem(Paths.get("/sdcard/Android/data/com.pranav.java.ide/files/compiler-modules"), loader);
				this.modules = this.jrtfs.getPath("/modules"); //$NON-NLS-1$
			}
		} else {
			return;
		}

		org.eclipse.jdt.internal.compiler.util.JRTUtil.walkModuleImage(this.file,
				new org.eclipse.jdt.internal.compiler.util.JRTUtil.JrtFileVisitor<Path>() {

			@Override
			public FileVisitResult visitPackage(Path dir, Path mod, BasicFileAttributes attrs)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path f, Path mod, BasicFileAttributes attrs)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitModule(Path path, String name) throws IOException {
				JrtFileSystem.this.modulePathMap.put(name, path);
				return FileVisitResult.CONTINUE;
			}
		}, JRTUtil.NOTIFY_MODULES);
	}

	public List<JrtFileObject> list(ModuleLocationWrapper location, String packageName,
			Set<JavaFileObject.Kind> kinds, boolean recurse, Charset charset) {
    	String module = location.modName;
    	Path mPath = this.modules.resolve(module);
    	Path resolve = mPath.resolve(packageName);
    	java.util.List<Path> files = null;
        try (Stream<Path> p = Files.list(resolve)) {
            files = p.filter((path) -> {
            	if (Files.isDirectory(path))
            		return false;
            	else
            		return true;
            }).collect(Collectors.toList());
        } catch (IOException e) {
        	// ignore
        }
        List<JrtFileObject> result = new ArrayList<>();
        for (Path p: files) {
        	result.add(new JrtFileObject(this.file, p, module, charset));
        }
        return result;
    }
	@Override
	public ArchiveFileObject getArchiveFileObject(String fileName, String module, Charset charset) {
		return new JrtFileObject(this.file, this.modules.resolve(module).resolve(fileName), module, charset);
	}

	@Override
	public boolean contains(String entryName) {
		// FIXME
		return false;
	}

	@Override
	public String toString() {
		return "JRT: " + (this.file == null ? "UNKNOWN_ARCHIVE" : this.file.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	class JrtFileObject extends ArchiveFileObject {
		String module;
		Path path;
		private JrtFileObject(File file, Path path, String module, Charset charset) {
			super(file, path.toString(), charset);
			this.path = path;
		}

		@Override
		protected ClassFileReader getClassReader() {
			ClassFileReader reader = null;
			try {
				byte[] content = JRTUtil.getClassfileContent(this.file, this.entryName, this.module);
				if (content == null) return null;
				return new ClassFileReader(content, this.entryName.toCharArray());
			} catch (ClassFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return reader;
		}


		/* (non-Javadoc)
		 * @see javax.tools.FileObject#getCharContent(boolean)
		 */
		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return Util.getCharContents(this, ignoreEncodingErrors,
					org.eclipse.jdt.internal.compiler.util.JRTUtil.getClassfileContent(this.file, this.entryName, this.module),
					this.charset.name());
		}

		/* (non-Javadoc)
		 * @see javax.tools.FileObject#getLastModified()
		 */
		@Override
		public long getLastModified() {
			return 0;
		}

		/* (non-Javadoc)
		 * @see javax.tools.FileObject#getName()
		 */
		@Override
		public String getName() {
			return this.path.toString();
		}

		/* (non-Javadoc)
		 * @see javax.tools.FileObject#openInputStream()
		 */
		@Override
		public InputStream openInputStream() throws IOException {
			return Files.newInputStream(this.path);
		}

		/* (non-Javadoc)
		 * @see javax.tools.FileObject#openOutputStream()
		 */
		@Override
		public OutputStream openOutputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see javax.tools.FileObject#openReader(boolean)
		 */
		@Override
		public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see javax.tools.FileObject#openWriter()
		 */
		@Override
		public Writer openWriter() throws IOException {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see javax.tools.FileObject#toUri()
		 */
		@Override
		public URI toUri() {
			try {
				return new URI("JRT:" + this.file.toURI().getPath() + "!" + this.entryName); //$NON-NLS-1$//$NON-NLS-2$
			} catch (URISyntaxException e) {
				return null;
			}
		}


		@Override
		public String toString() {
			return this.file.getAbsolutePath() + "[" + this.entryName + "]";//$NON-NLS-1$//$NON-NLS-2$
		}
	}
}
