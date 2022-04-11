/*******************************************************************************
 * Copyright (c) 2015, 2016 GK Software AG.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Track changes of external annotation files and trigger closing / reloading of affected ClassFiles.
 */
public class ExternalAnnotationTracker implements IResourceChangeListener {

	/**
	 * Nodes in a tree that represents external annotation attachments.
	 * Each node is either an intermediate node, or an annotation base.
	 * <p>
	 * <b>Intermediate nodes</b> represent the workspace structure holding the
	 * external annotations. They may have children.<br/>
	 * <em>Note: we don't flatten these intermediate nodes as to facilitate
	 * matching against the exact structure of resource deltas.</em>
	 * </p><p>
	 * An <b>annotation base</b> is a leaf in the represented directory structure
	 * and may have a map of known class files.
	 * </p>
	 */
	static class DirectoryNode {

		DirectoryNode parent;
		IPath path;

		/** Key is a full workspace path. */
		Map<IPath,DirectoryNode> children;
		/**
		 * Key is the path of an external annotation file (.eea), relative to this annotation base.
		 * The annotation file need not exist, in which case we are waiting for its creation.
		 */
		Map<IPath, ClassFile> classFiles;
		IPackageFragmentRoot modelRoot; // TODO: for handling zipped annotations

		public DirectoryNode(DirectoryNode parent, IPath path) {
			this.parent = parent;
			this.path = path;
		}

		Map<IPath, DirectoryNode> getChildren() {
			if (this.children == null)
				this.children = new HashMap<>();
			return this.children;
		}

		void registerClassFile(IPath relativeAnnotationPath, ClassFile classFile) {
			if (this.classFiles == null)
				this.classFiles = new HashMap<>();
			this.classFiles.put(relativeAnnotationPath, classFile);
			if (this.modelRoot == null)
				this.modelRoot = classFile.getPackageFragmentRoot();
		}

