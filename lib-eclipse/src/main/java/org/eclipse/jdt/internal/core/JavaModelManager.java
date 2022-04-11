/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Theodora Yeung (tyeung@bea.com) - ensure that JarPackageFragmentRoot make it into cache
 *                                                           before its contents
 *                                                           (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=102422)
 *     Stephan Herrmann - Contributions for
 *								Bug 346010 - [model] strange initialization dependency in OptionTests
 *								Bug 440477 - [null] Infrastructure for feeding external annotations into compilation
 *     Terry Parker <tparker@google.com> - DeltaProcessor misses state changes in archive files, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=357425
 *     Thirumala Reddy Mutchukota <thirumala@google.com> - Contribution to bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=411423
 *     Terry Parker <tparker@google.com> - [performance] Low hit rates in JavaModel caches - https://bugs.eclipse.org/421165
 *     Terry Parker <tparker@google.com> - Enable the Java model caches to recover from IO errors - https://bugs.eclipse.org/455042
 *     Gábor Kövesdán - Contribution for Bug 350000 - [content assist] Include non-prefix matches in auto-complete suggestions
 *     Karsten Thoms - Bug 532505 - Reduce memory footprint of ClasspathAccessRule
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeManager.ContentTypeChangeEvent;
import org.eclipse.core.runtime.content.IContentTypeManager.IContentTypeChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.jdt.internal.codeassist.SelectionEngine;
import org.eclipse.jdt.internal.compiler.AbstractAnnotationProcessorManager;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.eclipse.jdt.internal.compiler.util.ObjectVector;
import org.eclipse.jdt.internal.core.DeltaProcessor.RootInfo;
import org.eclipse.jdt.internal.core.JavaProjectElementInfo.ProjectCache;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;
import org.eclipse.jdt.internal.core.dom.SourceRangeVerifier;
import org.eclipse.jdt.internal.core.dom.rewrite.RewriteEventStore;
import org.eclipse.jdt.internal.core.hierarchy.TypeHierarchy;
import org.eclipse.jdt.internal.core.search.AbstractSearchScope;
import org.eclipse.jdt.internal.core.search.BasicSearchEngine;
import org.eclipse.jdt.internal.core.search.IRestrictedAccessTypeRequestor;
import org.eclipse.jdt.internal.core.search.JavaWorkspaceScope;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.core.search.processing.IJob;
import org.eclipse.jdt.internal.core.search.processing.JobManager;
import org.eclipse.jdt.internal.core.util.DeduplicationUtil;
import org.eclipse.jdt.internal.core.util.HashtableOfArrayToObject;
import org.eclipse.jdt.internal.core.util.LRUCache;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.BackingStoreException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The <code>JavaModelManager</code> manages instances of <code>IJavaModel</code>.
 * <code>IElementChangedListener</code>s register with the <code>JavaModelManager</code>,
 * and receive <code>ElementChangedEvent</code>s for all <code>IJavaModel</code>s.
 * <p>
 * The single instance of <code>JavaModelManager</code> is available from
 * the static method <code>JavaModelManager.getJavaModelManager()</code>.
 */
public class JavaModelManager implements ISaveParticipant, IContentTypeChangeListener {
	/** Thread count for parallel save - if any value is set. Any value <= 1 will disable parallel save. **/
	private static final Integer SAVE_THREAD_COUNT = Integer.getInteger("org.eclipse.jdt.model_save_threads"); //$NON-NLS-1$
	/** should the state.dat be gzip compressed? **/
	private static final boolean SAVE_ZIPPED = !Boolean.getBoolean("org.eclipse.jdt.disable_gzip"); //$NON-NLS-1$
	private static ServiceRegistration<DebugOptionsListener> DEBUG_REGISTRATION;
	private static final String NON_CHAINING_JARS_CACHE = "nonChainingJarsCache"; //$NON-NLS-1$
	private static final String EXTERNAL_FILES_CACHE = "externalFilesCache";  //$NON-NLS-1$
	private static final String ASSUMED_EXTERNAL_FILES_CACHE = "assumedExternalFilesCache";  //$NON-NLS-1$

	public static enum ArchiveValidity {
		INVALID, VALID;

		public boolean isValid() {
			return this == VALID;
		}
	}

	/**
	 * Define a zip cache object.
	 */
	static class ZipCache {
		private Map<Object, ZipFile> map;
		Object owner;

		ZipCache(Object owner) {
			this.map = new HashMap<>();
			this.owner = owner;
		}

