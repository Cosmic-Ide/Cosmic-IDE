/*******************************************************************************
 *  Copyright (c) 2011, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.index;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.util.NLS;

/**
 * Implements an Eclipse Application for {@link org.eclipse.jdt.core.index.JavaIndexer}.
 *
 * <p>
 * On MacOS, when invoked using the Eclipse executable, the "user.dir" property is set to the folder in which the
 * eclipse.ini file is located. This makes it harder to use relative paths to point to the files to be jar'd or to
 * the index file that is generated.
 * </p>
 *
 *
 * @since 3.8
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JavaIndexerApplication implements IApplication {

	private final static class Messages extends NLS {
		private static final String MESSAGES_NAME = "org.eclipse.jdt.core.index.messages";//$NON-NLS-1$

		public static String CommandLineProcessing;
		public static String CommandLineUsage;
		public static String CommandLineOnlyOneOutputError;
		public static String CommandLineOutputTakesArgs;
		public static String CommandLineOnlyOneJarError;
		public static String CommandLineJarNotSpecified;
		public static String CommandLineIndexFileNotSpecified;
		public static String CaughtException;
		public static String CommandLineJarFileNotExist;

		static {
			NLS.initializeMessages(MESSAGES_NAME, Messages.class);
		}

		public static String bind(String message) {
			return bind(message, null);
		}

		public static String bind(String message, Object binding) {
			return bind(message, new Object[] { binding });
		}

		public static String bind(String message, Object binding1, Object binding2) {
			return bind(message, new Object[] { binding1, binding2 });
		}

		public static String bind(String message, Object[] bindings) {
			return MessageFormat.format(message, bindings);
		}
	}

	private String jarToIndex;
	private String indexFile;
	private boolean verbose = false;
	private static final String PDE_LAUNCH = "-pdelaunch"; //$NON-NLS-1$
	private static final String ARG_HELP = "-help"; //$NON-NLS-1$
	private static final String ARG_VERBOSE = "-verbose"; //$NON-NLS-1$
	private static final String ARG_OUTPUT = "-output"; //$NON-NLS-1$

	private void displayHelp() {
		System.out.println(Messages.bind(Messages.CommandLineUsage));
	}

	private void displayError(String message) {
		System.out.println(message);
		System.out.println();
		displayHelp();
	}

	private boolean processCommandLine(String[] argsArray) {
		ArrayList args = new ArrayList();
		for (int i = 0, max = argsArray.length; i < max; i++) {
			args.add(argsArray[i]);
		}
		int index = 0;
		final int argCount = argsArray.length;

		loop: while (index < argCount) {
			String currentArg = argsArray[index++];
			if (PDE_LAUNCH.equals(currentArg)) {
				continue loop;
			} else if (ARG_HELP.equals(currentArg)) {
				displayHelp();
				return false;
			} else if (ARG_VERBOSE.equals(currentArg)) {
				this.verbose = true;
				continue loop;
			} else if (ARG_OUTPUT.equals(currentArg)) {
				if (this.indexFile != null) {
					displayError(Messages.bind(Messages.CommandLineOnlyOneOutputError));
					return false;
				} else if (index == argCount) {
					displayError(Messages.bind(Messages.CommandLineOutputTakesArgs));
					return false;
				}
				this.indexFile = argsArray[index++];
			} else {
				if (this.jarToIndex != null) {
					displayError(Messages.bind(Messages.CommandLineOnlyOneJarError));
					return false;
				}
				this.jarToIndex = currentArg;
			}
		}
		return true;
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		boolean execute = processCommandLine((String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
		if (execute) {
			if (this.jarToIndex != null && this.indexFile != null) {
				File f = new File(this.jarToIndex);
				if (f.exists()) {
					if (this.verbose) {
						System.out.println(Messages.bind(Messages.CommandLineProcessing, this.indexFile, this.jarToIndex));
					}
					try {
						JavaIndexer.generateIndexForJar(this.jarToIndex, this.indexFile);
					} catch (IOException e) {
						System.out.println(Messages.bind(Messages.CaughtException, "IOException", e.getLocalizedMessage())); //$NON-NLS-1$
					}
				} else {
						System.out.println(Messages.bind(Messages.CommandLineJarFileNotExist, this.jarToIndex));
				}
			} else if (this.jarToIndex == null) {
				System.out.println(Messages.bind(Messages.CommandLineJarNotSpecified));
			} else if (this.indexFile == null) {
				System.out.println(Messages.bind(Messages.CommandLineIndexFileNotSpecified));
			}
		}
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// do nothing
	}

}
