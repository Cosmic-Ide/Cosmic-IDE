/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Ben Konrath <ben@bagu.org> - initial implementation
 *     Red Hat Incorporated - improvements based on comments from JDT developers
 *     IBM Corporation - Code review and integration
 *     IBM Corporation - Fix for 340181
 *******************************************************************************/
package org.eclipse.jdt.core.formatter;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.core.util.Util;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.TextEdit;

/**
 * Implements an Eclipse Application for org.eclipse.jdt.core.JavaCodeFormatter.
 *
 * <p>On MacOS, when invoked using the Eclipse executable, the "user.dir" property is set to the folder
 * in which the eclipse.ini file is located. This makes it harder to use relative paths to point to the
 * files to be formatted or the configuration file to use to set the code formatter's options.</p>
 *
 * <p>There are a couple improvements that could be made: 1. Make a list of all the
 * files first so that a file does not get formatted twice. 2. Use a text based
 * progress monitor for output.</p>
 *
 * @author Ben Konrath <bkonrath@redhat.com>
 * @since 3.2
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings({"rawtypes"})
public class CodeFormatterApplication implements IApplication {

	/**
	 * Deals with the messages in the properties file (cut n' pasted from a
	 * generated class).
	 */
	private final static class Messages extends NLS {
		private static final String BUNDLE_NAME = "org.eclipse.jdt.core.formatter.messages";//$NON-NLS-1$

		public static String CommandLineConfigFile;

		public static String CommandLineDone;

		public static String CommandLineErrorConfig;

		public static String CommandLineErrorFileTryFullPath;

		public static String CommandLineErrorFile;

		public static String CommandLineErrorFileDir;

		public static String CommandLineErrorQuietVerbose;

		public static String CommandLineErrorNoConfigFile;

		public static String CommandLineFormatting;

		public static String CommandLineStart;

		public static String CommandLineUsage;

		public static String ConfigFileNotFoundErrorTryFullPath;

		public static String ConfigFileReadingError;

		public static String FormatProblem;

		public static String CaughtException;

		public static String ExceptionSkip;

		static {
			NLS.initializeMessages(BUNDLE_NAME, Messages.class);
		}

		/**
		 * Bind the given message's substitution locations with the given string
		 * values.
		 *
		 * @param message
		 *            the message to be manipulated
		 * @return the manipulated String
		 */
		public static String bind(String message) {
			return bind(message, null);
		}

		/**
		 * Bind the given message's substitution locations with the given string
		 * values.
		 *
		 * @param message
		 *            the message to be manipulated
		 * @param binding
		 *            the object to be inserted into the message
		 * @return the manipulated String
		 */
		public static String bind(String message, Object binding) {
			return bind(message, new Object[] {
				binding
			});
		}

		/**
		 * Bind the given message's substitution locations with the given string
		 * values.
		 *
		 * @param message
		 *            the message to be manipulated
		 * @param binding1
		 *            An object to be inserted into the message
		 * @param binding2
		 *            A second object to be inserted into the message
		 * @return the manipulated String
		 */
		public static String bind(String message, Object binding1, Object binding2) {
			return bind(message, new Object[] {
					binding1, binding2
			});
		}

		/**
		 * Bind the given message's substitution locations with the given string
		 * values.
		 *
		 * @param message
		 *            the message to be manipulated
		 * @param bindings
		 *            An array of objects to be inserted into the message
		 * @return the manipulated String
		 */
		public static String bind(String message, Object[] bindings) {
			return MessageFormat.format(message, bindings);
		}
	}

	private static final String ARG_CONFIG = "-config"; //$NON-NLS-1$

	private static final String ARG_HELP = "-help"; //$NON-NLS-1$

	private static final String ARG_QUIET = "-quiet"; //$NON-NLS-1$

	private static final String ARG_VERBOSE = "-verbose"; //$NON-NLS-1$

	private String configName;

	private Map options = null;

	private static final String PDE_LAUNCH = "-pdelaunch"; //$NON-NLS-1$

	private boolean quiet = false;

	private boolean verbose = false;

	/**
	 * Display the command line usage message.
	 */
	private void displayHelp() {
		System.out.println(Messages.bind(Messages.CommandLineUsage));
	}

	private void displayHelp(String message) {
		System.err.println(message);
		System.out.println();
		displayHelp();
	}

	/**
	 * Recursively format the Java source code that is contained in the
	 * directory rooted at dir.
	 */
	private void formatDirTree(File dir, CodeFormatter codeFormatter) {

		File[] files = dir.listFiles();
		if (files == null)
			return;

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				formatDirTree(file, codeFormatter);
			} else if (Util.isJavaLikeFileName(file.getPath())) {
				formatFile(file, codeFormatter);
			}
		}
	}

	/**
	 * Format the given Java source file.
	 */
	private void formatFile(File file, CodeFormatter codeFormatter) {
		IDocument doc = new Document();
		try {
			// read the file
			if (this.verbose) {
				System.out.println(Messages.bind(Messages.CommandLineFormatting, file.getAbsolutePath()));
			}
			String contents = new String(org.eclipse.jdt.internal.compiler.util.Util.getFileCharContent(file, null));
			// format the file (the meat and potatoes)
			doc.set(contents);
			int kind = (file.getName().equals(IModule.MODULE_INFO_JAVA)? CodeFormatter.K_MODULE_INFO
					: CodeFormatter.K_COMPILATION_UNIT) | CodeFormatter.F_INCLUDE_COMMENTS;
			TextEdit edit = codeFormatter.format(kind, contents, 0, contents.length(), 0, null);
			if (edit != null) {
				edit.apply(doc);
			} else {
				System.err.println(Messages.bind(Messages.FormatProblem, file.getAbsolutePath()));
				return;
			}

			// write the file
			final BufferedWriter out = new BufferedWriter(new FileWriter(file));
			try {
				out.write(doc.get());
				out.flush();
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
		} catch (IOException e) {
			String errorMessage = Messages.bind(Messages.CaughtException, "IOException", e.getLocalizedMessage()); //$NON-NLS-1$
			Util.log(e, errorMessage);
			System.err.println(Messages.bind(Messages.ExceptionSkip ,errorMessage));
		} catch (BadLocationException e) {
			String errorMessage = Messages.bind(Messages.CaughtException, "BadLocationException", e.getLocalizedMessage()); //$NON-NLS-1$
			Util.log(e, errorMessage);
			System.err.println(Messages.bind(Messages.ExceptionSkip ,errorMessage));
		}
	}

	private File[] processCommandLine(String[] argsArray) {

		int index = 0;
		final int argCount = argsArray.length;

		final int DEFAULT_MODE = 0;
		final int CONFIG_MODE = 1;

		int mode = DEFAULT_MODE;
		final int INITIAL_SIZE = 1;
		int fileCounter = 0;

		File[] filesToFormat = new File[INITIAL_SIZE];

		loop: while (index < argCount) {
			String currentArg = argsArray[index++];

			switch(mode) {
				case DEFAULT_MODE :
					if (PDE_LAUNCH.equals(currentArg)) {
						continue loop;
					}
					if (ARG_HELP.equals(currentArg)) {
						displayHelp();
						return null;
					}
					if (ARG_VERBOSE.equals(currentArg)) {
						this.verbose = true;
						continue loop;
					}
					if (ARG_QUIET.equals(currentArg)) {
						this.quiet = true;
						continue loop;
					}
					if (ARG_CONFIG.equals(currentArg)) {
						mode = CONFIG_MODE;
						continue loop;
					}
					// the current arg should be a file or a directory name
					File file = new File(currentArg);
					if (file.exists()) {
						if (filesToFormat.length == fileCounter) {
							System.arraycopy(filesToFormat, 0, (filesToFormat = new File[fileCounter * 2]), 0, fileCounter);
						}
						filesToFormat[fileCounter++] = file;
					} else {
						String canonicalPath;
						try {
							canonicalPath = file.getCanonicalPath();
						} catch(IOException e2) {
							canonicalPath = file.getAbsolutePath();
						}
						String errorMsg = file.isAbsolute()?
										  Messages.bind(Messages.CommandLineErrorFile, canonicalPath):
										  Messages.bind(Messages.CommandLineErrorFileTryFullPath, canonicalPath);
						displayHelp(errorMsg);
						return null;
					}
					break;
				case CONFIG_MODE :
					this.configName = currentArg;
					this.options = readConfig(currentArg);
					if (this.options == null) {
						displayHelp(Messages.bind(Messages.CommandLineErrorConfig, currentArg));
						return null;
					}
					mode = DEFAULT_MODE;
					continue loop;
			}
		}

		if (mode == CONFIG_MODE || this.options == null) {
			displayHelp(Messages.bind(Messages.CommandLineErrorNoConfigFile));
			return null;
		}
		if (this.quiet && this.verbose) {
			displayHelp(
				Messages.bind(
					Messages.CommandLineErrorQuietVerbose,
					new String[] { ARG_QUIET, ARG_VERBOSE }
				));
			return null;
		}
		if (fileCounter == 0) {
			displayHelp(Messages.bind(Messages.CommandLineErrorFileDir));
			return null;
		}
		if (filesToFormat.length != fileCounter) {
			System.arraycopy(filesToFormat, 0, (filesToFormat = new File[fileCounter]), 0, fileCounter);
		}
		return filesToFormat;
	}

	/**
	 * Return a Java Properties file representing the options that are in the
	 * specified configuration file.
	 */
	private Properties readConfig(String filename) {
		BufferedInputStream stream = null;
		File configFile = new File(filename);
		try {
			stream = new BufferedInputStream(new FileInputStream(configFile));
			final Properties formatterOptions = new Properties();
			formatterOptions.load(stream);
			return formatterOptions;
		} catch (IOException e) {
			String canonicalPath = null;
			try {
				canonicalPath = configFile.getCanonicalPath();
			} catch(IOException e2) {
				canonicalPath = configFile.getAbsolutePath();
			}
			String errorMessage;
			if (!configFile.exists() && !configFile.isAbsolute()) {
				errorMessage = Messages.bind(Messages.ConfigFileNotFoundErrorTryFullPath, new Object[] {
					canonicalPath,
					System.getProperty("user.dir") //$NON-NLS-1$
				});

			} else {
				errorMessage = Messages.bind(Messages.ConfigFileReadingError, canonicalPath);
			}
			Util.log(e, errorMessage);
			System.err.println(errorMessage);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
		}
		return null;
	}

	/**
	 * Runs the Java code formatter application
	 */
	@Override
	public Object start(IApplicationContext context) throws Exception {
		File[] filesToFormat = processCommandLine((String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS));

		if (filesToFormat == null) {
			return IApplication.EXIT_OK;
		}

		if (!this.quiet) {
			if (this.configName != null) {
				System.out.println(Messages.bind(Messages.CommandLineConfigFile, this.configName));
			}
			System.out.println(Messages.bind(Messages.CommandLineStart));
		}

		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(this.options,
				ToolFactory.M_FORMAT_EXISTING);
		// format the list of files and/or directories
		for (int i = 0, max = filesToFormat.length; i < max; i++) {
			final File file = filesToFormat[i];
			if (file.isDirectory()) {
				formatDirTree(file, codeFormatter);
			} else if (Util.isJavaLikeFileName(file.getPath())) {
				formatFile(file, codeFormatter);
			}
		}
		if (!this.quiet) {
			System.out.println(Messages.bind(Messages.CommandLineDone));
		}

		return IApplication.EXIT_OK;
	}
	@Override
	public void stop() {
		// do nothing
	}
}