		void unregisterClassFile(IPath relativeAnnotationPath) {
			if (this.classFiles != null) {
				this.classFiles.remove(relativeAnnotationPath);
				if (this.classFiles.isEmpty() && this.parent != null)
					this.parent.unregisterDirectory(this);
			}
		}
		void unregisterDirectory(DirectoryNode child) {
			if (this.children != null)
				this.children.remove(child.path);
			if ((this.children == null || this.children.isEmpty()) && this.parent != null)
				this.parent.unregisterDirectory(this);
		}

		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			if (this.classFiles != null)
				buf.append("annotation base "); //$NON-NLS-1$
			buf.append("directory\n"); //$NON-NLS-1$
			if (this.children != null)
				buf.append("\twith ").append(this.children.size()).append(" children\n"); //$NON-NLS-1$ //$NON-NLS-2$
			buf.append("\t#classFiles: ").append(numClassFiles()); //$NON-NLS-1$
			return buf.toString();
		}
		int numClassFiles() {
			if (this.classFiles != null)
				return this.classFiles.size();
			int count = 0;
			if (this.children != null)
				for (DirectoryNode child : this.children.values())
					count += child.numClassFiles();
			return count;
		}
		boolean isEmpty() {
			return (this.children == null || this.children.isEmpty()) && (this.classFiles == null || this.classFiles.isEmpty());
		}
	}

	/** The tree of tracked annotation bases and class files. */
	DirectoryNode tree = new DirectoryNode(null, null);

	private static ExternalAnnotationTracker singleton;
	private ExternalAnnotationTracker() { }

	/** Start listening. */
	static void start(IWorkspace workspace) {
		singleton = new ExternalAnnotationTracker();
		workspace.addResourceChangeListener(singleton);
	}

	/** Stop listening & clean up. */
	static void shutdown(IWorkspace workspace) {
		if (singleton != null) {
			workspace.removeResourceChangeListener(singleton);
			singleton.tree.children = null;
		}
	}

	/**
	 * Register a ClassFile, to which the annotation attachment 'annotationBase' applies.
	 * This is done for the purpose to listen to changes in the corresponding external annotations
	 * and to force reloading the class file when necessary.
	 * @param annotationBase the path of the annotation attachment (workspace absolute)
	 * @param relativeAnnotationPath path corresponding to the qualified name of the main type of the class file.
	 *  The path is relative to 'annotationBase'.
	 *  When appending the file extension for annotation files it points to the annotation file
	 *  that would correspond to the given class file. The annotation file may or may not yet exist.
	 * @param classFile the ClassFile to register.
	 */
	public static void registerClassFile(IPath annotationBase, IPath relativeAnnotationPath, ClassFile classFile) {
		int baseDepth = annotationBase.segmentCount();
		if (baseDepth == 0) {
			Util.log(new IllegalArgumentException("annotationBase cannot be empty")); //$NON-NLS-1$
		} else {
			relativeAnnotationPath = relativeAnnotationPath.addFileExtension(ExternalAnnotationProvider.ANNOTATION_FILE_EXTENSION);
			DirectoryNode base = singleton.getAnnotationBase(singleton.tree, annotationBase, baseDepth, 1);
			base.registerClassFile(relativeAnnotationPath, classFile);
		}
	}

	/**
	 * Unregister a class file that is being closed.
	 * Only to be invoked for class files that potentially are affected by external annotations.
	 * @param annotationBase path of the corresponding annotation attachment (workspace absolute)
	 * @param relativeAnnotationPath path of the annotation file that would correspond to the given class file.
	 */
	public static void unregisterClassFile(IPath annotationBase, IPath relativeAnnotationPath) {
		int baseDepth = annotationBase.segmentCount();
		if (baseDepth == 0) {
			Util.log(new IllegalArgumentException("annotationBase cannot be empty")); //$NON-NLS-1$
		} else {
			relativeAnnotationPath = relativeAnnotationPath.addFileExtension(ExternalAnnotationProvider.ANNOTATION_FILE_EXTENSION);
			DirectoryNode base = singleton.getAnnotationBase(singleton.tree, annotationBase, baseDepth, 1);
			base.unregisterClassFile(relativeAnnotationPath);
		}
	}

	private DirectoryNode getAnnotationBase(DirectoryNode current, IPath annotationBase, int baseDepth, int nextDepth) {
		IPath nextHead = annotationBase.uptoSegment(nextDepth);
		Map<IPath, DirectoryNode> children = current.getChildren(); // create if necessary
		DirectoryNode nextHeadNode = children.get(nextHead);
		if (nextHeadNode == null)
			children.put(nextHead, nextHeadNode = new DirectoryNode(current, nextHead));
		if (baseDepth == nextDepth)
			return nextHeadNode;
		return getAnnotationBase(nextHeadNode, annotationBase, baseDepth, nextDepth+1);
	}

	/**
	 * Listen to resource change events concerning external annotations, that potentially affect a cached ClassFile.
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta != null && delta.getFullPath().isRoot() && this.tree.children != null) {
			for (IResourceDelta child : delta.getAffectedChildren()) {
				DirectoryNode directoryNode = this.tree.children.get(child.getFullPath());
				if (directoryNode != null)
					traverseForDirectories(directoryNode, child);
			}
		}
	}

	// co-traversal of directory nodes & delta nodes:
	private void traverseForDirectories(DirectoryNode directoryNode, IResourceDelta matchedDelta) {
		if (directoryNode.classFiles != null) {
			// annotation base reached, switch strategy:
			traverseForClassFiles(directoryNode.classFiles, matchedDelta, matchedDelta.getFullPath().segmentCount());
			// ignore further children, if we already have classFiles (i.e., nested annotation bases are tolerated but ignored).
		} else if (directoryNode.children != null) {
			for (IResourceDelta child : matchedDelta.getAffectedChildren()) {
				DirectoryNode childDir = directoryNode.children.get(child.getFullPath());
				if (childDir != null) {
					if (child.getKind() == IResourceDelta.REMOVED)
						directoryNode.children.remove(child.getFullPath());
					else
						traverseForDirectories(childDir, child);
				}
			}
		}
		if (directoryNode.isEmpty())
			directoryNode.parent.children.remove(matchedDelta.getFullPath());
	}

	// traversal of delta nodes to be matched against map of class files:
	private void traverseForClassFiles(Map<IPath, ClassFile> classFiles, IResourceDelta matchedDelta, int baseDepth) {
		for (IResourceDelta delta : matchedDelta.getAffectedChildren()) {
			IPath deltaRelativePath = delta.getFullPath().removeFirstSegments(baseDepth);
			ClassFile classFile = classFiles.remove(deltaRelativePath);
			if (classFile != null) {
				try {
					// the payload: unload the class file corresponding to a changed external annotation file:
					classFile.closeAndRemoveFromJarTypeCache();
				} catch (JavaModelException e) {
					Util.log(e, "Failed to close ClassFile "+classFile.name); //$NON-NLS-1$
				}
			} else {
				traverseForClassFiles(classFiles, delta, baseDepth);
			}
		}
	}
}