		public void flush() {
			Thread currentThread = Thread.currentThread();
			Iterator<ZipFile> iterator = this.map.values().iterator();
			while (iterator.hasNext()) {
				ZipFile zipFile = iterator.next();
				try {
					if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
						System.out.println("(" + currentThread + ") [ZipCache[" + this.owner //$NON-NLS-1$//$NON-NLS-2$
								+ "].flush()] Closing ZipFile on " + zipFile.getName()); //$NON-NLS-1$
					}
					zipFile.close();
				} catch (IOException e) {
					// problem occured closing zip file: cannot do much more
					JavaCore.getPlugin().getLog().log(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, "Error closing " + zipFile.getName(), e)); //$NON-NLS-1$
				}
			}
		}

		public ZipFile getCache(IPath path) {
			return this.map.get(path);
		}

		public void setCache(IPath path, ZipFile zipFile) {
			try (ZipFile old = this.map.put(path, zipFile)) {
				if (old != null) {
					if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
						Thread currentThread = Thread.currentThread();
						System.out.println("(" + currentThread + ") [ZipCache[" + this.owner //$NON-NLS-1$//$NON-NLS-2$
								+ "].setCache()] leaked ZipFile on " + old.getName() + " for path: " + path); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * Unique handle onto the JavaModel
	 */
	final JavaModel javaModel = new JavaModel();

	/**
	 * Classpath variables pool
	 */
	public HashMap<String, IPath> variables = new HashMap<>(5);
	public HashSet<String> variablesWithInitializer = new HashSet<>(5);
	public HashMap<String, String> deprecatedVariables = new HashMap<>(5);
	public HashSet<String> readOnlyVariables = new HashSet<>(5);
	public HashMap<String, IPath> previousSessionVariables = new HashMap<>(5);
	private ThreadLocal<Set<String>> variableInitializationInProgress = new ThreadLocal<>();

	/**
	 * Classpath containers pool
	 */
	public HashMap<IJavaProject, Map<IPath, IClasspathContainer>> containers = new HashMap<>(5);
	public HashMap<IJavaProject, Map<IPath, IClasspathContainer>> previousSessionContainers = new HashMap<>(5);
	private ThreadLocal<Map<IJavaProject, Set<IPath>>> containerInitializationInProgress = new ThreadLocal<>();
	ThreadLocal<Map<IJavaProject, Map<IPath, IClasspathContainer>>> containersBeingInitialized = new ThreadLocal<>();

	public static final int NO_BATCH_INITIALIZATION = 0;
	public static final int NEED_BATCH_INITIALIZATION = 1;
	public static final int BATCH_INITIALIZATION_IN_PROGRESS = 2;
	public static final int BATCH_INITIALIZATION_FINISHED = 3;
	public int batchContainerInitializations = NO_BATCH_INITIALIZATION;
	public Object batchContainerInitializationsLock = new Object();

	public BatchInitializationMonitor batchContainerInitializationsProgress = new BatchInitializationMonitor();
	public Hashtable<String, ClasspathContainerInitializer> containerInitializersCache = new Hashtable<>(5);

	/*
	 * A HashSet that contains the IJavaProject whose classpath is being resolved.
	 */
	private ThreadLocal<Set<IJavaProject>> classpathsBeingResolved = new ThreadLocal<>();

	/*
	 * The unique workspace scope
	 */
	public JavaWorkspaceScope workspaceScope;

	/*
	 * Extension used to construct Java 6 annotation processor managers
	 */
	private IConfigurationElement annotationProcessorManagerFactory = null;

	/*
	 * Map from a package fragment root's path to a source attachment property (source path + ATTACHMENT_PROPERTY_DELIMITER + source root path)
	 */
	public Map<IPath, String> rootPathToAttachments = new Hashtable<>();

	public final static String CP_VARIABLE_PREFERENCES_PREFIX = JavaCore.PLUGIN_ID+".classpathVariable."; //$NON-NLS-1$
	public final static String CP_CONTAINER_PREFERENCES_PREFIX = JavaCore.PLUGIN_ID+".classpathContainer."; //$NON-NLS-1$
	public final static String CP_USERLIBRARY_PREFERENCES_PREFIX = JavaCore.PLUGIN_ID+".userLibrary."; //$NON-NLS-1$
	public final static String CP_ENTRY_IGNORE = "##<cp entry ignore>##"; //$NON-NLS-1$
	public final static IPath CP_ENTRY_IGNORE_PATH = new Path(CP_ENTRY_IGNORE);
	public final static String TRUE = "true"; //$NON-NLS-1$

	private final static int VARIABLES_AND_CONTAINERS_FILE_VERSION = 2;

	/**
	 * Name of the extension point for contributing classpath variable initializers
	 */
	public static final String CPVARIABLE_INITIALIZER_EXTPOINT_ID = "classpathVariableInitializer" ; //$NON-NLS-1$

	/**
	 * Name of the extension point for contributing classpath container initializers
	 */
	public static final String CPCONTAINER_INITIALIZER_EXTPOINT_ID = "classpathContainerInitializer" ; //$NON-NLS-1$

	/**
	 * Name of the extension point for contributing a source code formatter
	 */
	public static final String FORMATTER_EXTPOINT_ID = "codeFormatter" ; //$NON-NLS-1$

	/**
	 * Name of the extension point for contributing a compilation participant
	 */
	public static final String COMPILATION_PARTICIPANT_EXTPOINT_ID = "compilationParticipant" ; //$NON-NLS-1$

	/**
	 * Name of the extension point for contributing the Java 6 annotation processor manager
	 */
	public static final String ANNOTATION_PROCESSOR_MANAGER_EXTPOINT_ID = "annotationProcessorManager" ;  //$NON-NLS-1$

	/**
	 * Name of the JVM parameter to specify whether or not referenced JAR should be resolved for container libraries.
	 */
	private static final String RESOLVE_REFERENCED_LIBRARIES_FOR_CONTAINERS = "resolveReferencedLibrariesForContainers"; //$NON-NLS-1$

	/**
	 * Name of the JVM parameter to specify how many compilation units must be handled at once by the builder.
	 * The default value is represented by <code>AbstractImageBuilder#MAX_AT_ONCE</code>.
	 */
	public static final String MAX_COMPILED_UNITS_AT_ONCE = "maxCompiledUnitsAtOnce"; //$NON-NLS-1$

	/**
	 * Special value used for recognizing ongoing initialization and breaking initialization cycles
	 */
	public final static IPath VARIABLE_INITIALIZATION_IN_PROGRESS = new Path("Variable Initialization In Progress"); //$NON-NLS-1$
	public final static IClasspathContainer CONTAINER_INITIALIZATION_IN_PROGRESS = new IClasspathContainer() {
		@Override
		public IClasspathEntry[] getClasspathEntries() { return null; }
		@Override
		public String getDescription() { return "Container Initialization In Progress"; } //$NON-NLS-1$
		@Override
		public int getKind() { return 0; }
		@Override
		public IPath getPath() { return null; }
		@Override
		public String toString() { return getDescription(); }
	};

	private static final String DEBUG = JavaCore.PLUGIN_ID + "/debug"; //$NON-NLS-1$
	private static final String BUFFER_MANAGER_DEBUG = JavaCore.PLUGIN_ID + "/debug/buffermanager" ; //$NON-NLS-1$
	private static final String INDEX_MANAGER_DEBUG = JavaCore.PLUGIN_ID + "/debug/indexmanager" ; //$NON-NLS-1$
	private static final String INDEX_MANAGER_ADVANCED_DEBUG = JavaCore.PLUGIN_ID + "/debug/indexmanager/advanced" ; //$NON-NLS-1$
	private static final String COMPILER_DEBUG = JavaCore.PLUGIN_ID + "/debug/compiler" ; //$NON-NLS-1$
	private static final String JAVAMODEL_CLASSPATH = JavaCore.PLUGIN_ID + "/debug/javamodel/classpath" ; //$NON-NLS-1$
	private static final String JAVAMODEL_DEBUG = JavaCore.PLUGIN_ID + "/debug/javamodel" ; //$NON-NLS-1$
	private static final String JAVAMODEL_INVALID_ARCHIVES = JavaCore.PLUGIN_ID + "/debug/javamodel/invalid_archives" ; //$NON-NLS-1$
	private static final String JAVAMODELCACHE_DEBUG = JavaCore.PLUGIN_ID + "/debug/javamodel/cache" ; //$NON-NLS-1$
	private static final String JAVAMODELCACHE_INSERTIONS_DEBUG = JavaCore.PLUGIN_ID + "/debug/javamodel/insertions" ; //$NON-NLS-1$
	private static final String CP_RESOLVE_DEBUG = JavaCore.PLUGIN_ID + "/debug/cpresolution" ; //$NON-NLS-1$
	private static final String CP_RESOLVE_ADVANCED_DEBUG = JavaCore.PLUGIN_ID + "/debug/cpresolution/advanced" ; //$NON-NLS-1$
	private static final String CP_RESOLVE_FAILURE_DEBUG = JavaCore.PLUGIN_ID + "/debug/cpresolution/failure" ; //$NON-NLS-1$
	private static final String ZIP_ACCESS_DEBUG = JavaCore.PLUGIN_ID + "/debug/zipaccess" ; //$NON-NLS-1$
	private static final String DELTA_DEBUG =JavaCore.PLUGIN_ID + "/debug/javadelta" ; //$NON-NLS-1$
	private static final String DELTA_DEBUG_VERBOSE =JavaCore.PLUGIN_ID + "/debug/javadelta/verbose" ; //$NON-NLS-1$
	private static final String DOM_AST_DEBUG = JavaCore.PLUGIN_ID + "/debug/dom/ast" ; //$NON-NLS-1$
	private static final String DOM_AST_DEBUG_THROW = JavaCore.PLUGIN_ID + "/debug/dom/ast/throw" ; //$NON-NLS-1$
	private static final String DOM_REWRITE_DEBUG = JavaCore.PLUGIN_ID + "/debug/dom/rewrite" ; //$NON-NLS-1$
	private static final String HIERARCHY_DEBUG = JavaCore.PLUGIN_ID + "/debug/hierarchy" ; //$NON-NLS-1$
	private static final String POST_ACTION_DEBUG = JavaCore.PLUGIN_ID + "/debug/postaction" ; //$NON-NLS-1$
	private static final String BUILDER_DEBUG = JavaCore.PLUGIN_ID + "/debug/builder" ; //$NON-NLS-1$
	private static final String BUILDER_STATS_DEBUG = JavaCore.PLUGIN_ID + "/debug/builder/stats" ; //$NON-NLS-1$
	private static final String COMPLETION_DEBUG = JavaCore.PLUGIN_ID + "/debug/completion" ; //$NON-NLS-1$
	private static final String RESOLUTION_DEBUG = JavaCore.PLUGIN_ID + "/debug/resolution" ; //$NON-NLS-1$
	private static final String SELECTION_DEBUG = JavaCore.PLUGIN_ID + "/debug/selection" ; //$NON-NLS-1$
	private static final String SEARCH_DEBUG = JavaCore.PLUGIN_ID + "/debug/search" ; //$NON-NLS-1$
	private static final String SOURCE_MAPPER_DEBUG_VERBOSE = JavaCore.PLUGIN_ID + "/debug/sourcemapper" ; //$NON-NLS-1$
	private static final String FORMATTER_DEBUG = JavaCore.PLUGIN_ID + "/debug/formatter" ; //$NON-NLS-1$

	public static final String COMPLETION_PERF = JavaCore.PLUGIN_ID + "/perf/completion" ; //$NON-NLS-1$
	public static final String SELECTION_PERF = JavaCore.PLUGIN_ID + "/perf/selection" ; //$NON-NLS-1$
	public static final String DELTA_LISTENER_PERF = JavaCore.PLUGIN_ID + "/perf/javadeltalistener" ; //$NON-NLS-1$
	public static final String VARIABLE_INITIALIZER_PERF = JavaCore.PLUGIN_ID + "/perf/variableinitializer" ; //$NON-NLS-1$
	public static final String CONTAINER_INITIALIZER_PERF = JavaCore.PLUGIN_ID + "/perf/containerinitializer" ; //$NON-NLS-1$
	public static final String RECONCILE_PERF = JavaCore.PLUGIN_ID + "/perf/reconcile" ; //$NON-NLS-1$

	public static boolean PERF_VARIABLE_INITIALIZER = false;
	public static boolean PERF_CONTAINER_INITIALIZER = false;
	// Non-static, which will give it a chance to retain the default when and if JavaModelManager is restarted.
	boolean resolveReferencedLibrariesForContainers = false;

	public final static ICompilationUnit[] NO_WORKING_COPY = new ICompilationUnit[0];

	// Options
	private final static int UNKNOWN_OPTION = 0;
	private final static int DEPRECATED_OPTION = 1;
	private final static int VALID_OPTION = 2;
	HashSet<String> optionNames = new HashSet<>(20);
	Map<String, String[]> deprecatedOptions = new HashMap<>();
	Hashtable<String, String> optionsCache;

	// Preferences
	public final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];
	static final int PREF_INSTANCE = 0;
	static final int PREF_DEFAULT = 1;

	static final Object[][] NO_PARTICIPANTS = new Object[0][];

	public static class CompilationParticipants {

		static final int MAX_SOURCE_LEVEL = JavaCore.getAllVersions().size() - 1; // All except VERSION_CLDC_1_1

		/*
		 * The registered compilation participants (a table from int (source level) to Object[])
		 * The Object array contains first IConfigurationElements when not resolved yet, then
		 * it contains CompilationParticipants.
		 */
		private Object[][] registeredParticipants = null;
		private HashSet<String> managedMarkerTypes;

		public CompilationParticipant[] getCompilationParticipants(IJavaProject project) {
			final Object[][] participantsPerSource = getRegisteredParticipants();
			if (participantsPerSource == NO_PARTICIPANTS)
				return null;
			String sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true/*inherit options*/);
			final int sourceLevelIndex = indexForSourceLevel(sourceLevel);
			final Object[] participants = participantsPerSource[sourceLevelIndex];
			int length = participants.length;
			CompilationParticipant[] result = new CompilationParticipant[length];
			int index = 0;
			for (int i = 0; i < length; i++) {
				if (participants[i] instanceof IConfigurationElement) {
					final IConfigurationElement configElement = (IConfigurationElement) participants[i];
					final int participantIndex = i;
					SafeRunner.run(new ISafeRunnable() {
						@Override
						public void handleException(Throwable exception) {
							Util.log(exception, "Exception occurred while creating compilation participant"); //$NON-NLS-1$
						}
						@Override
						public void run() throws Exception {
							Object executableExtension = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							for (int j = sourceLevelIndex; j < MAX_SOURCE_LEVEL; j++)
								participantsPerSource[j][participantIndex] = executableExtension;
						}
					});
				}
				CompilationParticipant participant;
				if ((participants[i] instanceof CompilationParticipant) && (participant = (CompilationParticipant) participants[i]).isActive(project))
					result[index++] = participant;
			}
			if (index == 0)
				return null;
			if (index < length)
				System.arraycopy(result, 0, result = new CompilationParticipant[index], 0, index);
			return result;
		}

		public HashSet<String> managedMarkerTypes() {
			if (this.managedMarkerTypes == null) {
				// force extension points to be read
				getRegisteredParticipants();
			}
			return this.managedMarkerTypes;
		}

		private synchronized Object[][] getRegisteredParticipants() {
			if (this.registeredParticipants != null) {
				return this.registeredParticipants;
			}
			this.managedMarkerTypes = new HashSet<>();
			IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(JavaCore.PLUGIN_ID, COMPILATION_PARTICIPANT_EXTPOINT_ID);
			if (extension == null)
				return this.registeredParticipants = NO_PARTICIPANTS;
			final ArrayList<IConfigurationElement> modifyingEnv = new ArrayList<>();
			final ArrayList<IConfigurationElement> creatingProblems = new ArrayList<>();
			final ArrayList<IConfigurationElement> others = new ArrayList<>();
			IExtension[] extensions = extension.getExtensions();
			// for all extensions of this point...
			for(int i = 0; i < extensions.length; i++) {
				IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
				// for all config elements named "compilationParticipant"
				for(int j = 0; j < configElements.length; j++) {
					final IConfigurationElement configElement = configElements[j];
					String elementName =configElement.getName();
					if (!("compilationParticipant".equals(elementName))) { //$NON-NLS-1$
						continue;
					}
					// add config element in the group it belongs to
					if (TRUE.equals(configElement.getAttribute("modifiesEnvironment"))) //$NON-NLS-1$
						modifyingEnv.add(configElement);
					else if (TRUE.equals(configElement.getAttribute("createsProblems"))) //$NON-NLS-1$
						creatingProblems.add(configElement);
					else
						others.add(configElement);
					// add managed marker types
					IConfigurationElement[] managedMarkers = configElement.getChildren("managedMarker"); //$NON-NLS-1$
					for (int k = 0, length = managedMarkers.length; k < length; k++) {
						IConfigurationElement element = managedMarkers[k];
						String markerType = element.getAttribute("markerType"); //$NON-NLS-1$
						if (markerType != null)
							this.managedMarkerTypes.add(markerType);
					}
				}
			}
			int size = modifyingEnv.size() + creatingProblems.size() + others.size();
			if (size == 0)
				return this.registeredParticipants = NO_PARTICIPANTS;

			// sort config elements in each group
			IConfigurationElement[] configElements = new IConfigurationElement[size];
			int index = 0;
			index = sortParticipants(modifyingEnv, configElements, index);
			index = sortParticipants(creatingProblems, configElements, index);
			index = sortParticipants(others, configElements, index);

			// create result table
			Object[][] result = new Object[MAX_SOURCE_LEVEL][];
			int length = configElements.length;
			for (int i = 0; i < MAX_SOURCE_LEVEL; i++) {
				result[i] = new Object[length];
			}
			for (int i = 0; i < length; i++) {
				String sourceLevel = configElements[i].getAttribute("requiredSourceLevel"); //$NON-NLS-1$
				int sourceLevelIndex = indexForSourceLevel(sourceLevel);
				for (int j = sourceLevelIndex; j < MAX_SOURCE_LEVEL; j++) {
					result[j][i] = configElements[i];
				}
			}
			return this.registeredParticipants = result;
		}

		/*
		 * 1.1 -> 0
		 * 1.2 -> 1
		 * ...
		 * 1.6 -> 5
		 * 1.7 -> 6
		 * 1.8 -> 7
		 * 9 -> 8
		 * null -> 0
		 */
		private int indexForSourceLevel(String sourceLevel) {
			if (sourceLevel == null) return 0;
			int majVersion = (int) (CompilerOptions.versionToJdkLevel(sourceLevel) >>> 16);
			if (majVersion > ClassFileConstants.MAJOR_VERSION_1_2) {
				return (majVersion - ClassFileConstants.MAJOR_VERSION_1_1);
			}
			// all other cases including ClassFileConstants.MAJOR_VERSION_1_1
			return 0;
		}

		private int sortParticipants(ArrayList<IConfigurationElement> group, IConfigurationElement[] configElements, int index) {
			int size = group.size();
			if (size == 0) return index;
			Object[] elements = group.toArray();
			Util.sort(elements, new Util.Comparer() {
				@Override
				public int compare(Object a, Object b) {
					if (a == b) return 0;
					String id = ((IConfigurationElement) a).getAttribute("id"); //$NON-NLS-1$
					if (id == null) return -1;
					IConfigurationElement[] requiredElements = ((IConfigurationElement) b).getChildren("requires"); //$NON-NLS-1$
					for (int i = 0, length = requiredElements.length; i < length; i++) {
						IConfigurationElement required = requiredElements[i];
						if (id.equals(required.getAttribute("id"))) //$NON-NLS-1$
							return 1;
					}
					return -1;
				}
			});
			for (int i = 0; i < size; i++)
				configElements[index+i] = (IConfigurationElement) elements[i];
			return index + size;
		}
	}

	public final CompilationParticipants compilationParticipants = new CompilationParticipants();

	/* whether an AbortCompilationUnit should be thrown when the source of a compilation unit cannot be retrieved */
	public ThreadLocal<Boolean> abortOnMissingSource = new ThreadLocal<>();

	private ExternalFoldersManager externalFoldersManager = ExternalFoldersManager.getExternalFoldersManager();

	/**
	 * Returns whether the given full path (for a package) conflicts with the output location
	 * of the given project.
	 */
	public static boolean conflictsWithOutputLocation(IPath folderPath, JavaProject project) {
		try {
			IPath outputLocation = project.getOutputLocation();
			if (outputLocation == null) {
				// in doubt, there is a conflict
				return true;
			}
			if (outputLocation.isPrefixOf(folderPath)) {
				// only allow nesting in project's output if there is a corresponding source folder
				// or if the project's output is not used (in other words, if all source folders have their custom output)
				IClasspathEntry[] classpath = project.getResolvedClasspath();
				boolean isOutputUsed = false;
				for (int i = 0, length = classpath.length; i < length; i++) {
					IClasspathEntry entry = classpath[i];
					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if (entry.getPath().equals(outputLocation)) {
							return false;
						}
						if (entry.getOutputLocation() == null) {
							isOutputUsed = true;
						}
					}
				}
				return isOutputUsed;
			}
			return false;
		} catch (JavaModelException e) {
			// in doubt, there is a conflict
			return true;
		}
	}

	public synchronized IClasspathContainer containerGet(IJavaProject project, IPath containerPath) {
		// check initialization in progress first
		if (containerIsInitializationInProgress(project, containerPath)) {
			return CONTAINER_INITIALIZATION_IN_PROGRESS;
		}

		Map<IPath, IClasspathContainer> projectContainers = this.containers.get(project);
		if (projectContainers == null){
			return null;
		}
		IClasspathContainer container = projectContainers.get(containerPath);
		return container;
	}

	synchronized boolean containerIsSet(IJavaProject project, IPath containerPath) {
		Map<IPath, IClasspathContainer> projectContainers = this.containers.get(project);
		if (projectContainers == null){
			return false;
		}
		IClasspathContainer container = projectContainers.get(containerPath);
		return container != null;
	}

	public synchronized IClasspathContainer containerGetDefaultToPreviousSession(IJavaProject project, IPath containerPath) {
		Map<IPath, IClasspathContainer> projectContainers = this.containers.get(project);
		if (projectContainers == null)
			return getPreviousSessionContainer(containerPath, project);
		IClasspathContainer container = projectContainers.get(containerPath);
		if (container == null)
			return getPreviousSessionContainer(containerPath, project);
		return container;
	}

	private boolean containerIsInitializationInProgress(IJavaProject project, IPath containerPath) {
		Map<IJavaProject, Set<IPath>> initializations = this.containerInitializationInProgress.get();
		if (initializations == null)
			return false;
		Set<IPath> projectInitializations = initializations.get(project);
		if (projectInitializations == null)
			return false;
		return projectInitializations.contains(containerPath);
	}

	private void containerAddInitializationInProgress(IJavaProject project, IPath containerPath) {
		Map<IJavaProject, Set<IPath>> initializations = this.containerInitializationInProgress.get();
		if (initializations == null)
			this.containerInitializationInProgress.set(initializations = new HashMap<>());
		Set<IPath> projectInitializations = initializations.get(project);
		if (projectInitializations == null)
			initializations.put(project, projectInitializations = new HashSet<>());
		projectInitializations.add(containerPath);
	}

	public void containerBeingInitializedPut(IJavaProject project, IPath containerPath, IClasspathContainer container) {
		Map<IJavaProject, Map<IPath, IClasspathContainer>> perProjectContainers = this.containersBeingInitialized.get();
		if (perProjectContainers == null)
			this.containersBeingInitialized.set(perProjectContainers = new HashMap<>());
		Map<IPath, IClasspathContainer> perPathContainers = perProjectContainers.get(project);
		if (perPathContainers == null)
			perProjectContainers.put(project, perPathContainers = new HashMap<>());
		perPathContainers.put(containerPath, container);
	}

	public IClasspathContainer containerBeingInitializedGet(IJavaProject project, IPath containerPath) {
		Map<IJavaProject, Map<IPath, IClasspathContainer>> perProjectContainers = this.containersBeingInitialized.get();
		if (perProjectContainers == null)
			return null;
		Map<IPath, IClasspathContainer> perPathContainers = perProjectContainers.get(project);
		if (perPathContainers == null)
			return null;
		return perPathContainers.get(containerPath);
	}

	public IClasspathContainer containerBeingInitializedRemove(IJavaProject project, IPath containerPath) {
		Map<IJavaProject, Map<IPath, IClasspathContainer>> perProjectContainers = this.containersBeingInitialized.get();
		if (perProjectContainers == null)
			return null;
		Map<IPath, IClasspathContainer> perPathContainers = perProjectContainers.get(project);
		if (perPathContainers == null)
			return null;
		IClasspathContainer container = perPathContainers.remove(containerPath);
		if (perPathContainers.size() == 0)
			perProjectContainers.remove(project);
		if (perProjectContainers.size() == 0)
			this.containersBeingInitialized.remove();
		return container;
	}

	public synchronized void containerPut(IJavaProject project, IPath containerPath, IClasspathContainer container){

		// set/unset the initialization in progress
		if (container == CONTAINER_INITIALIZATION_IN_PROGRESS) {
			containerAddInitializationInProgress(project, containerPath);

			// do not write out intermediate initialization value
			return;
		} else {
			containerRemoveInitializationInProgress(project, containerPath);

			Map<IPath, IClasspathContainer> projectContainers = this.containers.get(project);
 			if (projectContainers == null){
				projectContainers = new HashMap<>(1);
				this.containers.put(project, projectContainers);
			}

			if (container == null) {
				projectContainers.remove(containerPath);
			} else {
  				projectContainers.put(containerPath, container);
			}
			// discard obsoleted information about previous session
			Map<IPath, IClasspathContainer> previousContainers = this.previousSessionContainers.get(project);
			if (previousContainers != null){
				previousContainers.remove(containerPath);
			}
		}
		// container values are persisted in preferences during save operations, see #saving(ISaveContext)
	}

	/*
	 * The given project is being removed. Remove all containers for this project from the cache.
	 */
	public synchronized void containerRemove(IJavaProject project) {
		Map<IJavaProject, Set<IPath>> initializations = this.containerInitializationInProgress.get();
		if (initializations != null) {
			initializations.remove(project);
		}
		this.containers.remove(project);
	}

	public boolean containerPutIfInitializingWithSameEntries(IPath containerPath, IJavaProject[] projects, IClasspathContainer[] respectiveContainers) {
		int projectLength = projects.length;
		if (projectLength != 1)
			return false;
		final IClasspathContainer container = respectiveContainers[0];
		IJavaProject project = projects[0];
		// optimize only if initializing, otherwise we are in a regular setContainer(...) call
		if (!containerIsInitializationInProgress(project, containerPath))
			return false;
		IClasspathContainer previousContainer = containerGetDefaultToPreviousSession(project, containerPath);
		if (container == null) {
			if (previousContainer == null) {
				containerPut(project, containerPath, null);
				return true;
			}
			return false;
		}
		final IClasspathEntry[] newEntries = container.getClasspathEntries();
		if (previousContainer == null)
			if (newEntries.length == 0) {
				containerPut(project, containerPath, container);
				return true;
			} else {
				if (CP_RESOLVE_VERBOSE || CP_RESOLVE_VERBOSE_FAILURE)
					verbose_missbehaving_container(containerPath, projects, respectiveContainers, container, newEntries, null/*no old entries*/);
				return false;
			}
		final IClasspathEntry[] oldEntries = previousContainer.getClasspathEntries();
		if (oldEntries.length != newEntries.length) {
			if (CP_RESOLVE_VERBOSE || CP_RESOLVE_VERBOSE_FAILURE)
				verbose_missbehaving_container(containerPath, projects, respectiveContainers, container, newEntries, oldEntries);
			return false;
		}
		for (int i = 0, length = newEntries.length; i < length; i++) {
			if (newEntries[i] == null) {
				if (CP_RESOLVE_VERBOSE || CP_RESOLVE_VERBOSE_FAILURE)
					verbose_missbehaving_container(project, containerPath, newEntries);
				return false;
			}
			if (!newEntries[i].equals(oldEntries[i])) {
				if (CP_RESOLVE_VERBOSE || CP_RESOLVE_VERBOSE_FAILURE)
					verbose_missbehaving_container(containerPath, projects, respectiveContainers, container, newEntries, oldEntries);
				return false;
			}
		}
		containerPut(project, containerPath, container);
		return true;
	}

	private void verbose_missbehaving_container(
			IPath containerPath,
			IJavaProject[] projects,
			IClasspathContainer[] respectiveContainers,
			final IClasspathContainer container,
			final IClasspathEntry[] newEntries,
			final IClasspathEntry[] oldEntries) {
		Util.verbose(
			"CPContainer SET  - missbehaving container\n" + //$NON-NLS-1$
			"	container path: " + containerPath + '\n' + //$NON-NLS-1$
			"	projects: {" +//$NON-NLS-1$
			org.eclipse.jdt.internal.compiler.util.Util.toString(
				projects,
				new org.eclipse.jdt.internal.compiler.util.Util.Displayable(){
					@Override
					public String displayString(Object o) { return ((IJavaProject) o).getElementName(); }
				}) +
			"}\n	values on previous session: {\n"  +//$NON-NLS-1$
			org.eclipse.jdt.internal.compiler.util.Util.toString(
				respectiveContainers,
				new org.eclipse.jdt.internal.compiler.util.Util.Displayable(){
					@Override
					public String displayString(Object o) {
						StringBuilder buffer = new StringBuilder("		"); //$NON-NLS-1$
						if (o == null) {
							buffer.append("<null>"); //$NON-NLS-1$
							return buffer.toString();
						}
						buffer.append(container.getDescription());
						buffer.append(" {\n"); //$NON-NLS-1$
						if (oldEntries == null) {
							buffer.append(" 			"); //$NON-NLS-1$
							buffer.append("<null>\n"); //$NON-NLS-1$
						} else {
							for (int j = 0; j < oldEntries.length; j++){
								buffer.append(" 			"); //$NON-NLS-1$
								buffer.append(oldEntries[j]);
								buffer.append('\n');
							}
						}
						buffer.append(" 		}"); //$NON-NLS-1$
						return buffer.toString();
					}
				}) +
			"}\n	new values: {\n"  +//$NON-NLS-1$
			org.eclipse.jdt.internal.compiler.util.Util.toString(
				respectiveContainers,
				new org.eclipse.jdt.internal.compiler.util.Util.Displayable(){
					@Override
					public String displayString(Object o) {
						StringBuilder buffer = new StringBuilder("		"); //$NON-NLS-1$
						if (o == null) {
							buffer.append("<null>"); //$NON-NLS-1$
							return buffer.toString();
						}
						buffer.append(container.getDescription());
						buffer.append(" {\n"); //$NON-NLS-1$
						for (int j = 0; j < newEntries.length; j++){
							buffer.append(" 			"); //$NON-NLS-1$
							buffer.append(newEntries[j]);
							buffer.append('\n');
						}
						buffer.append(" 		}"); //$NON-NLS-1$
						return buffer.toString();
					}
				}) +
			"\n	}"); //$NON-NLS-1$
	}

	void verbose_missbehaving_container(IJavaProject project, IPath containerPath, IClasspathEntry[] classpathEntries) {
		Util.verbose(
			"CPContainer GET - missbehaving container (returning null classpath entry)\n" + //$NON-NLS-1$
			"	project: " + project.getElementName() + '\n' + //$NON-NLS-1$
			"	container path: " + containerPath + '\n' + //$NON-NLS-1$
			"	classpath entries: {\n" + //$NON-NLS-1$
			org.eclipse.jdt.internal.compiler.util.Util.toString(
				classpathEntries,
				new org.eclipse.jdt.internal.compiler.util.Util.Displayable(){
					@Override
					public String displayString(Object o) {
						StringBuilder buffer = new StringBuilder("		"); //$NON-NLS-1$
						if (o == null) {
							buffer.append("<null>"); //$NON-NLS-1$
							return buffer.toString();
						}
						buffer.append(o);
						return buffer.toString();
					}
				}) +
			"\n	}" //$NON-NLS-1$
		);
	}

	void verbose_missbehaving_container_null_entries(IJavaProject project, IPath containerPath) {
		Util.verbose(
			"CPContainer GET - missbehaving container (returning null as classpath entries)\n" + //$NON-NLS-1$
			"	project: " + project.getElementName() + '\n' + //$NON-NLS-1$
			"	container path: " + containerPath + '\n' + //$NON-NLS-1$
			"	classpath entries: <null>" //$NON-NLS-1$
		);
	}

	void containerRemoveInitializationInProgress(IJavaProject project, IPath containerPath) {
		Map<IJavaProject, Set<IPath>> initializations = this.containerInitializationInProgress.get();
		if (initializations == null)
			return;
		Set<IPath> projectInitializations = initializations.get(project);
		if (projectInitializations == null)
			return;
		projectInitializations.remove(containerPath);
		if (projectInitializations.size() == 0)
			initializations.remove(project);
		if (initializations.size() == 0)
			this.containerInitializationInProgress.remove();
	}

	private synchronized void containersReset(String[] containerIDs) {
		for (int i = 0; i < containerIDs.length; i++) {
			String containerID = containerIDs[i];
			Iterator<Map<IPath, IClasspathContainer>> projectIterator = this.containers.values().iterator();
			while (projectIterator.hasNext()){
				Map<IPath, IClasspathContainer> projectContainers = projectIterator.next();
				if (projectContainers != null){
					Iterator<IPath> containerIterator = projectContainers.keySet().iterator();
					while (containerIterator.hasNext()){
						IPath containerPath = containerIterator.next();
						if (containerID.equals(containerPath.segment(0))) { // registered container
							projectContainers.put(containerPath, null); // reset container value, but leave entry in Map
						}
					}
				}
			}
		}
	}

	/**
	 * Returns the Java element corresponding to the given resource, or
	 * <code>null</code> if unable to associate the given resource
	 * with a Java element.
	 * <p>
	 * The resource must be one of:<ul>
	 *	<li>a project - the element returned is the corresponding <code>IJavaProject</code></li>
	 *	<li>a <code>.java</code> file - the element returned is the corresponding <code>ICompilationUnit</code></li>
	 *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
	 *	<li>a ZIP archive (e.g. a <code>.jar</code>, a <code>.zip</code> file, etc.) - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
	 *  <li>a folder - the element returned is the corresponding <code>IPackageFragmentRoot</code>
	 *			or <code>IPackageFragment</code></li>
	 *  <li>the workspace root resource - the element returned is the <code>IJavaModel</code></li>
	 *	</ul>
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 */
	public static IJavaElement create(IResource resource, IJavaProject project) {
		if (resource == null) {
			return null;
		}
		int type = resource.getType();
		switch (type) {
			case IResource.PROJECT :
				return JavaCore.create((IProject) resource);
			case IResource.FILE :
				return create((IFile) resource, project);
			case IResource.FOLDER :
				return create((IFolder) resource, project);
			case IResource.ROOT :
				return JavaCore.create((IWorkspaceRoot) resource);
			default :
				return null;
		}
	}

	/**
	 * Returns the Java element corresponding to the given file, its project being the given
	 * project.
	 * Returns <code>null</code> if unable to associate the given file
	 * with a Java element.
	 *
	 * <p>The file must be one of:<ul>
	 *	<li>a <code>.java</code> file - the element returned is the corresponding <code>ICompilationUnit</code></li>
	 *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
	 *	<li>a ZIP archive (e.g. a <code>.jar</code>, a <code>.zip</code> file, etc.) - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
	 *	</ul>
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 */
	public static IJavaElement create(IFile file, IJavaProject project) {
		if (file == null) {
			return null;
		}
		if (project == null) {
			project = JavaCore.create(file.getProject());
		}

		if (file.getFileExtension() != null) {
			String name = file.getName();
			if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(name))
				return createCompilationUnitFrom(file, project);
			if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(name))
				return createClassFileFrom(file, project);
			return createJarPackageFragmentRootFrom(file, project);
		}
		return null;
	}

	/**
	 * Returns the package fragment or package fragment root corresponding to the given folder,
	 * its parent or great parent being the given project.
	 * or <code>null</code> if unable to associate the given folder with a Java element.
	 * <p>
	 * Note that a package fragment root is returned rather than a default package.
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 */
	public static IJavaElement create(IFolder folder, IJavaProject project) {
		if (folder == null) {
			return null;
		}
		IJavaElement element;
		if (project == null) {
			project = JavaCore.create(folder.getProject());
			element = determineIfOnClasspath(folder, project);
			if (element == null) {
				// walk all projects and find one that have the given folder on its classpath
				IJavaProject[] projects;
				try {
					projects = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProjects();
				} catch (JavaModelException e) {
					return null;
				}
				for (int i = 0, length = projects.length; i < length; i++) {
					project = projects[i];
					element = determineIfOnClasspath(folder, project);
					if (element != null)
						break;
				}
			}
		} else {
			element = determineIfOnClasspath(folder, project);
		}
		return element;
	}

	/**
	 * Creates and returns a class file element for the given <code>.class</code> file,
	 * its project being the given project. Returns <code>null</code> if unable
	 * to recognize the class file.
	 */
	public static IClassFile createClassFileFrom(IFile file, IJavaProject project ) {
		if (file == null) {
			return null;
		}
		if (project == null) {
			project = JavaCore.create(file.getProject());
		}
		IPackageFragment pkg = (IPackageFragment) determineIfOnClasspath(file, project);
		if (pkg == null) {
			// fix for 1FVS7WE
			// not on classpath - make the root its folder, and a default package
			PackageFragmentRoot root = (PackageFragmentRoot) project.getPackageFragmentRoot(file.getParent());
			pkg = root.getPackageFragment(CharOperation.NO_STRINGS);
		}
		String fileName = file.getName();
		if (TypeConstants.MODULE_INFO_CLASS_NAME_STRING.equals(fileName))
			return pkg.getModularClassFile();
		return pkg.getClassFile(file.getName());
	}

	/**
	 * Creates and returns a compilation unit element for the given <code>.java</code>
	 * file, its project being the given project. Returns <code>null</code> if unable
	 * to recognize the compilation unit.
	 */
	public static ICompilationUnit createCompilationUnitFrom(IFile file, IJavaProject project) {

		if (file == null) return null;

		if (project == null) {
			project = JavaCore.create(file.getProject());
		}
		IPackageFragment pkg = (IPackageFragment) determineIfOnClasspath(file, project);
		if (pkg == null) {
			// not on classpath - make the root its folder, and a default package
			PackageFragmentRoot root = (PackageFragmentRoot) project.getPackageFragmentRoot(file.getParent());
			pkg = root.getPackageFragment(CharOperation.NO_STRINGS);

			if (VERBOSE){
				System.out.println("WARNING : creating unit element outside classpath ("+ Thread.currentThread()+"): " + file.getFullPath()); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		return pkg.getCompilationUnit(file.getName());
	}

	/**
	 * Creates and returns a handle for the given JAR file, its project being the given project.
	 * The Java model associated with the JAR's project may be
	 * created as a side effect.
	 * Returns <code>null</code> if unable to create a JAR package fragment root.
	 * (for example, if the JAR file represents a non-Java resource)
	 */
	public static IPackageFragmentRoot createJarPackageFragmentRootFrom(IFile file, IJavaProject project) {
		if (file == null) {
			return null;
		}
		if (project == null) {
			project = JavaCore.create(file.getProject());
		}

		// Create a jar package fragment root only if on the classpath
		IPath resourcePath = file.getFullPath();
		try {
			IClasspathEntry entry = ((JavaProject)project).getClasspathEntryFor(resourcePath);
			if (entry != null) {
				return project.getPackageFragmentRoot(file);
			}
		} catch (JavaModelException e) {
			// project doesn't exist: return null
		}
		return null;
	}

	/**
	 * Returns the package fragment root represented by the resource, or
	 * the package fragment the given resource is located in, or <code>null</code>
	 * if the given resource is not on the classpath of the given project.
	 */
	public static IJavaElement determineIfOnClasspath(IResource resource, IJavaProject project) {
		IPath resourcePath = resource.getFullPath();
		boolean isExternal = ExternalFoldersManager.isInternalPathForExternalFolder(resourcePath);
		if (isExternal)
			resourcePath = resource.getLocation();

		try {
			JavaProjectElementInfo projectInfo = (JavaProjectElementInfo) getJavaModelManager().getInfo(project);
			ProjectCache projectCache = projectInfo == null ? null : projectInfo.projectCache;
			HashtableOfArrayToObject allPkgFragmentsCache = projectCache == null ? null : projectCache.allPkgFragmentsCache;
			boolean isJavaLike = org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(resourcePath.lastSegment());
			IClasspathEntry[] entries = isJavaLike ? project.getRawClasspath() // JAVA file can only live inside SRC folder (on the raw path)
					: ((JavaProject)project).getResolvedClasspath();

			int length	= entries.length;
			if (length > 0) {
				String sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
				String complianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
				for (int i = 0; i < length; i++) {
					IClasspathEntry entry = entries[i];
					if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) continue;
					IPath rootPath = entry.getPath();
					if (rootPath.equals(resourcePath)) {
						if (isJavaLike)
							return null;
						return project.getPackageFragmentRoot(resource);
					} else if (rootPath.isPrefixOf(resourcePath)) {
						// allow creation of package fragment if it contains a .java file that is included
						if (!Util.isExcluded(resource, ((ClasspathEntry)entry).fullInclusionPatternChars(), ((ClasspathEntry)entry).fullExclusionPatternChars())) {
							// given we have a resource child of the root, it cannot be a JAR pkg root
							PackageFragmentRoot root =
								isExternal ?
									new ExternalPackageFragmentRoot(rootPath, (JavaProject) project) :
									(PackageFragmentRoot) ((JavaProject) project).getFolderPackageFragmentRoot(rootPath);
							if (root == null) return null;
							IPath pkgPath = resourcePath.removeFirstSegments(rootPath.segmentCount());

							if (resource.getType() == IResource.FILE) {
								// if the resource is a file, then remove the last segment which
								// is the file name in the package
								pkgPath = pkgPath.removeLastSegments(1);
							}
							String[] pkgName = pkgPath.segments();

							// if package name is in the cache, then it has already been validated
							// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=133141)
							if (allPkgFragmentsCache != null && allPkgFragmentsCache.containsKey(pkgName))
								return root.getPackageFragment(pkgName);

							if (pkgName.length != 0 && JavaConventions.validatePackageName(Util.packageName(pkgPath, sourceLevel, complianceLevel), sourceLevel, complianceLevel).getSeverity() == IStatus.ERROR) {
								return null;
							}
							return root.getPackageFragment(pkgName);
						}
					}
				}
			}
		} catch (JavaModelException npe) {
			return null;
		}
		return null;
	}

	/**
	 * The singleton manager
	 */
	private static JavaModelManager MANAGER= new JavaModelManager();

	/**
	 * Infos cache.
	 */
	private JavaModelCache cache;

	/*
	 * Temporary cache of newly opened elements
	 */
	private ThreadLocal<HashMap<IJavaElement, Object>> temporaryCache = new ThreadLocal<>();

	/**
	 * Set of elements which are out of sync with their buffers.
	 */
	protected HashSet<Openable> elementsOutOfSynchWithBuffers = new HashSet<>(11);

	/**
	 * Holds the state used for delta processing.
	 */
	public DeltaProcessingState deltaState = new DeltaProcessingState();

	public IndexManager indexManager = null;

	/**
	 * Table from IProject to PerProjectInfo.
	 * NOTE: this object itself is used as a lock to synchronize creation/removal of per project infos
	 */
	protected Map<IProject, PerProjectInfo> perProjectInfos = new HashMap<>(5);

	/**
	 * Table from WorkingCopyOwner to a table of ICompilationUnit (working copy handle) to PerWorkingCopyInfo.
	 * NOTE: this object itself is used as a lock to synchronize creation/removal of per working copy infos
	 */
	protected HashMap<WorkingCopyOwner, Map<CompilationUnit, PerWorkingCopyInfo>> perWorkingCopyInfos = new HashMap<>(5);

	/**
	 * A weak set of the known search scopes.
	 */
	protected WeakHashMap<AbstractSearchScope, ?> searchScopes = new WeakHashMap<>();

	public static class PerProjectInfo {
		private static final int JAVADOC_CACHE_INITIAL_SIZE = 10;

		static final IJavaModelStatus NEED_RESOLUTION = new JavaModelStatus();

		public IProject project;
		public Object savedState;
		public boolean triedRead;
		public IClasspathEntry[] rawClasspath;
		public IClasspathEntry[] referencedEntries;
		public IJavaModelStatus rawClasspathStatus;
		public int rawTimeStamp = 0;
		public boolean writtingRawClasspath = false;
		public IClasspathEntry[] resolvedClasspath;
		public IJavaModelStatus unresolvedEntryStatus;
		public Map<IPath, IClasspathEntry> rootPathToRawEntries; // reverse map from a package fragment root's path to the raw entry
		public Map<IPath, IClasspathEntry> rootPathToResolvedEntries; // map from a package fragment root's path to the resolved entry
		public IPath outputLocation;
		public Map<IPath, ObjectVector> jrtRoots; // A map between a JRT file system (as a string) and the package fragment roots found in it.

		public IEclipsePreferences preferences;
		public Hashtable<String, String> options;
		public Hashtable<String, Map<String, IType>> secondaryTypes;
		/**
		 * The temporary structure used while indexing, previously known as INDEXED_SECONDARY_TYPES entry
		 */
		volatile Map<IFile, Map<String, Map<String, IType>>> indexingSecondaryCache;


		// NB: PackageFragment#getAttachedJavadoc uses this map differently
		// and stores String data, not JavadocContents as values
		public LRUCache<IJavaElement, Object> javadocCache;

		public PerProjectInfo(IProject project) {

			this.triedRead = false;
			this.savedState = null;
			this.project = project;
			this.javadocCache = new LRUCache<>(JAVADOC_CACHE_INITIAL_SIZE);
		}

		public synchronized IClasspathEntry[] getResolvedClasspath() {
			if (this.unresolvedEntryStatus == NEED_RESOLUTION)
				return null;
			return this.resolvedClasspath;
		}

		public void forgetExternalTimestampsAndIndexes() {
			IClasspathEntry[] classpath = this.resolvedClasspath;
			if (classpath == null) return;
			JavaModelManager manager = JavaModelManager.getJavaModelManager();
			IndexManager indexManager = manager.indexManager;
			Hashtable<IPath, Long> externalTimeStamps = manager.deltaState.getExternalLibTimeStamps();
			Map<IPath, List<RootInfo>> rootInfos = JavaModelManager.getDeltaState().otherRoots;
			for (int i = 0, length = classpath.length; i < length; i++) {
				IClasspathEntry entry = classpath[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					IPath path = entry.getPath();
					if (rootInfos.get(path) == null) {
						externalTimeStamps.remove(path);
						indexManager.removeIndex(path); // force reindexing on next reference (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=250083 )
					}
				}
			}
		}

		public void rememberExternalLibTimestamps() {
			IClasspathEntry[] classpath = this.resolvedClasspath;
			if (classpath == null) return;
			Map<IPath, Long> externalTimeStamps = JavaModelManager.getJavaModelManager().deltaState.getExternalLibTimeStamps();
			for (int i = 0, length = classpath.length; i < length; i++) {
				IClasspathEntry entry = classpath[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					IPath path = entry.getPath();
					if (externalTimeStamps.get(path) == null) {
						Object target = JavaModel.getExternalTarget(path, true);
						if (target instanceof File) {
							long timestamp = DeltaProcessor.getTimeStamp((java.io.File)target);
							externalTimeStamps.put(path, Long.valueOf(timestamp));
						}
					}
				}
			}
		}

		public synchronized ClasspathChange resetResolvedClasspath() {
			// clear non-chaining jars cache and external jars cache
			JavaModelManager.getJavaModelManager().resetClasspathListCache();

			// null out resolved information
			return setResolvedClasspath(null, null, null, null, this.rawTimeStamp, true/*add classpath change*/);
		}

		private ClasspathChange setClasspath(IClasspathEntry[] newRawClasspath, IClasspathEntry[] referencedEntries, IPath newOutputLocation, IJavaModelStatus newRawClasspathStatus, IClasspathEntry[] newResolvedClasspath, Map<IPath, IClasspathEntry> newRootPathToRawEntries, Map<IPath, IClasspathEntry> newRootPathToResolvedEntries, IJavaModelStatus newUnresolvedEntryStatus, boolean addClasspathChange) {
			if (DEBUG_CLASSPATH) {
				System.out.println("Setting resolved classpath for " + this.project.getFullPath()); //$NON-NLS-1$
				if (newResolvedClasspath == null) {
					System.out.println("New classpath = null"); //$NON-NLS-1$
				} else {
					for (IClasspathEntry next : newResolvedClasspath) {
						System.out.println("    " + next); //$NON-NLS-1$
					}
				}
			}
			ClasspathChange classpathChange = addClasspathChange ? addClasspathChange() : null;

			if (referencedEntries != null)	this.referencedEntries = referencedEntries;
			if (this.referencedEntries == null) this.referencedEntries = ClasspathEntry.NO_ENTRIES;
			this.rawClasspath = newRawClasspath;
			this.outputLocation = newOutputLocation;
			this.rawClasspathStatus = newRawClasspathStatus;
			this.resolvedClasspath = newResolvedClasspath;
			this.rootPathToRawEntries = newRootPathToRawEntries;
			this.rootPathToResolvedEntries = newRootPathToResolvedEntries;
			this.unresolvedEntryStatus = newUnresolvedEntryStatus;
			this.javadocCache = new LRUCache<>(JAVADOC_CACHE_INITIAL_SIZE);

			return classpathChange;
		}

		protected ClasspathChange addClasspathChange() {
			// remember old info
			JavaModelManager manager = JavaModelManager.getJavaModelManager();
			ClasspathChange classpathChange = manager.deltaState.addClasspathChange(this.project, this.rawClasspath, this.outputLocation, this.resolvedClasspath);
			return classpathChange;
		}

		public ClasspathChange setRawClasspath(IClasspathEntry[] newRawClasspath, IPath newOutputLocation, IJavaModelStatus newRawClasspathStatus) {
			return setRawClasspath(newRawClasspath, null, newOutputLocation, newRawClasspathStatus);
		}

		public synchronized ClasspathChange setRawClasspath(IClasspathEntry[] newRawClasspath, IClasspathEntry[] referencedEntries, IPath newOutputLocation, IJavaModelStatus newRawClasspathStatus) {
			this.rawTimeStamp++;
			return setClasspath(newRawClasspath, referencedEntries, newOutputLocation, newRawClasspathStatus, null/*resolved classpath*/, null/*root to raw map*/, null/*root to resolved map*/, null/*unresolved status*/, true/*add classpath change*/);
		}

		public ClasspathChange setResolvedClasspath(IClasspathEntry[] newResolvedClasspath, Map<IPath, IClasspathEntry> newRootPathToRawEntries, Map<IPath, IClasspathEntry> newRootPathToResolvedEntries, IJavaModelStatus newUnresolvedEntryStatus, int timeStamp, boolean addClasspathChange) {
			return setResolvedClasspath(newResolvedClasspath, null, newRootPathToRawEntries, newRootPathToResolvedEntries, newUnresolvedEntryStatus, timeStamp, addClasspathChange);
		}

		public synchronized ClasspathChange setResolvedClasspath(IClasspathEntry[] newResolvedClasspath, IClasspathEntry[] referencedEntries, Map<IPath, IClasspathEntry> newRootPathToRawEntries, Map<IPath, IClasspathEntry> newRootPathToResolvedEntries, IJavaModelStatus newUnresolvedEntryStatus, int timeStamp, boolean addClasspathChange) {
			if (this.rawTimeStamp != timeStamp)
				return null;
			return setClasspath(this.rawClasspath, referencedEntries, this.outputLocation, this.rawClasspathStatus, newResolvedClasspath, newRootPathToRawEntries, newRootPathToResolvedEntries, newUnresolvedEntryStatus, addClasspathChange);
		}

		public synchronized void setJrtPackageRoots(IPath jrtPath, ObjectVector roots) {
			if (this.jrtRoots == null) this.jrtRoots = new HashMap<>();
			this.jrtRoots.put(jrtPath, roots);
		}

		/**
		 * Reads the classpath and caches the entries. Returns a two-dimensional array, where the number of elements in the row is fixed to 2.
		 * The first element is an array of raw classpath entries and the second element is an array of referenced entries that may have been stored
		 * by the client earlier. See {@link IJavaProject#getReferencedClasspathEntries()} for more details.
		 *
		 */
		public synchronized IClasspathEntry[][] readAndCacheClasspath(JavaProject javaProject) {
			// read file entries and update status
			IClasspathEntry[][] classpath;
			IJavaModelStatus status;
			try {
				classpath = javaProject.readFileEntriesWithException(null/*not interested in unknown elements*/);
				status = JavaModelStatus.VERIFIED_OK;
			} catch (CoreException e) {
				classpath = new IClasspathEntry[][]{JavaProject.INVALID_CLASSPATH, ClasspathEntry.NO_ENTRIES};
				status =
					new JavaModelStatus(
						IJavaModelStatusConstants.INVALID_CLASSPATH_FILE_FORMAT,
						Messages.bind(Messages.classpath_cannotReadClasspathFile, javaProject.getElementName()));
			} catch (IOException e) {
				classpath = new IClasspathEntry[][]{JavaProject.INVALID_CLASSPATH, ClasspathEntry.NO_ENTRIES};
				if (Messages.file_badFormat.equals(e.getMessage()))
					status =
						new JavaModelStatus(
							IJavaModelStatusConstants.INVALID_CLASSPATH_FILE_FORMAT,
							Messages.bind(Messages.classpath_xmlFormatError, javaProject.getElementName(), Messages.file_badFormat));
				else
					status =
						new JavaModelStatus(
							IJavaModelStatusConstants.INVALID_CLASSPATH_FILE_FORMAT,
							Messages.bind(Messages.classpath_cannotReadClasspathFile, javaProject.getElementName()));
			} catch (ClasspathEntry.AssertionFailedException e) {
				classpath = new IClasspathEntry[][]{JavaProject.INVALID_CLASSPATH, ClasspathEntry.NO_ENTRIES};
				status =
					new JavaModelStatus(
						IJavaModelStatusConstants.INVALID_CLASSPATH_FILE_FORMAT,
						Messages.bind(Messages.classpath_illegalEntryInClasspathFile, new String[] {javaProject.getElementName(), e.getMessage()}));
			}

			// extract out the output location
			int rawClasspathLength = classpath[0].length;
			IPath output = null;
			if (rawClasspathLength > 0) {
				IClasspathEntry entry = classpath[0][rawClasspathLength - 1];
				if (entry.getContentKind() == ClasspathEntry.K_OUTPUT) {
					output = entry.getPath();
					IClasspathEntry[] copy = new IClasspathEntry[rawClasspathLength - 1];
					System.arraycopy(classpath[0], 0, copy, 0, copy.length);
					classpath[0] = copy;
				}
			}

			// store new raw classpath, new output and new status, and null out resolved info
			setRawClasspath(classpath[0], classpath[1], output, status);

			return classpath;
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("Info for "); //$NON-NLS-1$
			buffer.append(this.project.getFullPath());
			buffer.append("\nRaw classpath:\n"); //$NON-NLS-1$
			if (this.rawClasspath == null) {
				buffer.append("  <null>\n"); //$NON-NLS-1$
			} else {
				for (int i = 0, length = this.rawClasspath.length; i < length; i++) {
					buffer.append("  "); //$NON-NLS-1$
					buffer.append(this.rawClasspath[i]);
					buffer.append('\n');
				}
			}
			buffer.append("Resolved classpath:\n"); //$NON-NLS-1$
			IClasspathEntry[] resolvedCP = this.resolvedClasspath;
			if (resolvedCP == null) {
				buffer.append("  <null>\n"); //$NON-NLS-1$
			} else {
				for (int i = 0, length = resolvedCP.length; i < length; i++) {
					buffer.append("  "); //$NON-NLS-1$
					buffer.append(resolvedCP[i]);
					buffer.append('\n');
				}
			}
			buffer.append("Resolved classpath status: "); //$NON-NLS-1$
			if (this.unresolvedEntryStatus == NEED_RESOLUTION)
				buffer.append("NEED RESOLUTION"); //$NON-NLS-1$
			else
				buffer.append(this.unresolvedEntryStatus == null ? "<null>\n" : this.unresolvedEntryStatus.toString()); //$NON-NLS-1$
			buffer.append("Output location:\n  "); //$NON-NLS-1$
			if (this.outputLocation == null) {
				buffer.append("<null>"); //$NON-NLS-1$
			} else {
				buffer.append(this.outputLocation);
			}
			return buffer.toString();
		}

		public boolean writeAndCacheClasspath(
				JavaProject javaProject,
				final IClasspathEntry[] newRawClasspath,
				IClasspathEntry[] newReferencedEntries,
				final IPath newOutputLocation) throws JavaModelException {
			try {
				this.writtingRawClasspath = true;
				if (newReferencedEntries == null) newReferencedEntries = this.referencedEntries;

				// write .classpath
				if (!javaProject.writeFileEntries(newRawClasspath, newReferencedEntries,  newOutputLocation)) {
					return false;
				}
				// store new raw classpath, new output and new status, and null out resolved info
				setRawClasspath(newRawClasspath, newReferencedEntries, newOutputLocation, JavaModelStatus.VERIFIED_OK);
			} finally {
				this.writtingRawClasspath = false;
			}
			return true;
		}

		public boolean writeAndCacheClasspath(JavaProject javaProject, final IClasspathEntry[] newRawClasspath, final IPath newOutputLocation) throws JavaModelException {
			return writeAndCacheClasspath(javaProject, newRawClasspath, null, newOutputLocation);
		}

	}

	public static class PerWorkingCopyInfo implements IProblemRequestor {
		int useCount = 0;
		IProblemRequestor problemRequestor;
		CompilationUnit workingCopy;
		public PerWorkingCopyInfo(CompilationUnit workingCopy, IProblemRequestor problemRequestor) {
			this.workingCopy = workingCopy;
			this.problemRequestor = problemRequestor;
		}
		@Override
		public void acceptProblem(IProblem problem) {
			IProblemRequestor requestor = getProblemRequestor();
			if (requestor == null) return;
			requestor.acceptProblem(problem);
		}
		@Override
		public void beginReporting() {
			IProblemRequestor requestor = getProblemRequestor();
			if (requestor == null) return;
			requestor.beginReporting();
		}
		@Override
		public void endReporting() {
			IProblemRequestor requestor = getProblemRequestor();
			if (requestor == null) return;
			requestor.endReporting();
		}
		public IProblemRequestor getProblemRequestor() {
			if (this.problemRequestor == null && this.workingCopy.owner != null) {
				return this.workingCopy.owner.getProblemRequestor(this.workingCopy);
			}
			return this.problemRequestor;
		}
		public ICompilationUnit getWorkingCopy() {
			return this.workingCopy;
		}
		@Override
		public boolean isActive() {
			IProblemRequestor requestor = getProblemRequestor();
			return requestor != null && requestor.isActive();
		}
		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("Info for "); //$NON-NLS-1$
			buffer.append(((JavaElement)this.workingCopy).toStringWithAncestors());
			buffer.append("\nUse count = "); //$NON-NLS-1$
			buffer.append(this.useCount);
			buffer.append("\nProblem requestor:\n  "); //$NON-NLS-1$
			buffer.append(this.problemRequestor);
			if (this.problemRequestor == null) {
				IProblemRequestor requestor = getProblemRequestor();
				buffer.append("\nOwner problem requestor:\n  "); //$NON-NLS-1$
				buffer.append(requestor);
			}
			return buffer.toString();
		}
	}

	public static boolean VERBOSE = false;
	public static boolean DEBUG_CLASSPATH = false;
	public static boolean DEBUG_INVALID_ARCHIVES = false;
	public static boolean CP_RESOLVE_VERBOSE = false;
	public static boolean CP_RESOLVE_VERBOSE_ADVANCED = false;
	public static boolean CP_RESOLVE_VERBOSE_FAILURE = false;
	public static boolean ZIP_ACCESS_VERBOSE = false;
	public static boolean JRT_ACCESS_VERBOSE = false;

	/**
	 * A cache of opened zip files per thread.
	 * (for a given thread, the object value is a HashMap from IPath to java.io.ZipFile)
	 */
	private ThreadLocal<ZipCache> zipFiles = new ThreadLocal<>();

	private UserLibraryManager userLibraryManager;

	private ModuleSourcePathManager modulePathManager;
	/*
	 * A set of IPaths for jars that are known to not contain a chaining (through MANIFEST.MF) to another library
	 */
	private Set<IPath> nonChainingJars;

	// The amount of time from when an invalid archive is first sensed until that state is considered stale.
	private static long INVALID_ARCHIVE_TTL_MILLISECONDS = 2 * 60 * 1000;

	private static class InvalidArchiveInfo {
		/**
		 * Time at which this entry will be removed from the invalid archive list.
		 */
		final long evictionTimestamp;

		/**
		 * Reason the entry was added to the invalid archive list.
		 */
		final ArchiveValidity reason;

		InvalidArchiveInfo(long evictionTimestamp, ArchiveValidity reason) {
			this.evictionTimestamp = evictionTimestamp;
			this.reason = reason;
		}
	}

	/*
	 * A map of IPaths for jars with known validity (such as being in a valid/known format or not), to an eviction timestamp.
	 * Synchronize on invalidArchives before accessing.
	 */
	private final Map<IPath, InvalidArchiveInfo> invalidArchives = new HashMap<>();

	/*
	 * A set of IPaths for files that are known to be external to the workspace.
	 * Need not be referenced by the classpath.
	 */
	private Set<IPath> externalFiles;

	/*
	 * A set of IPaths for files that do not exist on the file system but are assumed to be
	 * external archives (rather than external folders).
	 */
	private Set<IPath> assumedExternalFiles;

	/**
	 * Update the classpath variable cache
	 */
	public static class EclipsePreferencesListener implements IEclipsePreferences.IPreferenceChangeListener {
		/**
         * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener#preferenceChange(org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent)
         */
        @Override
		public void preferenceChange(IEclipsePreferences.PreferenceChangeEvent event) {
        	String propertyName = event.getKey();
        	if (propertyName.startsWith(JavaCore.PLUGIN_ID)) {
	        	if (propertyName.startsWith(CP_VARIABLE_PREFERENCES_PREFIX)) {
	        		String varName = propertyName.substring(CP_VARIABLE_PREFERENCES_PREFIX.length());
	        		JavaModelManager manager = getJavaModelManager();
	        		if (manager.variablesWithInitializer.contains(varName)) {
	        			// revert preference value as we will not apply it to JavaCore classpath variable
	        			String oldValue = (String) event.getOldValue();
	        			if (oldValue == null) {
	        				// unexpected old value => remove variable from set
	        				manager.variablesWithInitializer.remove(varName);
	        			} else {
	        				manager.getInstancePreferences().put(varName, oldValue);
	        			}
	        		} else {
	        			String newValue = (String)event.getNewValue();
	        			IPath newPath;
	        			if (newValue != null && !(newValue = newValue.trim()).equals(CP_ENTRY_IGNORE)) {
	        				newPath = new Path(newValue);
	        			} else {
	        				newPath = null;
	        			}
	        			try {
	        				SetVariablesOperation operation = new SetVariablesOperation(new String[] {varName}, new IPath[] {newPath}, false/*don't update preferences*/);
	        				operation.runOperation(null/*no progress available*/);
	        			} catch (JavaModelException e) {
	        				Util.log(e, "Could not set classpath variable " + varName + " to " + newPath); //$NON-NLS-1$ //$NON-NLS-2$
	        			}
	        		}
	        	} else if (propertyName.startsWith(CP_CONTAINER_PREFERENCES_PREFIX)) {
	        		recreatePersistedContainer(propertyName, (String)event.getNewValue(), false);
	        	} else if (propertyName.equals(JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER) ||
					propertyName.equals(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER) ||
					propertyName.equals(JavaCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE) ||
					propertyName.equals(JavaCore.CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER) ||
					propertyName.equals(JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH) ||
					propertyName.equals(JavaCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS) ||
					propertyName.equals(JavaCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS) ||
					propertyName.equals(JavaCore.CORE_INCOMPLETE_CLASSPATH) ||
					propertyName.equals(JavaCore.CORE_CIRCULAR_CLASSPATH) ||
					propertyName.equals(JavaCore.CORE_INCOMPATIBLE_JDK_LEVEL) ||
					propertyName.equals(JavaCore.CORE_MAIN_ONLY_PROJECT_HAS_TEST_ONLY_DEPENDENCY) ||
					propertyName.equals(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM) ||
					propertyName.equals(JavaCore.CORE_OUTPUT_LOCATION_OVERLAPPING_ANOTHER_SOURCE)) {
					JavaModelManager manager = JavaModelManager.getJavaModelManager();
					IJavaModel model = manager.getJavaModel();
					IJavaProject[] jProjects;
					try {
						jProjects = model.getJavaProjects();
						IProject[] projects = new IProject[jProjects.length];
						for (int i = 0, pl = jProjects.length; i < pl; i++) {
							JavaProject javaProject = (JavaProject) jProjects[i];
							projects[i] = javaProject.getProject();
							manager.deltaState.addClasspathValidation(javaProject);
						}
						manager.touchProjects(projects, null);
					} catch (JavaModelException e) {
						// skip
					}
	        	} else if (propertyName.startsWith(CP_USERLIBRARY_PREFERENCES_PREFIX)) {
					String libName = propertyName.substring(CP_USERLIBRARY_PREFERENCES_PREFIX.length());
					UserLibraryManager manager = JavaModelManager.getUserLibraryManager();
	        		manager.updateUserLibrary(libName, (String)event.getNewValue());
	        	}
	        }
        	// Reset all project caches (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=233568 )
        	try {
        		IJavaProject[] projects = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProjects();
	        	for (int i = 0, length = projects.length; i < length; i++) {
					((JavaProject) projects[i]).resetCaches();
				}
        	} catch (JavaModelException e) {
        		// cannot retrieve Java projects
        	}
        }
	}
	/**
	 * Listener on eclipse preferences changes.
	 */
	EclipsePreferencesListener instancePreferencesListener = new EclipsePreferencesListener();
	/**
	 * Listener on eclipse preferences default/instance node changes.
	 */
	IEclipsePreferences.INodeChangeListener instanceNodeListener = new IEclipsePreferences.INodeChangeListener() {
		@Override
		public void added(IEclipsePreferences.NodeChangeEvent event) {
			// do nothing
		}
		@Override
		public void removed(IEclipsePreferences.NodeChangeEvent event) {
			if (event.getChild() == JavaModelManager.this.preferencesLookup[PREF_INSTANCE]) {
				JavaModelManager.this.preferencesLookup[PREF_INSTANCE] = InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
				JavaModelManager.this.preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(new EclipsePreferencesListener());
			}
		}
	};
	IEclipsePreferences.INodeChangeListener defaultNodeListener = new IEclipsePreferences.INodeChangeListener() {
		@Override
		public void added(IEclipsePreferences.NodeChangeEvent event) {
			// do nothing
		}
		@Override
		public void removed(IEclipsePreferences.NodeChangeEvent event) {
			if (event.getChild() == JavaModelManager.this.preferencesLookup[PREF_DEFAULT]) {
				JavaModelManager.this.preferencesLookup[PREF_DEFAULT] = DefaultScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
			}
		}
	};
	/**
	 * Listener on properties changes.
	 */
	IEclipsePreferences.IPreferenceChangeListener propertyListener;
	IEclipsePreferences.IPreferenceChangeListener resourcesPropertyListener;

	/**
	 * Constructs a new JavaModelManager
	 */
	private JavaModelManager() {
		// singleton: prevent others from creating a new instance
		/*
		 * It is required to initialize all fields that depends on a headless environment
		 * only if the platform is running. Otherwise this breaks the ability to use
		 * ASTParser in a non-headless environment.
		 */
		if (Platform.isRunning()) {
			this.indexManager = new IndexManager();
			this.nonChainingJars = loadClasspathListCache(NON_CHAINING_JARS_CACHE);
			this.externalFiles = loadClasspathListCache(EXTERNAL_FILES_CACHE);
			this.assumedExternalFiles = loadClasspathListCache(ASSUMED_EXTERNAL_FILES_CACHE);
			String includeContainerReferencedLib = System.getProperty(RESOLVE_REFERENCED_LIBRARIES_FOR_CONTAINERS);
			this.resolveReferencedLibrariesForContainers = TRUE.equalsIgnoreCase(includeContainerReferencedLib);
		}
	}

	/**
	 * @deprecated
	 */
	private void addDeprecatedOptions(Hashtable<String, String> options) {
		options.put(JavaCore.COMPILER_PB_INVALID_IMPORT, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNREACHABLE_CODE, JavaCore.ERROR);
	}

	public void addNonChainingJar(IPath path) {
		if (this.nonChainingJars != null)
			this.nonChainingJars.add(path);
	}

	public void addInvalidArchive(IPath path, ArchiveValidity reason) {
		if (DEBUG_INVALID_ARCHIVES) {
			System.out.println("JAR cache: adding " + reason + " " + path);  //$NON-NLS-1$//$NON-NLS-2$
		}
		synchronized (this.invalidArchives) {
			this.invalidArchives.put(path, new InvalidArchiveInfo(System.currentTimeMillis() + INVALID_ARCHIVE_TTL_MILLISECONDS, reason));
		}
	}

	/**
	 * Adds a path to the external files cache. It is the responsibility of callers to
	 * determine the file's existence, as determined by  {@link File#isFile()}.
	 */
	public void addExternalFile(IPath path) {
		// unlikely to be null
		if (this.externalFiles == null) {
			this.externalFiles = Collections.synchronizedSet(new HashSet<IPath>());
		}
		if(this.externalFiles != null) {
			this.externalFiles.add(path);
		}
	}

	/**
	 * Starts caching ZipFiles.
	 * Ignores if there are already clients.
	 */
	public void cacheZipFiles(Object owner) {
		ZipCache zipCache = this.zipFiles.get();
		if (zipCache != null) {
			return;
		}
		// the owner will be responsible for flushing the cache
		this.zipFiles.set(new ZipCache(owner));
	}

	public void closeZipFile(ZipFile zipFile) {
		if (zipFile == null) return;
		if (this.zipFiles.get() != null) {
			if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
				System.out.println("(" + Thread.currentThread() + ") [JavaModelManager.closeZipFile(ZipFile)] NOT closed ZipFile (cache exist!) on " +zipFile.getName()); //$NON-NLS-1$	//$NON-NLS-2$
			}
			return; // zip file will be closed by call to flushZipFiles
		}
		try {
			if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
				System.out.println("(" + Thread.currentThread() + ") [JavaModelManager.closeZipFile(ZipFile)] Closing ZipFile on " +zipFile.getName()); //$NON-NLS-1$	//$NON-NLS-2$
			}
			zipFile.close();
		} catch (IOException e) {
			// problem occured closing zip file: cannot do much more
			JavaCore.getPlugin().getLog().log(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, "Error closing " + zipFile.getName(), e)); //$NON-NLS-1$
		}
	}

	public static void registerDebugOptionsListener(BundleContext context) {
		// register debug options listener
		Hashtable<String, String> properties = new Hashtable<>(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, JavaCore.PLUGIN_ID);
		DEBUG_REGISTRATION = context.registerService(DebugOptionsListener.class, new DebugOptionsListener() {
			@Override
			public void optionsChanged(DebugOptions options) {
				boolean debug = options.getBooleanOption(DEBUG, false);
				BufferManager.VERBOSE = debug && options.getBooleanOption(BUFFER_MANAGER_DEBUG, false);
				JavaBuilder.DEBUG = debug && options.getBooleanOption(BUILDER_DEBUG, false);
				Compiler.DEBUG = debug && options.getBooleanOption(COMPILER_DEBUG, false);
				JavaBuilder.SHOW_STATS = debug && options.getBooleanOption(BUILDER_STATS_DEBUG, false);
				CompletionEngine.DEBUG = debug && options.getBooleanOption(COMPLETION_DEBUG, false);
				JavaModelManager.CP_RESOLVE_VERBOSE = debug && options.getBooleanOption(CP_RESOLVE_DEBUG, false);
				JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED = debug && options.getBooleanOption(CP_RESOLVE_ADVANCED_DEBUG, false);
				JavaModelManager.CP_RESOLVE_VERBOSE_FAILURE = debug && options.getBooleanOption(CP_RESOLVE_FAILURE_DEBUG, false);
				DeltaProcessor.DEBUG = debug && options.getBooleanOption(DELTA_DEBUG, false);
				DeltaProcessor.VERBOSE = debug && options.getBooleanOption(DELTA_DEBUG_VERBOSE, false);
				SourceRangeVerifier.DEBUG = debug && options.getBooleanOption(DOM_AST_DEBUG, false);
				SourceRangeVerifier.DEBUG_THROW = debug && options.getBooleanOption(DOM_AST_DEBUG_THROW, false);
				SourceRangeVerifier.DEBUG |= SourceRangeVerifier.DEBUG_THROW;
				RewriteEventStore.DEBUG = debug && options.getBooleanOption(DOM_REWRITE_DEBUG, false);
				TypeHierarchy.DEBUG = debug && options.getBooleanOption(HIERARCHY_DEBUG, false);
				JobManager.VERBOSE = debug && options.getBooleanOption(INDEX_MANAGER_DEBUG, false);
				IndexManager.DEBUG = debug && options.getBooleanOption(INDEX_MANAGER_ADVANCED_DEBUG, false);
				JavaModelManager.DEBUG_CLASSPATH = debug && options.getBooleanOption(JAVAMODEL_CLASSPATH, false);
				JavaModelManager.DEBUG_INVALID_ARCHIVES = debug && options.getBooleanOption(JAVAMODEL_INVALID_ARCHIVES, false);
				JavaModelManager.VERBOSE = debug && options.getBooleanOption(JAVAMODEL_DEBUG, false);
				JavaModelCache.VERBOSE = debug && options.getBooleanOption(JAVAMODELCACHE_DEBUG, false);
				JavaModelCache.DEBUG_CACHE_INSERTIONS = debug && options.getBooleanOption(JAVAMODELCACHE_INSERTIONS_DEBUG, false);
				JavaModelOperation.POST_ACTION_VERBOSE = debug && options.getBooleanOption(POST_ACTION_DEBUG, false);
				NameLookup.VERBOSE = debug && options.getBooleanOption(RESOLUTION_DEBUG, false);
				BasicSearchEngine.VERBOSE = debug && options.getBooleanOption(SEARCH_DEBUG, false);
				SelectionEngine.DEBUG = debug && options.getBooleanOption(SELECTION_DEBUG, false);
				JavaModelManager.ZIP_ACCESS_VERBOSE = debug && options.getBooleanOption(ZIP_ACCESS_DEBUG, false);
				SourceMapper.VERBOSE = debug && options.getBooleanOption(SOURCE_MAPPER_DEBUG_VERBOSE, false);
				DefaultCodeFormatter.DEBUG = debug && options.getBooleanOption(FORMATTER_DEBUG, false);

				// configure performance options
				if(PerformanceStats.ENABLED) {
					CompletionEngine.PERF = PerformanceStats.isEnabled(COMPLETION_PERF);
					SelectionEngine.PERF = PerformanceStats.isEnabled(SELECTION_PERF);
					DeltaProcessor.PERF = PerformanceStats.isEnabled(DELTA_LISTENER_PERF);
					JavaModelManager.PERF_VARIABLE_INITIALIZER = PerformanceStats.isEnabled(VARIABLE_INITIALIZER_PERF);
					JavaModelManager.PERF_CONTAINER_INITIALIZER = PerformanceStats.isEnabled(CONTAINER_INITIALIZER_PERF);
					ReconcileWorkingCopyOperation.PERF = PerformanceStats.isEnabled(RECONCILE_PERF);
				}
			}
		}, properties);
	}

	public static void unregisterDebugOptionsListener() {
		// unregister debug options listener
		DEBUG_REGISTRATION.unregister();
		DEBUG_REGISTRATION = null;
	}

	/*
	 * Return a new Java 6 annotation processor manager.  The manager will need to
	 * be configured before it can be used.  Returns null if a manager cannot be
	 * created, i.e. if the current VM does not support Java 6 annotation processing.
	 */
	public AbstractAnnotationProcessorManager createAnnotationProcessorManager() {
		synchronized(this) {
			if (this.annotationProcessorManagerFactory == null) {
				IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(JavaCore.PLUGIN_ID, ANNOTATION_PROCESSOR_MANAGER_EXTPOINT_ID);
				if (extension == null)
					return null;
				IExtension[] extensions = extension.getExtensions();
				for(int i = 0; i < extensions.length; i++) {
					if (i > 0) {
						Util.log(null, "An annotation processor manager is already registered: ignoring " + extensions[i].getUniqueIdentifier()); //$NON-NLS-1$
						break;
					}
					IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
					for(int j = 0; j < configElements.length; j++) {
						final IConfigurationElement configElement = configElements[j];
						if ("annotationProcessorManager".equals(configElement.getName())) { //$NON-NLS-1$
							this.annotationProcessorManagerFactory = configElement;
							break;
						}
					}
				}
			}
		}

		if (this.annotationProcessorManagerFactory == null) {
			return null;
		}
		final AbstractAnnotationProcessorManager[] apm = new AbstractAnnotationProcessorManager[1];
		apm[0] = null;
		final IConfigurationElement factory = this.annotationProcessorManagerFactory;
		SafeRunner.run(new ISafeRunnable() {
			@Override
			public void handleException(Throwable exception) {
				Util.log(exception, "Exception occurred while loading annotation processor manager"); //$NON-NLS-1$
			}
			@Override
			public void run() throws Exception {
				Object executableExtension = factory.createExecutableExtension("class"); //$NON-NLS-1$
				if (executableExtension instanceof AbstractAnnotationProcessorManager) {
					apm[0] = (AbstractAnnotationProcessorManager) executableExtension;
				}
			}
		});
		return apm[0];
	}

	/*
	 * Discards the per working copy info for the given working copy (making it a compilation unit)
	 * if its use count was 1. Otherwise, just decrement the use count.
	 * If the working copy is primary, computes the delta between its state and the original compilation unit
	 * and register it.
	 * Close the working copy, its buffer and remove it from the shared working copy table.
	 * Ignore if no per-working copy info existed.
	 * NOTE: it must NOT be synchronized as it may interact with the element info cache (if useCount is decremented to 0), see bug 50667.
	 * Returns the new use count (or -1 if it didn't exist).
	 */
	public int discardPerWorkingCopyInfo(CompilationUnit workingCopy) throws JavaModelException {

		// create the delta builder (this remembers the current content of the working copy)
		// outside the perWorkingCopyInfos lock (see bug 50667)
		JavaElementDeltaBuilder deltaBuilder = null;
		if (workingCopy.isPrimary() && workingCopy.hasUnsavedChanges()) {
			deltaBuilder = new JavaElementDeltaBuilder(workingCopy);
		}
		PerWorkingCopyInfo info = null;
		synchronized(this.perWorkingCopyInfos) {
			WorkingCopyOwner owner = workingCopy.owner;
			Map<CompilationUnit, PerWorkingCopyInfo> workingCopyToInfos = this.perWorkingCopyInfos.get(owner);
			if (workingCopyToInfos == null) return -1;

			info = workingCopyToInfos.get(workingCopy);
			if (info == null) return -1;

			if (--info.useCount == 0) {
				// remove per working copy info
				workingCopyToInfos.remove(workingCopy);
				if (workingCopyToInfos.isEmpty()) {
					this.perWorkingCopyInfos.remove(owner);
				}
			}
		}
		if (info.useCount == 0) { // info cannot be null here (check was done above)
			// remove infos + close buffer (since no longer working copy)
			// outside the perWorkingCopyInfos lock (see bug 50667)
			removeInfoAndChildren(workingCopy);
			workingCopy.closeBuffer();

			// compute the delta if needed and register it if there are changes
			if (deltaBuilder != null) {
				deltaBuilder.buildDeltas();
				if (deltaBuilder.delta != null) {
					getDeltaProcessor().registerJavaModelDelta(deltaBuilder.delta);
				}
			}
		}
		return info.useCount;
	}

	/**
	 * @see ISaveParticipant
	 */
	@Override
	public void doneSaving(ISaveContext context){
		// nothing to do for jdt.core
	}

	/**
	 * Flushes ZipFiles cache if there are no more clients.
	 */
	public void flushZipFiles(Object owner) {
		ZipCache zipCache = this.zipFiles.get();
		if (zipCache == null) {
			if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
				System.out.println("(" + Thread.currentThread() + ") [JavaModelManager.flushZipFiles(String)] NOT found cache for " + owner); //$NON-NLS-1$	//$NON-NLS-2$
			}
			return;
		}
		// the owner will be responsible for flushing the cache
		// we want to check object identity to make sure this is the owner that created the cache
		if (zipCache.owner == owner) {
			this.zipFiles.remove();
			zipCache.flush();
		} else {
			if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
				System.out.println("(" + Thread.currentThread() //$NON-NLS-1$
						+ ") [JavaModelManager.flushZipFiles(String)] NOT closed cache, wrong owner, expected: " //$NON-NLS-1$
						+ zipCache.owner + ", got: " + owner); //$NON-NLS-1$
			}
		}
	}

	/*
	 * Returns true if forcing batch initialization was successful.
	 * Returns false if batch initialization is already running.
	 */
	public synchronized boolean forceBatchInitializations(boolean initAfterLoad) {
		switch (this.batchContainerInitializations) {
		case NO_BATCH_INITIALIZATION:
			this.batchContainerInitializations = NEED_BATCH_INITIALIZATION;
			return true;
		case BATCH_INITIALIZATION_FINISHED:
			if (initAfterLoad)
				return false; // no need to initialize again
			this.batchContainerInitializations = NEED_BATCH_INITIALIZATION;
			return true;
		}
		return false;
	}

	private synchronized boolean batchContainerInitializations() {
		switch (this.batchContainerInitializations) {
		case NEED_BATCH_INITIALIZATION:
			this.batchContainerInitializations = BATCH_INITIALIZATION_IN_PROGRESS;
			return true;
		case BATCH_INITIALIZATION_IN_PROGRESS:
			return true;
		}
		return false;
	}

	private synchronized void batchInitializationFinished() {
		this.batchContainerInitializations = BATCH_INITIALIZATION_FINISHED;
	}

	public IClasspathContainer getClasspathContainer(final IPath containerPath, final IJavaProject project) throws JavaModelException {

		IClasspathContainer container = containerGet(project, containerPath);

		if (container == null) {
			if (batchContainerInitializations()) {
				// avoid deep recursion while initializing container on workspace restart
				// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=60437)
				try {
					container = initializeAllContainers(project, containerPath);
				} finally {
					batchInitializationFinished();
				}
			} else {
				container = initializeContainer(project, containerPath);
				containerBeingInitializedRemove(project, containerPath);
				SetContainerOperation operation = new SetContainerOperation(containerPath, new IJavaProject[] {project}, new IClasspathContainer[] {container});
				operation.runOperation(null);
			}
		}
		return container;
	}

	public IClasspathEntry[] getReferencedClasspathEntries(IClasspathEntry libraryEntry, IJavaProject project) {

		IClasspathEntry[] referencedEntries = ((ClasspathEntry)libraryEntry).resolvedChainedLibraries();

		if (project == null)
			return referencedEntries;

		PerProjectInfo perProjectInfo = getPerProjectInfo(project.getProject(), false);
		if(perProjectInfo == null)
			return referencedEntries;

		LinkedHashSet<IPath> pathToReferencedEntries = new LinkedHashSet<>(referencedEntries.length);
		for (int index = 0; index < referencedEntries.length; index++) {

			if (pathToReferencedEntries.contains(referencedEntries[index].getPath()))
				continue;

			IClasspathEntry persistedEntry = null;
			if ((persistedEntry = perProjectInfo.rootPathToResolvedEntries.get(referencedEntries[index].getPath())) != null) {
				// TODO: reconsider this - may want to copy the values instead of reference assignment?
				referencedEntries[index] = persistedEntry;
			}
			pathToReferencedEntries.add(referencedEntries[index].getPath());
		}
		return referencedEntries;
	}

	public DeltaProcessor getDeltaProcessor() {
		return this.deltaState.getDeltaProcessor();
	}

	public static DeltaProcessingState getDeltaState() {
		return MANAGER.deltaState;
	}

	/**
	 * Returns the set of elements which are out of synch with their buffers.
	 */
	protected HashSet<Openable> getElementsOutOfSynchWithBuffers() {
		return this.elementsOutOfSynchWithBuffers;
	}

	public static ExternalFoldersManager getExternalManager() {
		return MANAGER.externalFoldersManager;
	}

	public static IndexManager getIndexManager() {
		return MANAGER.indexManager;
	}

	/**
	 *  Returns the info for the element.
	 */
	public synchronized Object getInfo(IJavaElement element) {
		HashMap<IJavaElement, Object> tempCache = this.temporaryCache.get();
		if (tempCache != null) {
			Object result = tempCache.get(element);
			if (result != null) {
				return result;
			}
		}
		return this.cache.getInfo(element);
	}

	/**
	 *  Returns the existing element in the cache that is equal to the given element.
	 */
	public synchronized IJavaElement getExistingElement(IJavaElement element) {
		return this.cache.getExistingElement(element);
	}

	public HashSet<IJavaProject> getExternalWorkingCopyProjects() {
		synchronized (this.perWorkingCopyInfos) {
			HashSet<IJavaProject> result = null;
			Iterator<Map<CompilationUnit, PerWorkingCopyInfo>> values = this.perWorkingCopyInfos.values().iterator();
			while (values.hasNext()) {
				Map<CompilationUnit, PerWorkingCopyInfo> ownerCopies = values.next();
				Iterator<CompilationUnit> workingCopies = ownerCopies.keySet().iterator();
				while (workingCopies.hasNext()) {
					ICompilationUnit workingCopy = workingCopies.next();
					IJavaProject project = workingCopy.getJavaProject();
					if (project.getElementName().equals(ExternalJavaProject.EXTERNAL_PROJECT_NAME)) {
						if (result == null)
							result = new HashSet<>();
						result.add(project);
					}
				}
			}
			return result;
		}
	}

	/**
	 * Get workspace eclipse preference for JavaCore plug-in.
	 */
	public IEclipsePreferences getInstancePreferences() {
		return this.preferencesLookup[PREF_INSTANCE];
	}

	// If modified, also modify the method getDefaultOptionsNoInitialization()
	public Hashtable<String, String> getDefaultOptions(){

		Hashtable<String, String> defaultOptions = new Hashtable<>(10);

		// see JavaCorePreferenceInitializer#initializeDefaultPluginPreferences() for changing default settings
		// If modified, also modify the method getDefaultOptionsNoInitialization()
		IEclipsePreferences defaultPreferences = getDefaultPreferences();

		// initialize preferences to their default
		Iterator<String> iterator = this.optionNames.iterator();
		while (iterator.hasNext()) {
		    String propertyName = iterator.next();
		    String value = defaultPreferences.get(propertyName, null);
		    if (value != null) defaultOptions.put(propertyName, value);
		}
		// get encoding through resource plugin
		defaultOptions.put(JavaCore.CORE_ENCODING, JavaCore.getEncoding());
		// backward compatibility
		addDeprecatedOptions(defaultOptions);

		return defaultOptions;
	}

	/**
	 * Get default eclipse preference for JavaCore plugin.
	 */
	public IEclipsePreferences getDefaultPreferences() {
		return this.preferencesLookup[PREF_DEFAULT];
	}

	/**
	 * Returns the handle to the active Java Model.
	 */
	public final JavaModel getJavaModel() {
		return this.javaModel;
	}

	/**
	 * Returns the singleton JavaModelManager
	 */
	public final static JavaModelManager getJavaModelManager() {
		return MANAGER;
	}

	/**
	 * Returns the last built state for the given project, or null if there is none.
	 * Deserializes the state if necessary.
	 *
	 * For use by image builder and evaluation support only
	 */
	public Object getLastBuiltState(IProject project, IProgressMonitor monitor) {
		if (!JavaProject.hasJavaNature(project)) {
			if (JavaBuilder.DEBUG)
				System.out.println(project + " is not a Java project"); //$NON-NLS-1$
			return null; // should never be requested on non-Java projects
		}
		PerProjectInfo info = getPerProjectInfo(project, true/*create if missing*/);
		if (!info.triedRead) {
			info.triedRead = true;
			try {
				if (monitor != null)
					monitor.subTask(Messages.bind(Messages.build_readStateProgress, project.getName()));
				info.savedState = readState(project);
			} catch (CoreException e) {
				Util.log(e, "Exception while reading last build state for: " + project); //$NON-NLS-1$
			}
		}
		return info.savedState;
	}

	public String getOption(String optionName) {

		if (JavaCore.CORE_ENCODING.equals(optionName)){
			return JavaCore.getEncoding();
		}
		// backward compatibility
		if (isDeprecatedOption(optionName)) {
			return JavaCore.ERROR;
		}
		int optionLevel = getOptionLevel(optionName);
		if (optionLevel != UNKNOWN_OPTION){
			IPreferencesService service = Platform.getPreferencesService();
			String value = service.get(optionName, null, this.preferencesLookup);
			if (value == null && optionLevel == DEPRECATED_OPTION) {
				// May be a deprecated option, retrieve the new value in compatible options
				String[] compatibleOptions = this.deprecatedOptions.get(optionName);
				value = service.get(compatibleOptions[0], null, this.preferencesLookup);
			}
			return value==null ? null : value.trim();
		}
		return null;
	}

	/**
	 * Returns the value of the given option for the given Eclipse preferences.
	 * If no value was already set, then inherits from the global options if specified.
	 *
	 * @param optionName The name of the option
	 * @param inheritJavaCoreOptions Tells whether the value can be inherited from global JavaCore options
	 * @param projectPreferences The eclipse preferences from which to get the value
	 * @return The value of the option. May be <code>null</code>
	 */
	public String getOption(String optionName, boolean inheritJavaCoreOptions, IEclipsePreferences projectPreferences) {
		// Return the option value depending on its level
		switch (getOptionLevel(optionName)) {
			case VALID_OPTION:
				// Valid option, return the preference value
				String javaCoreDefault = inheritJavaCoreOptions ? JavaCore.getOption(optionName) : null;
				if (projectPreferences == null) return javaCoreDefault;
				String value = projectPreferences.get(optionName, javaCoreDefault);
				return value == null ? null : value.trim();
			case DEPRECATED_OPTION:
				// Return the deprecated option value if it was already set
				String oldValue = projectPreferences.get(optionName, null);
				if (oldValue != null) {
					return oldValue.trim();
				}
				// Get the new compatible value
				String[] compatibleOptions = this.deprecatedOptions.get(optionName);
				String newDefault = inheritJavaCoreOptions ? JavaCore.getOption(compatibleOptions[0]) : null;
				String newValue = projectPreferences.get(compatibleOptions[0], newDefault);
				return newValue == null ? null : newValue.trim();
		}
		return null;
	}

	/**
	 * Returns whether an option name is known or not.
	 *
	 * @param optionName The name of the option
	 * @return <code>true</code> when the option name is either
	 * {@link #VALID_OPTION valid} or {@link #DEPRECATED_OPTION deprecated},
	 * <code>false</code> otherwise.
	 */
	public boolean knowsOption(String optionName) {
		boolean knownOption = this.optionNames.contains(optionName);
		if (!knownOption) {
			knownOption = this.deprecatedOptions.get(optionName) != null;
		}
		return knownOption;
	}

	/**
	 * Returns the level of the given option.
	 *
	 * @param optionName The name of the option
	 * @return The level of the option as an int which may have the following
	 * values:
	 * <ul>
	 * <li>{@link #UNKNOWN_OPTION}: the given option is unknown</li>
	 * <li>{@link #DEPRECATED_OPTION}: the given option is deprecated</li>
	 * <li>{@link #VALID_OPTION}: the given option is valid</li>
	 * </ul>
	 */
	public int getOptionLevel(String optionName) {
		if (this.optionNames.contains(optionName)) {
			return VALID_OPTION;
		}
		if (this.deprecatedOptions.get(optionName) != null) {
			return DEPRECATED_OPTION;
		}
		return UNKNOWN_OPTION;
	}

	public Hashtable<String, String> getOptions() {

		// return cached options if already computed
		Hashtable<String, String> cachedOptions; // use a local variable to avoid race condition (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=256329 )
		if ((cachedOptions = this.optionsCache) != null) {
			return new Hashtable<>(cachedOptions);
		}
		if (!Platform.isRunning()) {
			this.optionsCache = getDefaultOptionsNoInitialization();
			return new Hashtable<>(this.optionsCache);
		}
		// init
		Hashtable<String, String> options = new Hashtable<>(10);
		IPreferencesService service = Platform.getPreferencesService();

		// set options using preferences service lookup
		Iterator<String> iterator = this.optionNames.iterator();
		while (iterator.hasNext()) {
			String propertyName = iterator.next();
			String propertyValue = service.get(propertyName, null, this.preferencesLookup);
			if (propertyValue != null) {
				options.put(propertyName, propertyValue);
			}
		}

		// set deprecated options using preferences service lookup
		Iterator<Entry<String, String[]>> deprecatedEntries = this.deprecatedOptions.entrySet().iterator();
		while (deprecatedEntries.hasNext()) {
			Entry<String, String[]> entry = deprecatedEntries.next();
			String propertyName = entry.getKey();
			String propertyValue = service.get(propertyName, null, this.preferencesLookup);
			if (propertyValue != null) {
				options.put(propertyName, propertyValue);
				String[] compatibleOptions = entry.getValue();
				for (int co=0, length=compatibleOptions.length; co < length; co++) {
					String compatibleOption = compatibleOptions[co];
					if (!options.containsKey(compatibleOption))
						options.put(compatibleOption, propertyValue);
				}
			}
		}

		// get encoding through resource plugin
		options.put(JavaCore.CORE_ENCODING, JavaCore.getEncoding());

		// backward compatibility
		addDeprecatedOptions(options);

		Util.fixTaskTags(options);
		// store built map in cache
		this.optionsCache = new Hashtable<>(options);
		// return built map
		return options;
	}

	// Do not modify without modifying getDefaultOptions()
	private Hashtable<String, String> getDefaultOptionsNoInitialization() {
		Map<String, String> defaultOptionsMap = new CompilerOptions().getMap(); // compiler defaults

		// Override some compiler defaults
		defaultOptionsMap.put(JavaCore.COMPILER_LOCAL_VARIABLE_ATTR, JavaCore.GENERATE);
		defaultOptionsMap.put(JavaCore.COMPILER_CODEGEN_UNUSED_LOCAL, JavaCore.PRESERVE);
		defaultOptionsMap.put(JavaCore.COMPILER_TASK_TAGS, JavaCore.DEFAULT_TASK_TAGS);
		defaultOptionsMap.put(JavaCore.COMPILER_TASK_PRIORITIES, JavaCore.DEFAULT_TASK_PRIORITIES);
		defaultOptionsMap.put(JavaCore.COMPILER_TASK_CASE_SENSITIVE, JavaCore.ENABLED);
		defaultOptionsMap.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
		defaultOptionsMap.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, JavaCore.ERROR);

		// Builder settings
		defaultOptionsMap.put(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, JavaCore.ABORT);
		defaultOptionsMap.put(JavaCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE, JavaCore.WARNING);
		defaultOptionsMap.put(JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER, JavaCore.CLEAN);
		defaultOptionsMap.put(JavaCore.CORE_JAVA_BUILD_EXTERNAL_ANNOTATIONS_FROM_ALL_LOCATIONS, JavaCore.DISABLED);

		// JavaCore settings
		defaultOptionsMap.put(JavaCore.CORE_JAVA_BUILD_ORDER, JavaCore.IGNORE);
		defaultOptionsMap.put(JavaCore.CORE_INCOMPLETE_CLASSPATH, JavaCore.ERROR);
		defaultOptionsMap.put(JavaCore.CORE_CIRCULAR_CLASSPATH, JavaCore.ERROR);
		defaultOptionsMap.put(JavaCore.CORE_INCOMPATIBLE_JDK_LEVEL, JavaCore.IGNORE);
		defaultOptionsMap.put(JavaCore.CORE_MAIN_ONLY_PROJECT_HAS_TEST_ONLY_DEPENDENCY, JavaCore.ERROR);
		defaultOptionsMap.put(JavaCore.CORE_OUTPUT_LOCATION_OVERLAPPING_ANOTHER_SOURCE, JavaCore.ERROR);
		defaultOptionsMap.put(JavaCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS, JavaCore.ENABLED);
		defaultOptionsMap.put(JavaCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS, JavaCore.ENABLED);

		// Formatter settings
		defaultOptionsMap.putAll(DefaultCodeFormatterConstants.getEclipseDefaultSettings());

		// CodeAssist settings
		defaultOptionsMap.put(JavaCore.CODEASSIST_VISIBILITY_CHECK, JavaCore.DISABLED);
		defaultOptionsMap.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.DISABLED);
		defaultOptionsMap.put(JavaCore.CODEASSIST_IMPLICIT_QUALIFICATION, JavaCore.DISABLED);
		defaultOptionsMap.put(JavaCore.CODEASSIST_FIELD_PREFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_STATIC_FINAL_FIELD_PREFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_LOCAL_PREFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_ARGUMENT_PREFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_FIELD_SUFFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_STATIC_FINAL_FIELD_SUFFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_ARGUMENT_SUFFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaCore.CODEASSIST_FORBIDDEN_REFERENCE_CHECK, JavaCore.ENABLED);
		defaultOptionsMap.put(JavaCore.CODEASSIST_DISCOURAGED_REFERENCE_CHECK, JavaCore.DISABLED);
		defaultOptionsMap.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		defaultOptionsMap.put(JavaCore.CODEASSIST_SUBWORD_MATCH, JavaCore.ENABLED);
		defaultOptionsMap.put(JavaCore.CODEASSIST_SUGGEST_STATIC_IMPORTS, JavaCore.ENABLED);

		// Time out for parameter names
		defaultOptionsMap.put(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC, "50"); //$NON-NLS-1$

		return new Hashtable<>(defaultOptionsMap);
	}

	/*
	 * Returns the per-project info for the given project. If specified, create the info if the info doesn't exist.
	 */
	public PerProjectInfo getPerProjectInfo(IProject project, boolean create) {
		synchronized(this.perProjectInfos) { // use the perProjectInfo collection as its own lock
			PerProjectInfo info= this.perProjectInfos.get(project);
			if (info == null && create) {
				info= new PerProjectInfo(project);
				this.perProjectInfos.put(project, info);
			}
			return info;
		}
	}

	/*
	 * Returns  the per-project info for the given project.
	 * If the info doesn't exist, check for the project existence and create the info.
	 * @throws JavaModelException if the project doesn't exist.
	 */
	public PerProjectInfo getPerProjectInfoCheckExistence(IProject project) throws JavaModelException {
		JavaModelManager.PerProjectInfo info = getPerProjectInfo(project, false /* don't create info */);
		if (info == null) {
			if (!JavaProject.hasJavaNature(project)) {
				throw ((JavaProject)JavaCore.create(project)).newNotPresentException();
			}
			info = getPerProjectInfo(project, true /* create info */);
		}
		return info;
	}

	/*
	 * Returns the per-working copy info for the given working copy at the given path.
	 * If it doesn't exist and if create, add a new per-working copy info with the given problem requestor.
	 * If recordUsage, increment the per-working copy info's use count.
	 * Returns null if it doesn't exist and not create.
	 */
	public PerWorkingCopyInfo getPerWorkingCopyInfo(CompilationUnit workingCopy,boolean create, boolean recordUsage, IProblemRequestor problemRequestor) {
		synchronized(this.perWorkingCopyInfos) { // use the perWorkingCopyInfo collection as its own lock
			WorkingCopyOwner owner = workingCopy.owner;
			Map<CompilationUnit, PerWorkingCopyInfo> workingCopyToInfos = this.perWorkingCopyInfos.get(owner);
			if (workingCopyToInfos == null && create) {
				workingCopyToInfos = new HashMap<>();
				this.perWorkingCopyInfos.put(owner, workingCopyToInfos);
			}

			PerWorkingCopyInfo info = workingCopyToInfos == null ? null : workingCopyToInfos.get(workingCopy);
			if (info == null && create) {
				info= new PerWorkingCopyInfo(workingCopy, problemRequestor);
				workingCopyToInfos.put(workingCopy, info);
			}
			if (info != null && recordUsage) info.useCount++;
			return info;
		}
	}

	/**
	 * Returns a persisted container from previous session if any. Note that it is not the original container from previous
	 * session (i.e. it did not get serialized) but rather a summary of its entries recreated for CP initialization purpose.
	 * As such it should not be stored into container caches.
	 */
	public IClasspathContainer getPreviousSessionContainer(IPath containerPath, IJavaProject project) {
			Map<IPath, IClasspathContainer> previousContainerValues = this.previousSessionContainers.get(project);
			if (previousContainerValues != null){
			    IClasspathContainer previousContainer = previousContainerValues.get(containerPath);
			    if (previousContainer != null) {
					if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
						verbose_reentering_project_container_access(containerPath, project, previousContainer);
					return previousContainer;
			    }
			}
		    return null; // break cycle if none found
	}

	private void verbose_reentering_project_container_access(	IPath containerPath, IJavaProject project, IClasspathContainer previousContainer) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("CPContainer INIT - reentering access to project container during its initialization, will see previous value\n"); //$NON-NLS-1$
		buffer.append("	project: " + project.getElementName() + '\n'); //$NON-NLS-1$
		buffer.append("	container path: " + containerPath + '\n'); //$NON-NLS-1$
		buffer.append("	previous value: "); //$NON-NLS-1$
		buffer.append(previousContainer.getDescription());
		buffer.append(" {\n"); //$NON-NLS-1$
		IClasspathEntry[] entries = previousContainer.getClasspathEntries();
		if (entries != null){
			for (int j = 0; j < entries.length; j++){
				buffer.append(" 		"); //$NON-NLS-1$
				buffer.append(entries[j]);
				buffer.append('\n');
			}
		}
		buffer.append(" 	}"); //$NON-NLS-1$
		Util.verbose(buffer.toString());
		new Exception("<Fake exception>").printStackTrace(System.out); //$NON-NLS-1$
	}

	/**
	 * Returns a persisted container from previous session if any
	 */
	public IPath getPreviousSessionVariable(String variableName) {
		IPath previousPath = this.previousSessionVariables.get(variableName);
		if (previousPath != null){
			if (CP_RESOLVE_VERBOSE_ADVANCED)
				verbose_reentering_variable_access(variableName, previousPath);
			return previousPath;
		}
	    return null; // break cycle
	}

	private void verbose_reentering_variable_access(String variableName, IPath previousPath) {
		Util.verbose(
			"CPVariable INIT - reentering access to variable during its initialization, will see previous value\n" + //$NON-NLS-1$
			"	variable: "+ variableName + '\n' + //$NON-NLS-1$
			"	previous value: " + previousPath); //$NON-NLS-1$
		new Exception("<Fake exception>").printStackTrace(System.out); //$NON-NLS-1$
	}

	/**
	 * Returns the temporary cache for newly opened elements for the current thread.
	 * Creates it if not already created.
	 */
	public HashMap<IJavaElement, Object> getTemporaryCache() {
		HashMap<IJavaElement, Object> result = this.temporaryCache.get();
		if (result == null) {
			result = new HashMap<>();
			this.temporaryCache.set(result);
		}
		return result;
	}

	private File getVariableAndContainersFile() {
		return JavaCore.getPlugin().getStateLocation().append("variablesAndContainers.dat").toFile(); //$NON-NLS-1$
	}

	/**
 	 * Returns the name of the variables for which an CP variable initializer is registered through an extension point
 	 */
	public static String[] getRegisteredVariableNames(){

		Plugin jdtCorePlugin = JavaCore.getPlugin();
		if (jdtCorePlugin == null) return null;

		ArrayList<String> variableList = new ArrayList<>(5);
		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(JavaCore.PLUGIN_ID, JavaModelManager.CPVARIABLE_INITIALIZER_EXTPOINT_ID);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for(int i = 0; i < extensions.length; i++){
				IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
				for(int j = 0; j < configElements.length; j++){
					String varAttribute = configElements[j].getAttribute("variable"); //$NON-NLS-1$
					if (varAttribute != null) variableList.add(varAttribute);
				}
			}
		}
		String[] variableNames = new String[variableList.size()];
		variableList.toArray(variableNames);
		return variableNames;
	}

	/**
 	 * Returns the name of the container IDs for which an CP container initializer is registered through an extension point
 	 */
	public static String[] getRegisteredContainerIDs(){

		Plugin jdtCorePlugin = JavaCore.getPlugin();
		if (jdtCorePlugin == null) return null;

		ArrayList<String> containerIDList = new ArrayList<>(5);
		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(JavaCore.PLUGIN_ID, JavaModelManager.CPCONTAINER_INITIALIZER_EXTPOINT_ID);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for(int i = 0; i < extensions.length; i++){
				IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
				for(int j = 0; j < configElements.length; j++){
					String idAttribute = configElements[j].getAttribute("id"); //$NON-NLS-1$
					if (idAttribute != null) containerIDList.add(idAttribute);
				}
			}
		}
		String[] containerIDs = new String[containerIDList.size()];
		containerIDList.toArray(containerIDs);
		return containerIDs;
	}

	public IClasspathEntry resolveVariableEntry(IClasspathEntry entry, boolean usePreviousSession) {

		if (entry.getEntryKind() != IClasspathEntry.CPE_VARIABLE)
			return entry;

		IPath resolvedPath = getResolvedVariablePath(entry.getPath(), usePreviousSession);
		if (resolvedPath == null)
			return null;
		// By passing a null reference path, we keep it relative to workspace root.
		resolvedPath = ClasspathEntry.resolveDotDot(null, resolvedPath);

		Object target = JavaModel.getTarget(resolvedPath, false);
		if (target == null)
			return null;

		// inside the workspace
		if (target instanceof IResource) {
			IResource resolvedResource = (IResource) target;
			switch (resolvedResource.getType()) {

				case IResource.PROJECT :
					// internal project
					return JavaCore.newProjectEntry(
							resolvedPath,
							entry.getAccessRules(),
							entry.combineAccessRules(),
							entry.getExtraAttributes(),
							entry.isExported());
				case IResource.FILE :
					// internal binary archive
					return JavaCore.newLibraryEntry(
							resolvedPath,
							getResolvedVariablePath(entry.getSourceAttachmentPath(), usePreviousSession),
							getResolvedVariablePath(entry.getSourceAttachmentRootPath(), usePreviousSession),
							entry.getAccessRules(),
							entry.getExtraAttributes(),
							entry.isExported());
				case IResource.FOLDER :
					// internal binary folder
					return JavaCore.newLibraryEntry(
							resolvedPath,
							getResolvedVariablePath(entry.getSourceAttachmentPath(), usePreviousSession),
							getResolvedVariablePath(entry.getSourceAttachmentRootPath(), usePreviousSession),
							entry.getAccessRules(),
							entry.getExtraAttributes(),
							entry.isExported());
			}
		}
		if (target instanceof File) {
			File externalFile = JavaModel.getFile(target);
			if (externalFile != null) {
				// external binary archive
				return JavaCore.newLibraryEntry(
						resolvedPath,
						getResolvedVariablePath(entry.getSourceAttachmentPath(), usePreviousSession),
						getResolvedVariablePath(entry.getSourceAttachmentRootPath(), usePreviousSession),
						entry.getAccessRules(),
						entry.getExtraAttributes(),
						entry.isExported());
			} else {
				// non-existing file
				if (resolvedPath.isAbsolute()){
					return JavaCore.newLibraryEntry(
							resolvedPath,
							getResolvedVariablePath(entry.getSourceAttachmentPath(), usePreviousSession),
							getResolvedVariablePath(entry.getSourceAttachmentRootPath(), usePreviousSession),
							entry.getAccessRules(),
							entry.getExtraAttributes(),
							entry.isExported());
				}
			}
		}
		return null;
	}

	public IPath getResolvedVariablePath(IPath variablePath, boolean usePreviousSession) {

		if (variablePath == null)
			return null;
		int count = variablePath.segmentCount();
		if (count == 0)
			return null;

		// lookup variable
		String variableName = variablePath.segment(0);
		IPath resolvedPath = usePreviousSession ? getPreviousSessionVariable(variableName) : JavaCore.getClasspathVariable(variableName);
		if (resolvedPath == null)
			return null;

		// append path suffix
		if (count > 1) {
			resolvedPath = resolvedPath.append(variablePath.removeFirstSegments(1));
		}
		return resolvedPath;
	}

	/**
	 * Returns the File to use for saving and restoring the last built state for the given project.
	 */
	private File getSerializationFile(IProject project) {
		if (!project.exists()) return null;
		IPath workingLocation = project.getWorkingLocation(JavaCore.PLUGIN_ID);
		return workingLocation.append("state.dat").toFile(); //$NON-NLS-1$
	}

	public static UserLibraryManager getUserLibraryManager() {
		if (MANAGER.userLibraryManager == null) {
			UserLibraryManager libraryManager = new UserLibraryManager();
			synchronized(MANAGER) {
				if (MANAGER.userLibraryManager == null) { // ensure another library manager was not set while creating the instance above
					MANAGER.userLibraryManager = libraryManager;
				}
			}
		}
		return MANAGER.userLibraryManager;
	}

	public static ModuleSourcePathManager getModulePathManager() {
		if (MANAGER.modulePathManager == null) {
			ModuleSourcePathManager modulePathManager = new ModuleSourcePathManager();
			synchronized(MANAGER) {
				if (MANAGER.modulePathManager == null) { // ensure another library manager was not set while creating the instance above
					MANAGER.modulePathManager = modulePathManager;
				}
			}
		}
		return MANAGER.modulePathManager;
	}
	/*
	 * Returns all the working copies which have the given owner.
	 * Adds the working copies of the primary owner if specified.
	 * Returns null if it has none.
	 */
	public ICompilationUnit[] getWorkingCopies(WorkingCopyOwner owner, boolean addPrimary) {
		synchronized(this.perWorkingCopyInfos) {
			ICompilationUnit[] primaryWCs = addPrimary && owner != DefaultWorkingCopyOwner.PRIMARY
				? getWorkingCopies(DefaultWorkingCopyOwner.PRIMARY, false)
				: null;
			Map<CompilationUnit, PerWorkingCopyInfo> workingCopyToInfos = this.perWorkingCopyInfos.get(owner);
			if (workingCopyToInfos == null) return primaryWCs;
			int primaryLength = primaryWCs == null ? 0 : primaryWCs.length;
			int size = workingCopyToInfos.size(); // note size is > 0 otherwise pathToPerWorkingCopyInfos would be null
			ICompilationUnit[] result = new ICompilationUnit[primaryLength + size];
			int index = 0;
			if (primaryWCs != null) {
				for (int i = 0; i < primaryLength; i++) {
					ICompilationUnit primaryWorkingCopy = primaryWCs[i];
					ICompilationUnit workingCopy = new CompilationUnit((PackageFragment) primaryWorkingCopy.getParent(), primaryWorkingCopy.getElementName(), owner);
					if (!workingCopyToInfos.containsKey(workingCopy))
						result[index++] = primaryWorkingCopy;
				}
				if (index != primaryLength)
					System.arraycopy(result, 0, result = new ICompilationUnit[index+size], 0, index);
			}
			Iterator<PerWorkingCopyInfo> iterator = workingCopyToInfos.values().iterator();
			while(iterator.hasNext()) {
				result[index++] = iterator.next().getWorkingCopy();
			}
			return result;
		}
	}

	public JavaWorkspaceScope getWorkspaceScope() {
		if (this.workspaceScope == null) {
			this.workspaceScope = new JavaWorkspaceScope();
		}
		return this.workspaceScope;
	}

	public static boolean isJrt(IPath path) {
		return path.toString().endsWith(JRTUtil.JRT_FS_JAR);
	}

	public static boolean isJrt(String path) {
		return isJrt(new Path(path));
	}

	public void verifyArchiveContent(IPath path) throws CoreException {
		// TODO: we haven't finalized what path the JRT is represented by. Don't attempt to validate it.
		if (isJrt(path)) {
			return;
		}
		if (isArchiveStateKnownToBeValid(path)) {
			return; // known to be valid
		}
		ZipFile file = getZipFile(path);
		closeZipFile(file);
	}

	/**
	 * Returns the open ZipFile at the given path. If the ZipFile
	 * does not yet exist, it is created, opened, and added to the cache
	 * of open ZipFiles.
	 *
	 * The path must be a file system path if representing an external
	 * zip/jar, or it must be an absolute workspace relative path if
	 * representing a zip/jar inside the workspace.
	 *
	 * @exception CoreException If unable to create/open the ZipFile. The
	 * cause will be a {@link ZipException} if the file was corrupt, a
	 * {@link FileNotFoundException} if the file does not exist, or a
	 * {@link IOException} if we were unable to read the file.
	 */
	public ZipFile getZipFile(IPath path) throws CoreException {
		return getZipFile(path, true);
	}

	/**
	 * For use in the JDT unit tests only. Used for testing error handling. Causes an
	 * {@link IOException} to be thrown in {@link #getZipFile} whenever it attempts to
	 * read a zip file.
	 *
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public static boolean throwIoExceptionsInGetZipFile = false;

	public ZipFile getZipFile(IPath path, boolean checkInvalidArchiveCache) throws CoreException {
		if (checkInvalidArchiveCache) {
			isArchiveStateKnownToBeValid(path);
		}
		ZipCache zipCache;
		ZipFile zipFile;
		if ((zipCache = this.zipFiles.get()) != null
				&& (zipFile = zipCache.getCache(path)) != null) {
			return zipFile;
		}
		File localFile = getLocalFile(path);

		try {
			if (ZIP_ACCESS_VERBOSE) {
				System.out.println("(" + Thread.currentThread() + ") [JavaModelManager.getZipFile(IPath)] Creating ZipFile on " + localFile ); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (throwIoExceptionsInGetZipFile) {
				throw new IOException();
			}
			zipFile = new ZipFile(localFile);
			if (zipCache != null) {
				zipCache.setCache(path, zipFile);
			}
			addInvalidArchive(path, ArchiveValidity.VALID); // remember its valid
			return zipFile;
		} catch (IOException e) {
			// file may exist but for some reason is inaccessible
			ArchiveValidity reason=ArchiveValidity.INVALID;
			addInvalidArchive(path, reason);
			throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, -1, Messages.status_IOException, e));
		}
	}

	public static File getLocalFile(IPath path) throws CoreException {
		File localFile = null;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource file = root.findMember(path);
		if (file != null) {
			// internal resource
			URI location;
			if (file.getType() != IResource.FILE || (location = file.getLocationURI()) == null) {
				throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, -1, Messages.bind(Messages.file_notFound, path.toString()), null));
			}
			localFile = Util.toLocalFile(location, null/*no progress availaible*/);
			if (localFile == null)
				throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, -1, Messages.bind(Messages.file_notFound, path.toString()), null));
		} else {
			// external resource -> it is ok to use toFile()
			localFile= path.toFile();
		}
		return localFile;
	}

	private boolean isArchiveStateKnownToBeValid(IPath path) throws CoreException {
		ArchiveValidity validity = getArchiveValidity(path);
		if (validity == null || validity == ArchiveValidity.INVALID) {
			return false; // chance the file has become accessible/readable now.
		}
		return true;
	}

	/*
	 * Returns whether there is a temporary cache for the current thread.
	 */
	public boolean hasTemporaryCache() {
		return this.temporaryCache.get() != null;
	}

	/*
	 * Initialize all container at the same time as the given container.
	 * Return the container for the given path and project.
	 */
	private IClasspathContainer initializeAllContainers(IJavaProject javaProjectToInit, IPath containerToInit) throws JavaModelException {
		if (CP_RESOLVE_VERBOSE_ADVANCED)
			verbose_batching_containers_initialization(javaProjectToInit, containerToInit);

		// collect all container paths
		final HashMap<IJavaProject, Set<IPath>> allContainerPaths = new HashMap<>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0, length = projects.length; i < length; i++) {
			IProject project = projects[i];
			if (!JavaProject.hasJavaNature(project)) continue;
			IJavaProject javaProject = new JavaProject(project, getJavaModel());
			Set<IPath> paths = allContainerPaths.get(javaProject);
			IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
			for (int j = 0, length2 = rawClasspath.length; j < length2; j++) {
				IClasspathEntry entry = rawClasspath[j];
				IPath path = entry.getPath();
				if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
						&& containerGet(javaProject, path) == null) {
					if (paths == null) {
						paths = new HashSet<>();
						allContainerPaths.put(javaProject, paths);
					}
					paths.add(path);
				}
			}
			/* TODO (frederic) put back when JDT/UI dummy project will be thrown away...
			 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=97524
			 *
			if (javaProject.equals(javaProjectToInit)) {
				if (paths == null) {
					paths = new HashSet();
					allContainerPaths.put(javaProject, paths);
				}
				paths.add(containerToInit);
			}
			*/
		}
		// TODO (frederic) remove following block when JDT/UI dummy project will be thrown away...
		if (javaProjectToInit != null) {
			Set<IPath> containerPaths = allContainerPaths.get(javaProjectToInit);
			if (containerPaths == null) {
				containerPaths = new HashSet<>();
				allContainerPaths.put(javaProjectToInit, containerPaths);
			}
			containerPaths.add(containerToInit);
		}
		// end block

		// initialize all containers
		boolean ok = false;
		try {
			// if possible run inside an IWokspaceRunnable with AVOID_UPATE to avoid unwanted builds
			// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=118507)
			IWorkspaceRunnable runnable =
				new IWorkspaceRunnable() {
					@Override
					public void run(IProgressMonitor monitor) throws CoreException {
						try {
							// Collect all containers
							Set<Entry<IJavaProject, Set<IPath>>> entrySet = allContainerPaths.entrySet();
							int length = entrySet.size();
							if (monitor != null)
								monitor.beginTask("", length); //$NON-NLS-1$
							Set<Entry<IJavaProject, Set<IPath>>> entries = new HashSet<>(entrySet);  // clone as the following will have a side effect
							for (Entry<IJavaProject, Set<IPath>> entry : entries) {
								IJavaProject javaProject = entry.getKey();
								Set<IPath> pathSet = entry.getValue();
								if (pathSet == null) continue;
								int length2 = pathSet.size();
								IPath[] paths = new IPath[length2];
								pathSet.toArray(paths); // clone as the following will have a side effect
								for (int j = 0; j < length2; j++) {
									IPath path = paths[j];
									synchronized(JavaModelManager.this.batchContainerInitializationsLock) {
										if (containerIsSet(javaProject, path)) {
											// another thread has concurrently initialized the container.
											continue;
										}
									}
									initializeContainer(javaProject, path);
									IClasspathContainer container = containerBeingInitializedGet(javaProject, path);
									if (container != null) {
										synchronized(JavaModelManager.this.batchContainerInitializationsLock) {
											if (containerIsSet(javaProject, path)) {
												// another thread has concurrently initialized the container.
												containerBeingInitializedRemove(javaProject, path);
												containerRemoveInitializationInProgress(javaProject, path);
											} else {
												containerPut(javaProject, path, container);
											}
										}
									}
								}
								if (monitor != null)
									monitor.worked(1);
							}

							// Set all containers
							Map<IJavaProject, Map<IPath, IClasspathContainer>> perProjectContainers = JavaModelManager.this.containersBeingInitialized.get();
							// Note that during the operation below new containers could be added to the map,
							// so we should loop until containersBeingInitialized will be empty
							while (perProjectContainers != null && !perProjectContainers.isEmpty()) {
								initKnownContainers(perProjectContainers, monitor);
							}
							JavaModelManager.this.containersBeingInitialized.remove();
						} finally {
							if (monitor != null)
								monitor.done();
						}
					}

					private void initKnownContainers(Map<IJavaProject, Map<IPath, IClasspathContainer>> perProjectContainers, IProgressMonitor monitor)
							throws JavaModelException {
						Iterator<Entry<IJavaProject, Map<IPath, IClasspathContainer>>> entriesIterator = perProjectContainers.entrySet().iterator();
						List<SetContainerOperation> operations = new ArrayList<>();
						while (entriesIterator.hasNext()) {
							Entry<IJavaProject, Map<IPath, IClasspathContainer>> entry = entriesIterator.next();
							IJavaProject project = entry.getKey();
							Map<IPath, IClasspathContainer> perPathContainers = entry.getValue();
							Iterator<Entry<IPath, IClasspathContainer>> containersIterator = perPathContainers.entrySet().iterator();
							while (containersIterator.hasNext()) {
								Entry<IPath, IClasspathContainer> containerEntry = containersIterator.next();
								IPath containerPath = containerEntry.getKey();
								IClasspathContainer container = containerEntry.getValue();
								SetContainerOperation operation = new SetContainerOperation(containerPath, new IJavaProject[] {project}, new IClasspathContainer[] {container});
								operations.add(operation);
							}
						}
						// operation.runOperation() below could put something into the map again
						// so we clear the map to make sure we only see new content there
						perProjectContainers.clear();
						for (SetContainerOperation operation : operations) {
							operation.runOperation(monitor);
						}
					}
				};
			IProgressMonitor monitor = this.batchContainerInitializationsProgress;
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			if (workspace.isTreeLocked())
				runnable.run(monitor);
			else
				workspace.run(
					runnable,
					null/*don't take any lock*/,
					IWorkspace.AVOID_UPDATE,
					monitor);
			ok = true;
		} catch (CoreException e) {
			// ignore
			Util.log(e, "Exception while initializing all containers"); //$NON-NLS-1$
		} finally {
			if (!ok) {
				// if we're being traversed by an exception, ensure that that containers are
				// no longer marked as initialization in progress
				// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=66437)
				this.containerInitializationInProgress.remove();
			}
		}

		return containerGet(javaProjectToInit, containerToInit);
	}

	private void verbose_batching_containers_initialization(IJavaProject javaProjectToInit, IPath containerToInit) {
		Util.verbose(
			"CPContainer INIT - batching containers initialization\n" + //$NON-NLS-1$
			"	project to init: " + (javaProjectToInit == null ? "null" : javaProjectToInit.getElementName()) + '\n' + //$NON-NLS-1$ //$NON-NLS-2$
			"	container path to init: " + containerToInit); //$NON-NLS-1$
	}

	IClasspathContainer initializeContainer(IJavaProject project, IPath containerPath) throws JavaModelException {

		IProgressMonitor monitor = this.batchContainerInitializationsProgress;
		if (monitor != null && monitor.isCanceled())
			throw new OperationCanceledException();

		IClasspathContainer container = null;
		final ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(containerPath.segment(0));
		if (initializer != null){
			if (CP_RESOLVE_VERBOSE)
				verbose_triggering_container_initialization(project, containerPath, initializer);
			if (CP_RESOLVE_VERBOSE_ADVANCED)
				verbose_triggering_container_initialization_invocation_trace();
			PerformanceStats stats = null;
			if(JavaModelManager.PERF_CONTAINER_INITIALIZER) {
				stats = PerformanceStats.getStats(JavaModelManager.CONTAINER_INITIALIZER_PERF, this);
				stats.startRun(containerPath + " of " + project.getPath()); //$NON-NLS-1$
			}
			containerPut(project, containerPath, CONTAINER_INITIALIZATION_IN_PROGRESS); // avoid initialization cycles
			boolean ok = false;
			try {
				if (monitor != null)
					monitor.subTask(Messages.bind(Messages.javamodel_configuring, initializer.getDescription(containerPath, project)));

				// let OperationCanceledException go through
				// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=59363)
				initializer.initialize(containerPath, project);

				if (monitor != null)
					monitor.subTask(""); //$NON-NLS-1$

				// retrieve value (if initialization was successful)
				container = containerBeingInitializedGet(project, containerPath);
				if (container == null && containerGet(project, containerPath) == CONTAINER_INITIALIZATION_IN_PROGRESS) {
					// initializer failed to do its job: redirect to the failure container
					container = initializer.getFailureContainer(containerPath, project);
					if (container == null) {
						if (CP_RESOLVE_VERBOSE || CP_RESOLVE_VERBOSE_FAILURE)
							verbose_container_null_failure_container(project, containerPath, initializer);
						return null; // break cycle
					}
					if (CP_RESOLVE_VERBOSE || CP_RESOLVE_VERBOSE_FAILURE)
						verbose_container_using_failure_container(project, containerPath, initializer);
					containerPut(project, containerPath, container);
				}
				ok = true;
			} catch (CoreException e) {
				if (e instanceof JavaModelException) {
					throw (JavaModelException) e;
				} else {
					throw new JavaModelException(e);
				}
			} catch (RuntimeException | Error e) {
				if (JavaModelManager.CP_RESOLVE_VERBOSE || CP_RESOLVE_VERBOSE_FAILURE)
					e.printStackTrace();
				throw e;
			} finally {
				if(JavaModelManager.PERF_CONTAINER_INITIALIZER) {
					stats.endRun();
				}
				if (!ok) {
					// just remove initialization in progress and keep previous session container so as to avoid a full build
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=92588
					containerRemoveInitializationInProgress(project, containerPath);
					if (CP_RESOLVE_VERBOSE || CP_RESOLVE_VERBOSE_FAILURE)
						verbose_container_initialization_failed(project, containerPath, container, initializer);
				}
			}
			if (CP_RESOLVE_VERBOSE_ADVANCED)
				verbose_container_value_after_initialization(project, containerPath, container);
		} else {
			// create a dummy initializer and get the default failure container
			container = (new ClasspathContainerInitializer() {
				@Override
				public void initialize(IPath path, IJavaProject javaProject) throws CoreException {
					// not used
				}
			}).getFailureContainer(containerPath, project);
			if (CP_RESOLVE_VERBOSE_ADVANCED || CP_RESOLVE_VERBOSE_FAILURE)
				verbose_no_container_initializer_found(project, containerPath);
		}
		return container;
	}

	private void verbose_no_container_initializer_found(IJavaProject project, IPath containerPath) {
		Util.verbose(
			"CPContainer INIT - no initializer found\n" + //$NON-NLS-1$
			"	project: " + project.getElementName() + '\n' + //$NON-NLS-1$
			"	container path: " + containerPath); //$NON-NLS-1$
	}

	private void verbose_container_value_after_initialization(IJavaProject project, IPath containerPath, IClasspathContainer container) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("CPContainer INIT - after resolution\n"); //$NON-NLS-1$
		buffer.append("	project: " + project.getElementName() + '\n'); //$NON-NLS-1$
		buffer.append("	container path: " + containerPath + '\n'); //$NON-NLS-1$
		if (container != null){
			buffer.append("	container: "+container.getDescription()+" {\n"); //$NON-NLS-2$//$NON-NLS-1$
			IClasspathEntry[] entries = container.getClasspathEntries();
			if (entries != null){
				for (int i = 0; i < entries.length; i++) {
					buffer.append("		" + entries[i] + '\n'); //$NON-NLS-1$
				}
			}
			buffer.append("	}");//$NON-NLS-1$
		} else {
			buffer.append("	container: {unbound}");//$NON-NLS-1$
		}
		Util.verbose(buffer.toString());
	}

	private void verbose_container_initialization_failed(IJavaProject project, IPath containerPath, IClasspathContainer container, ClasspathContainerInitializer initializer) {
		if (container == CONTAINER_INITIALIZATION_IN_PROGRESS) {
			Util.verbose(
				"CPContainer INIT - FAILED (initializer did not initialize container)\n" + //$NON-NLS-1$
				"	project: " + project.getElementName() + '\n' + //$NON-NLS-1$
				"	container path: " + containerPath + '\n' + //$NON-NLS-1$
				"	initializer: " + initializer); //$NON-NLS-1$

		} else {
			Util.verbose(
				"CPContainer INIT - FAILED (see exception above)\n" + //$NON-NLS-1$
				"	project: " + project.getElementName() + '\n' + //$NON-NLS-1$
				"	container path: " + containerPath + '\n' + //$NON-NLS-1$
				"	initializer: " + initializer); //$NON-NLS-1$
		}
	}

	private void verbose_container_null_failure_container(IJavaProject project, IPath containerPath,  ClasspathContainerInitializer initializer) {
		Util.verbose(
			"CPContainer INIT - FAILED (and failure container is null)\n" + //$NON-NLS-1$
			"	project: " + project.getElementName() + '\n' + //$NON-NLS-1$
			"	container path: " + containerPath + '\n' + //$NON-NLS-1$
			"	initializer: " + initializer); //$NON-NLS-1$
	}

	private void verbose_container_using_failure_container(IJavaProject project, IPath containerPath,  ClasspathContainerInitializer initializer) {
		Util.verbose(
			"CPContainer INIT - FAILED (using failure container)\n" + //$NON-NLS-1$
			"	project: " + project.getElementName() + '\n' + //$NON-NLS-1$
			"	container path: " + containerPath + '\n' + //$NON-NLS-1$
			"	initializer: " + initializer); //$NON-NLS-1$
	}

	private void verbose_triggering_container_initialization(IJavaProject project, IPath containerPath,  ClasspathContainerInitializer initializer) {
		Util.verbose(
			"CPContainer INIT - triggering initialization\n" + //$NON-NLS-1$
			"	project: " + project.getElementName() + '\n' + //$NON-NLS-1$
			"	container path: " + containerPath + '\n' + //$NON-NLS-1$
			"	initializer: " + initializer); //$NON-NLS-1$
	}

	private void verbose_triggering_container_initialization_invocation_trace() {
		Util.verbose(
			"CPContainer INIT - triggering initialization\n" + //$NON-NLS-1$
			"	invocation trace:"); //$NON-NLS-1$
		new Exception("<Fake exception>").printStackTrace(System.out); //$NON-NLS-1$
	}

	/**
	 * Initialize preferences lookups for JavaCore plug-in.
	 */
	public void initializePreferences() {

		// Create lookups
		this.preferencesLookup[PREF_INSTANCE] = InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
		this.preferencesLookup[PREF_DEFAULT] = DefaultScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);

		// Listen to instance preferences node removal from parent in order to refresh stored one
		this.instanceNodeListener = new IEclipsePreferences.INodeChangeListener() {
			@Override
			public void added(IEclipsePreferences.NodeChangeEvent event) {
				// do nothing
			}
			@Override
			public void removed(IEclipsePreferences.NodeChangeEvent event) {
				if (event.getChild() == JavaModelManager.this.preferencesLookup[PREF_INSTANCE]) {
					JavaModelManager.this.preferencesLookup[PREF_INSTANCE] = InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
					JavaModelManager.this.preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(new EclipsePreferencesListener());
				}
			}
		};
		((IEclipsePreferences) this.preferencesLookup[PREF_INSTANCE].parent()).addNodeChangeListener(this.instanceNodeListener);
		this.preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(this.instancePreferencesListener = new EclipsePreferencesListener());

		// Listen to default preferences node removal from parent in order to refresh stored one
		this.defaultNodeListener = new IEclipsePreferences.INodeChangeListener() {
			@Override
			public void added(IEclipsePreferences.NodeChangeEvent event) {
				// do nothing
			}
			@Override
			public void removed(IEclipsePreferences.NodeChangeEvent event) {
				if (event.getChild() == JavaModelManager.this.preferencesLookup[PREF_DEFAULT]) {
					JavaModelManager.this.preferencesLookup[PREF_DEFAULT] = DefaultScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
				}
			}
		};
		((IEclipsePreferences) this.preferencesLookup[PREF_DEFAULT].parent()).addNodeChangeListener(this.defaultNodeListener);
	}

	public char[] intern(char[] array) {
		return DeduplicationUtil.intern(array);
	}

	public String intern(String s) {
		return DeduplicationUtil.intern(s);
	}

	void touchProjects(final IProject[] projectsToTouch, IProgressMonitor progressMonitor) throws JavaModelException {
		WorkspaceJob touchJob = new WorkspaceJob(Messages.synchronizing_projects_job) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				SubMonitor subMonitor = SubMonitor.convert(monitor, projectsToTouch.length);
				for (IProject iProject : projectsToTouch) {
					if (JavaBuilder.DEBUG) {
						System.out.println("Touching project " + iProject.getName()); //$NON-NLS-1$
					}
					if (iProject.isAccessible()) {
						iProject.touch(subMonitor.split(1));
					}
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_REFRESH == family;
			}
		};
		touchJob.schedule();
	}

	private Set<IJavaProject> getClasspathBeingResolved() {
	    Set<IJavaProject> result = this.classpathsBeingResolved.get();
	    if (result == null) {
	        result = new HashSet<>();
	        this.classpathsBeingResolved.set(result);
	    }
	    return result;
	}

	public boolean isClasspathBeingResolved(IJavaProject project) {
	    return getClasspathBeingResolved().contains(project);
	}

	/**
	 * @deprecated
	 */
	private boolean isDeprecatedOption(String optionName) {
		return JavaCore.COMPILER_PB_INVALID_IMPORT.equals(optionName)
				|| JavaCore.COMPILER_PB_UNREACHABLE_CODE.equals(optionName);
	}

	public boolean isNonChainingJar(IPath path) {
		return this.nonChainingJars != null && this.nonChainingJars.contains(path);
	}

	public ArchiveValidity getArchiveValidity(IPath path) {
		InvalidArchiveInfo invalidArchiveInfo;
		synchronized (this.invalidArchives) {
			invalidArchiveInfo = this.invalidArchives.get(path);
		}
		if (invalidArchiveInfo == null) {
			if (DEBUG_INVALID_ARCHIVES) {
				System.out.println("JAR cache: UNKNOWN validity for " + path);  //$NON-NLS-1$
			}
			return null;
		}
		long now = System.currentTimeMillis();

		// If the TTL for this cache entry has expired, directly check whether the archive is still invalid.
		if (now > invalidArchiveInfo.evictionTimestamp) {
			try {
				ZipFile zipFile = getZipFile(path, false);
				closeZipFile(zipFile);
				removeFromInvalidArchiveCache(path);
				addInvalidArchive(path, ArchiveValidity.VALID); // update TTL
				return ArchiveValidity.VALID;
			} catch (CoreException e) {
				// Archive is still invalid, fall through to reporting it is invalid.
			}
			addInvalidArchive(path, ArchiveValidity.INVALID); // update TTL
			return ArchiveValidity.INVALID;
		}
		if (DEBUG_INVALID_ARCHIVES) {
			System.out.println("JAR cache: " + invalidArchiveInfo.reason + " " + path);  //$NON-NLS-1$ //$NON-NLS-2$
		}
		return invalidArchiveInfo.reason;
	}

	public void removeFromInvalidArchiveCache(IPath path) {
		synchronized(this.invalidArchives) {
			InvalidArchiveInfo entry = this.invalidArchives.get(path);
			if (entry != null && entry.reason == ArchiveValidity.VALID) {
				if (DEBUG_INVALID_ARCHIVES) {
					System.out.println("JAR cache: keep VALID " + path);  //$NON-NLS-1$
				}
				return; // do not remove the VALID information
			}
			// If it transitioned to being valid then force an update to project caches.
			if (this.invalidArchives.remove(path) != null) {
				if (DEBUG_INVALID_ARCHIVES) {
					System.out.println("JAR cache: removed INVALID " + path);  //$NON-NLS-1$
				}
				try {
					// Bug 455042: Force an update of the JavaProjectElementInfo project caches.
					for (IJavaProject project : getJavaModel().getJavaProjects()) {
						if (project.findPackageFragmentRoot(path) != null) {
							((JavaProject) project).resetCaches();
						}
					}
				} catch (JavaModelException e) {
					Util.log(e, "Unable to retrieve the Java model."); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Returns the cached value for whether the file referred to by <code>path</code> exists
	 * and is a file, as determined by the return value of {@link File#isFile()}.
	 */
	public boolean isExternalFile(IPath path) {
		return this.externalFiles != null && this.externalFiles.contains(path);
	}

	/**
	 * Removes the cached state of a single entry in the externalFiles cache.
	 */
	public void clearExternalFileState(IPath path) {
		if (this.externalFiles != null) {
			this.externalFiles.remove(path);
		}
	}

	/**
	 * Resets the entire externalFiles cache.
	 */
	public void resetExternalFilesCache() {
		if (this.externalFiles != null) {
			this.externalFiles.clear();
		}
	}

	/**
	 * Returns whether the provided {@link IPath} appears to be an external file,
	 * which is true if the path does not represent an internal resource, does not
	 * exist on the file system, and does have a file extension (this is the definition
	 * provided by {@link ExternalFoldersManager#isExternalFolderPath}).
	 */
	public boolean isAssumedExternalFile(IPath path) {
		if (this.assumedExternalFiles == null) {
			return false;
		}
		return this.assumedExternalFiles.contains(path);
	}

	/**
	 * Adds the provided {@link IPath} to the list of assumed external files.
	 */
	public void addAssumedExternalFile(IPath path) {
		this.assumedExternalFiles.add(path);
	}

	public void setClasspathBeingResolved(IJavaProject project, boolean classpathIsResolved) {
	    if (classpathIsResolved) {
	        getClasspathBeingResolved().add(project);
	    } else {
	        getClasspathBeingResolved().remove(project);
	    }
	}

	private Set<IPath> loadClasspathListCache(String cacheName) {
		Set<IPath> pathCache = new HashSet<>();
		File cacheFile = getClasspathListFile(cacheName);
		DataInputStream in = null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(cacheFile)));
			int size = in.readInt();
			while (size-- > 0) {
				String path = in.readUTF();
				pathCache.add(Path.fromPortableString(path));
			}
		} catch (IOException e) {
			if (cacheFile.exists())
				Util.log(e, "Unable to read JavaModelManager " + cacheName + " file"); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// nothing we can do: ignore
				}
			}
		}
		return Collections.synchronizedSet(pathCache);
	}

	private File getClasspathListFile(String fileName) {
		return JavaCore.getPlugin().getStateLocation().append(fileName).toFile();
	}

	private Set<IPath> getNonChainingJarsCache() throws CoreException {
		// Even if there is one entry in the cache, just return it. It may not be
		// the complete cache, but avoid going through all the projects to populate the cache.
		if (this.nonChainingJars != null && this.nonChainingJars.size() > 0) {
			return this.nonChainingJars;
		}
		Set<IPath> result = new HashSet<>();
		IJavaProject[] projects = getJavaModel().getJavaProjects();
		for (int i = 0, length = projects.length; i < length; i++) {
			IJavaProject javaProject = projects[i];
			IClasspathEntry[] classpath = ((JavaProject) javaProject).getResolvedClasspath();
			for (int j = 0, length2 = classpath.length; j < length2; j++) {
				IClasspathEntry entry = classpath[j];
				IPath path;
				if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY
					&& !result.contains(path = entry.getPath())
					&& ClasspathEntry.resolvedChainedLibraries(path).length == 0) {
						result.add(path);
				}
			}
		}
		this.nonChainingJars = Collections.synchronizedSet(result);
		return this.nonChainingJars;
	}

	private Set<IPath> getClasspathListCache(String cacheName) throws CoreException {
		if (cacheName == NON_CHAINING_JARS_CACHE)
			return getNonChainingJarsCache();
		else if (cacheName == EXTERNAL_FILES_CACHE)
			return this.externalFiles;
		else if (cacheName == ASSUMED_EXTERNAL_FILES_CACHE)
			return this.assumedExternalFiles;
		else
			return null;
	}

	public void loadVariablesAndContainers() throws CoreException {
		// backward compatibility, consider persistent property
		QualifiedName qName = new QualifiedName(JavaCore.PLUGIN_ID, "variables"); //$NON-NLS-1$
		String xmlString = ResourcesPlugin.getWorkspace().getRoot().getPersistentProperty(qName);

		try {
			if (xmlString != null){
				StringReader reader = new StringReader(xmlString);
				Element cpElement;
				try {
					DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					cpElement = parser.parse(new InputSource(reader)).getDocumentElement();
				} catch(SAXException | ParserConfigurationException e){
					return;
				} finally {
					reader.close();
				}
				if (cpElement == null) return;
				if (!cpElement.getNodeName().equalsIgnoreCase("variables")) { //$NON-NLS-1$
					return;
				}

				NodeList list= cpElement.getChildNodes();
				int length= list.getLength();
				for (int i= 0; i < length; ++i) {
					Node node= list.item(i);
					short type= node.getNodeType();
					if (type == Node.ELEMENT_NODE) {
						Element element= (Element) node;
						if (element.getNodeName().equalsIgnoreCase("variable")) { //$NON-NLS-1$
							variablePut(
								element.getAttribute("name"), //$NON-NLS-1$
								new Path(element.getAttribute("path"))); //$NON-NLS-1$
						}
					}
				}
			}
		} catch(IOException e){
			// problem loading xml file: nothing we can do
		} finally {
			if (xmlString != null){
				ResourcesPlugin.getWorkspace().getRoot().setPersistentProperty(qName, null); // flush old one
			}
		}

		// backward compatibility, load variables and containers from preferences into cache
		loadVariablesAndContainers(getDefaultPreferences());
		loadVariablesAndContainers(getInstancePreferences());

		// load variables and containers from saved file into cache
		File file = getVariableAndContainersFile();
		DataInputStream in = null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			switch (in.readInt()) {
				case 2 :
					new VariablesAndContainersLoadHelper(in).load();
					break;
				case 1 : // backward compatibility, load old format
					// variables
					int size = in.readInt();
					while (size-- > 0) {
						String varName = in.readUTF();
						String pathString = in.readUTF();
						if (CP_ENTRY_IGNORE.equals(pathString))
							continue;
						IPath varPath = Path.fromPortableString(pathString);
						this.variables.put(varName, varPath);
						this.previousSessionVariables.put(varName, varPath);
					}

					// containers
					IJavaModel model = getJavaModel();
					int projectSize = in.readInt();
					while (projectSize-- > 0) {
						String projectName = in.readUTF();
						IJavaProject project = model.getJavaProject(projectName);
						int containerSize = in.readInt();
						while (containerSize-- > 0) {
							IPath containerPath = Path.fromPortableString(in.readUTF());
							int length = in.readInt();
							byte[] containerString = new byte[length];
							in.readFully(containerString);
							recreatePersistedContainer(project, containerPath, new String(containerString), true/*add to container values*/);
						}
					}
					break;
			}
		} catch (IOException e) {
			if (file.exists())
				Util.log(e, "Unable to read variable and containers file"); //$NON-NLS-1$
		} catch (RuntimeException e) {
			if (file.exists())
				Util.log(e, "Unable to read variable and containers file (file is corrupt)"); //$NON-NLS-1$
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// nothing we can do: ignore
				}
			}
		}

		// override persisted values for variables which have a registered initializer
		String[] registeredVariables = getRegisteredVariableNames();
		for (int i = 0; i < registeredVariables.length; i++) {
			String varName = registeredVariables[i];
			this.variables.put(varName, null); // reset variable, but leave its entry in the Map, so it will be part of variable names.
		}
		// override persisted values for containers which have a registered initializer
		containersReset(getRegisteredContainerIDs());
	}

	private void loadVariablesAndContainers(IEclipsePreferences preferences) {
		try {
			// only get variable from preferences not set to their default
			String[] propertyNames = preferences.keys();
			int variablePrefixLength = CP_VARIABLE_PREFERENCES_PREFIX.length();
			for (int i = 0; i < propertyNames.length; i++){
				String propertyName = propertyNames[i];
				if (propertyName.startsWith(CP_VARIABLE_PREFERENCES_PREFIX)){
					String varName = propertyName.substring(variablePrefixLength);
					String propertyValue = preferences.get(propertyName, null);
					if (propertyValue != null) {
						String pathString = propertyValue.trim();

						if (CP_ENTRY_IGNORE.equals(pathString)) {
							// cleanup old preferences
							preferences.remove(propertyName);
							continue;
						}

						// add variable to table
						IPath varPath = new Path(pathString);
						this.variables.put(varName, varPath);
						this.previousSessionVariables.put(varName, varPath);
					}
				} else if (propertyName.startsWith(CP_CONTAINER_PREFERENCES_PREFIX)){
					String propertyValue = preferences.get(propertyName, null);
					if (propertyValue != null) {
						// cleanup old preferences
						preferences.remove(propertyName);

						// recreate container
						recreatePersistedContainer(propertyName, propertyValue, true/*add to container values*/);
					}
				}
			}
		} catch (BackingStoreException e1) {
			// TODO (frederic) see if it's necessary to report this failure...
		}
	}

	private static final class PersistedClasspathContainer implements
			IClasspathContainer {

		private final IPath containerPath;

		private final IClasspathEntry[] entries;

		private final IJavaProject project;

		PersistedClasspathContainer(IJavaProject project, IPath containerPath,
				IClasspathEntry[] entries) {
			super();
			this.containerPath = containerPath;
			this.entries = entries;
			this.project = project;
		}

		@Override
		public IClasspathEntry[] getClasspathEntries() {
			return this.entries;
		}

		@Override
		public String getDescription() {
			return "Persisted container [" + this.containerPath //$NON-NLS-1$
					+ " for project [" + this.project.getElementName() //$NON-NLS-1$
					+ "]]"; //$NON-NLS-1$
		}

		@Override
		public int getKind() {
			return 0;
		}

		@Override
		public IPath getPath() {
			return this.containerPath;
		}

		@Override
		public String toString() {
			return getDescription();
		}
	}

	private final class VariablesAndContainersLoadHelper {

		private static final int ARRAY_INCREMENT = 200;

		private IClasspathEntry[] allClasspathEntries;
		private int allClasspathEntryCount;

		private final Map<String, IPath> allPaths;

		private String[] allStrings;
		private int allStringsCount;

		private final DataInputStream in;

		VariablesAndContainersLoadHelper(DataInputStream in) {
			super();
			this.allClasspathEntries = null;
			this.allClasspathEntryCount = 0;
			this.allPaths = new HashMap<>();
			this.allStrings = null;
			this.allStringsCount = 0;
			this.in = in;
		}

		void load() throws IOException {
			loadProjects(getJavaModel());
			loadVariables();
		}

		private IAccessRule loadAccessRule() throws IOException {
			int problemId = loadInt();
			IPath pattern = loadPath();
			return getAccessRuleForProblemId(pattern.toString().toCharArray(), problemId);
		}

		private IAccessRule[] loadAccessRules() throws IOException {
			int count = loadInt();

			if (count == 0)
				return ClasspathEntry.NO_ACCESS_RULES;

			IAccessRule[] rules = new IAccessRule[count];

			for (int i = 0; i < count; ++i)
				rules[i] = loadAccessRule();

			return rules;
		}

		private IClasspathAttribute loadAttribute() throws IOException {
			String name = loadString();
			String value = loadString();

			return new ClasspathAttribute(name, value);
		}

		private IClasspathAttribute[] loadAttributes() throws IOException {
			int count = loadInt();

			if (count == 0)
				return ClasspathEntry.NO_EXTRA_ATTRIBUTES;

			IClasspathAttribute[] attributes = new IClasspathAttribute[count];

			for (int i = 0; i < count; ++i)
				attributes[i] = loadAttribute();

			return attributes;
		}

		private boolean loadBoolean() throws IOException {
			return this.in.readBoolean();
		}

		private IClasspathEntry[] loadClasspathEntries() throws IOException {
			int count = loadInt();
			IClasspathEntry[] entries = new IClasspathEntry[count];

			for (int i = 0; i < count; ++i)
				entries[i] = loadClasspathEntry();

			return entries;
		}

		private IClasspathEntry loadClasspathEntry() throws IOException {
			int id = loadInt();

			if (id < 0 || id > this.allClasspathEntryCount)
				throw new IOException("Unexpected classpathentry id"); //$NON-NLS-1$

			if (id < this.allClasspathEntryCount)
				return this.allClasspathEntries[id];

			int contentKind = loadInt();
			int entryKind = loadInt();
			IPath path = loadPath();
			IPath[] inclusionPatterns = loadPaths();
			IPath[] exclusionPatterns = loadPaths();
			IPath sourceAttachmentPath = loadPath();
			IPath sourceAttachmentRootPath = loadPath();
			IPath specificOutputLocation = loadPath();
			boolean isExported = loadBoolean();
			IAccessRule[] accessRules = loadAccessRules();
			boolean combineAccessRules = loadBoolean();
			IClasspathAttribute[] extraAttributes = loadAttributes();

			IClasspathEntry entry = new ClasspathEntry(contentKind, entryKind,
					path, inclusionPatterns, exclusionPatterns,
					sourceAttachmentPath, sourceAttachmentRootPath,
					specificOutputLocation, isExported, accessRules,
					combineAccessRules, extraAttributes);

			IClasspathEntry[] array = this.allClasspathEntries;

			if (array == null || id == array.length) {
				array = new IClasspathEntry[id + ARRAY_INCREMENT];

				if (id != 0)
					System.arraycopy(this.allClasspathEntries, 0, array, 0, id);

				this.allClasspathEntries = array;
			}

			array[id] = entry;
			this.allClasspathEntryCount = id + 1;

			return entry;
		}

		private void loadContainers(IJavaProject project) throws IOException {
			boolean projectIsAccessible = project.getProject().isAccessible();
			int count = loadInt();
			for (int i = 0; i < count; ++i) {
				IPath path = loadPath();
				IClasspathEntry[] entries = loadClasspathEntries();

				if (!projectIsAccessible)
					// avoid leaking deleted project's persisted container,
					// but still read the container as it is is part of the file format
					continue;

				IClasspathContainer container = new PersistedClasspathContainer(project, path, entries);

				containerPut(project, path, container);

				Map<IPath, IClasspathContainer> oldContainers = JavaModelManager.this.previousSessionContainers.get(project);

				if (oldContainers == null) {
					oldContainers = new HashMap<>();
					JavaModelManager.this.previousSessionContainers.put(project, oldContainers);
				}

				oldContainers.put(path, container);
			}
		}

		private int loadInt() throws IOException {
			return this.in.readInt();
		}

		private IPath loadPath() throws IOException {
			if (loadBoolean())
				return null;

			String portableString = loadString();
			IPath path = this.allPaths.get(portableString);

			if (path == null) {
				path = Path.fromPortableString(portableString);
				this.allPaths.put(portableString, path);
			}

			return path;
		}

		private IPath[] loadPaths() throws IOException {
			int count = loadInt();
			IPath[] pathArray = new IPath[count];

			for (int i = 0; i < count; ++i)
				pathArray[i] = loadPath();

			return pathArray;
		}

		private void loadProjects(IJavaModel model) throws IOException {
			int count = loadInt();

			for (int i = 0; i < count; ++i) {
				String projectName = loadString();

				loadContainers(model.getJavaProject(projectName));
			}
		}

		private String loadString() throws IOException {
			int id = loadInt();

			if (id < 0 || id > this.allStringsCount)
				throw new IOException("Unexpected string id"); //$NON-NLS-1$

			if (id < this.allStringsCount)
				return this.allStrings[id];

			String string = this.in.readUTF();
			String[] array = this.allStrings;

			if (array == null || id == array.length) {
				array = new String[id + ARRAY_INCREMENT];

				if (id != 0)
					System.arraycopy(this.allStrings, 0, array, 0, id);

				this.allStrings = array;
			}

			array[id] = string;
			this.allStringsCount = id + 1;

			return string;
		}

		private void loadVariables() throws IOException {
			int size = loadInt();
			Map<String, IPath> loadedVars = new HashMap<>(size);

			for (int i = 0; i < size; ++i) {
				String varName = loadString();
				IPath varPath = loadPath();

				if (varPath != null)
					loadedVars.put(varName, varPath);
			}

			JavaModelManager.this.previousSessionVariables.putAll(loadedVars);
			JavaModelManager.this.variables.putAll(loadedVars);
		}
	}

	/**
	 *  Returns the info for this element without
	 *  disturbing the cache ordering.
	 */
	protected synchronized Object peekAtInfo(IJavaElement element) {
		HashMap<IJavaElement, Object> tempCache = this.temporaryCache.get();
		if (tempCache != null) {
			Object result = tempCache.get(element);
			if (result != null) {
				return result;
			}
		}
		return this.cache.peekAtInfo(element);
	}

	/**
	 * @see ISaveParticipant
	 */
	@Override
	public void prepareToSave(ISaveContext context) /*throws CoreException*/ {
		// nothing to do
	}
	/*
	 * Puts the infos in the given map (keys are IJavaElements and values are JavaElementInfos)
	 * in the Java model cache in an atomic way if the info is not already present in the cache.
	 * If the info is already present in the cache, it depends upon the forceAdd parameter.
	 * If forceAdd is false it just returns the existing info and if true, this element and it's children are closed and then
	 * this particular info is added to the cache.
	 */
	protected synchronized Object putInfos(IJavaElement openedElement, Object newInfo, boolean forceAdd, Map<IJavaElement, Object> newElements) {
		// remove existing children as the are replaced with the new children contained in newElements
		Object existingInfo = this.cache.peekAtInfo(openedElement);
		if (existingInfo != null && !forceAdd) {
			// If forceAdd is false, then it could mean that the particular element
			// wasn't in cache at that point of time, but would have got added through
			// another thread. In that case, removing the children could remove it's own
			// children. So, we should not remove the children but return the already existing
			// info.
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=372687
			return existingInfo;
		}
		if (openedElement instanceof IParent) {
			closeChildren(existingInfo);
		}

		// Need to put any JarPackageFragmentRoot in first.
		// This is due to the way the LRU cache flushes entries.
		// When a JarPackageFragment is flushed from the LRU cache, the entire
		// jar is flushed by removing the JarPackageFragmentRoot and all of its
		// children (see ElementCache.close()). If we flush the JarPackageFragment
		// when its JarPackageFragmentRoot is not in the cache and the root is about to be
		// added (during the 'while' loop), we will end up in an inconsistent state.
		// Subsequent resolution against package in the jar would fail as a result.
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102422
		// (theodora)
		for(Iterator<Entry<IJavaElement, Object>> it = newElements.entrySet().iterator(); it.hasNext(); ) {
			Entry<IJavaElement, Object> entry = it.next();
			IJavaElement element = entry.getKey();
			if (element instanceof JarPackageFragmentRoot) {
				JavaElementInfo info = (JavaElementInfo) entry.getValue();
				it.remove();
				this.cache.putInfo(element, info);
			}
		}

		Iterator<Entry<IJavaElement, Object>> iterator = newElements.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<IJavaElement, Object> entry = iterator.next();
			this.cache.putInfo(entry.getKey(), entry.getValue());
		}
		return newInfo;
	}

	private void closeChildren(Object info) {
		if (info instanceof JavaElementInfo) {
			IJavaElement[] children = ((JavaElementInfo)info).getChildren();
			for (int i = 0, size = children.length; i < size; ++i) {
				JavaElement child = (JavaElement) children[i];
				try {
					child.close();
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Remember the info for the jar binary type
	 * @param info instanceof IBinaryType or {@link JavaModelCache#NON_EXISTING_JAR_TYPE_INFO}
	 */
	protected synchronized void putJarTypeInfo(IJavaElement type, Object info) {
		this.cache.jarTypeCache.put(type, info);
	}

	/**
	 * Reads the build state for the relevant project.
	 */
	protected Object readState(IProject project) throws CoreException {
		long startTime = System.currentTimeMillis();
		Object result = readStateTimed(project);
		if (JavaBuilder.DEBUG) {
			long stopTime = System.currentTimeMillis();
			System.out.println("readState took " + (stopTime - startTime) + "ms:" + project.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}

	private Object readStateTimed(IProject project) throws CoreException {
		File file = getSerializationFile(project);
		if (file != null && file.exists()) {
			try (DataInputStream in = new DataInputStream(createInputStream(file))) {
				String pluginID = in.readUTF();
				if (!pluginID.equals(JavaCore.PLUGIN_ID))
					throw new IOException(Messages.build_wrongFileFormat);
				String kind = in.readUTF();
				if (!kind.equals("STATE")) //$NON-NLS-1$
					throw new IOException(Messages.build_wrongFileFormat);
				if (in.readBoolean())
					return JavaBuilder.readState(project, in);
				if (JavaBuilder.DEBUG)
					System.out.println("Saved state thinks last build failed for " + project.getName()); //$NON-NLS-1$
			} catch (Exception e) {
				e.printStackTrace();
				throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, Platform.PLUGIN_ERROR, "Error reading last build state for project "+ project.getName(), e)); //$NON-NLS-1$
			}
		} else if (JavaBuilder.DEBUG) {
			if (file == null)
				System.out.println("Project does not exist: " + project); //$NON-NLS-1$
			else
				System.out.println("Build state file " + file.getPath() + " does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	public static void recreatePersistedContainer(String propertyName, String containerString, boolean addToContainerValues) {
		int containerPrefixLength = CP_CONTAINER_PREFERENCES_PREFIX.length();
		int index = propertyName.indexOf('|', containerPrefixLength);
		if (containerString != null) containerString = containerString.trim();
		if (index > 0) {
			String projectName = propertyName.substring(containerPrefixLength, index).trim();
			IJavaProject project = getJavaModelManager().getJavaModel().getJavaProject(projectName);
			IPath containerPath = new Path(propertyName.substring(index+1).trim());
			recreatePersistedContainer(project, containerPath, containerString, addToContainerValues);
		}
	}

	private static void recreatePersistedContainer(final IJavaProject project, final IPath containerPath, String containerString, boolean addToContainerValues) {
		if (!project.getProject().isAccessible()) return; // avoid leaking deleted project's persisted container
		if (containerString == null) {
			getJavaModelManager().containerPut(project, containerPath, null);
		} else {
			IClasspathEntry[] entries;
			try {
				entries = ((JavaProject) project).decodeClasspath(containerString, null/*not interested in unknown elements*/)[0];
			} catch (IOException e) {
				Util.log(e, "Could not recreate persisted container: \n" + containerString); //$NON-NLS-1$
				entries = JavaProject.INVALID_CLASSPATH;
			}
			if (entries != JavaProject.INVALID_CLASSPATH) {
				final IClasspathEntry[] containerEntries = entries;
				IClasspathContainer container = new IClasspathContainer() {
					@Override
					public IClasspathEntry[] getClasspathEntries() {
						return containerEntries;
					}
					@Override
					public String getDescription() {
						return "Persisted container ["+containerPath+" for project ["+ project.getElementName()+"]"; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					}
					@Override
					public int getKind() {
						return 0;
					}
					@Override
					public IPath getPath() {
						return containerPath;
					}
					@Override
					public String toString() {
						return getDescription();
					}

				};
				if (addToContainerValues) {
					getJavaModelManager().containerPut(project, containerPath, container);
				}
				Map<IPath, IClasspathContainer> projectContainers = getJavaModelManager().previousSessionContainers.get(project);
				if (projectContainers == null){
					projectContainers = new HashMap<>(1);
					getJavaModelManager().previousSessionContainers.put(project, projectContainers);
				}
				projectContainers.put(containerPath, container);
			}
		}
	}

	/**
	 * Remembers the given scope in a weak set
	 * (so no need to remove it: it will be removed by the garbage collector)
	 */
	public void rememberScope(AbstractSearchScope scope) {
		// NB: The value has to be null so as to not create a strong reference on the scope
		this.searchScopes.put(scope, null);
	}

	/*
	 * Removes all cached info for the given element (including all children)
	 * from the cache.
	 * Returns the info for the given element, or null if it was closed.
	 */
	public synchronized Object removeInfoAndChildren(JavaElement element) throws JavaModelException {
		Object info = this.cache.peekAtInfo(element);
		if (info != null) {
			boolean wasVerbose = false;
			try {
				if (JavaModelCache.VERBOSE) {
					String elementType = JavaModelCache.getElementType(element);
					System.out.println(Thread.currentThread() + " CLOSING "+ elementType + " " + element.toStringWithAncestors());  //$NON-NLS-1$//$NON-NLS-2$
					wasVerbose = true;
					JavaModelCache.VERBOSE = false;
				}
				element.closing(info);
				if (element instanceof IParent) {
					closeChildren(info);
				}
				this.cache.removeInfo(element);
				if (wasVerbose) {
					System.out.println(this.cache.toStringFillingRation("-> ")); //$NON-NLS-1$
				}
			} finally {
				JavaModelCache.VERBOSE = wasVerbose;
			}
			return info;
		}
		return null;
	}

	void removeFromJarTypeCache(BinaryType type) {
		this.cache.removeFromJarTypeCache(type);
	}

	public void removePerProjectInfo(JavaProject javaProject, boolean removeExtJarInfo) {
		synchronized(this.perProjectInfos) { // use the perProjectInfo collection as its own lock
			IProject project = javaProject.getProject();
			PerProjectInfo info= this.perProjectInfos.get(project);
			if (info != null) {
				this.perProjectInfos.remove(project);
				if (removeExtJarInfo) {
					info.forgetExternalTimestampsAndIndexes();
				}
			}
		}
		resetClasspathListCache();
	}

	/*
	 * Reset project options stored in info cache.
	 */
	public void resetProjectOptions(JavaProject javaProject) {
		synchronized(this.perProjectInfos) { // use the perProjectInfo collection as its own lock
			IProject project = javaProject.getProject();
			PerProjectInfo info= this.perProjectInfos.get(project);
			if (info != null) {
				info.options = null;
			}
		}
	}

	/*
	 * Reset project preferences stored in info cache.
	 */
	public void resetProjectPreferences(JavaProject javaProject) {
		synchronized(this.perProjectInfos) { // use the perProjectInfo collection as its own lock
			IProject project = javaProject.getProject();
			PerProjectInfo info= this.perProjectInfos.get(project);
			if (info != null) {
				info.preferences = null;
			}
		}
	}

	public static final void doNotUse() {
		// used by tests to simulate a startup
		MANAGER.deltaState.doNotUse();
		MANAGER = new JavaModelManager();
	}

	/*
	 * Resets the cache that holds on binary type in jar files
	 */
	protected synchronized void resetJarTypeCache() {
		this.cache.resetJarTypeCache();
	}

	public void resetClasspathListCache() {
		if (this.nonChainingJars != null)
			this.nonChainingJars.clear();
		if (this.externalFiles != null)
			this.externalFiles.clear();
		if (this.assumedExternalFiles != null)
			this.assumedExternalFiles.clear();
	}

	/*
	 * Resets the temporary cache for newly created elements to null.
	 */
	public void resetTemporaryCache() {
		this.temporaryCache.remove();
	}

	/**
	 * @see ISaveParticipant
	 */
	@Override
	public void rollback(ISaveContext context){
		// nothing to do
	}

	private void saveState(PerProjectInfo info, ISaveContext context) throws CoreException {

		// passed this point, save actions are non trivial
		if (context.getKind() == ISaveContext.SNAPSHOT) return;

		// save built state
		if (info.triedRead) {
			long startTime = System.currentTimeMillis();
			saveBuiltState(info);
			if (JavaBuilder.DEBUG) {
				long stopTime = System.currentTimeMillis();
				System.out.println("saveState took " + (stopTime - startTime) + "ms:" + info.project.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Saves the built state for the project.
	 */
	private void saveBuiltState(PerProjectInfo info) throws CoreException {
		if (JavaBuilder.DEBUG)
			System.out.println(Messages.bind(Messages.build_saveStateProgress, info.project.getName()));
		File file = getSerializationFile(info.project);
		if (file == null) return;
		long t = System.currentTimeMillis();
		try {
			try (DataOutputStream out = new DataOutputStream(createOutputStream(file))) {
				out.writeUTF(JavaCore.PLUGIN_ID);
				out.writeUTF("STATE"); //$NON-NLS-1$
				if (info.savedState == null) {
					out.writeBoolean(false);
				} else {
					out.writeBoolean(true);
					JavaBuilder.writeState(info.savedState, out);
				}
			}
		} catch (RuntimeException | IOException e) {
			try {
				file.delete();
			} catch(SecurityException se) {
				// could not delete file: cannot do much more
			}
			throw new CoreException(
				new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, Platform.PLUGIN_ERROR,
					Messages.bind(Messages.build_cannotSaveState, info.project.getName()), e));
		}
		if (JavaBuilder.DEBUG) {
			t = System.currentTimeMillis() - t;
			System.out.println(Messages.bind(Messages.build_saveStateComplete, String.valueOf(t)));
		}
	}

	private InputStream createInputStream(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			return new BufferedInputStream(new java.util.zip.GZIPInputStream(in, 8192));
		} catch (ZipException e) {
			// probably not zipped (old format), but may also be corrupted.
			in.close();
			boolean isZip;
			try (DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
				isZip = din.readShort() == (short) java.util.zip.GZIPInputStream.GZIP_MAGIC;
			}
			if (isZip) {
				throw e; // corrupted
			}
			return new BufferedInputStream(new FileInputStream(file));
		}
	}

	private OutputStream createOutputStream(File file) throws IOException {
		if (SAVE_ZIPPED) {
			return new BufferedOutputStream(new java.util.zip.GZIPOutputStream(new FileOutputStream(file), 8192));
		} else {
			return new BufferedOutputStream(new FileOutputStream(file));
		}
	}

	private void saveClasspathListCache(String cacheName) throws CoreException {
		File file = getClasspathListFile(cacheName);
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))){
			Set<IPath> pathCache = getClasspathListCache(cacheName);
			synchronized (pathCache) {
				out.writeInt(pathCache.size());
				Iterator<IPath> entries = pathCache.iterator();
				while (entries.hasNext()) {
					IPath path = entries.next();
					out.writeUTF(path.toPortableString());
				}
			}
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, IStatus.ERROR, "Problems while saving non-chaining jar cache", e); //$NON-NLS-1$
			throw new CoreException(status);
		}
	}

	private void saveVariablesAndContainers(ISaveContext context) throws CoreException {
		File file = getVariableAndContainersFile();
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
			out.writeInt(VARIABLES_AND_CONTAINERS_FILE_VERSION);
			new VariablesAndContainersSaveHelper(out).save(context);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, IStatus.ERROR, "Problems while saving variables and containers", e); //$NON-NLS-1$
			throw new CoreException(status);
		}
	}

	private final class VariablesAndContainersSaveHelper {

		private final HashtableOfObjectToInt classpathEntryIds; // IClasspathEntry -> int
		private final DataOutputStream out;
		private final HashtableOfObjectToInt stringIds; // Strings -> int

		VariablesAndContainersSaveHelper(DataOutputStream out) {
			super();
			this.classpathEntryIds = new HashtableOfObjectToInt();
			this.out = out;
			this.stringIds = new HashtableOfObjectToInt();
		}

		void save(ISaveContext context) throws IOException, JavaModelException {
			saveProjects(getJavaModel().getJavaProjects());
			// remove variables that should not be saved
			HashMap<String, IPath> varsToSave = null;
			Iterator<Entry<String, IPath>> iterator = JavaModelManager.this.variables.entrySet().iterator();
			IEclipsePreferences defaultPreferences = getDefaultPreferences();
			while (iterator.hasNext()) {
				Entry<String, IPath> entry = iterator.next();
				String varName = entry.getKey();
				if (defaultPreferences.get(CP_VARIABLE_PREFERENCES_PREFIX + varName, null) != null // don't save classpath variables from the default preferences as there is no delta if they are removed
						|| CP_ENTRY_IGNORE_PATH.equals(entry.getValue())) {

					if (varsToSave == null)
						varsToSave = new HashMap<>(JavaModelManager.this.variables);
					varsToSave.remove(varName);
				}
			}
			saveVariables(varsToSave != null ? varsToSave : JavaModelManager.this.variables);
		}

		private void saveAccessRule(ClasspathAccessRule rule) throws IOException {
			saveInt(rule.problemId);
			savePath(rule.getPattern());
		}

		private void saveAccessRules(IAccessRule[] rules) throws IOException {
			int count = rules == null ? 0 : rules.length;

			saveInt(count);
			for (int i = 0; i < count; ++i)
				saveAccessRule((ClasspathAccessRule) rules[i]);
		}

		private void saveAttribute(IClasspathAttribute attribute)
				throws IOException {
			saveString(attribute.getName());
			saveString(attribute.getValue());
		}

		private void saveAttributes(IClasspathAttribute[] attributes)
				throws IOException {
			int count = attributes == null ? 0 : attributes.length;

			saveInt(count);
			for (int i = 0; i < count; ++i)
				saveAttribute(attributes[i]);
		}

		private void saveClasspathEntries(IClasspathEntry[] entries)
				throws IOException {
			int count = entries == null ? 0 : entries.length;

			saveInt(count);
			for (int i = 0; i < count; ++i)
				saveClasspathEntry(entries[i]);
		}

		private void saveClasspathEntry(IClasspathEntry entry)
				throws IOException {
			if (saveNewId(entry, this.classpathEntryIds)) {
				saveInt(entry.getContentKind());
				saveInt(entry.getEntryKind());
				savePath(entry.getPath());
				savePaths(entry.getInclusionPatterns());
				savePaths(entry.getExclusionPatterns());
				savePath(entry.getSourceAttachmentPath());
				savePath(entry.getSourceAttachmentRootPath());
				savePath(entry.getOutputLocation());
				this.out.writeBoolean(entry.isExported());
				saveAccessRules(entry.getAccessRules());
				this.out.writeBoolean(entry.combineAccessRules());
				saveAttributes(entry.getExtraAttributes());
			}
		}

		private void saveContainers(IJavaProject project, Map<IPath, IClasspathContainer> containerMap)
				throws IOException {
			saveInt(containerMap.size());

			for (Iterator<Entry<IPath, IClasspathContainer>> i = containerMap.entrySet().iterator(); i.hasNext();) {
				Entry<IPath, IClasspathContainer> entry = i.next();
				IPath path = entry.getKey();
				IClasspathContainer container = entry.getValue();
				IClasspathEntry[] cpEntries = null;

				if (container == null) {
					// container has not been initialized yet, use previous
					// session value
					// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=73969)
					container = getPreviousSessionContainer(path, project);
				}

				if (container != null)
					cpEntries = container.getClasspathEntries();

				savePath(path);
				saveClasspathEntries(cpEntries);
			}
		}

		private void saveInt(int value) throws IOException {
			this.out.writeInt(value);
		}

		private boolean saveNewId(Object key, HashtableOfObjectToInt map) throws IOException {
			int id = map.get(key);

			if (id == -1) {
				int newId = map.size();

				map.put(key, newId);

				saveInt(newId);

				return true;
			} else {
				saveInt(id);

				return false;
			}
		}

		private void savePath(IPath path) throws IOException {
			if (path == null) {
				this.out.writeBoolean(true);
			} else {
				this.out.writeBoolean(false);
				saveString(path.toPortableString());
			}
		}

		private void savePaths(IPath[] paths) throws IOException {
			int count = paths == null ? 0 : paths.length;

			saveInt(count);
			for (int i = 0; i < count; ++i)
				savePath(paths[i]);
		}

		private void saveProjects(IJavaProject[] projects) throws IOException,
				JavaModelException {
			int count = projects.length;

			saveInt(count);

			for (int i = 0; i < count; ++i) {
				IJavaProject project = projects[i];

				saveString(project.getElementName());

				Map<IPath, IClasspathContainer> containerMap = JavaModelManager.this.containers.get(project);

				if (containerMap == null) {
					containerMap = Collections.EMPTY_MAP;
				} else {
					// clone while iterating
					// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=59638)
					containerMap = new HashMap<>(containerMap);
				}

				saveContainers(project, containerMap);
			}
		}

		private void saveString(String string) throws IOException {
			if (saveNewId(string, this.stringIds))
				this.out.writeUTF(string);
		}

		private void saveVariables(Map<String, IPath> map) throws IOException {
			saveInt(map.size());

			for (Iterator<Entry<String, IPath>> i = map.entrySet().iterator(); i.hasNext();) {
				Entry<String, IPath> entry = i.next();
				String varName = entry.getKey();
				IPath varPath = entry.getValue();

				saveString(varName);
				savePath(varPath);
			}
		}
	}

	private void traceVariableAndContainers(String action, long start) {

		Long delta = Long.valueOf(System.currentTimeMillis() - start);
		Long length = Long.valueOf(getVariableAndContainersFile().length());
		String pattern = "{0} {1} bytes in variablesAndContainers.dat in {2}ms"; //$NON-NLS-1$
		String message = MessageFormat.format(pattern, new Object[]{action, length, delta});

		System.out.println(message);
	}

	/**
	 * @see ISaveParticipant
	 */
	@Override
	public void saving(ISaveContext context) throws CoreException {
		long startTime = System.currentTimeMillis();
		savingTimed(context);
		if (JavaBuilder.DEBUG) {
			long stopTime = System.currentTimeMillis();
			System.out.println("saving took " + (stopTime - startTime) + "ms:" + this.perProjectInfos.values().size()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void savingTimed(ISaveContext context) throws CoreException {

	    long start = -1;
		if (VERBOSE)
			start = System.currentTimeMillis();

		// save variable and container values on snapshot/full save
		saveVariablesAndContainers(context);

		if (VERBOSE)
			traceVariableAndContainers("Saved", start); //$NON-NLS-1$

		switch(context.getKind()) {
			case ISaveContext.FULL_SAVE : {
				// save non-chaining jar, invalid jar and external file caches on full save
				saveClasspathListCache(NON_CHAINING_JARS_CACHE);
				saveClasspathListCache(EXTERNAL_FILES_CACHE);
				saveClasspathListCache(ASSUMED_EXTERNAL_FILES_CACHE);

				// will need delta since this save (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38658)
				context.needDelta();

				// clean up indexes on workspace full save
				// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=52347)
				IndexManager manager = this.indexManager;
				if (manager != null
						// don't force initialization of workspace scope as we could be shutting down
						// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=93941)
						&& this.workspaceScope != null) {
					manager.cleanUpIndexes();
				}
			}
			//$FALL-THROUGH$
			case ISaveContext.SNAPSHOT : {
				// clean up external folders on full save or snapshot
				this.externalFoldersManager.cleanUp(null);
			}
		}

		IProject savedProject = context.getProject();
		if (savedProject != null) {
			if (!JavaProject.hasJavaNature(savedProject)) return; // ignore
			PerProjectInfo info = getPerProjectInfo(savedProject, true /* create info */);
			saveState(info, context);
			return;
		}

		ArrayList<PerProjectInfo> infos;
		synchronized (this.perProjectInfos) {
			infos = new ArrayList<>(this.perProjectInfos.values());
		}
		int parallelism = Math.max(1, SAVE_THREAD_COUNT == null ? ForkJoinPool.getCommonPoolParallelism() : SAVE_THREAD_COUNT.intValue());
		// never use a shared ForkJoinPool.commonPool() as it may be busy with other tasks, which might deadlock:
		ForkJoinPool forkJoinPool =  new ForkJoinPool(parallelism);
		IStatus[] stats;
		try {
			stats = forkJoinPool.submit(() -> infos.stream().parallel().map(info -> {
				try {
					saveState(info, context);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return null;
			}).filter(Objects::nonNull).toArray(IStatus[]::new)).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new CoreException(Status.error(Messages.build_cannotSaveStates, e));
		} finally {
			forkJoinPool.shutdown();
		}
		if (stats.length > 0) {
			throw new CoreException(new MultiStatus(JavaCore.PLUGIN_ID, IStatus.ERROR, stats, Messages.build_cannotSaveStates, null));
		}

		// save external libs timestamps
		this.deltaState.saveExternalLibTimeStamps();

	}

	/**
	 * Add a secondary type in temporary indexing cache for a project got from given path.
	 *
	 * Current secondary types cache is not modified as we want to wait that indexing
	 * was finished before taking new secondary types into account.
	 *
	 * @see #secondaryTypes(IJavaProject, boolean, IProgressMonitor)
	 */
	public void secondaryTypeAdding(String path, char[] typeName, char[] packageName) {
		if (VERBOSE) {
			StringBuilder buffer = new StringBuilder("JavaModelManager.addSecondaryType("); //$NON-NLS-1$
			buffer.append(path);
			buffer.append(',');
			buffer.append('[');
			buffer.append(new String(packageName));
			buffer.append('.');
			buffer.append(new String(typeName));
			buffer.append(']');
			buffer.append(')');
			Util.verbose(buffer.toString());
		}
		IWorkspaceRoot wRoot = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = wRoot.findMember(path);
		if (resource instanceof IFile) {
			if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(path)) {
				IProject project = resource.getProject();
				try {
					PerProjectInfo projectInfo = getPerProjectInfoCheckExistence(project);
					// Get or create map to cache secondary types while indexing (can be not synchronized as indexing insure a non-concurrent usage)
					Map<IFile, Map<String, Map<String, IType>>> indexedSecondaryTypes;
					if (projectInfo.secondaryTypes == null) {
						projectInfo.secondaryTypes = new Hashtable<>(3);
						indexedSecondaryTypes = Collections.synchronizedMap(new HashMap<>(3));
						projectInfo.indexingSecondaryCache = indexedSecondaryTypes;
					} else {
						indexedSecondaryTypes = projectInfo.indexingSecondaryCache;
						if (indexedSecondaryTypes == null) {
							indexedSecondaryTypes = Collections.synchronizedMap(new HashMap<>(3));
							projectInfo.indexingSecondaryCache = indexedSecondaryTypes;
						}
					}
					// Store the secondary type in temporary cache (these are just handles => no problem to create it now...)
					Map<String, Map<String, IType>> allTypes = indexedSecondaryTypes.get(resource);
					if (allTypes == null) {
						allTypes = new HashMap<>(3);
						indexedSecondaryTypes.put((IFile) resource, allTypes);
					}
					ICompilationUnit unit = JavaModelManager.createCompilationUnitFrom((IFile)resource, null);
					if (unit != null) {
						String typeString = new String(typeName);
						IType type = unit.getType(typeString);
						// String packageString = new String(packageName);
						// use package fragment name instead of parameter as it may be invalid...
						// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=186781
						String packageString = type.getPackageFragment().getElementName();
						Map<String, IType> packageTypes = allTypes.get(packageString);
						if (packageTypes == null) {
							packageTypes = new HashMap<>(3);
							allTypes.put(packageString, packageTypes);
						}
						packageTypes.put(typeString, type);
					}
					if (VERBOSE) {
						Util.verbose("	- indexing cache:"); //$NON-NLS-1$
						Iterator<Entry<IFile, Map<String, Map<String, IType>>>> entries = indexedSecondaryTypes.entrySet().iterator();
						while (entries.hasNext()) {
							Entry<IFile, Map<String, Map<String, IType>>> entry = entries.next();
							IFile file = entry.getKey();
							Util.verbose("		+ "+file.getFullPath()+':'+ entry.getValue()); //$NON-NLS-1$
						}
					}
				}
				catch (JavaModelException jme) {
					// do nothing
				}
			}
		}
	}

	/**
	 * Get all secondary types for a project and store result in per project info cache.
	 * <p>
	 * This cache is an <code>Hashtable&lt;String, HashMap&lt;String, IType&gt;&gt;</code>:
	 *  <ul>
	 * 	<li>key: package name
	 * 	<li>value:
	 * 		<ul>
	 * 		<li>key: type name
	 * 		<li>value: java model handle for the secondary type
	 * 		</ul>
	 * </ul>
	 * Hashtable was used to protect callers from possible concurrent access.
	 * </p>
	 * Note, if indexing is not finished and caller does
	 * not wait for the end of indexing, returned map is the current secondary
	 * types cache content which may be invalid...
	 *
	 * @param project Project we want get secondary types from
	 * @return HashMap Table of secondary type names->path for given project
	 */
	public Map<String, Map<String, IType>> secondaryTypes(IJavaProject project, boolean waitForIndexes, IProgressMonitor monitor) throws JavaModelException {
		if (VERBOSE) {
			StringBuilder buffer = new StringBuilder("JavaModelManager.secondaryTypes("); //$NON-NLS-1$
			buffer.append(project.getElementName());
			buffer.append(',');
			buffer.append(waitForIndexes);
			buffer.append(')');
			Util.verbose(buffer.toString());
		}

		// Return cache if not empty and there's no new secondary types created during indexing
		final PerProjectInfo projectInfo = getPerProjectInfoCheckExistence(project.getProject());
		Map<IFile, Map<String, Map<String, IType>>> indexingSecondaryCache = projectInfo.secondaryTypes == null ? null : projectInfo.indexingSecondaryCache;
		if (projectInfo.secondaryTypes != null && indexingSecondaryCache == null) {
			return projectInfo.secondaryTypes;
		}

		// Perform search request only if secondary types cache is not initialized yet (this will happen only once!)
		if (projectInfo.secondaryTypes == null) {
			return secondaryTypesSearching(project, waitForIndexes, monitor, projectInfo);
		}

		// New secondary types have been created while indexing secondary types cache
		// => need to know whether the indexing is finished or not
		boolean indexing = this.indexManager.awaitingJobsCount() > 0;
		if (indexing) {
			if (!waitForIndexes)  {
				// Indexing is running but caller cannot wait => return current cache
				return projectInfo.secondaryTypes;
			}

			// Wait for the end of indexing or a cancel
			try {
				this.indexManager.performConcurrentJob(new IJob() {
					@Override
					public boolean belongsTo(String jobFamily) {
						return true;
					}

					@Override
					public void cancel() {
						// job is cancelled through progress
					}

					@Override
					public void ensureReadyToRun() {
						// always ready
					}

					@Override
					public boolean execute(IProgressMonitor progress) {
						return progress == null || !progress.isCanceled();
					}

					@Override
					public String getJobFamily() {
						return ""; //$NON-NLS-1$
					}

				}, IJob.WaitUntilReady, monitor);
			} catch (OperationCanceledException oce) {
				return projectInfo.secondaryTypes;
			}
		}

		// Indexing is finished => merge caches and return result
		return secondaryTypesMerging(projectInfo);
	}

	/*
	 * Return secondary types cache merged with new secondary types created while indexing
	 * Note that merge result is directly stored in given parameter map.
	 */
	private Map<String, Map<String, IType>> secondaryTypesMerging(PerProjectInfo projectInfo) {
		Map<String, Map<String, IType>> secondaryTypes = projectInfo.secondaryTypes;
		if (VERBOSE) {
			Util.verbose("JavaModelManager.getSecondaryTypesMerged()"); //$NON-NLS-1$
			Util.verbose("	- current cache to merge:"); //$NON-NLS-1$
			Iterator<Entry<String, Map<String, IType>>> entries = secondaryTypes.entrySet().iterator();
			while (entries.hasNext()) {
				Entry<String, Map<String, IType>> entry = entries.next();
				String packName = entry.getKey();
				Util.verbose("		+ "+packName+':'+ entry.getValue() ); //$NON-NLS-1$
			}
		}

		// Return current cache if there's no indexing cache (double check, this should not happen)
		Map<IFile, Map<String, Map<String, IType>>> indexedSecondaryTypes = projectInfo.indexingSecondaryCache;
		projectInfo.indexingSecondaryCache = null;
		if (indexedSecondaryTypes == null) {
			return secondaryTypes;
		}
		Map<IFile, Map<String, Map<String, IType>>> indexedSecondaryTypesCopy;
		synchronized (indexedSecondaryTypes) {
			indexedSecondaryTypesCopy = new HashMap<>(indexedSecondaryTypes);
		}

		// Merge indexing cache in secondary types one
		Iterator<Entry<IFile, Map<String, Map<String, IType>>>> entries = indexedSecondaryTypesCopy.entrySet().iterator();
		while (entries.hasNext()) {
			Entry<IFile, Map<String, Map<String, IType>>> entry = entries.next();
			IFile file = entry.getKey();

			// Remove all secondary types of indexed file from cache
			secondaryTypesRemoving(secondaryTypes, file);

			// Add all indexing file secondary types in given secondary types cache
			Map<String, Map<String, IType>> fileSecondaryTypes = entry.getValue();
			Iterator<Entry<String, Map<String, IType>>> entries2 = fileSecondaryTypes.entrySet().iterator();
			while (entries2.hasNext()) {
				Entry<String, Map<String, IType>> entry2 = entries2.next();
				String packageName = entry2.getKey();
				Map<String, IType> cachedTypes = secondaryTypes.get(packageName);
				if (cachedTypes == null) {
					secondaryTypes.put(packageName, entry2.getValue());
				} else {
					Map<String, IType> types = entry2.getValue();
					Iterator<Entry<String, IType>> entries3 = types.entrySet().iterator();
					while (entries3.hasNext()) {
						Entry<String, IType> entry3 = entries3.next();
						String typeName = entry3.getKey();
						cachedTypes.put(typeName, entry3.getValue());
					}
				}
			}
		}
		if (VERBOSE) {
			Util.verbose("	- secondary types cache merged:"); //$NON-NLS-1$
			Iterator<Entry<String, Map<String, IType>>> entries2 = secondaryTypes.entrySet().iterator();
			while (entries.hasNext()) {
				Entry<String, Map<String, IType>> entry = entries2.next();
				String packName = entry.getKey();
				Util.verbose("		+ "+packName+':'+ entry.getValue()); //$NON-NLS-1$
			}
		}
		return secondaryTypes;
	}

	/*
	 * Perform search request to get all secondary types of a given project.
	 * If not waiting for indexes and indexing is running, will return types found in current built indexes...
	 */
	private static Map<String, Map<String, IType>> secondaryTypesSearching(IJavaProject project, boolean waitForIndexes, IProgressMonitor monitor, final PerProjectInfo projectInfo) throws JavaModelException {
		if (VERBOSE || BasicSearchEngine.VERBOSE) {
			StringBuilder buffer = new StringBuilder("JavaModelManager.secondaryTypesSearch("); //$NON-NLS-1$
			buffer.append(project.getElementName());
			buffer.append(',');
			buffer.append(waitForIndexes);
			buffer.append(')');
			Util.verbose(buffer.toString());
		}

		final Hashtable<String, Map<String, String>> secondaryTypesSearch = new Hashtable<>(3);
		IRestrictedAccessTypeRequestor nameRequestor = new IRestrictedAccessTypeRequestor() {
			@Override
			public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path, AccessRestriction access) {
				String key = packageName==null ? "" : new String(packageName); //$NON-NLS-1$
				Map<String, String> types = secondaryTypesSearch.get(key);
				if (types == null) types = new HashMap<>(3);
				types.put(new String(simpleTypeName), path);
				secondaryTypesSearch.put(key, types);
			}
		};

		// Build scope using prereq projects but only source folders
		IPackageFragmentRoot[] allRoots = project.getAllPackageFragmentRoots();
		int length = allRoots.length, size = 0;
		IPackageFragmentRoot[] allSourceFolders = new IPackageFragmentRoot[length];
		for (int i=0; i<length; i++) {
			if (allRoots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
				allSourceFolders[size++] = allRoots[i];
			}
		}
		if (size < length) {
			System.arraycopy(allSourceFolders, 0, allSourceFolders = new IPackageFragmentRoot[size], 0, size);
		}

		// Search all secondary types on scope
		new BasicSearchEngine().searchAllSecondaryTypeNames(allSourceFolders, nameRequestor, waitForIndexes, monitor);

		// Build types from paths
		final Hashtable<String, Map<String, IType>> secondaryTypes = new Hashtable<>(secondaryTypesSearch.size());
		for (Entry<String, Map<String, String>> packageEntry : secondaryTypesSearch.entrySet()) {
			String packageName = packageEntry.getKey();
			Map<String, String> types = packageEntry.getValue();
			Map<String, IType> tempTypes = new HashMap<>(types.size());
			for (Entry<String, String> entry : types.entrySet()) {
				String typeName = entry.getKey();
				String path = entry.getValue();
				if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(path)) {
					IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
					ICompilationUnit unit = JavaModelManager.createCompilationUnitFrom(file, null);
					IType type = unit.getType(typeName);
					tempTypes.put(typeName, type);
				}
			}
			secondaryTypes.put(packageName, tempTypes);
		}

		// Store result in per project info cache if still null or there's still an indexing cache (may have been set by another thread...)
		if (projectInfo.secondaryTypes == null || projectInfo.indexingSecondaryCache != null) {
			projectInfo.secondaryTypes = secondaryTypes;
			if (VERBOSE || BasicSearchEngine.VERBOSE) {
				System.out.print(Thread.currentThread() + "	-> secondary paths stored in cache: ");  //$NON-NLS-1$
				System.out.println();
				Iterator<Entry<String, Map<String, IType>>> entries = secondaryTypes.entrySet().iterator();
				while (entries.hasNext()) {
					Entry<String, Map<String, IType>> entry = entries.next();
					String qualifiedName = entry.getKey();
					Util.verbose("		- "+qualifiedName+'-'+ entry.getValue()); //$NON-NLS-1$
				}
			}
		}
		return projectInfo.secondaryTypes;
	}

	/**
	 * Remove from secondary types cache all types belonging to a given file.
	 * Clean secondary types cache built while indexing if requested.
	 *
	 * Project's secondary types cache is found using file location.
	 *
	 * @param file File to remove
	 */
	public void secondaryTypesRemoving(IFile file, boolean cleanIndexCache) {
		if (VERBOSE) {
			StringBuilder buffer = new StringBuilder("JavaModelManager.removeFromSecondaryTypesCache("); //$NON-NLS-1$
			buffer.append(file.getName());
			buffer.append(')');
			Util.verbose(buffer.toString());
		}
		if (file != null) {
			PerProjectInfo projectInfo = getPerProjectInfo(file.getProject(), false);
			if (projectInfo != null && projectInfo.secondaryTypes != null) {
				if (VERBOSE) {
					Util.verbose("-> remove file from cache of project: "+file.getProject().getName()); //$NON-NLS-1$
				}

				// Clean current cache
				secondaryTypesRemoving(projectInfo.secondaryTypes, file);

				// Clean indexing cache if necessary
				Map<IFile, Map<String, Map<String, IType>>> indexingCache = projectInfo.indexingSecondaryCache;
				if (!cleanIndexCache) {
					if (indexingCache == null) {
						// Need to signify that secondary types indexing will happen before any request happens
						// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=152841
						projectInfo.indexingSecondaryCache = Collections.synchronizedMap(new HashMap<>());
					}
					return;
				}
				if (indexingCache != null) {
					indexingCache.remove(file);
				}
			}
		}
	}

	/*
	 * Remove from a given cache map all secondary types belonging to a given file.
	 * Note that there can have several secondary types per file...
	 */
	private void secondaryTypesRemoving(Map<String, Map<String, IType>> secondaryTypesMap, IFile file) {
		if (VERBOSE) {
			StringBuilder buffer = new StringBuilder("JavaModelManager.removeSecondaryTypesFromMap("); //$NON-NLS-1$
			Iterator<Entry<String, Map<String, IType>>> entries = secondaryTypesMap.entrySet().iterator();
			while (entries.hasNext()) {
				Entry<String, Map<String, IType>> entry = entries.next();
				String qualifiedName = entry.getKey();
				buffer.append(qualifiedName+':'+ entry.getValue());
			}
			buffer.append(',');
			buffer.append(file.getFullPath());
			buffer.append(')');
			Util.verbose(buffer.toString());
		}
		Set<Entry<String, Map<String, IType>>> packageEntries = secondaryTypesMap.entrySet();
		int packagesSize = packageEntries.size(), removedPackagesCount = 0;
		String[] removedPackages = null;
		Iterator<Entry<String, Map<String, IType>>> packages = packageEntries.iterator();
		while (packages.hasNext()) {
			Entry<String, Map<String, IType>> entry = packages.next();
			String packName = entry.getKey();
			Map<String, IType> types = entry.getValue();
			Set<Entry<String, IType>> nameEntries = types.entrySet();
			int namesSize = nameEntries.size(), removedNamesCount = 0;
			String[] removedNames = null;
			Iterator<Entry<String, IType>> names = nameEntries.iterator();
			while (names.hasNext()) {
				Entry<String, IType> entry2 = names.next();
				String typeName = entry2.getKey();
				JavaElement type = (JavaElement) entry2.getValue();
				if (file.equals(type.resource())) {
					if (removedNames == null) removedNames = new String[namesSize];
					namesSize--;
					removedNames[removedNamesCount++] = typeName;
				}
			}
			if (removedNames != null) {
				for (int i=0; i<removedNamesCount; i++) {
					types.remove(removedNames[i]);
				}
			}
			if (types.size() == 0) {
				if (removedPackages == null) removedPackages = new String[packagesSize];
				packagesSize--;
				removedPackages[removedPackagesCount++] = packName;
			}
		}
		if (removedPackages != null) {
			for (int i=0; i<removedPackagesCount; i++) {
				secondaryTypesMap.remove(removedPackages[i]);
			}
		}
		if (VERBOSE) {
			Util.verbose("	- new secondary types map:"); //$NON-NLS-1$
			Iterator<Entry<String, Map<String, IType>>> entries = secondaryTypesMap.entrySet().iterator();
			while (entries.hasNext()) {
				Entry<String, Map<String, IType>> entry = entries.next();
				String qualifiedName = entry.getKey();
				Util.verbose("		+ "+qualifiedName+':'+ entry.getValue()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Record the order in which to build the java projects (batch build). This order is based
	 * on the projects classpath settings.
	 */
	protected void setBuildOrder(String[] javaBuildOrder) throws JavaModelException {

		// optional behaviour
		// possible value of index 0 is Compute
		if (!JavaCore.COMPUTE.equals(JavaCore.getOption(JavaCore.CORE_JAVA_BUILD_ORDER))) return; // cannot be customized at project level

		if (javaBuildOrder == null || javaBuildOrder.length <= 1) return;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description = workspace.getDescription();
		String[] wksBuildOrder = description.getBuildOrder();

		String[] newOrder;
		if (wksBuildOrder == null){
			newOrder = javaBuildOrder;
		} else {
			// remove projects which are already mentionned in java builder order
			int javaCount = javaBuildOrder.length;
			HashMap<String, String> newSet = new HashMap<>(javaCount); // create a set for fast check
			for (int i = 0; i < javaCount; i++){
				newSet.put(javaBuildOrder[i], javaBuildOrder[i]);
			}
			int removed = 0;
			int oldCount = wksBuildOrder.length;
			for (int i = 0; i < oldCount; i++){
				if (newSet.containsKey(wksBuildOrder[i])){
					wksBuildOrder[i] = null;
					removed++;
				}
			}
			// add Java ones first
			newOrder = new String[oldCount - removed + javaCount];
			System.arraycopy(javaBuildOrder, 0, newOrder, 0, javaCount); // java projects are built first

			// copy previous items in their respective order
			int index = javaCount;
			for (int i = 0; i < oldCount; i++){
				if (wksBuildOrder[i] != null){
					newOrder[index++] = wksBuildOrder[i];
				}
			}
		}
		// commit the new build order out
		description.setBuildOrder(newOrder);
		try {
			workspace.setDescription(description);
		} catch(CoreException e){
			throw new JavaModelException(e);
		}
	}

	/**
	 * Sets the last built state for the given project, or null to reset it.
	 */
	public void setLastBuiltState(IProject project, Object state) {
		if (JavaProject.hasJavaNature(project)) {
			// should never be requested on non-Java projects
			PerProjectInfo info = getPerProjectInfo(project, true /*create if missing*/);
			info.triedRead = true; // no point trying to re-read once using setter
			info.savedState = state;
		}
		if (state == null) { // delete state file to ensure a full build happens if the workspace crashes
			try {
				File file = getSerializationFile(project);
				if (file != null && file.exists())
					file.delete();
			} catch(SecurityException se) {
				// could not delete file: cannot do much more
			}
		}
	}

	/**
	 * Store the preferences value for the given option name.
	 *
	 * @param optionName The name of the option
	 * @param optionValue The value of the option. If <code>null</code>, then
	 * 	the option will be removed from the preferences instead.
	 * @param eclipsePreferences The eclipse preferences to be updated
	 * @param otherOptions more options being stored, used to avoid conflict between deprecated option and its compatible
	 * @return <code>true</code> if the preferences have been changed,
	 * 	<code>false</code> otherwise.
	 */
	public boolean storePreference(String optionName, String optionValue, IEclipsePreferences eclipsePreferences, Map<String, String> otherOptions) {
		int optionLevel = this.getOptionLevel(optionName);
		if (optionLevel == UNKNOWN_OPTION) return false; // unrecognized option

		// Store option value
		switch (optionLevel) {
			case JavaModelManager.VALID_OPTION:
				if (optionValue == null) {
					eclipsePreferences.remove(optionName);
				} else {
					eclipsePreferences.put(optionName, optionValue);
				}
				break;
			case JavaModelManager.DEPRECATED_OPTION:
				// Try to migrate deprecated option
				eclipsePreferences.remove(optionName); // get rid off old preference
				String[] compatibleOptions = this.deprecatedOptions.get(optionName);
				for (int co=0, length=compatibleOptions.length; co < length; co++) {
					if (otherOptions != null && otherOptions.containsKey(compatibleOptions[co]))
						continue; // don't overwrite explicit value of otherOptions at compatibleOptions[co]
					if (optionValue == null) {
						eclipsePreferences.remove(compatibleOptions[co]);
					} else {
						eclipsePreferences.put(compatibleOptions[co], optionValue);
					}
				}
				break;
			default:
				return false;
		}
		return true;
	}

	public void setOptions(Hashtable<String, String> newOptions) {
		Hashtable<String, String> cachedValue = newOptions == null ? null : new Hashtable<>(newOptions);
		IEclipsePreferences defaultPreferences = getDefaultPreferences();
		IEclipsePreferences instancePreferences = getInstancePreferences();

		if (newOptions == null){
			try {
				instancePreferences.clear();
			} catch(BackingStoreException e) {
				// ignore
			}
		} else {
			Enumeration<String> keys = newOptions.keys();
			while (keys.hasMoreElements()){
				String key = keys.nextElement();
				int optionLevel = getOptionLevel(key);
				if (optionLevel == UNKNOWN_OPTION) continue; // unrecognized option
				if (key.equals(JavaCore.CORE_ENCODING)) {
					if (cachedValue != null) {
						cachedValue.put(key, JavaCore.getEncoding());
					}
					continue; // skipped, contributed by resource prefs
				}
				String value = newOptions.get(key);
				String defaultValue = defaultPreferences.get(key, null);
				// Store value in preferences
				if (defaultValue != null && defaultValue.equals(value)) {
					value = null;
				}
				storePreference(key, value, instancePreferences, newOptions);
			}
			try {
				// persist options
				instancePreferences.flush();
			} catch(BackingStoreException e) {
				// ignore
			}
		}
		// update cache
		Util.fixTaskTags(cachedValue);
		this.optionsCache = cachedValue;
	}

	public void startup() throws CoreException {
		try {
			// initialize Java model cache
			this.cache = new JavaModelCache();

			// request state folder creation (workaround 19885)
			JavaCore.getPlugin().getStateLocation();

			// Initialize eclipse preferences
			initializePreferences();

			// Listen to preference changes
			this.propertyListener = new IEclipsePreferences.IPreferenceChangeListener() {
				@Override
				public void preferenceChange(PreferenceChangeEvent event) {
					JavaModelManager.this.optionsCache = null;
				}
			};
			InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID).addPreferenceChangeListener(this.propertyListener);

			// listen for encoding changes (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=255501 )
			this.resourcesPropertyListener = new IEclipsePreferences.IPreferenceChangeListener() {
				@Override
				public void preferenceChange(PreferenceChangeEvent event) {
					if (ResourcesPlugin.PREF_ENCODING.equals(event.getKey())) {
						JavaModelManager.this.optionsCache = null;
					}
				}
			};
			String resourcesPluginId = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
			InstanceScope.INSTANCE.getNode(resourcesPluginId).addPreferenceChangeListener(this.resourcesPropertyListener);

			// Listen to content-type changes
			 Platform.getContentTypeManager().addContentTypeChangeListener(this);

			// retrieve variable values
			long start = -1;
			if (VERBOSE)
				start = System.currentTimeMillis();
			loadVariablesAndContainers();
			if (VERBOSE)
				traceVariableAndContainers("Loaded", start); //$NON-NLS-1$

			// listen for resource changes
			this.deltaState.initializeRootsWithPreviousSession();
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.addResourceChangeListener(
				this.deltaState,
				/* update spec in JavaCore#addPreProcessingResourceChangedListener(...) if adding more event types */
				IResourceChangeEvent.PRE_BUILD
					| IResourceChangeEvent.POST_BUILD
					| IResourceChangeEvent.POST_CHANGE
					| IResourceChangeEvent.PRE_DELETE
					| IResourceChangeEvent.PRE_CLOSE
					| IResourceChangeEvent.PRE_REFRESH);

			// New index is disabled, see bug 544898
			// Indexer.getInstance().addListener(this.deltaState);

			// listen to resource changes affecting external annotations
			ExternalAnnotationTracker.start(workspace);

			startIndexing();

			// process deltas since last activated in indexer thread so that indexes are up-to-date.
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38658
			Job processSavedState = new Job(Messages.savedState_jobName) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						// add save participant and process delta atomically
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=59937
						workspace.run(
							new IWorkspaceRunnable() {
								@Override
								public void run(IProgressMonitor progress) throws CoreException {
									ISavedState savedState = workspace.addSaveParticipant(JavaCore.PLUGIN_ID, JavaModelManager.this);
									if (savedState != null) {
										// the event type coming from the saved state is always POST_AUTO_BUILD
										// force it to be POST_CHANGE so that the delta processor can handle it
										JavaModelManager.this.deltaState.getDeltaProcessor().overridenEventType = IResourceChangeEvent.POST_CHANGE;
										savedState.processResourceChangeEvents(JavaModelManager.this.deltaState);
									}
								}
							},
							monitor);
					} catch (CoreException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			};
			processSavedState.setSystem(true);
			processSavedState.setPriority(Job.SHORT); // process asap
			processSavedState.schedule();
		} catch (RuntimeException e) {
			try {
				shutdown();
			} catch (RuntimeException e2) {
				e.addSuppressed(e2);
			}
			throw e;
		}
	}

	/**
	 * Initiate the background indexing process.
	 * This should be deferred after the plug-in activation.
	 */
	private void startIndexing() {
		if (this.indexManager != null) this.indexManager.reset();
	}

	public void shutdown () {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			Util.log(e, "Could not save JavaCore preferences"); //$NON-NLS-1$
		}
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(this.deltaState);
		workspace.removeSaveParticipant(JavaCore.PLUGIN_ID);

		ExternalAnnotationTracker.shutdown(workspace);

		// Stop listening to content-type changes
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		if (contentTypeManager != null) {
			contentTypeManager.removeContentTypeChangeListener(this);
		}

		// Stop indexing
		if (this.indexManager != null) {
			this.indexManager.shutdown();
		}

		// Stop listening to preferences changes
		preferences.removePreferenceChangeListener(this.propertyListener);
		((IEclipsePreferences) this.preferencesLookup[PREF_DEFAULT].parent()).removeNodeChangeListener(this.defaultNodeListener);
		this.preferencesLookup[PREF_DEFAULT] = null;
		((IEclipsePreferences) this.preferencesLookup[PREF_INSTANCE].parent()).removeNodeChangeListener(this.instanceNodeListener);
		this.preferencesLookup[PREF_INSTANCE].removePreferenceChangeListener(this.instancePreferencesListener);
		this.preferencesLookup[PREF_INSTANCE] = null;
		String resourcesPluginId = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
		InstanceScope.INSTANCE.getNode(resourcesPluginId).removePreferenceChangeListener(this.resourcesPropertyListener);

		// wait for the initialization job to finish
		try {
			Job.getJobManager().join(JavaCore.PLUGIN_ID, null);
		} catch (InterruptedException e) {
			// ignore
		}

		// Note: no need to close the Java model as this just removes Java element infos from the Java model cache
	}

	public synchronized IPath variableGet(String variableName){
		// check initialization in progress first
		Set<String> initializations = variableInitializationInProgress();
		if (initializations.contains(variableName)) {
			return VARIABLE_INITIALIZATION_IN_PROGRESS;
		}
		return this.variables.get(variableName);
	}

	private synchronized IPath variableGetDefaultToPreviousSession(String variableName){
		IPath variablePath = this.variables.get(variableName);
		if (variablePath == null)
			return getPreviousSessionVariable(variableName);
		return variablePath;
	}

	/*
	 * Returns the set of variable names that are being initialized in the current thread.
	 */
	private Set<String> variableInitializationInProgress() {
		Set<String> initializations = this.variableInitializationInProgress.get();
		if (initializations == null) {
			initializations = new HashSet<>();
			this.variableInitializationInProgress.set(initializations);
		}
		return initializations;
	}

	public synchronized String[] variableNames(){
		int length = this.variables.size();
		String[] result = new String[length];
		Iterator<String> vars = this.variables.keySet().iterator();
		int index = 0;
		while (vars.hasNext()) {
			result[index++] = vars.next();
		}
		return result;
	}

	public synchronized void variablePut(String variableName, IPath variablePath){

		// set/unset the initialization in progress
		Set<String> initializations = variableInitializationInProgress();
		if (variablePath == VARIABLE_INITIALIZATION_IN_PROGRESS) {
			initializations.add(variableName);

			// do not write out intermediate initialization value
			return;
		} else {
			initializations.remove(variableName);

			// update cache - do not only rely on listener refresh
			if (variablePath == null) {
				// if path is null, record that the variable was removed to avoid asking the initializer to initialize it again
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=112609
				this.variables.put(variableName, CP_ENTRY_IGNORE_PATH);
				// clean other variables caches
				this.variablesWithInitializer.remove(variableName);
				this.deprecatedVariables.remove(variableName);
			} else {
				this.variables.put(variableName, variablePath);
			}
			// discard obsoleted information about previous session
			this.previousSessionVariables.remove(variableName);
		}
	}

	public void variablePreferencesPut(String variableName, IPath variablePath) {
		String variableKey = CP_VARIABLE_PREFERENCES_PREFIX+variableName;
		if (variablePath == null) {
			getInstancePreferences().remove(variableKey);
		} else {
			getInstancePreferences().put(variableKey, variablePath.toString());
		}
		try {
			getInstancePreferences().flush();
		} catch (BackingStoreException e) {
			// ignore exception
		}
	}

	/*
	 * Optimize startup case where 1 variable is initialized at a time with the same value as on shutdown.
	 */
	public boolean variablePutIfInitializingWithSameValue(String[] variableNames, IPath[] variablePaths) {
		if (variableNames.length != 1)
			return false;
		String variableName = variableNames[0];
		IPath oldPath = variableGetDefaultToPreviousSession(variableName);
		if (oldPath == null)
			return false;
		IPath newPath = variablePaths[0];
		if (!oldPath.equals(newPath))
			return false;
		variablePut(variableName, newPath);
		return true;
	}

	@Override
	public void contentTypeChanged(ContentTypeChangeEvent event) {
		Util.resetJavaLikeExtensions();

		// Walk through projects to reset their secondary types cache
		IJavaProject[] projects;
		try {
			projects = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProjects();
		} catch (JavaModelException e) {
			return;
		}
		for (int i = 0, length = projects.length; i < length; i++) {
			IJavaProject project = projects[i];
			final PerProjectInfo projectInfo = getPerProjectInfo(project.getProject(), false /* don't create info */);
			if (projectInfo != null) {
				projectInfo.secondaryTypes = null;
			}
		}
	}

	public synchronized String cacheToString(String prefix) {
		return this.cache.toStringFillingRation(prefix);
	}

	public ElementCache<ITypeRoot>.Stats debugNewOpenableCacheStats() {
		return this.cache.openableCache.new Stats();
	}

	public int getOpenableCacheSize() {
		return this.cache.openableCache.getSpaceLimit();
	}

	/**
	 * Get a cached access rule, or when the cache did not contain the rule, creates a new one.
	 *
	 * @param filePattern the file pattern this access rule should match
	 * @param kind one of {@link IAccessRule#K_ACCESSIBLE}, {@link IAccessRule#K_DISCOURAGED},
	 *                     or {@link IAccessRule#K_NON_ACCESSIBLE}, optionally combined with
	 *                     {@link IAccessRule#IGNORE_IF_BETTER}
	 * @return an access rule
	 */
	public IAccessRule getAccessRule(IPath filePattern, int kind) {
		ClasspathAccessRule rule = new ClasspathAccessRule(filePattern, kind);
		return getFromCache(rule);
	}

	/**
	 * Used only for loading rules from disk.
	 */
	public ClasspathAccessRule getAccessRuleForProblemId(char [] filePattern, int problemId) {
		ClasspathAccessRule rule = new ClasspathAccessRule(filePattern, problemId);
		return getFromCache(rule);
	}

	private ClasspathAccessRule getFromCache(ClasspathAccessRule rule) {
		return DeduplicationUtil.internObject(rule);
	}
}
