package org.eclipse.jdt.internal.compiler.batch;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationDecorator;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding.ExternalAnnotationStatus;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;

public class ClasspathMultiReleaseJar extends ClasspathJar {
	private java.nio.file.FileSystem fs = null;
	Path releasePath = null;
	String compliance = null;

	public ClasspathMultiReleaseJar(File file, boolean closeZipFileAtEnd,
			AccessRuleSet accessRuleSet, String destinationPath, String compliance) {
		super(file, closeZipFileAtEnd, accessRuleSet, destinationPath);
		this.compliance = compliance;
	}
	@Override
	public void initialize() throws IOException {
		super.initialize();
		URI t = this.file.toURI();
		if (this.file.exists()) {
			URI uri = URI.create("jar:file:" + t.getRawPath()); //$NON-NLS-1$

			try {
				HashMap<String, ?> env = new HashMap<>();
				this.fs = FileSystems.newFileSystem(uri, env);
			} catch (FileSystemAlreadyExistsException e) {
				this.fs = FileSystems.getFileSystem(uri);
			}
			this.releasePath = this.fs.getPath("/", "META-INF", "versions", this.compliance); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (!Files.exists(this.releasePath)) {
				this.releasePath = null;
			}
		}
	}
	@SuppressWarnings("rawtypes")
	@Override
	public synchronized char[][] getModulesDeclaringPackage(String qualifiedPackageName, String moduleName) {
		if (this.releasePath == null) {
			return super.getModulesDeclaringPackage(qualifiedPackageName, moduleName);
		}
		if (this.packageCache != null)
			return singletonModuleNameIf(this.packageCache.contains(qualifiedPackageName));

		this.packageCache = new HashSet<>(41);
		this.packageCache.add(Util.EMPTY_STRING);

		for (Enumeration e = this.zipFile.entries(); e.hasMoreElements(); ) {
			String fileName = ((ZipEntry) e.nextElement()).getName();
			addToPackageCache(fileName, false);
		}
		try {
			if (this.releasePath != null && Files.exists(this.releasePath)) {
				// go through the packages
				try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(this.releasePath)) {
					for (final java.nio.file.Path subdir: stream) {
						Files.walkFileTree(subdir, new FileVisitor<java.nio.file.Path>() {
							@Override
							public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs)
									throws IOException {
								return FileVisitResult.CONTINUE;
							}
							@Override
							public FileVisitResult visitFile(java.nio.file.Path f, BasicFileAttributes attrs)
									throws IOException {
								Path p = ClasspathMultiReleaseJar.this.releasePath.relativize(f);
								addToPackageCache(p.toString(), false);
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult visitFileFailed(java.nio.file.Path f, IOException exc) throws IOException {
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc)
									throws IOException {
								return FileVisitResult.CONTINUE;
							}
						});
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// move on;
		}
		return singletonModuleNameIf(this.packageCache.contains(qualifiedPackageName));
	}
	@Override
	public NameEnvironmentAnswer findClass(char[] binaryFileName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName, boolean asBinaryOnly) {
		if (!isPackage(qualifiedPackageName, moduleName)) return null; // most common case
		if (this.releasePath != null) {
			try {
				Path p = this.releasePath.resolve(qualifiedBinaryFileName);
				byte[] content = Files.readAllBytes(p);
				IBinaryType reader = null;
				if (content != null) {
					reader = new ClassFileReader(content, qualifiedBinaryFileName.toCharArray());
				}
				if (reader != null) {
					char[] modName = this.module == null ? null : this.module.name();
					if (reader instanceof ClassFileReader) {
						ClassFileReader classReader = (ClassFileReader) reader;
						if (classReader.moduleName == null)
							classReader.moduleName = modName;
						else
							modName = classReader.moduleName;
						}
					String fileNameWithoutExtension = qualifiedBinaryFileName.substring(0, qualifiedBinaryFileName.length() - SuffixConstants.SUFFIX_CLASS.length);
					searchPaths:
						if (this.annotationPaths != null) {
							String qualifiedClassName = qualifiedBinaryFileName.substring(0, qualifiedBinaryFileName.length()-SuffixConstants.EXTENSION_CLASS.length()-1);
							for (String annotationPath : this.annotationPaths) {
								try {
									if (this.annotationZipFile == null) {
										this.annotationZipFile = ExternalAnnotationDecorator.getAnnotationZipFile(annotationPath, null);
									}
									reader = ExternalAnnotationDecorator.create(reader, annotationPath, qualifiedClassName, this.annotationZipFile);

									if (reader.getExternalAnnotationStatus() == ExternalAnnotationStatus.TYPE_IS_ANNOTATED) {
										break searchPaths;
									}
								} catch (IOException e) {
									// don't let error on annotations fail class reading
								}
							}
							// location is configured for external annotations, but no .eea found, decorate in order to answer NO_EEA_FILE:
							reader = new ExternalAnnotationDecorator(reader, null);
						}
					if (this.accessRuleSet == null)
						return new NameEnvironmentAnswer(reader, null, modName);
					return new NameEnvironmentAnswer(reader,
							this.accessRuleSet.getViolatedRestriction(fileNameWithoutExtension.toCharArray()),
							modName);
				}
			} catch (IOException | ClassFormatException e) {
				// treat as if class file is missing
			}
		}
		return super.findClass(binaryFileName, qualifiedPackageName, moduleName, qualifiedBinaryFileName, asBinaryOnly);
	}
}
