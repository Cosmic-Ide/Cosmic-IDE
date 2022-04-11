/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Tom Tromey - Contribution for bug 125961
 *     Tom Tromey - Contribution for bug 159641
 *     Benjamin Muskalla - Contribution for bug 239066
 *     Stephan Herrmann  - Contributions for
 *     							bug 236385 - [compiler] Warn for potential programming problem if an object is created but not used
 *     							bug 295551 - Add option to automatically promote all warnings to errors
 *     							bug 359721 - [options] add command line option for new warning token "resource"
 *								bug 365208 - [compiler][batch] command line options for annotation based null analysis
 *								bug 374605 - Unreasonable warning for enum-based switch statements
 *								bug 375366 - ECJ ignores unusedParameterIncludeDocCommentReference unless enableJavadoc option is set
 *								bug 388281 - [compiler][null] inheritance of null annotations as an option
 *								bug 381443 - [compiler][null] Allow parameter widening from @NonNull to unannotated
 *								Bug 440477 - [null] Infrastructure for feeding external annotations into compilation
 *								Bug 440687 - [compiler][batch][null] improve command line option for external annotations
 *								Bug 408815 - [batch][null] Add CLI option for COMPILER_PB_SYNTACTIC_NULL_ANALYSIS_FOR_FIELDS
 *     Jesper S Moller   - Contributions for
 *								bug 407297 - [1.8][compiler] Control generation of parameter names by option
 *    Mat Booth - Contribution for bug 405176
 *    Frits Jalvingh - fix for bug 533830.
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.batch;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Function;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;
import org.eclipse.jdt.internal.compiler.AbstractAnnotationProcessorManager;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.FileSystem.Classpath;
import org.eclipse.jdt.internal.compiler.batch.ModuleFinder.AddExport;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.AccessRule;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IPackageExport;
import org.eclipse.jdt.internal.compiler.env.IUpdatableModule.UpdateKind;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.CompilerStats;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.GenericXMLWriter;
import org.eclipse.jdt.internal.compiler.util.HashtableOfInt;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.jdt.internal.compiler.util.Messages;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;

public class Main implements ProblemSeverities, SuffixConstants {

	public static class Logger {
		private PrintWriter err;
		private PrintWriter log;
		private Main main;
		private PrintWriter out;
		int tagBits;
		private static final String CLASS = "class"; //$NON-NLS-1$
		private static final String CLASS_FILE = "classfile"; //$NON-NLS-1$
		private static final String CLASSPATH = "classpath"; //$NON-NLS-1$
		private static final String CLASSPATH_FILE = "FILE"; //$NON-NLS-1$
		private static final String CLASSPATH_FOLDER = "FOLDER"; //$NON-NLS-1$
		private static final String CLASSPATH_ID = "id"; //$NON-NLS-1$
		private static final String CLASSPATH_JAR = "JAR"; //$NON-NLS-1$
		private static final String CLASSPATHS = "classpaths"; //$NON-NLS-1$
		private static final String COMMAND_LINE_ARGUMENT = "argument"; //$NON-NLS-1$
		private static final String COMMAND_LINE_ARGUMENTS = "command_line"; //$NON-NLS-1$
		private static final String COMPILER = "compiler"; //$NON-NLS-1$
		private static final String COMPILER_COPYRIGHT = "copyright"; //$NON-NLS-1$
		private static final String COMPILER_NAME = "name"; //$NON-NLS-1$
		private static final String COMPILER_VERSION = "version"; //$NON-NLS-1$
		public static final int EMACS = 2;
		private static final String ERROR = "ERROR"; //$NON-NLS-1$
		private static final String ERROR_TAG = "error"; //$NON-NLS-1$
		private static final String WARNING_TAG = "warning"; //$NON-NLS-1$
		private static final String EXCEPTION = "exception"; //$NON-NLS-1$
		private static final String EXTRA_PROBLEM_TAG = "extra_problem"; //$NON-NLS-1$
		private static final String EXTRA_PROBLEMS = "extra_problems"; //$NON-NLS-1$
		private static final HashtableOfInt FIELD_TABLE = new HashtableOfInt();
		private static final String KEY = "key"; //$NON-NLS-1$
		private static final String MESSAGE = "message"; //$NON-NLS-1$
		private static final String NUMBER_OF_CLASSFILES = "number_of_classfiles"; //$NON-NLS-1$
		private static final String NUMBER_OF_ERRORS = "errors"; //$NON-NLS-1$
		private static final String NUMBER_OF_LINES = "number_of_lines"; //$NON-NLS-1$
		private static final String NUMBER_OF_PROBLEMS = "problems"; //$NON-NLS-1$
		private static final String NUMBER_OF_TASKS = "tasks"; //$NON-NLS-1$
		private static final String NUMBER_OF_WARNINGS = "warnings"; //$NON-NLS-1$
		private static final String NUMBER_OF_INFOS = "infos"; //$NON-NLS-1$
		private static final String OPTION = "option"; //$NON-NLS-1$
		private static final String OPTIONS = "options"; //$NON-NLS-1$
		private static final String OUTPUT = "output"; //$NON-NLS-1$
		private static final String PACKAGE = "package"; //$NON-NLS-1$
		private static final String PATH = "path"; //$NON-NLS-1$
		private static final String PROBLEM_ARGUMENT = "argument"; //$NON-NLS-1$
		private static final String PROBLEM_ARGUMENT_VALUE = "value"; //$NON-NLS-1$
		private static final String PROBLEM_ARGUMENTS = "arguments"; //$NON-NLS-1$
		private static final String PROBLEM_CATEGORY_ID = "categoryID"; //$NON-NLS-1$
		private static final String ID = "id"; //$NON-NLS-1$
		private static final String PROBLEM_ID = "problemID"; //$NON-NLS-1$
		private static final String PROBLEM_LINE = "line"; //$NON-NLS-1$
		private static final String PROBLEM_OPTION_KEY = "optionKey"; //$NON-NLS-1$
		private static final String PROBLEM_MESSAGE = "message"; //$NON-NLS-1$
		private static final String PROBLEM_SEVERITY = "severity"; //$NON-NLS-1$
		private static final String PROBLEM_SOURCE_END = "charEnd"; //$NON-NLS-1$
		private static final String PROBLEM_SOURCE_START = "charStart"; //$NON-NLS-1$
		private static final String PROBLEM_SUMMARY = "problem_summary"; //$NON-NLS-1$
		private static final String PROBLEM_TAG = "problem"; //$NON-NLS-1$
		private static final String PROBLEMS = "problems"; //$NON-NLS-1$
		private static final String SOURCE = "source"; //$NON-NLS-1$
		private static final String SOURCE_CONTEXT = "source_context"; //$NON-NLS-1$
		private static final String SOURCE_END = "sourceEnd"; //$NON-NLS-1$
		private static final String SOURCE_START = "sourceStart"; //$NON-NLS-1$
		private static final String SOURCES = "sources"; //$NON-NLS-1$

		private static final String STATS = "stats"; //$NON-NLS-1$

		private static final String TASK = "task"; //$NON-NLS-1$
		private static final String TASKS = "tasks"; //$NON-NLS-1$
		private static final String TIME = "time"; //$NON-NLS-1$
		private static final String VALUE = "value"; //$NON-NLS-1$
		private static final String WARNING = "WARNING"; //$NON-NLS-1$
		private static final String INFO = "INFO"; //$NON-NLS-1$

		public static final int XML = 1;
		private static final String XML_DTD_DECLARATION = "<!DOCTYPE compiler PUBLIC \"-//Eclipse.org//DTD Eclipse JDT 3.2.006 Compiler//EN\" \"http://www.eclipse.org/jdt/core/compiler_32_006.dtd\">"; //$NON-NLS-1$
		static {
			try {
				Class<?> c = IProblem.class;
				Field[] fields = c.getFields();
				for (int i = 0, max = fields.length; i < max; i++) {
					Field field = fields[i];
					if (field.getType().equals(Integer.TYPE)) {
						Integer value = (Integer) field.get(null);
						int key2 = value.intValue() & IProblem.IgnoreCategoriesMask;
						if (key2 == 0) {
							key2 = Integer.MAX_VALUE;
						}
						Logger.FIELD_TABLE.put(key2, field.getName());
					}
				}
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		public Logger(Main main, PrintWriter out, PrintWriter err) {
			this.out = out;
			this.err = err;
			this.main = main;
		}

		public String buildFileName(
			String outputPath,
			String relativeFileName) {
			char fileSeparatorChar = File.separatorChar;
			String fileSeparator = File.separator;

			outputPath = outputPath.replace('/', fileSeparatorChar);
			// To be able to pass the mkdirs() method we need to remove the extra file separator at the end of the outDir name
			StringBuilder outDir = new StringBuilder(outputPath);
			if (!outputPath.endsWith(fileSeparator)) {
				outDir.append(fileSeparator);
			}
			StringTokenizer tokenizer =
				new StringTokenizer(relativeFileName, fileSeparator);
			String token = tokenizer.nextToken();
			while (tokenizer.hasMoreTokens()) {
				outDir.append(token).append(fileSeparator);
				token = tokenizer.nextToken();
			}
			// token contains the last one
			return outDir.append(token).toString();
		}

		public void close() {
			if (this.log != null) {
				if ((this.tagBits & Logger.XML) != 0) {
					endTag(Logger.COMPILER);
					flush();
				}
				this.log.close();
			}
		}

		/**
		 *
		 */
		public void compiling() {
			printlnOut(this.main.bind("progress.compiling")); //$NON-NLS-1$
		}
		private void endLoggingExtraProblems() {
			endTag(Logger.EXTRA_PROBLEMS);
		}
		/**
		 * Used to stop logging problems.
		 * Only use in xml mode.
		 */
		private void endLoggingProblems() {
			endTag(Logger.PROBLEMS);
		}
		public void endLoggingSource() {
			if ((this.tagBits & Logger.XML) != 0) {
				endTag(Logger.SOURCE);
			}
		}

		public void endLoggingSources() {
			if ((this.tagBits & Logger.XML) != 0) {
				endTag(Logger.SOURCES);
			}
		}

		public void endLoggingTasks() {
			if ((this.tagBits & Logger.XML) != 0) {
				endTag(Logger.TASKS);
			}
		}
		private void endTag(String name) {
			if (this.log != null) {
				((GenericXMLWriter) this.log).endTag(name, true, true);
			}
		}
		private String errorReportSource(CategorizedProblem problem, char[] unitSource, int bits) {
			//extra from the source the innacurate     token
			//and "highlight" it using some underneath ^^^^^
			//put some context around too.

			//this code assumes that the font used in the console is fixed size

			//sanity .....
			int startPosition = problem.getSourceStart();
			int endPosition = problem.getSourceEnd();
			if (unitSource == null) {
				if (problem.getOriginatingFileName() != null) {
					try {
						unitSource = Util.getFileCharContent(new File(new String(problem.getOriginatingFileName())), null);
					} catch (IOException e) {
						// ignore;
					}
				}
			}
			int length;
			if ((startPosition > endPosition)
				|| ((startPosition < 0) && (endPosition < 0))
				|| (unitSource == null)
				|| (length = unitSource.length) == 0)
				return Messages.problem_noSourceInformation;

			StringBuilder errorBuffer = new StringBuilder();
			if ((bits & Main.Logger.EMACS) == 0) {
				errorBuffer.append(' ').append(Messages.bind(Messages.problem_atLine, String.valueOf(problem.getSourceLineNumber())));
				errorBuffer.append(Util.LINE_SEPARATOR);
			}
			errorBuffer.append('\t');

			char c;
			final char SPACE = '\u0020';
			final char MARK = '^';
			final char TAB = '\t';
			//the next code tries to underline the token.....
			//it assumes (for a good display) that token source does not
			//contain any \r \n. This is false on statements !
			//(the code still works but the display is not optimal !)

			// expand to line limits
			int begin;
			int end;
			for (begin = startPosition >= length ? length - 1 : startPosition; begin > 0; begin--) {
				if ((c = unitSource[begin - 1]) == '\n' || c == '\r') break;
			}
			for (end = endPosition >= length ? length - 1 : endPosition ; end+1 < length; end++) {
				if ((c = unitSource[end + 1]) == '\r' || c == '\n') break;
			}

			// trim left and right spaces/tabs
			while ((c = unitSource[begin]) == ' ' || c == '\t') begin++;
			//while ((c = unitSource[end]) == ' ' || c == '\t') end--; TODO (philippe) should also trim right, but all tests are to be updated

			// copy source
			errorBuffer.append(unitSource, begin, end-begin+1);
			errorBuffer.append(Util.LINE_SEPARATOR).append("\t"); //$NON-NLS-1$

			// compute underline
			for (int i = begin; i <startPosition; i++) {
				errorBuffer.append((unitSource[i] == TAB) ? TAB : SPACE);
			}
			for (int i = startPosition; i <= (endPosition >= length ? length - 1 : endPosition); i++) {
				errorBuffer.append(MARK);
			}
			return errorBuffer.toString();
		}

		private void extractContext(CategorizedProblem problem, char[] unitSource) {
			//sanity .....
			int startPosition = problem.getSourceStart();
			int endPosition = problem.getSourceEnd();
			if (unitSource == null) {
				if (problem.getOriginatingFileName() != null) {
					try {
						unitSource = Util.getFileCharContent(new File(new String(problem.getOriginatingFileName())), null);
					} catch(IOException e) {
						// ignore
					}
				}
			}
			int length;
			if ((startPosition > endPosition)
					|| ((startPosition < 0) && (endPosition < 0))
					|| (unitSource == null)
					|| ((length = unitSource.length) <= 0)
					|| (endPosition > length)) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.VALUE, Messages.problem_noSourceInformation);
				parameters.put(Logger.SOURCE_START, "-1"); //$NON-NLS-1$
				parameters.put(Logger.SOURCE_END, "-1"); //$NON-NLS-1$
				printTag(Logger.SOURCE_CONTEXT, parameters, true, true);
				return;
			}

			char c;
			//the next code tries to underline the token.....
			//it assumes (for a good display) that token source does not
			//contain any \r \n. This is false on statements !
			//(the code still works but the display is not optimal !)

			// expand to line limits
			int begin, end;
			for (begin = startPosition >= length ? length - 1 : startPosition; begin > 0; begin--) {
				if ((c = unitSource[begin - 1]) == '\n' || c == '\r') break;
			}
			for (end = endPosition >= length ? length - 1 : endPosition ; end+1 < length; end++) {
				if ((c = unitSource[end + 1]) == '\r' || c == '\n') break;
			}

			// trim left and right spaces/tabs
			while ((c = unitSource[begin]) == ' ' || c == '\t') begin++;
			while ((c = unitSource[end]) == ' ' || c == '\t') end--;

			// copy source
			StringBuffer buffer = new StringBuffer();
			buffer.append(unitSource, begin, end - begin + 1);
			HashMap<String, Object> parameters = new HashMap<>();
			parameters.put(Logger.VALUE, String.valueOf(buffer));
			parameters.put(Logger.SOURCE_START, Integer.toString(startPosition - begin));
			parameters.put(Logger.SOURCE_END, Integer.toString(endPosition - begin));
			printTag(Logger.SOURCE_CONTEXT, parameters, true, true);
		}
		public void flush() {
			this.out.flush();
			this.err.flush();
			if (this.log != null) {
				this.log.flush();
			}
		}

		private String getFieldName(int id) {
			int key2 = id & IProblem.IgnoreCategoriesMask;
			if (key2 == 0) {
				key2 = Integer.MAX_VALUE;
			}
			return (String) Logger.FIELD_TABLE.get(key2);
		}

		// find out an option name controlling a given problemID
		private String getProblemOptionKey(int problemID) {
			int irritant = ProblemReporter.getIrritant(problemID);
			return CompilerOptions.optionKeyFromIrritant(irritant);
		}

		public void logAverage() {
			Arrays.sort(this.main.compilerStats);
			long lineCount = this.main.compilerStats[0].lineCount;
			final int length = this.main.maxRepetition;
			long sum = 0;
			long parseSum = 0, resolveSum = 0, analyzeSum = 0, generateSum = 0;
			for (int i = 1, max = length - 1; i < max; i++) {
				CompilerStats stats = this.main.compilerStats[i];
				sum += stats.elapsedTime();
				parseSum += stats.parseTime;
				resolveSum += stats.resolveTime;
				analyzeSum += stats.analyzeTime;
				generateSum += stats.generateTime;
			}
			long time = sum / (length - 2);
			long parseTime = parseSum/(length - 2);
			long resolveTime = resolveSum/(length - 2);
			long analyzeTime = analyzeSum/(length - 2);
			long generateTime = generateSum/(length - 2);
			printlnOut(this.main.bind(
				"compile.averageTime", //$NON-NLS-1$
				new String[] {
					String.valueOf(lineCount),
					String.valueOf(time),
					String.valueOf(((int) (lineCount * 10000.0 / time)) / 10.0),
				}));
			if ((this.main.timing & Main.TIMING_DETAILED) != 0) {
				printlnOut(
						this.main.bind("compile.detailedTime", //$NON-NLS-1$
							new String[] {
								String.valueOf(parseTime),
								String.valueOf(((int) (parseTime * 1000.0 / time)) / 10.0),
								String.valueOf(resolveTime),
								String.valueOf(((int) (resolveTime * 1000.0 / time)) / 10.0),
								String.valueOf(analyzeTime),
								String.valueOf(((int) (analyzeTime * 1000.0 / time)) / 10.0),
								String.valueOf(generateTime),
								String.valueOf(((int) (generateTime * 1000.0 / time)) / 10.0),
							}));
			}
		}
		public void logClassFile(boolean generatePackagesStructure, String outputPath, String relativeFileName) {
			if ((this.tagBits & Logger.XML) != 0) {
				String fileName = null;
				if (generatePackagesStructure) {
					fileName = buildFileName(outputPath, relativeFileName);
				} else {
					char fileSeparatorChar = File.separatorChar;
					String fileSeparator = File.separator;
					// First we ensure that the outputPath exists
					outputPath = outputPath.replace('/', fileSeparatorChar);
					// To be able to pass the mkdirs() method we need to remove the extra file separator at the end of the outDir name
					int indexOfPackageSeparator = relativeFileName.lastIndexOf(fileSeparatorChar);
					if (indexOfPackageSeparator == -1) {
						if (outputPath.endsWith(fileSeparator)) {
							fileName = outputPath + relativeFileName;
						} else {
							fileName = outputPath + fileSeparator + relativeFileName;
						}
					} else {
						int length = relativeFileName.length();
						if (outputPath.endsWith(fileSeparator)) {
							fileName = outputPath + relativeFileName.substring(indexOfPackageSeparator + 1, length);
						} else {
							fileName = outputPath + fileSeparator + relativeFileName.substring(indexOfPackageSeparator + 1, length);
						}
					}
				}
				File f = new File(fileName);
				try {
					HashMap<String, Object> parameters = new HashMap<>();
					parameters.put(Logger.PATH, f.getCanonicalPath());
					printTag(Logger.CLASS_FILE, parameters, true, true);
				} catch (IOException e) {
					logNoClassFileCreated(outputPath, relativeFileName, e);
				}
			}
		}
		public void logClasspath(FileSystem.Classpath[] classpaths) {
			if (classpaths == null) return;
			if ((this.tagBits & Logger.XML) != 0) {
				final int length = classpaths.length;
				if (length != 0) {
					// generate xml output
					printTag(Logger.CLASSPATHS, new HashMap<>(), true, false);
					HashMap<String, Object> parameters = new HashMap<>();
					for (int i = 0; i < length; i++) {
						String classpath = classpaths[i].getPath();
						parameters.put(Logger.PATH, classpath);
						File f = new File(classpath);
						String id = null;
						if (f.isFile()) {
							int kind = Util.archiveFormat(classpath);
							switch (kind) {
								case Util.ZIP_FILE:
									id = Logger.CLASSPATH_JAR;
									break;
								default:
									id = Logger.CLASSPATH_FILE;
									break;
							}
						} else if (f.isDirectory()) {
							id = Logger.CLASSPATH_FOLDER;
						}
						if (id != null) {
							parameters.put(Logger.CLASSPATH_ID, id);
							printTag(Logger.CLASSPATH, parameters, true, true);
						}
					}
					endTag(Logger.CLASSPATHS);
				}
			}

		}

		public void logCommandLineArguments(String[] commandLineArguments) {
			if (commandLineArguments == null) return;
			if ((this.tagBits & Logger.XML) != 0) {
				final int length = commandLineArguments.length;
				if (length != 0) {
					// generate xml output
					printTag(Logger.COMMAND_LINE_ARGUMENTS, new HashMap<>(), true, false);
					for (int i = 0; i < length; i++) {
						HashMap<String, Object> parameters = new HashMap<>();
						parameters.put(Logger.VALUE, commandLineArguments[i]);
						printTag(Logger.COMMAND_LINE_ARGUMENT, parameters, true, true);
					}
					endTag(Logger.COMMAND_LINE_ARGUMENTS);
				}
			}
		}

		/**
		 * @param e the given exception to log
		 */
		public void logException(Exception e) {
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			e.printStackTrace(printWriter);
			printWriter.flush();
			printWriter.close();
			final String stackTrace = writer.toString();
			if ((this.tagBits & Logger.XML) != 0) {
				LineNumberReader reader = new LineNumberReader(new StringReader(stackTrace));
				String line;
				int i = 0;
				StringBuilder buffer = new StringBuilder();
				String message = e.getMessage();
				if (message != null) {
					buffer.append(message).append(Util.LINE_SEPARATOR);
				}
				try {
					while ((line = reader.readLine()) != null && i < 4) {
						buffer.append(line).append(Util.LINE_SEPARATOR);
						i++;
					}
					reader.close();
				} catch (IOException e1) {
					// ignore
				}
				message = buffer.toString();
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.MESSAGE, message);
				parameters.put(Logger.CLASS, e.getClass());
				printTag(Logger.EXCEPTION, parameters, true, true);
			}
			String message = e.getMessage();
			if (message == null) {
				this.printlnErr(stackTrace);
			} else {
				this.printlnErr(message);
			}
		}

		private void logExtraProblem(CategorizedProblem problem, int localErrorCount, int globalErrorCount) {
			char[] originatingFileName = problem.getOriginatingFileName();
			if (originatingFileName == null) {
				// simplified message output
				String severity = problem.isError() ? "requestor.extraerror" //$NON-NLS-1$
						: problem.isInfo() ? "requestor.extrainfo" : "requestor.extrawarning"; //$NON-NLS-1$ //$NON-NLS-2$
				printErr(this.main.bind(
								severity,
								Integer.toString(globalErrorCount)));
				printErr(" "); //$NON-NLS-1$
				this.printlnErr(problem.getMessage());
			} else {
				String fileName = new String(originatingFileName);
				if ((this.tagBits & Logger.EMACS) != 0) {
					String severity = problem.isError() ? "output.emacs.error" : //$NON-NLS-1$
										problem.isInfo() ? "output.emacs.info" //$NON-NLS-1$
													: "output.emacs.warning"; //$NON-NLS-1$
					String result = fileName
							+ ":" //$NON-NLS-1$
							+ problem.getSourceLineNumber()
							+ ": " //$NON-NLS-1$
							+ this.main.bind(severity)
							+ ": " //$NON-NLS-1$
							+ problem.getMessage();
					this.printlnErr(result);
					final String errorReportSource = errorReportSource(problem, null, this.tagBits);
					this.printlnErr(errorReportSource);
				} else {
					if (localErrorCount == 0) {
						this.printlnErr("----------"); //$NON-NLS-1$
					}
					String severity = problem.isError() ? "requestor.error" //$NON-NLS-1$
							: problem.isInfo() ? "requestor.info" : "requestor.warning"; //$NON-NLS-1$ //$NON-NLS-2$
					printErr(this.main.bind(
								severity,
								Integer.toString(globalErrorCount),
								fileName));
					final String errorReportSource = errorReportSource(problem, null, 0);
					this.printlnErr(errorReportSource);
					this.printlnErr(problem.getMessage());
					this.printlnErr("----------"); //$NON-NLS-1$
				}
			}
		}

		public void loggingExtraProblems(Main currentMain) {
			ArrayList<CategorizedProblem> problems = currentMain.extraProblems;
			final int count = problems.size();
			int localProblemCount = 0;
			if (count != 0) {
				int errors = 0;
				int warnings = 0;
				int infos = 0;
				for (int i = 0; i < count; i++) {
					CategorizedProblem problem = problems.get(i);
					if (!this.main.isIgnored(problem)) {
						currentMain.globalProblemsCount++;
						logExtraProblem(problem, localProblemCount, currentMain.globalProblemsCount);
						localProblemCount++;
						if (problem.isError()) {
							errors++;
							currentMain.globalErrorsCount++;
						} else if (problem.isInfo()) {
							currentMain.globalInfoCount++;
							infos++;
						} else {
							currentMain.globalWarningsCount++;
							warnings++;
						}
					}
				}
				if ((this.tagBits & Logger.XML) != 0) {
					if ((errors + warnings + infos) != 0) {
						startLoggingExtraProblems(count);
						for (int i = 0; i < count; i++) {
							CategorizedProblem problem = problems.get(i);
							if (!this.main.isIgnored(problem)) {
								if (problem.getID() != IProblem.Task) {
									logXmlExtraProblem(problem, localProblemCount, currentMain.globalProblemsCount);
								}
							}
						}
						endLoggingExtraProblems();
					}
				}
			}
		}

		public void logUnavaibleAPT(String className) {
			if ((this.tagBits & Logger.XML) != 0) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.MESSAGE, this.main.bind("configure.unavailableAPT", className)); //$NON-NLS-1$
				printTag(Logger.ERROR_TAG, parameters, true, true);
			}
			this.printlnErr(this.main.bind("configure.unavailableAPT", className)); //$NON-NLS-1$
		}

		public void logIncorrectVMVersionForAnnotationProcessing() {
			if ((this.tagBits & Logger.XML) != 0) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.MESSAGE, this.main.bind("configure.incorrectVMVersionforAPT")); //$NON-NLS-1$
				printTag(Logger.ERROR_TAG, parameters, true, true);
			}
			this.printlnErr(this.main.bind("configure.incorrectVMVersionforAPT")); //$NON-NLS-1$
		}

		/**
		 *
		 */
		public void logNoClassFileCreated(String outputDir, String relativeFileName, IOException e) {
			if ((this.tagBits & Logger.XML) != 0) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.MESSAGE, this.main.bind("output.noClassFileCreated", //$NON-NLS-1$
					new String[] {
						outputDir,
						relativeFileName,
						e.getMessage()
					}));
				printTag(Logger.ERROR_TAG, parameters, true, true);
			}
			this.printlnErr(this.main.bind("output.noClassFileCreated", //$NON-NLS-1$
				new String[] {
					outputDir,
					relativeFileName,
					e.getMessage()
				}));
		}

		/**
		 * @param exportedClassFilesCounter
		 */
		public void logNumberOfClassFilesGenerated(int exportedClassFilesCounter) {
			if ((this.tagBits & Logger.XML) != 0) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.VALUE, Integer.valueOf(exportedClassFilesCounter));
				printTag(Logger.NUMBER_OF_CLASSFILES, parameters, true, true);
			}
			if (exportedClassFilesCounter == 1) {
				printlnOut(this.main.bind("compile.oneClassFileGenerated")); //$NON-NLS-1$
			} else {
				printlnOut(this.main.bind("compile.severalClassFilesGenerated", //$NON-NLS-1$
					String.valueOf(exportedClassFilesCounter)));
			}
		}

		/**
		 * @param options the given compiler options
		 */
		public void logOptions(Map<String, String> options) {
			if ((this.tagBits & Logger.XML) != 0) {
				printTag(Logger.OPTIONS, new HashMap<>(), true, false);
				final Set<Map.Entry<String, String>> entriesSet = options.entrySet();
				Map.Entry<String, String>[] entries = entriesSet.toArray(new Map.Entry[entriesSet.size()]);
				Arrays.sort(entries, new Comparator<Map.Entry<String, String>>() {
					@Override
					public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
						Map.Entry<String, String> entry1 = o1;
						Map.Entry<String, String> entry2 = o2;
						return entry1.getKey().compareTo(entry2.getKey());
					}
				});
				HashMap<String, Object> parameters = new HashMap<>();
				for (int i = 0, max = entries.length; i < max; i++) {
					Map.Entry<String, String> entry = entries[i];
					String key = entry.getKey();
					parameters.put(Logger.KEY, key);
					parameters.put(Logger.VALUE, entry.getValue());
					printTag(Logger.OPTION, parameters, true, true);
				}
				endTag(Logger.OPTIONS);
			}
		}

		/**
		 * @param error the given error
		 */
		public void logPendingError(String error) {
			if ((this.tagBits & Logger.XML) != 0) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.MESSAGE, error);
				printTag(Logger.ERROR_TAG, parameters, true, true);
			}
			this.printlnErr(error);
		}

		/**
		 * @param message the given message
		 */
		public void logWarning(String message) {
			if ((this.tagBits & Logger.XML) != 0) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.MESSAGE, message);
				printTag(Logger.WARNING_TAG, parameters, true, true);
			}
			this.printlnOut(message);
		}

		private void logProblem(CategorizedProblem problem, int localErrorCount,
			int globalErrorCount, char[] unitSource) {
			if(problem instanceof DefaultProblem) {
				((DefaultProblem) problem).reportError();
			}
			if ((this.tagBits & Logger.EMACS) != 0) {
				String severity = problem.isError() ? "output.emacs.error" : //$NON-NLS-1$
									problem.isInfo() ? "output.emacs.info" //$NON-NLS-1$
											: "output.emacs.warning"; //$NON-NLS-1$
				String result = (new String(problem.getOriginatingFileName())
						+ ":" //$NON-NLS-1$
						+ problem.getSourceLineNumber()
						+ ": " //$NON-NLS-1$
						+ (this.main.bind(severity))
						+ ": " //$NON-NLS-1$
						+ problem.getMessage());
				this.printlnErr(result);
				final String errorReportSource = errorReportSource(problem, unitSource, this.tagBits);
				if (errorReportSource.length() != 0) this.printlnErr(errorReportSource);
			} else {
				if (localErrorCount == 0) {
					this.printlnErr("----------"); //$NON-NLS-1$
				}
				String severity = problem.isError() ? "requestor.error" : problem.isInfo() ? "requestor.info" : "requestor.warning";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				printErr(this.main.bind(severity,
										Integer.toString(globalErrorCount),
										new String(problem.getOriginatingFileName())));
				try {
					final String errorReportSource = errorReportSource(problem, unitSource, 0);
					this.printlnErr(errorReportSource);
					this.printlnErr(problem.getMessage());
				} catch (Exception e) {
					this.printlnErr(this.main.bind(
						"requestor.notRetrieveErrorMessage", problem.toString())); //$NON-NLS-1$
				}
				this.printlnErr("----------"); //$NON-NLS-1$
			}
		}

		public int logProblems(CategorizedProblem[] problems, char[] unitSource, Main currentMain) {
			final int count = problems.length;
			int localErrorCount = 0;
			int localProblemCount = 0;
			if (count != 0) {
				int errors = 0;
				int warnings = 0;
				int infos = 0;
				int tasks = 0;
				for (int i = 0; i < count; i++) {
					CategorizedProblem problem = problems[i];
					if (problem != null) {
						currentMain.globalProblemsCount++;
						logProblem(problem, localProblemCount, currentMain.globalProblemsCount, unitSource);
						localProblemCount++;
						if (problem.isError()) {
							localErrorCount++;
							errors++;
							currentMain.globalErrorsCount++;
						} else if (problem.getID() == IProblem.Task) {
							currentMain.globalTasksCount++;
							tasks++;
						} else if (problem.isInfo()) {
							currentMain.globalInfoCount++;
							infos++;
						} else {
							currentMain.globalWarningsCount++;
							warnings++;
						}
					}
				}
				if ((this.tagBits & Logger.XML) != 0) {
					if ((errors + warnings + infos) != 0) {
						startLoggingProblems(errors, warnings, infos);
						for (int i = 0; i < count; i++) {
							CategorizedProblem problem = problems[i];
							if (problem!= null) {
								if (problem.getID() != IProblem.Task) {
									logXmlProblem(problem, unitSource);
								}
							}
						}
						endLoggingProblems();
					}
					if (tasks != 0) {
						startLoggingTasks(tasks);
						for (int i = 0; i < count; i++) {
							CategorizedProblem problem = problems[i];
							if (problem!= null) {
								if (problem.getID() == IProblem.Task) {
									logXmlTask(problem, unitSource);
								}
							}
						}
						endLoggingTasks();
					}
				}
			}
			return localErrorCount;
		}

		/**
		 * @param globalProblemsCount
		 * @param globalErrorsCount
		 * @param globalWarningsCount
		 */
		public void logProblemsSummary(int globalProblemsCount,
			int globalErrorsCount, int globalWarningsCount, int globalInfoCount, int globalTasksCount) {
			if ((this.tagBits & Logger.XML) != 0) {
				// generate xml
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.NUMBER_OF_PROBLEMS, Integer.valueOf(globalProblemsCount));
				parameters.put(Logger.NUMBER_OF_ERRORS, Integer.valueOf(globalErrorsCount));
				parameters.put(Logger.NUMBER_OF_WARNINGS, Integer.valueOf(globalWarningsCount));
				parameters.put(Logger.NUMBER_OF_INFOS, Integer.valueOf(globalInfoCount));
				parameters.put(Logger.NUMBER_OF_TASKS, Integer.valueOf(globalTasksCount));
				printTag(Logger.PROBLEM_SUMMARY, parameters, true, true);
			}
			if (globalProblemsCount == 1) {
				String message = null;
				if (globalErrorsCount == 1) {
					message = this.main.bind("compile.oneError"); //$NON-NLS-1$
				} else if (globalInfoCount == 1) {
					message = this.main.bind("compile.oneInfo"); //$NON-NLS-1$
				} else {
					message = this.main.bind("compile.oneWarning"); //$NON-NLS-1$
				}
				printErr(this.main.bind("compile.oneProblem", message)); //$NON-NLS-1$
			} else {
				String errorMessage = null;
				String warningMessage = null;
				String infoMessage = null;
				if (globalErrorsCount > 0) {
					if (globalErrorsCount == 1) {
						errorMessage = this.main.bind("compile.oneError"); //$NON-NLS-1$
					} else {
						errorMessage = this.main.bind("compile.severalErrors", String.valueOf(globalErrorsCount)); //$NON-NLS-1$
					}
				}
				int warningsNumber = globalWarningsCount + globalTasksCount;
				if (warningsNumber > 0) {
					if (warningsNumber == 1) {
						warningMessage = this.main.bind("compile.oneWarning"); //$NON-NLS-1$
					} else {
						warningMessage = this.main.bind("compile.severalWarnings", String.valueOf(warningsNumber)); //$NON-NLS-1$
					}
				}
				if (globalInfoCount == 1) {
					infoMessage = this.main.bind("compile.oneInfo"); //$NON-NLS-1$
				} else if (globalInfoCount > 1) {
					infoMessage = this.main.bind("compile.severalInfos", String.valueOf(globalInfoCount)); //$NON-NLS-1$
				}
				if (globalProblemsCount == globalInfoCount || globalProblemsCount == globalErrorsCount || globalProblemsCount == globalWarningsCount) {
					String msg = errorMessage != null ? errorMessage : warningMessage != null ? warningMessage : infoMessage;
					printErr(this.main.bind(
						"compile.severalProblemsErrorsOrWarnings", //$NON-NLS-1$
						String.valueOf(globalProblemsCount),
						msg));
				} else {
					if (globalInfoCount == 0) {
						printErr(this.main.bind(
								"compile.severalProblemsErrorsAndWarnings", //$NON-NLS-1$
								new String[] {
									String.valueOf(globalProblemsCount),
									errorMessage,
									warningMessage
								}));
					} else {
						if (errorMessage == null) {
							errorMessage  = this.main.bind("compile.severalErrors", String.valueOf(globalErrorsCount)); //$NON-NLS-1$
						}
						if (warningMessage == null) {
							warningMessage  = this.main.bind("compile.severalWarnings", String.valueOf(warningsNumber)); //$NON-NLS-1$
						}
						printErr(this.main.bind(
								"compile.severalProblems", //$NON-NLS-1$
								new String[] {
									String.valueOf(globalProblemsCount),
									errorMessage,
									warningMessage,
									infoMessage
								}));
					}
				}
			}
			if (this.main.failOnWarning && globalWarningsCount > 0) {
				printErr("\n"); //$NON-NLS-1$
				printErr(this.main.bind("compile.failOnWarning")); //$NON-NLS-1$
			}
			if ((this.tagBits & Logger.XML) == 0) {
				this.printlnErr();
			}
		}

		/**
		 *
		 */
		public void logProgress() {
			printOut('.');
		}

		/**
		 * @param i
		 *            the current repetition number
		 * @param repetitions
		 *            the given number of repetitions
		 */
		public void logRepetition(int i, int repetitions) {
			printlnOut(this.main.bind("compile.repetition", //$NON-NLS-1$
				String.valueOf(i + 1), String.valueOf(repetitions)));
		}
		/**
		 * @param compilerStats
		 */
		public void logTiming(CompilerStats compilerStats) {
			long time = compilerStats.elapsedTime();
			long lineCount = compilerStats.lineCount;
			if ((this.tagBits & Logger.XML) != 0) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.VALUE, Long.valueOf(time));
				printTag(Logger.TIME, parameters, true, true);
				parameters.put(Logger.VALUE, Long.valueOf(lineCount));
				printTag(Logger.NUMBER_OF_LINES, parameters, true, true);
			}
			if (lineCount != 0) {
				printlnOut(
					this.main.bind("compile.instantTime", //$NON-NLS-1$
						new String[] {
							String.valueOf(lineCount),
							String.valueOf(time),
							String.valueOf(((int) (lineCount * 10000.0 / time)) / 10.0),
						}));
			} else {
				printlnOut(
					this.main.bind("compile.totalTime", //$NON-NLS-1$
						new String[] {
							String.valueOf(time),
						}));
			}
			if ((this.main.timing & Main.TIMING_DETAILED) != 0) {
				printlnOut(
						this.main.bind("compile.detailedTime", //$NON-NLS-1$
							new String[] {
								String.valueOf(compilerStats.parseTime),
								String.valueOf(((int) (compilerStats.parseTime * 1000.0 / time)) / 10.0),
								String.valueOf(compilerStats.resolveTime),
								String.valueOf(((int) (compilerStats.resolveTime * 1000.0 / time)) / 10.0),
								String.valueOf(compilerStats.analyzeTime),
								String.valueOf(((int) (compilerStats.analyzeTime * 1000.0 / time)) / 10.0),
								String.valueOf(compilerStats.generateTime),
								String.valueOf(((int) (compilerStats.generateTime * 1000.0 / time)) / 10.0),
							}));
			}
		}

		/**
		 * Print the usage of the compiler
		 * @param usage
		 */
		public void logUsage(String usage) {
			printlnOut(usage);
		}

		/**
		 * Print the version of the compiler in the log and/or the out field
		 */
		public void logVersion(final boolean printToOut) {
			if (this.log != null && (this.tagBits & Logger.XML) == 0) {
				final String version = this.main.bind("misc.version", //$NON-NLS-1$
					new String[] {
						this.main.bind("compiler.name"), //$NON-NLS-1$
						this.main.bind("compiler.version"), //$NON-NLS-1$
						this.main.bind("compiler.copyright") //$NON-NLS-1$
					}
				);
				this.log.println("# " + version); //$NON-NLS-1$
				if (printToOut) {
					this.out.println(version);
					this.out.flush();
				}
			} else if (printToOut) {
				final String version = this.main.bind("misc.version", //$NON-NLS-1$
					new String[] {
						this.main.bind("compiler.name"), //$NON-NLS-1$
						this.main.bind("compiler.version"), //$NON-NLS-1$
						this.main.bind("compiler.copyright") //$NON-NLS-1$
					}
				);
				this.out.println(version);
				this.out.flush();
			}
		}

		/**
		 * Print the usage of wrong JDK
		 */
		public void logWrongJDK() {
			if ((this.tagBits & Logger.XML) != 0) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.MESSAGE, this.main.bind("configure.requiresJDK1.2orAbove")); //$NON-NLS-1$
				printTag(Logger.ERROR, parameters, true, true);
			}
			this.printlnErr(this.main.bind("configure.requiresJDK1.2orAbove")); //$NON-NLS-1$
		}

		private void logXmlExtraProblem(CategorizedProblem problem, int globalErrorCount, int localErrorCount) {
			final int sourceStart = problem.getSourceStart();
			final int sourceEnd = problem.getSourceEnd();
			boolean isError = problem.isError();
			HashMap<String, Object> parameters = new HashMap<>();
			parameters.put(Logger.PROBLEM_SEVERITY, isError ? Logger.ERROR : (problem.isInfo() ? Logger.INFO : Logger.WARNING));
			parameters.put(Logger.PROBLEM_LINE, Integer.valueOf(problem.getSourceLineNumber()));
			parameters.put(Logger.PROBLEM_SOURCE_START, Integer.valueOf(sourceStart));
			parameters.put(Logger.PROBLEM_SOURCE_END, Integer.valueOf(sourceEnd));
			printTag(Logger.EXTRA_PROBLEM_TAG, parameters, true, false);
			parameters.put(Logger.VALUE, problem.getMessage());
			printTag(Logger.PROBLEM_MESSAGE, parameters, true, true);
			extractContext(problem, null);
			endTag(Logger.EXTRA_PROBLEM_TAG);
		}
		/**
		 * @param problem
		 *            the given problem to log
		 * @param unitSource
		 *            the given unit source
		 */
		private void logXmlProblem(CategorizedProblem problem, char[] unitSource) {
			final int sourceStart = problem.getSourceStart();
			final int sourceEnd = problem.getSourceEnd();
			final int id = problem.getID();
			HashMap<String, Object> parameters = new HashMap<>();
			parameters.put(Logger.ID, getFieldName(id)); // ID as field name
			parameters.put(Logger.PROBLEM_ID, Integer.valueOf(id)); // ID as numeric value
			boolean isError = problem.isError();
			int severity = isError ? ProblemSeverities.Error : ProblemSeverities.Warning;
			parameters.put(Logger.PROBLEM_SEVERITY, isError ? Logger.ERROR : (problem.isInfo() ? Logger.INFO : Logger.WARNING));
			parameters.put(Logger.PROBLEM_LINE, Integer.valueOf(problem.getSourceLineNumber()));
			parameters.put(Logger.PROBLEM_SOURCE_START, Integer.valueOf(sourceStart));
			parameters.put(Logger.PROBLEM_SOURCE_END, Integer.valueOf(sourceEnd));
			String problemOptionKey = getProblemOptionKey(id);
			if (problemOptionKey != null) {
				parameters.put(Logger.PROBLEM_OPTION_KEY, problemOptionKey);
			}
			int categoryID = ProblemReporter.getProblemCategory(severity, id);
			parameters.put(Logger.PROBLEM_CATEGORY_ID, Integer.valueOf(categoryID));
			printTag(Logger.PROBLEM_TAG, parameters, true, false);
			parameters.put(Logger.VALUE, problem.getMessage());
			printTag(Logger.PROBLEM_MESSAGE, parameters, true, true);
			extractContext(problem, unitSource);
			String[] arguments = problem.getArguments();
			final int length = arguments.length;
			if (length != 0) {
				parameters = new HashMap<>();
				printTag(Logger.PROBLEM_ARGUMENTS, parameters, true, false);
				for (int i = 0; i < length; i++) {
					parameters = new HashMap<>();
					parameters.put(Logger.PROBLEM_ARGUMENT_VALUE, arguments[i]);
					printTag(Logger.PROBLEM_ARGUMENT, parameters, true, true);
				}
				endTag(Logger.PROBLEM_ARGUMENTS);
			}
			endTag(Logger.PROBLEM_TAG);
		}
		/**
		 * @param problem
		 *            the given problem to log
		 * @param unitSource
		 *            the given unit source
		 */
		private void logXmlTask(CategorizedProblem problem, char[] unitSource) {
			HashMap<String, Object> parameters = new HashMap<>();
			parameters.put(Logger.PROBLEM_LINE, Integer.valueOf(problem.getSourceLineNumber()));
			parameters.put(Logger.PROBLEM_SOURCE_START, Integer.valueOf(problem.getSourceStart()));
			parameters.put(Logger.PROBLEM_SOURCE_END, Integer.valueOf(problem.getSourceEnd()));
			String problemOptionKey = getProblemOptionKey(problem.getID());
			if (problemOptionKey != null) {
				parameters.put(Logger.PROBLEM_OPTION_KEY, problemOptionKey);
			}
			printTag(Logger.TASK, parameters, true, false);
			parameters.put(Logger.VALUE, problem.getMessage());
			printTag(Logger.PROBLEM_MESSAGE, parameters, true, true);
			extractContext(problem, unitSource);
			endTag(Logger.TASK);
		}

		private void printErr(String s) {
			this.err.print(s);
			if ((this.tagBits & Logger.XML) == 0 && this.log != null) {
				this.log.print(s);
			}
		}

		private void printlnErr() {
			this.err.println();
			if ((this.tagBits & Logger.XML) == 0 && this.log != null) {
				this.log.println();
			}
		}

		private void printlnErr(String s) {
			this.err.println(s);
			if ((this.tagBits & Logger.XML) == 0 && this.log != null) {
				this.log.println(s);
			}
		}

		private void printlnOut(String s) {
			this.out.println(s);
			if ((this.tagBits & Logger.XML) == 0 && this.log != null) {
				this.log.println(s);
			}
		}

		/**
		 *
		 */
		public void printNewLine() {
			this.out.println();
		}

		private void printOut(char c) {
			this.out.print(c);
		}

		public void printStats() {
			final boolean isTimed = (this.main.timing & TIMING_ENABLED) != 0;
			if ((this.tagBits & Logger.XML) != 0) {
				printTag(Logger.STATS, new HashMap<>(), true, false);
			}
			if (isTimed) {
				CompilerStats compilerStats = this.main.batchCompiler.stats;
				compilerStats.startTime = this.main.startTime; // also include batch initialization times
				compilerStats.endTime = System.currentTimeMillis(); // also include batch output times
				logTiming(compilerStats);
			}
			if (this.main.globalProblemsCount > 0) {
				logProblemsSummary(this.main.globalProblemsCount, this.main.globalErrorsCount, this.main.globalWarningsCount,
						this.main.globalInfoCount, this.main.globalTasksCount);
			}
			if (this.main.exportedClassFilesCounter != 0
					&& (this.main.showProgress || isTimed || this.main.verbose)) {
				logNumberOfClassFilesGenerated(this.main.exportedClassFilesCounter);
			}
			if ((this.tagBits & Logger.XML) != 0) {
				endTag(Logger.STATS);
			}
		}

		private void printTag(String name, HashMap<String, Object> params, boolean insertNewLine, boolean closeTag) {
			if (this.log != null) {
				((GenericXMLWriter) this.log).printTag(name, params, true, insertNewLine, closeTag);
			}
			if (params != null) params.clear();
		}

		public void setEmacs() {
			this.tagBits |= Logger.EMACS;
		}
		public void setLog(String logFileName) {
			final Date date = new Date();
			final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.getDefault());
			try {
				int index = logFileName.lastIndexOf('.');
				if (index != -1) {
					if (logFileName.substring(index).toLowerCase().equals(".xml")) { //$NON-NLS-1$
						this.log = new GenericXMLWriter(new OutputStreamWriter(new FileOutputStream(logFileName, false), Util.UTF_8), Util.LINE_SEPARATOR, true);
						this.tagBits |= Logger.XML;
						// insert time stamp as comment
						this.log.println("<!-- " + dateFormat.format(date) + " -->");//$NON-NLS-1$//$NON-NLS-2$
						this.log.println(Logger.XML_DTD_DECLARATION);
						HashMap<String, Object> parameters = new HashMap<>();
						parameters.put(Logger.COMPILER_NAME, this.main.bind("compiler.name")); //$NON-NLS-1$
						parameters.put(Logger.COMPILER_VERSION, this.main.bind("compiler.version")); //$NON-NLS-1$
						parameters.put(Logger.COMPILER_COPYRIGHT, this.main.bind("compiler.copyright")); //$NON-NLS-1$
						printTag(Logger.COMPILER, parameters, true, false);
					} else {
						this.log = new PrintWriter(new FileOutputStream(logFileName, false));
						this.log.println("# " + dateFormat.format(date));//$NON-NLS-1$
					}
				} else {
					this.log = new PrintWriter(new FileOutputStream(logFileName, false));
					this.log.println("# " + dateFormat.format(date));//$NON-NLS-1$
				}
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException(this.main.bind("configure.cannotOpenLog", logFileName), e); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				throw new IllegalArgumentException(this.main.bind("configure.cannotOpenLogInvalidEncoding", logFileName), e); //$NON-NLS-1$
			}
		}
		private void startLoggingExtraProblems(int count) {
			HashMap<String, Object> parameters = new HashMap<>();
			parameters.put(Logger.NUMBER_OF_PROBLEMS, Integer.valueOf(count));
			printTag(Logger.EXTRA_PROBLEMS, parameters, true, false);
		}

		/**
		 * Used to start logging problems.
		 * Only use in xml mode.
		 */
		private void startLoggingProblems(int errors, int warnings, int infos) {
			HashMap<String, Object> parameters = new HashMap<>();
			parameters.put(Logger.NUMBER_OF_PROBLEMS, Integer.valueOf(errors + warnings));
			parameters.put(Logger.NUMBER_OF_ERRORS, Integer.valueOf(errors));
			parameters.put(Logger.NUMBER_OF_WARNINGS, Integer.valueOf(warnings));
			parameters.put(Logger.NUMBER_OF_INFOS, Integer.valueOf(infos));
			printTag(Logger.PROBLEMS, parameters, true, false);
		}

		public void startLoggingSource(CompilationResult compilationResult) {
			if ((this.tagBits & Logger.XML) != 0) {
				ICompilationUnit compilationUnit = compilationResult.compilationUnit;
				HashMap<String, Object> parameters = new HashMap<>();
				if (compilationUnit != null) {
    				char[] fileName = compilationUnit.getFileName();
    				File f = new File(new String(fileName));
    				if (fileName != null) {
    					parameters.put(Logger.PATH, f.getAbsolutePath());
    				}
    				char[][] packageName = compilationResult.packageName;
    				if (packageName != null) {
    					parameters.put(
    							Logger.PACKAGE,
    							new String(CharOperation.concatWith(packageName, File.separatorChar)));
    				}
    				CompilationUnit unit = (CompilationUnit) compilationUnit;
    				String destinationPath = unit.destinationPath;
					if (destinationPath == null) {
						destinationPath = this.main.destinationPath;
					}
					if (destinationPath != null && destinationPath != NONE) {
						if (File.separatorChar == '/') {
							parameters.put(Logger.OUTPUT, destinationPath);
						} else {
							parameters.put(Logger.OUTPUT, destinationPath.replace('/', File.separatorChar));
						}
					}
				}
				printTag(Logger.SOURCE, parameters, true, false);
			}
		}

		public void startLoggingSources() {
			if ((this.tagBits & Logger.XML) != 0) {
				printTag(Logger.SOURCES, new HashMap<>(), true, false);
			}
		}

		public void startLoggingTasks(int tasks) {
			if ((this.tagBits & Logger.XML) != 0) {
				HashMap<String, Object> parameters = new HashMap<>();
				parameters.put(Logger.NUMBER_OF_TASKS, Integer.valueOf(tasks));
				printTag(Logger.TASKS, parameters, true, false);
			}
		}
	}

	/**
	 * Resource bundle factory to share bundles for the same locale
	 */
	public static class ResourceBundleFactory {
		private static HashMap<Locale, ResourceBundle> Cache = new HashMap<>();
		public static synchronized ResourceBundle getBundle(Locale locale) {
			ResourceBundle bundle = Cache.get(locale);
			if (bundle == null) {
				bundle = ResourceBundle.getBundle(Main.bundleName, locale);
				Cache.put(locale, bundle);
			}
			return bundle;
		}
	}

	// used with -annotationpath to declare that annotations should be read from the classpath:
	private static final String ANNOTATION_SOURCE_CLASSPATH = "CLASSPATH"; //$NON-NLS-1$

	// javadoc analysis tuning
	boolean enableJavadocOn;

	boolean warnJavadocOn;
	boolean warnAllJavadocOn;

	public Compiler batchCompiler;
	/* Bundle containing messages */
	public ResourceBundle bundle;
	protected FileSystem.Classpath[] checkedClasspaths;
	// For single module mode
	protected IModule module;
	private String moduleVersion;
	// paths to external annotations:
	protected List<String> annotationPaths;
	protected boolean annotationsFromClasspath;

	private List<String> addonExports = Collections.EMPTY_LIST;
	private List<String> addonReads = Collections.EMPTY_LIST;
	public Set<String> rootModules = Collections.EMPTY_SET;
	public Set<String> limitedModules;

	public Locale compilerLocale;
	public CompilerOptions compilerOptions; // read-only
	public CompilationProgress progress;
	public String destinationPath;
	public String[] destinationPaths;
	// destination path for compilation units that get no more specific
	// one (through directory arguments or various classpath options);
	// coding is:
	// == null: unspecified, write class files close to their respective
	//          source files;
	// == Main.NONE: absorbent element, do not output class files;
	// else: use as the path of the directory into which class files must
	//       be written.
	protected boolean enablePreview;
	protected String releaseVersion;
	private boolean didSpecifySource;
	private boolean didSpecifyTarget;
	public String[] encodings;
	public int exportedClassFilesCounter;
	public String[] filenames;
	public String[] modNames;
	public String[] classNames;
	// overrides of destinationPath on a directory argument basis
	public int globalErrorsCount;
	public int globalProblemsCount;
	public int globalTasksCount;
	public int globalWarningsCount;
	public int globalInfoCount;

	private File javaHomeCache;

	private boolean javaHomeChecked = false;
	private boolean primaryNullAnnotationsSeen = false;
	public long lineCount0;

	public String log;

	public Logger logger;
	public int maxProblems;
	public Map<String, String> options;
	protected long complianceLevel;
	public char[][] ignoreOptionalProblemsFromFolders;
	protected PrintWriter out;
	public boolean proceed = true;
	public boolean proceedOnError = false;
	public boolean failOnWarning = false;
	public boolean produceRefInfo = false;
	public int currentRepetition, maxRepetition;
	public boolean showProgress = false;
	public long startTime;
	public ArrayList<String> pendingErrors;
	public boolean systemExitWhenFinished = true;

	public static final int TIMING_DISABLED = 0;
	public static final int TIMING_ENABLED = 1;
	public static final int TIMING_DETAILED = 2;

	public int timing = TIMING_DISABLED;
	public CompilerStats[] compilerStats;
	public boolean verbose = false;
	private String[] expandedCommandLine;

	private PrintWriter err;

	protected ArrayList<CategorizedProblem> extraProblems;

	public final static String bundleName = "org.eclipse.jdt.internal.compiler.batch.messages"; //$NON-NLS-1$
	// two uses: recognize 'none' in options; code the singleton none
	// for the '-d none' option (wherever it may be found)
	public static final int DEFAULT_SIZE_CLASSPATH = 4;

	public static final String NONE = "none"; //$NON-NLS-1$

/**
 * @deprecated - use {@link BatchCompiler#compile(String, PrintWriter, PrintWriter, CompilationProgress)} instead
 * 						  e.g. BatchCompiler.compile(commandLine, new PrintWriter(System.out), new PrintWriter(System.err), null);
 */
public static boolean compile(String commandLine) {
	return new Main(new PrintWriter(System.out), new PrintWriter(System.err), false /* systemExit */, null /* options */, null /* progress */).compile(tokenize(commandLine));
}

/**
 * @deprecated - use {@link BatchCompiler#compile(String, PrintWriter, PrintWriter, CompilationProgress)} instead
 *                       e.g. BatchCompiler.compile(commandLine, outWriter, errWriter, null);
 */
public static boolean compile(String commandLine, PrintWriter outWriter, PrintWriter errWriter) {
	return new Main(outWriter, errWriter, false /* systemExit */, null /* options */, null /* progress */).compile(tokenize(commandLine));
}

/*
 * Internal API for public API BatchCompiler#compile(String[], PrintWriter, PrintWriter, CompilationProgress)
 */
public static boolean compile(String[] commandLineArguments, PrintWriter outWriter, PrintWriter errWriter, CompilationProgress progress) {
	return new Main(outWriter, errWriter, false /* systemExit */, null /* options */, progress).compile(commandLineArguments);
}
public static File[][] getLibrariesFiles(File[] files) {
	FilenameFilter filter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return Util.archiveFormat(name) > -1;
		}
	};
	final int filesLength = files.length;
	File[][] result = new File[filesLength][];
	for (int i = 0; i < filesLength; i++) {
		File currentFile = files[i];
		if (currentFile.exists() && currentFile.isDirectory()) {
			result[i] = currentFile.listFiles(filter);
		}
	}
	return result;
}

public static void main(String[] argv) {
	new Main(new PrintWriter(System.out), new PrintWriter(System.err), true/*systemExit*/, null/*options*/, null/*progress*/).compile(argv);
}

public static String[] tokenize(String commandLine) {

	int count = 0;
	String[] arguments = new String[10];
	StringTokenizer tokenizer = new StringTokenizer(commandLine, " \"", true); //$NON-NLS-1$
	String token = Util.EMPTY_STRING;
	boolean insideQuotes = false;
	boolean startNewToken = true;

	// take care to quotes on the command line
	// 'xxx "aaa bbb";ccc yyy' --->  {"xxx", "aaa bbb;ccc", "yyy" }
	// 'xxx "aaa bbb;ccc" yyy' --->  {"xxx", "aaa bbb;ccc", "yyy" }
	// 'xxx "aaa bbb";"ccc" yyy' --->  {"xxx", "aaa bbb;ccc", "yyy" }
	// 'xxx/"aaa bbb";"ccc" yyy' --->  {"xxx/aaa bbb;ccc", "yyy" }
	while (tokenizer.hasMoreTokens()) {
		token = tokenizer.nextToken();

		if (token.equals(" ")) { //$NON-NLS-1$
			if (insideQuotes) {
				arguments[count - 1] += token;
				startNewToken = false;
			} else {
				startNewToken = true;
			}
		} else if (token.equals("\"")) { //$NON-NLS-1$
			if (!insideQuotes && startNewToken) {
				if (count == arguments.length)
					System.arraycopy(arguments, 0, (arguments = new String[count * 2]), 0, count);
				arguments[count++] = Util.EMPTY_STRING;
			}
			insideQuotes = !insideQuotes;
			startNewToken = false;
		} else {
			if (insideQuotes) {
				arguments[count - 1] += token;
			} else {
				if (token.length() > 0 && !startNewToken) {
					arguments[count - 1] += token;
				} else {
					if (count == arguments.length)
						System.arraycopy(arguments, 0, (arguments = new String[count * 2]), 0, count);
					String trimmedToken = token.trim();
					if (trimmedToken.length() != 0) {
						arguments[count++] = trimmedToken;
					}
				}
			}
			startNewToken = false;
		}
	}
	System.arraycopy(arguments, 0, arguments = new String[count], 0, count);
	return arguments;
}

/**
 * @deprecated - use {@link #Main(PrintWriter, PrintWriter, boolean, Map, CompilationProgress)} instead
 *                       e.g. Main(outWriter, errWriter, systemExitWhenFinished, null, null)
 */
public Main(PrintWriter outWriter, PrintWriter errWriter, boolean systemExitWhenFinished) {
	this(outWriter, errWriter, systemExitWhenFinished, null /* options */, null /* progress */);
}

/**
 * @deprecated - use {@link #Main(PrintWriter, PrintWriter, boolean, Map, CompilationProgress)} instead
 *                       e.g. Main(outWriter, errWriter, systemExitWhenFinished, customDefaultOptions, null)
 */
public Main(PrintWriter outWriter, PrintWriter errWriter, boolean systemExitWhenFinished, Map<String, String> customDefaultOptions) {
	this(outWriter, errWriter, systemExitWhenFinished, customDefaultOptions, null /* progress */);
}

public Main(PrintWriter outWriter, PrintWriter errWriter, boolean systemExitWhenFinished, Map<String, String> customDefaultOptions, CompilationProgress compilationProgress) {
	this.initialize(outWriter, errWriter, systemExitWhenFinished, customDefaultOptions, compilationProgress);
	this.relocalize();
}

public void addExtraProblems(CategorizedProblem problem) {
	if (this.extraProblems == null) {
		this.extraProblems = new ArrayList<>();
	}
	this.extraProblems.add(problem);
}
protected void addNewEntry(ArrayList<FileSystem.Classpath> paths, String currentClasspathName,
		ArrayList<String> currentRuleSpecs, String customEncoding,
		String destPath, boolean isSourceOnly,
		boolean rejectDestinationPathOnJars) {

	int rulesSpecsSize = currentRuleSpecs.size();
	AccessRuleSet accessRuleSet = null;
	if (rulesSpecsSize != 0) {
		AccessRule[] accessRules = new AccessRule[currentRuleSpecs.size()];
		boolean rulesOK = true;
		Iterator<String> i = currentRuleSpecs.iterator();
		int j = 0;
		while (i.hasNext()) {
			String ruleSpec = i.next();
			char key = ruleSpec.charAt(0);
			String pattern = ruleSpec.substring(1);
			if (pattern.length() > 0) {
				switch (key) {
					case '+':
						accessRules[j++] = new AccessRule(pattern
								.toCharArray(), 0);
						break;
					case '~':
						accessRules[j++] = new AccessRule(pattern
								.toCharArray(),
								IProblem.DiscouragedReference);
						break;
					case '-':
						accessRules[j++] = new AccessRule(pattern
								.toCharArray(),
								IProblem.ForbiddenReference);
						break;
					case '?':
						accessRules[j++] = new AccessRule(pattern
								.toCharArray(),
								IProblem.ForbiddenReference, true/*keep looking for accessible type*/);
						break;
					default:
						rulesOK = false;
				}
			} else {
				rulesOK = false;
			}
		}
		if (rulesOK) {
    		accessRuleSet = new AccessRuleSet(accessRules, AccessRestriction.COMMAND_LINE, currentClasspathName);
		} else {
			if (currentClasspathName.length() != 0) {
				// we go on anyway
				addPendingErrors(this.bind("configure.incorrectClasspath", currentClasspathName));//$NON-NLS-1$
			}
			return;
		}
	}
	if (NONE.equals(destPath)) {
		destPath = NONE; // keep == comparison valid
	}

	if (rejectDestinationPathOnJars && destPath != null &&
			Util.archiveFormat(currentClasspathName) > -1) {
		throw new IllegalArgumentException(
			this.bind("configure.unexpectedDestinationPathEntryFile", //$NON-NLS-1$
						currentClasspathName));
	}
	FileSystem.Classpath currentClasspath = FileSystem.getClasspath(
			currentClasspathName,
			customEncoding,
			isSourceOnly,
			accessRuleSet,
			destPath,
			this.options,
			this.releaseVersion);
	if (currentClasspath != null) {
		paths.add(currentClasspath);
	} else if (currentClasspathName.length() != 0) {
		// we go on anyway
		addPendingErrors(this.bind("configure.incorrectClasspath", currentClasspathName));//$NON-NLS-1$
	}
}
void addPendingErrors(String message) {
	if (this.pendingErrors == null) {
		this.pendingErrors = new ArrayList<>();
	}
	this.pendingErrors.add(message);
}
/*
 * Lookup the message with the given ID in this catalog
 */
public String bind(String id) {
	return bind(id, (String[]) null);
}
/*
 * Lookup the message with the given ID in this catalog and bind its
 * substitution locations with the given string.
 */
public String bind(String id, String binding) {
	return bind(id, new String[] { binding });
}

/*
 * Lookup the message with the given ID in this catalog and bind its
 * substitution locations with the given strings.
 */
public String bind(String id, String binding1, String binding2) {
	return bind(id, new String[] { binding1, binding2 });
}

/*
 * Lookup the message with the given ID in this catalog and bind its
 * substitution locations with the given string values.
 */
public String bind(String id, String[] arguments) {
	if (id == null)
		return "No message available"; //$NON-NLS-1$
	String message = null;
	try {
		message = this.bundle.getString(id);
	} catch (MissingResourceException e) {
		// If we got an exception looking for the message, fail gracefully by just returning
		// the id we were looking for.  In most cases this is semi-informative so is not too bad.
		return "Missing message: " + id + " in: " + Main.bundleName; //$NON-NLS-2$ //$NON-NLS-1$
	}
	return MessageFormat.format(message, (Object[]) arguments);
}
/**
 * Return true if and only if the running VM supports the given minimal version.
 *
 * <p>This only checks the major version, since the minor version is always 0 (at least for the useful cases).</p>
 * <p>The given minimalSupportedVersion is one of the constants:</p>
 * <ul>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK1_1</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK1_2</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK1_3</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK1_4</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK1_5</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK1_6</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK1_7</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK1_8</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK9</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK10</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK11</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK12</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK13</code></li>
 * <li><code>org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK14</code></li>
 *
 * </ul>
 * @param minimalSupportedVersion the given minimal version
 * @return true if and only if the running VM supports the given minimal version, false otherwise
 */
private boolean checkVMVersion(long minimalSupportedVersion) {
	// the format of this property is supposed to be xx.x where x are digits.
	String classFileVersion = System.getProperty("java.class.version"); //$NON-NLS-1$
	if (classFileVersion == null) {
		// by default we don't support a class file version we cannot recognize
		return false;
	}
	int index = classFileVersion.indexOf('.');
	if (index == -1) {
		// by default we don't support a class file version we cannot recognize
		return false;
	}
	int majorVersion;
	try {
		majorVersion = Integer.parseInt(classFileVersion.substring(0, index));
	} catch (NumberFormatException e) {
		// by default we don't support a class file version we cannot recognize
		return false;
	}
	return ClassFileConstants.getComplianceLevelForJavaVersion(majorVersion) >=minimalSupportedVersion;
}
/*
 *  Low-level API performing the actual compilation
 */
public boolean compile(String[] argv) {
	// decode command line arguments
	try {
		configure(argv);
		if (this.progress != null)
			this.progress.begin(this.filenames == null ? 0 : this.filenames.length * this.maxRepetition);
		if (this.proceed) {
//				if (this.verbose) {
//					System.out.println(new CompilerOptions(this.options));
//				}
			if (this.showProgress) this.logger.compiling();
			for (this.currentRepetition = 0; this.currentRepetition < this.maxRepetition; this.currentRepetition++) {
				this.globalProblemsCount = 0;
				this.globalErrorsCount = 0;
				this.globalWarningsCount = 0;
				this.globalInfoCount = 0;
				this.globalTasksCount = 0;
				this.exportedClassFilesCounter = 0;

				if (this.maxRepetition > 1) {
					this.logger.flush();
					this.logger.logRepetition(this.currentRepetition, this.maxRepetition);
				}
				// request compilation
				performCompilation();
			}
			if (this.compilerStats != null) {
				this.logger.logAverage();
			}
			if (this.showProgress) this.logger.printNewLine();
		}
		if (this.systemExitWhenFinished) {
			this.logger.flush();
			this.logger.close();
			if (this.failOnWarning && this.globalWarningsCount > 0) {
				System.exit(-1);
			}
			System.exit(this.globalErrorsCount > 0 ? -1 : 0);
		}
	} catch (Exception e) { // internal compiler failure
		this.logger.logException(e);
		if (this.systemExitWhenFinished) {
			this.logger.flush();
			this.logger.close();
			System.exit(-1);
		}
		return false;
	} finally {
		this.logger.flush();
		this.logger.close();
		if (this.progress != null)
			this.progress.done();
	}
	if (this.progress == null || !this.progress.isCanceled()) {
		if (this.failOnWarning && (this.globalWarningsCount > 0))
			return false;
		if (this.globalErrorsCount == 0)
			return true;
	}

	return false;
}

/*
Decode the command line arguments
 */
public void configure(String[] argv) {

	if ((argv == null) || (argv.length == 0)) {
		printUsage();
		return;
	}

	final int INSIDE_CLASSPATH_start = 1;
	final int INSIDE_DESTINATION_PATH = 3;
	final int INSIDE_TARGET = 4;
	final int INSIDE_LOG = 5;
	final int INSIDE_REPETITION = 6;
	final int INSIDE_SOURCE = 7;
	final int INSIDE_DEFAULT_ENCODING = 8;
	final int INSIDE_BOOTCLASSPATH_start = 9;
	final int INSIDE_MAX_PROBLEMS = 11;
	final int INSIDE_EXT_DIRS = 12;
	final int INSIDE_SOURCE_PATH_start = 13;
	final int INSIDE_ENDORSED_DIRS = 15;
	final int INSIDE_SOURCE_DIRECTORY_DESTINATION_PATH = 16;
	final int INSIDE_PROCESSOR_PATH_start = 17;
	final int INSIDE_PROCESSOR_start = 18;
	final int INSIDE_S_start = 19;
	final int INSIDE_CLASS_NAMES = 20;
	final int INSIDE_WARNINGS_PROPERTIES = 21;
	final int INSIDE_ANNOTATIONPATH_start = 22;
	final int INSIDE_MODULEPATH_start = 23;
	final int INSIDE_MODULESOURCEPATH_start = 24;
	final int INSIDE_ADD_EXPORTS = 25;
	final int INSIDE_ADD_READS = 26;
	final int INSIDE_SYSTEM = 27;
	final int INSIDE_PROCESSOR_MODULE_PATH_start = 28;
	final int INSIDE_ADD_MODULES = 29;
	final int INSIDE_RELEASE = 30;
	final int INSIDE_LIMIT_MODULES = 31;
	final int INSIDE_MODULE_VERSION = 32;

	final int DEFAULT = 0;
	ArrayList<String> bootclasspaths = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
	String sourcepathClasspathArg = null;
	String modulepathArg = null;
	String moduleSourcepathArg = null;
	ArrayList<String> sourcepathClasspaths = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
	ArrayList<String> classpaths = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
	ArrayList<String> extdirsClasspaths = null;
	ArrayList<String> endorsedDirClasspaths = null;
	this.annotationPaths = null;
	this.annotationsFromClasspath = false;

	int index = -1;
	int filesCount = 0;
	int classCount = 0;
	int argCount = argv.length;
	int mode = DEFAULT;
	this.maxRepetition = 0;
	boolean printUsageRequired = false;
	String usageSection = null;
	boolean printVersionRequired = false;

	boolean didSpecifyDeprecation = false;
	boolean didSpecifyCompliance = false;
	boolean didSpecifyDisabledAnnotationProcessing = false;

	String customEncoding = null;
	String customDestinationPath = null;
	String currentSourceDirectory = null;
	String currentArg = Util.EMPTY_STRING;

	Set<String> specifiedEncodings = null;

	// expand the command line if necessary
	boolean needExpansion = false;
	loop: for (int i = 0; i < argCount; i++) {
			if (argv[i].startsWith("@")) { //$NON-NLS-1$
				needExpansion = true;
				break loop;
			}
	}

	String[] newCommandLineArgs = null;
	if (needExpansion) {
		newCommandLineArgs = new String[argCount];
		index = 0;
		for (int i = 0; i < argCount; i++) {
			String[] newArgs = null;
			String arg = argv[i].trim();
			if (arg.startsWith("@")) { //$NON-NLS-1$
				try {
					LineNumberReader reader = new LineNumberReader(new StringReader(new String(Util.getFileCharContent(new File(arg.substring(1)), null))));
					StringBuilder buffer = new StringBuilder();
					String line;
					while((line = reader.readLine()) != null) {
						line = line.trim();
						if (!line.startsWith("#")) { //$NON-NLS-1$
							buffer.append(line).append(" "); //$NON-NLS-1$
						}
					}
					newArgs = tokenize(buffer.toString());
				} catch(IOException e) {
					throw new IllegalArgumentException(
						this.bind("configure.invalidexpansionargumentname", arg)); //$NON-NLS-1$
				}
			}
			if (newArgs != null) {
				int newCommandLineArgsLength = newCommandLineArgs.length;
				int newArgsLength = newArgs.length;
				System.arraycopy(newCommandLineArgs, 0, (newCommandLineArgs = new String[newCommandLineArgsLength + newArgsLength - 1]), 0, index);
				System.arraycopy(newArgs, 0, newCommandLineArgs, index, newArgsLength);
				index += newArgsLength;
			} else {
				newCommandLineArgs[index++] = arg;
			}
		}
		index = -1;
	} else {
		newCommandLineArgs = argv;
		for (int i = 0; i < argCount; i++) {
			newCommandLineArgs[i] = newCommandLineArgs[i].trim();
		}
	}
	argCount = newCommandLineArgs.length;
	this.expandedCommandLine = newCommandLineArgs;
	while (++index < argCount) {

		if (customEncoding != null) {
			throw new IllegalArgumentException(
				this.bind("configure.unexpectedCustomEncoding", currentArg, customEncoding)); //$NON-NLS-1$
		}

		currentArg = newCommandLineArgs[index];

		switch(mode) {
			case DEFAULT :
				if (currentArg.startsWith("-nowarn")) { //$NON-NLS-1$
					switch (currentArg.length()) {
						case 7:
							disableAll(ProblemSeverities.Warning);
							break;
						case 8:
							throw new IllegalArgumentException(this.bind(
									"configure.invalidNowarnOption", currentArg)); //$NON-NLS-1$
						default:
							int foldersStart = currentArg.indexOf('[') + 1;
							int foldersEnd = currentArg.lastIndexOf(']');
							if (foldersStart <= 8 || foldersEnd == -1 || foldersStart > foldersEnd
									|| foldersEnd < currentArg.length() - 1) {
								throw new IllegalArgumentException(this.bind(
										"configure.invalidNowarnOption", currentArg)); //$NON-NLS-1$
							}
							String folders = currentArg.substring(foldersStart, foldersEnd);
							if (folders.length() > 0) {
								char[][] currentFolders = decodeIgnoreOptionalProblemsFromFolders(folders);
								if (this.ignoreOptionalProblemsFromFolders != null) {
									int length = this.ignoreOptionalProblemsFromFolders.length + currentFolders.length;
									char[][] tempFolders = new char[length][];
									System.arraycopy(this.ignoreOptionalProblemsFromFolders, 0, tempFolders, 0, this.ignoreOptionalProblemsFromFolders.length);
									System.arraycopy(currentFolders, 0, tempFolders, this.ignoreOptionalProblemsFromFolders.length, currentFolders.length);
									this.ignoreOptionalProblemsFromFolders = tempFolders;
								} else {
									this.ignoreOptionalProblemsFromFolders = currentFolders;
								}
							} else {
								throw new IllegalArgumentException(this.bind(
										"configure.invalidNowarnOption", currentArg)); //$NON-NLS-1$
							}
					}
					mode = DEFAULT;
					continue;
				}
				if (currentArg.startsWith("[")) { //$NON-NLS-1$
					throw new IllegalArgumentException(
						this.bind("configure.unexpectedBracket", //$NON-NLS-1$
									currentArg));
				}

				if (currentArg.endsWith("]")) { //$NON-NLS-1$
					// look for encoding specification
					int encodingStart = currentArg.indexOf('[') + 1;
					if (encodingStart <= 1) {
						throw new IllegalArgumentException(
								this.bind("configure.unexpectedBracket", currentArg)); //$NON-NLS-1$
					}
					int encodingEnd = currentArg.length() - 1;
					if (encodingStart >= 1) {
						if (encodingStart < encodingEnd) {
							customEncoding = currentArg.substring(encodingStart, encodingEnd);
							try { // ensure encoding is supported
								new InputStreamReader(new ByteArrayInputStream(new byte[0]), customEncoding);
							} catch (UnsupportedEncodingException e) {
								throw new IllegalArgumentException(
									this.bind("configure.unsupportedEncoding", customEncoding), e); //$NON-NLS-1$
							}
						}
						currentArg = currentArg.substring(0, encodingStart - 1);
					}
				}

				if (currentArg.endsWith(SuffixConstants.SUFFIX_STRING_java)) {
					if (this.filenames == null) {
						this.filenames = new String[argCount - index];
						this.encodings = new String[argCount - index];
						this.destinationPaths = new String[argCount - index];
					} else if (filesCount == this.filenames.length) {
						int length = this.filenames.length;
						System.arraycopy(
							this.filenames,
							0,
							(this.filenames = new String[length + argCount - index]),
							0,
							length);
						System.arraycopy(
							this.encodings,
							0,
							(this.encodings = new String[length + argCount - index]),
							0,
							length);
						System.arraycopy(
							this.destinationPaths,
							0,
							(this.destinationPaths = new String[length + argCount - index]),
							0,
							length);
					}
					this.filenames[filesCount] = currentArg;
					this.encodings[filesCount++] = customEncoding;
					// destination path cannot be specified upon an individual file
					customEncoding = null;
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-log")) { //$NON-NLS-1$
					if (this.log != null)
						throw new IllegalArgumentException(
							this.bind("configure.duplicateLog", currentArg)); //$NON-NLS-1$
					mode = INSIDE_LOG;
					continue;
				}
				if (currentArg.equals("-repeat")) { //$NON-NLS-1$
					if (this.maxRepetition > 0)
						throw new IllegalArgumentException(
							this.bind("configure.duplicateRepeat", currentArg)); //$NON-NLS-1$
					mode = INSIDE_REPETITION;
					continue;
				}
				if (currentArg.equals("-maxProblems")) { //$NON-NLS-1$
					if (this.maxProblems > 0)
						throw new IllegalArgumentException(
							this.bind("configure.duplicateMaxProblems", currentArg)); //$NON-NLS-1$
					mode = INSIDE_MAX_PROBLEMS;
					continue;
				}
				if (currentArg.equals("--release")) { //$NON-NLS-1$
					mode = INSIDE_RELEASE;
					continue;
				}
				if (currentArg.equals("-source")) { //$NON-NLS-1$
					mode = INSIDE_SOURCE;
					continue;
				}
				if (currentArg.equals("-encoding")) { //$NON-NLS-1$
					mode = INSIDE_DEFAULT_ENCODING;
					continue;
				}
				if (currentArg.startsWith("-")) { //$NON-NLS-1$
					String version = optionStringToVersion(currentArg.substring(1));
					if (version != null) {
						if (didSpecifyCompliance) {
							throw new IllegalArgumentException(
								this.bind("configure.duplicateCompliance", currentArg));//$NON-NLS-1$
						}
						didSpecifyCompliance = true;
						this.options.put(CompilerOptions.OPTION_Compliance, version);
						mode = DEFAULT;
						continue;
					}
				}
				if (currentArg.equals("-d")) { //$NON-NLS-1$
					if (this.destinationPath != null) {
						StringBuilder errorMessage = new StringBuilder();
						errorMessage.append(currentArg);
						if ((index + 1) < argCount) {
							errorMessage.append(' ');
							errorMessage.append(newCommandLineArgs[index + 1]);
						}
						throw new IllegalArgumentException(
							this.bind("configure.duplicateOutputPath", errorMessage.toString())); //$NON-NLS-1$
					}
					mode = INSIDE_DESTINATION_PATH;
					continue;
				}
				if (currentArg.equals("-classpath") //$NON-NLS-1$
					|| currentArg.equals("-cp")) { //$NON-NLS-1$
					mode = INSIDE_CLASSPATH_start;
					continue;
				}
				if (currentArg.equals("-bootclasspath")) {//$NON-NLS-1$
					if (bootclasspaths.size() > 0) {
						StringBuilder errorMessage = new StringBuilder();
						errorMessage.append(currentArg);
						if ((index + 1) < argCount) {
							errorMessage.append(' ');
							errorMessage.append(newCommandLineArgs[index + 1]);
						}
						throw new IllegalArgumentException(
							this.bind("configure.duplicateBootClasspath", errorMessage.toString())); //$NON-NLS-1$
					}
					mode = INSIDE_BOOTCLASSPATH_start;
					continue;
				}
				if (currentArg.equals("--enable-preview")) { //$NON-NLS-1$
					this.enablePreview = true;
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("--system")) { //$NON-NLS-1$
					mode = INSIDE_SYSTEM;
					continue;
				}
				if (currentArg.equals("--module-path") || currentArg.equals("-p")) { //$NON-NLS-1$ //$NON-NLS-2$
					mode = INSIDE_MODULEPATH_start;
					continue;
				}
				if (currentArg.equals("--module-source-path")) { //$NON-NLS-1$
					if (sourcepathClasspathArg != null) {
						throw new IllegalArgumentException(this.bind("configure.OneOfModuleOrSourcePath")); //$NON-NLS-1$
					}
					mode = INSIDE_MODULESOURCEPATH_start;
					continue;
				}
				if (currentArg.equals("--add-exports")) { //$NON-NLS-1$
					mode = INSIDE_ADD_EXPORTS;
					continue;
				}
				if (currentArg.equals("--add-reads")) { //$NON-NLS-1$
					mode = INSIDE_ADD_READS;
					continue;
				}
				if (currentArg.equals("--add-modules")) { //$NON-NLS-1$
					mode = INSIDE_ADD_MODULES;
					continue;
				}
				if (currentArg.equals("--limit-modules")) { //$NON-NLS-1$
					mode = INSIDE_LIMIT_MODULES;
					continue;
				}
				if (currentArg.equals("--module-version")) { //$NON-NLS-1$
					mode = INSIDE_MODULE_VERSION;
					continue;
				}
				if (currentArg.equals("-sourcepath")) {//$NON-NLS-1$
					if (sourcepathClasspathArg != null) {
						StringBuilder errorMessage = new StringBuilder();
						errorMessage.append(currentArg);
						if ((index + 1) < argCount) {
							errorMessage.append(' ');
							errorMessage.append(newCommandLineArgs[index + 1]);
						}
						throw new IllegalArgumentException(
							this.bind("configure.duplicateSourcepath", errorMessage.toString())); //$NON-NLS-1$
					}
					if (moduleSourcepathArg != null) {
						throw new IllegalArgumentException(this.bind("configure.OneOfModuleOrSourcePath")); //$NON-NLS-1$
					}
					mode = INSIDE_SOURCE_PATH_start;
					continue;
				}
				if (currentArg.equals("-extdirs")) {//$NON-NLS-1$
					if (extdirsClasspaths != null) {
						StringBuilder errorMessage = new StringBuilder();
						errorMessage.append(currentArg);
						if ((index + 1) < argCount) {
							errorMessage.append(' ');
							errorMessage.append(newCommandLineArgs[index + 1]);
						}
						throw new IllegalArgumentException(
							this.bind("configure.duplicateExtDirs", errorMessage.toString())); //$NON-NLS-1$
					}
					mode = INSIDE_EXT_DIRS;
					continue;
				}
				if (currentArg.equals("-endorseddirs")) { //$NON-NLS-1$
					if (endorsedDirClasspaths != null) {
						StringBuilder errorMessage = new StringBuilder();
						errorMessage.append(currentArg);
						if ((index + 1) < argCount) {
							errorMessage.append(' ');
							errorMessage.append(newCommandLineArgs[index + 1]);
						}
						throw new IllegalArgumentException(
							this.bind("configure.duplicateEndorsedDirs", errorMessage.toString())); //$NON-NLS-1$
					}
					mode = INSIDE_ENDORSED_DIRS;
					continue;
				}
				if (currentArg.equals("-progress")) { //$NON-NLS-1$
					mode = DEFAULT;
					this.showProgress = true;
					continue;
				}
				if (currentArg.startsWith("-proceedOnError")) { //$NON-NLS-1$
					mode = DEFAULT;
					int length = currentArg.length();
					if (length > 15) {
						if (currentArg.equals("-proceedOnError:Fatal")) { //$NON-NLS-1$
							this.options.put(CompilerOptions.OPTION_FatalOptionalError, CompilerOptions.ENABLED);
						} else {
							throw new IllegalArgumentException(
									this.bind("configure.invalidWarningConfiguration", currentArg)); //$NON-NLS-1$
						}
					} else {
						this.options.put(CompilerOptions.OPTION_FatalOptionalError, CompilerOptions.DISABLED);
					}
					this.proceedOnError = true;
					continue;
				}
				if (currentArg.equals("-failOnWarning")) { //$NON-NLS-1$
					mode = DEFAULT;
					this.failOnWarning = true;
					continue;
				}
				if (currentArg.equals("-time")) { //$NON-NLS-1$
					mode = DEFAULT;
					this.timing = TIMING_ENABLED;
					continue;
				}
				if (currentArg.equals("-time:detail")) { //$NON-NLS-1$
					mode = DEFAULT;
					this.timing = TIMING_ENABLED|TIMING_DETAILED;
					continue;
				}
				if (currentArg.equals("-version") //$NON-NLS-1$
						|| currentArg.equals("-v")) { //$NON-NLS-1$
					this.logger.logVersion(true);
					this.proceed = false;
					return;
				}
				if (currentArg.equals("-showversion")) { //$NON-NLS-1$
					printVersionRequired = true;
					mode = DEFAULT;
					continue;
				}
				if ("-deprecation".equals(currentArg)) { //$NON-NLS-1$
					didSpecifyDeprecation = true;
					this.options.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.WARNING);
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-help") || currentArg.equals("-?")) { //$NON-NLS-1$ //$NON-NLS-2$
					printUsageRequired = true;
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-help:warn") || //$NON-NLS-1$
						currentArg.equals("-?:warn")) { //$NON-NLS-1$
					printUsageRequired = true;
					usageSection = "misc.usage.warn"; //$NON-NLS-1$
					continue;
				}
				if (currentArg.equals("-noExit")) { //$NON-NLS-1$
					this.systemExitWhenFinished = false;
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-verbose")) { //$NON-NLS-1$
					this.verbose = true;
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-referenceInfo")) { //$NON-NLS-1$
					this.produceRefInfo = true;
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-inlineJSR")) { //$NON-NLS-1$
					mode = DEFAULT;
					this.options.put(
							CompilerOptions.OPTION_InlineJsr,
							CompilerOptions.ENABLED);
					continue;
				}
				if (currentArg.equals("-parameters")) { //$NON-NLS-1$
					mode = DEFAULT;
					this.options.put(
							CompilerOptions.OPTION_MethodParametersAttribute,
							CompilerOptions.GENERATE);
					continue;
				}
				if (currentArg.equals("-genericsignature")) { //$NON-NLS-1$
					mode = DEFAULT;
					this.options.put(
							CompilerOptions.OPTION_LambdaGenericSignature,
							CompilerOptions.GENERATE);
					continue;
				}
				if (currentArg.startsWith("-g")) { //$NON-NLS-1$
					mode = DEFAULT;
					String debugOption = currentArg;
					int length = currentArg.length();
					if (length == 2) {
						this.options.put(
							CompilerOptions.OPTION_LocalVariableAttribute,
							CompilerOptions.GENERATE);
						this.options.put(
							CompilerOptions.OPTION_LineNumberAttribute,
							CompilerOptions.GENERATE);
						this.options.put(
							CompilerOptions.OPTION_SourceFileAttribute,
							CompilerOptions.GENERATE);
						continue;
					}
					if (length > 3) {
						this.options.put(
							CompilerOptions.OPTION_LocalVariableAttribute,
							CompilerOptions.DO_NOT_GENERATE);
						this.options.put(
							CompilerOptions.OPTION_LineNumberAttribute,
							CompilerOptions.DO_NOT_GENERATE);
						this.options.put(
							CompilerOptions.OPTION_SourceFileAttribute,
							CompilerOptions.DO_NOT_GENERATE);
						if (length == 7 && debugOption.equals("-g:" + NONE)) //$NON-NLS-1$
							continue;
						StringTokenizer tokenizer =
							new StringTokenizer(debugOption.substring(3, debugOption.length()), ","); //$NON-NLS-1$
						while (tokenizer.hasMoreTokens()) {
							String token = tokenizer.nextToken();
							if (token.equals("vars")) { //$NON-NLS-1$
								this.options.put(
									CompilerOptions.OPTION_LocalVariableAttribute,
									CompilerOptions.GENERATE);
							} else if (token.equals("lines")) { //$NON-NLS-1$
								this.options.put(
									CompilerOptions.OPTION_LineNumberAttribute,
									CompilerOptions.GENERATE);
							} else if (token.equals("source")) { //$NON-NLS-1$
								this.options.put(
									CompilerOptions.OPTION_SourceFileAttribute,
									CompilerOptions.GENERATE);
							} else {
								throw new IllegalArgumentException(
									this.bind("configure.invalidDebugOption", debugOption)); //$NON-NLS-1$
							}
						}
						continue;
					}
					throw new IllegalArgumentException(
						this.bind("configure.invalidDebugOption", debugOption)); //$NON-NLS-1$
				}
				if (currentArg.startsWith("-info")) { //$NON-NLS-1$
					mode = DEFAULT;
					String infoOption = currentArg;
					int length = currentArg.length();
					if (length == 10 && infoOption.equals("-info:" + NONE)) { //$NON-NLS-1$
						disableAll(ProblemSeverities.Info);
						continue;
					}
					if (length <= 6) {
						throw new IllegalArgumentException(
							this.bind("configure.invalidInfoConfiguration", infoOption)); //$NON-NLS-1$
					}
					int infoTokenStart;
					boolean isEnabling;
					switch (infoOption.charAt(6)) {
						case '+' :
							infoTokenStart = 7;
							isEnabling = true;
							break;
						case '-' :
							infoTokenStart = 7;
							isEnabling = false; // specified warnings are disabled
							break;
						default:
							disableAll(ProblemSeverities.Info);
							infoTokenStart = 6;
							isEnabling = true;
					}

					StringTokenizer tokenizer =
						new StringTokenizer(infoOption.substring(infoTokenStart, infoOption.length()), ","); //$NON-NLS-1$
					int tokenCounter = 0;

					while (tokenizer.hasMoreTokens()) {
						String token = tokenizer.nextToken();
						tokenCounter++;
						switch(token.charAt(0)) {
							case '+' :
								isEnabling = true;
								token = token.substring(1);
								break;
							case '-' :
								isEnabling = false;
								token = token.substring(1);
						}
						handleInfoToken(token, isEnabling);
					}
					if (tokenCounter == 0) {
						throw new IllegalArgumentException(
							this.bind("configure.invalidInfoOption", currentArg)); //$NON-NLS-1$
					}
					continue;
				}
				if (currentArg.startsWith("-warn")) { //$NON-NLS-1$
					mode = DEFAULT;
					String warningOption = currentArg;
					int length = currentArg.length();
					if (length == 10 && warningOption.equals("-warn:" + NONE)) { //$NON-NLS-1$
						disableAll(ProblemSeverities.Warning);
						continue;
					}
					if (length <= 6) {
						throw new IllegalArgumentException(
							this.bind("configure.invalidWarningConfiguration", warningOption)); //$NON-NLS-1$
					}
					int warnTokenStart;
					boolean isEnabling;
					switch (warningOption.charAt(6)) {
						case '+' :
							warnTokenStart = 7;
							isEnabling = true;
							break;
						case '-' :
							warnTokenStart = 7;
							isEnabling = false; // specified warnings are disabled
							break;
						default:
							disableAll(ProblemSeverities.Warning);
							warnTokenStart = 6;
							isEnabling = true;
					}

					StringTokenizer tokenizer =
						new StringTokenizer(warningOption.substring(warnTokenStart, warningOption.length()), ","); //$NON-NLS-1$
					int tokenCounter = 0;

					if (didSpecifyDeprecation) {  // deprecation could have also been set through -deprecation option
						this.options.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.WARNING);
					}

					while (tokenizer.hasMoreTokens()) {
						String token = tokenizer.nextToken();
						tokenCounter++;
						switch(token.charAt(0)) {
							case '+' :
								isEnabling = true;
								token = token.substring(1);
								break;
							case '-' :
								isEnabling = false;
								token = token.substring(1);
						}
						handleWarningToken(token, isEnabling);
					}
					if (tokenCounter == 0) {
						throw new IllegalArgumentException(
							this.bind("configure.invalidWarningOption", currentArg)); //$NON-NLS-1$
					}
					continue;
				}
				if (currentArg.startsWith("-err")) { //$NON-NLS-1$
					mode = DEFAULT;
					String errorOption = currentArg;
					int length = currentArg.length();
					if (length <= 5) {
						throw new IllegalArgumentException(
							this.bind("configure.invalidErrorConfiguration", errorOption)); //$NON-NLS-1$
					}
					int errorTokenStart;
					boolean isEnabling;
					switch (errorOption.charAt(5)) {
						case '+' :
							errorTokenStart = 6;
							isEnabling = true;
							break;
						case '-' :
							errorTokenStart = 6;
							isEnabling = false; // specified errors are disabled
							break;
						default:
							disableAll(ProblemSeverities.Error);
							errorTokenStart = 5;
							isEnabling = true;
					}

					StringTokenizer tokenizer =
						new StringTokenizer(errorOption.substring(errorTokenStart, errorOption.length()), ","); //$NON-NLS-1$
					int tokenCounter = 0;

					while (tokenizer.hasMoreTokens()) {
						String token = tokenizer.nextToken();
						tokenCounter++;
						switch(token.charAt(0)) {
							case '+' :
								isEnabling = true;
								token = token.substring(1);
								break;
							case '-' :
								isEnabling = false;
								token = token.substring(1);
								break;
						}
						handleErrorToken(token, isEnabling);
					}
					if (tokenCounter == 0) {
						throw new IllegalArgumentException(
							this.bind("configure.invalidErrorOption", currentArg)); //$NON-NLS-1$
					}
					continue;
				}
				if (currentArg.equals("-target")) { //$NON-NLS-1$
					mode = INSIDE_TARGET;
					continue;
				}
				if (currentArg.equals("-preserveAllLocals")) { //$NON-NLS-1$
					this.options.put(
						CompilerOptions.OPTION_PreserveUnusedLocal,
						CompilerOptions.PRESERVE);
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-enableJavadoc")) {//$NON-NLS-1$
					mode = DEFAULT;
					this.enableJavadocOn = true;
					continue;
				}
				if (currentArg.equals("-Xemacs")) { //$NON-NLS-1$
					mode = DEFAULT;
					this.logger.setEmacs();
					continue;
				}
				// annotation processing
				if (currentArg.startsWith("-A")) { //$NON-NLS-1$
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-processorpath")) { //$NON-NLS-1$
					mode = INSIDE_PROCESSOR_PATH_start;
					continue;
				}
				if (currentArg.equals("-processor")) { //$NON-NLS-1$
					mode = INSIDE_PROCESSOR_start;
					continue;
				}
				if (currentArg.equals("--processor-module-path")) { //$NON-NLS-1$
					mode = INSIDE_PROCESSOR_MODULE_PATH_start;
					continue;
				}
				if (currentArg.equals("-proc:only")) { //$NON-NLS-1$
					this.options.put(
						CompilerOptions.OPTION_GenerateClassFiles,
						CompilerOptions.DISABLED);
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-proc:none")) { //$NON-NLS-1$
					didSpecifyDisabledAnnotationProcessing = true;
					this.options.put(
						CompilerOptions.OPTION_Process_Annotations,
						CompilerOptions.DISABLED);
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-s")) { //$NON-NLS-1$
					mode = INSIDE_S_start;
					continue;
				}
				if (currentArg.equals("-XprintProcessorInfo") //$NON-NLS-1$
						|| currentArg.equals("-XprintRounds")) { //$NON-NLS-1$
					mode = DEFAULT;
					continue;
				}
				// tolerated javac options - quietly filtered out
				if (currentArg.startsWith("-X")) { //$NON-NLS-1$
					mode = DEFAULT;
					continue;
				}
				if (currentArg.startsWith("-J")) { //$NON-NLS-1$
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-O")) { //$NON-NLS-1$
					mode = DEFAULT;
					continue;
				}
				if (currentArg.equals("-classNames")) { //$NON-NLS-1$
					mode = INSIDE_CLASS_NAMES;
					continue;
				}
				if (currentArg.equals("-properties")) { //$NON-NLS-1$
					mode = INSIDE_WARNINGS_PROPERTIES;
					continue;
				}
				if (currentArg.equals("-missingNullDefault")) { //$NON-NLS-1$
					this.options.put(CompilerOptions.OPTION_ReportMissingNonNullByDefaultAnnotation, CompilerOptions.WARNING);
					continue;
				}
				if (currentArg.equals("-annotationpath")) { //$NON-NLS-1$
					mode = INSIDE_ANNOTATIONPATH_start;
					continue;
				}
				break;
			case INSIDE_TARGET :
				if (this.didSpecifyTarget) {
					throw new IllegalArgumentException(
						this.bind("configure.duplicateTarget", currentArg));//$NON-NLS-1$
				}
				if (this.releaseVersion != null) {
					throw new IllegalArgumentException(
							this.bind("configure.unsupportedWithRelease", "-target"));//$NON-NLS-1$ //$NON-NLS-2$
				}
				this.didSpecifyTarget = true;
				if (currentArg.equals("1.1")) { //$NON-NLS-1$
					this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_1);
				} else if (currentArg.equals("1.2")) { //$NON-NLS-1$
					this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_2);
				} else if (currentArg.equals("jsr14")) { //$NON-NLS-1$
					this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_JSR14);
				} else if (currentArg.equals("cldc1.1")) { //$NON-NLS-1$
					this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_CLDC1_1);
					this.options.put(CompilerOptions.OPTION_InlineJsr, CompilerOptions.ENABLED);
				} else {
					String version = optionStringToVersion(currentArg);
					if (version != null) {
						this.options.put(CompilerOptions.OPTION_TargetPlatform, version);
					} else {
						throw new IllegalArgumentException(this.bind("configure.targetJDK", currentArg)); //$NON-NLS-1$
					}
				}
				mode = DEFAULT;
				continue;
			case INSIDE_LOG :
				this.log = currentArg;
				mode = DEFAULT;
				continue;
			case INSIDE_REPETITION :
				try {
					this.maxRepetition = Integer.parseInt(currentArg);
					if (this.maxRepetition <= 0) {
						throw new IllegalArgumentException(this.bind("configure.repetition", currentArg)); //$NON-NLS-1$
					}
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(this.bind("configure.repetition", currentArg), e); //$NON-NLS-1$
				}
				mode = DEFAULT;
				continue;
			case INSIDE_MAX_PROBLEMS :
				try {
					this.maxProblems = Integer.parseInt(currentArg);
					if (this.maxProblems <= 0) {
						throw new IllegalArgumentException(this.bind("configure.maxProblems", currentArg)); //$NON-NLS-1$
					}
					this.options.put(CompilerOptions.OPTION_MaxProblemPerUnit, currentArg);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(this.bind("configure.maxProblems", currentArg), e); //$NON-NLS-1$
				}
				mode = DEFAULT;
				continue;
			case INSIDE_RELEASE:
				// If release is < 9, the following are disallowed:
				// bootclasspath, -Xbootclasspath, -Xbootclasspath/a:, -Xbootclasspath/p:,
				// -endorseddirs, -Djava.endorsed.dirs, -extdirs, -Djava.ext.dirs

				// If release >= 9, the following are disallowed
				// --system and --upgrade-module-path

				// -source and -target are disallowed for any --release
				this.releaseVersion = currentArg;
				long releaseToJDKLevel = CompilerOptions.releaseToJDKLevel(currentArg);
				if (releaseToJDKLevel == 0) {
					throw new IllegalArgumentException(
							this.bind("configure.unsupportedReleaseVersion", currentArg)); //$NON-NLS-1$
				}
				// Let's treat it as regular compliance mode
				this.complianceLevel = releaseToJDKLevel;
				String versionAsString = CompilerOptions.versionFromJdkLevel(releaseToJDKLevel);
				this.options.put(CompilerOptions.OPTION_Compliance, versionAsString);
				this.options.put(CompilerOptions.OPTION_Source, versionAsString);
				this.options.put(CompilerOptions.OPTION_TargetPlatform, versionAsString);
				mode = DEFAULT;
				continue;
			case INSIDE_SOURCE :
				if (this.didSpecifySource) {
					throw new IllegalArgumentException(
						this.bind("configure.duplicateSource", currentArg));//$NON-NLS-1$
				}
				if (this.releaseVersion != null) {
					throw new IllegalArgumentException(
							this.bind("configure.unsupportedWithRelease", "-source"));//$NON-NLS-1$ //$NON-NLS-2$
				}
				this.didSpecifySource = true;
				String version = optionStringToVersion(currentArg);
				if (version != null) {
					this.options.put(CompilerOptions.OPTION_Source, version);
				} else {
					throw new IllegalArgumentException(this.bind("configure.source", currentArg)); //$NON-NLS-1$
				}
				mode = DEFAULT;
				continue;
			case INSIDE_DEFAULT_ENCODING :
				if (specifiedEncodings != null) {
					// check already defined encoding
					if (!specifiedEncodings.contains(currentArg)) {
						if (specifiedEncodings.size() > 1) {
							this.logger.logWarning(
									this.bind("configure.differentencodings", //$NON-NLS-1$
									currentArg,
									getAllEncodings(specifiedEncodings)));
						} else {
							this.logger.logWarning(
									this.bind("configure.differentencoding", //$NON-NLS-1$
									currentArg,
									getAllEncodings(specifiedEncodings)));
						}
					}
				} else {
					specifiedEncodings = new HashSet<>();
				}
				try { // ensure encoding is supported
					new InputStreamReader(new ByteArrayInputStream(new byte[0]), currentArg);
				} catch (UnsupportedEncodingException e) {
					throw new IllegalArgumentException(
						this.bind("configure.unsupportedEncoding", currentArg), e); //$NON-NLS-1$
				}
				specifiedEncodings.add(currentArg);
				this.options.put(CompilerOptions.OPTION_Encoding, currentArg);
				mode = DEFAULT;
				continue;
			case INSIDE_DESTINATION_PATH :
				setDestinationPath(currentArg.equals(NONE) ? NONE : currentArg);
				mode = DEFAULT;
				continue;
			case INSIDE_SYSTEM:
				mode = DEFAULT;
				setJavaHome(currentArg);
				continue;
			case INSIDE_MODULEPATH_start:
				mode = DEFAULT;
				String[] modulepaths = new String[1];
				index += processPaths(newCommandLineArgs, index, currentArg, modulepaths);
				modulepathArg = modulepaths[0];
				continue;
			case INSIDE_MODULESOURCEPATH_start:
				mode = DEFAULT;
				String[] moduleSourcepaths = new String[1];
				index += processPaths(newCommandLineArgs, index, currentArg, moduleSourcepaths);
				moduleSourcepathArg = moduleSourcepaths[0];
				continue;
			case INSIDE_ADD_EXPORTS:
				mode = DEFAULT;
				// TODO: better to validate the option before processing it further?
				if (this.addonExports == Collections.EMPTY_LIST) {
					this.addonExports = new ArrayList<>();
				}
				this.addonExports.add(currentArg);
				continue;
			case INSIDE_ADD_READS:
				mode = DEFAULT;
				if (this.addonReads == Collections.EMPTY_LIST) {
					this.addonReads = new ArrayList<>();
				}
				this.addonReads.add(currentArg);
				continue;
			case INSIDE_ADD_MODULES:
				mode = DEFAULT;
				if (this.rootModules == Collections.EMPTY_SET) {
					this.rootModules = new HashSet<>();
				}
				StringTokenizer tokenizer = new StringTokenizer(currentArg, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens()) {
					this.rootModules.add(tokenizer.nextToken().trim());
				}
				continue;
			case INSIDE_LIMIT_MODULES:
				mode = DEFAULT;
				tokenizer = new StringTokenizer(currentArg, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens()) {
					if (this.limitedModules == null) {
						this.limitedModules = new HashSet<>();
					}
					this.limitedModules.add(tokenizer.nextToken().trim());
				}
				continue;
			case INSIDE_MODULE_VERSION:
				mode = DEFAULT;
				this.moduleVersion = validateModuleVersion(currentArg);
				continue;
			case INSIDE_CLASSPATH_start:
				mode = DEFAULT;
				index += processPaths(newCommandLineArgs, index, currentArg, classpaths);
				continue;
			case INSIDE_BOOTCLASSPATH_start:
				mode = DEFAULT;
				index += processPaths(newCommandLineArgs, index, currentArg, bootclasspaths);
				continue;
			case INSIDE_SOURCE_PATH_start:
				mode = DEFAULT;
				String[] sourcePaths = new String[1];
				index += processPaths(newCommandLineArgs, index, currentArg, sourcePaths);
				sourcepathClasspathArg = sourcePaths[0];
				continue;
			case INSIDE_EXT_DIRS:
				if (currentArg.indexOf("[-d") != -1) { //$NON-NLS-1$
					throw new IllegalArgumentException(
						this.bind("configure.unexpectedDestinationPathEntry", //$NON-NLS-1$
							"-extdir")); //$NON-NLS-1$
				}
				tokenizer = new StringTokenizer(currentArg,	File.pathSeparator, false);
				extdirsClasspaths = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
				while (tokenizer.hasMoreTokens())
					extdirsClasspaths.add(tokenizer.nextToken());
				mode = DEFAULT;
				continue;
			case INSIDE_ENDORSED_DIRS:
				if (currentArg.indexOf("[-d") != -1) { //$NON-NLS-1$
					throw new IllegalArgumentException(
						this.bind("configure.unexpectedDestinationPathEntry", //$NON-NLS-1$
							"-endorseddirs")); //$NON-NLS-1$
				}				tokenizer = new StringTokenizer(currentArg,	File.pathSeparator, false);
				endorsedDirClasspaths = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
				while (tokenizer.hasMoreTokens())
					endorsedDirClasspaths.add(tokenizer.nextToken());
				mode = DEFAULT;
				continue;
			case INSIDE_SOURCE_DIRECTORY_DESTINATION_PATH:
				if (currentArg.endsWith("]")) { //$NON-NLS-1$
					customDestinationPath = currentArg.substring(0,
						currentArg.length() - 1);
				} else {
					throw new IllegalArgumentException(
						this.bind("configure.incorrectDestinationPathEntry", //$NON-NLS-1$
							"[-d " + currentArg)); //$NON-NLS-1$
				}
				break;
			case INSIDE_PROCESSOR_PATH_start :
				// nothing to do here. This is consumed again by the AnnotationProcessorManager
				mode = DEFAULT;
				continue;
			case INSIDE_PROCESSOR_start :
				// nothing to do here. This is consumed again by the AnnotationProcessorManager
				mode = DEFAULT;
				continue;
			case INSIDE_PROCESSOR_MODULE_PATH_start :
				mode = DEFAULT;
				continue;
			case INSIDE_S_start :
				// nothing to do here. This is consumed again by the AnnotationProcessorManager
				mode = DEFAULT;
				continue;
			case INSIDE_CLASS_NAMES :
				tokenizer = new StringTokenizer(currentArg, ","); //$NON-NLS-1$
				if (this.classNames == null) {
					this.classNames = new String[DEFAULT_SIZE_CLASSPATH];
				}
				while (tokenizer.hasMoreTokens()) {
					if (this.classNames.length == classCount) {
						// resize
						System.arraycopy(
							this.classNames,
							0,
							(this.classNames = new String[classCount * 2]),
							0,
							classCount);
					}
					this.classNames[classCount++] = tokenizer.nextToken();
				}
				mode = DEFAULT;
				continue;
			case INSIDE_WARNINGS_PROPERTIES :
				initializeWarnings(currentArg);
				mode = DEFAULT;
				continue;
			case INSIDE_ANNOTATIONPATH_start:
				mode = DEFAULT;
				if (currentArg.isEmpty() || currentArg.charAt(0) == '-')
					throw new IllegalArgumentException(this.bind("configure.missingAnnotationPath", currentArg)); //$NON-NLS-1$
				if (ANNOTATION_SOURCE_CLASSPATH.equals(currentArg)) {
					this.annotationsFromClasspath = true;
				} else {
					if (this.annotationPaths == null)
						this.annotationPaths = new ArrayList<String>();
					StringTokenizer tokens = new StringTokenizer(currentArg, File.pathSeparator);
					while (tokens.hasMoreTokens())
						this.annotationPaths.add(tokens.nextToken());
				}
				continue;
		}

		// default is input directory, if no custom destination path exists
		if (customDestinationPath == null) {
			if (File.separatorChar != '/') {
				currentArg = currentArg.replace('/', File.separatorChar);
			}
			if (currentArg.endsWith("[-d")) { //$NON-NLS-1$
				currentSourceDirectory = currentArg.substring(0,
					currentArg.length() - 3);
				mode = INSIDE_SOURCE_DIRECTORY_DESTINATION_PATH;
				continue;
			}
			currentSourceDirectory = currentArg;
		}
		File dir = new File(currentSourceDirectory);
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException(
				this.bind("configure.unrecognizedOption", currentSourceDirectory)); //$NON-NLS-1$
		}
		String[] result = FileFinder.find(dir, SuffixConstants.SUFFIX_STRING_java);
		if (NONE.equals(customDestinationPath)) {
			customDestinationPath = NONE; // ensure == comparison
		}
		if (this.filenames != null) {
			// some source files were specified explicitly
			int length = result.length;
			System.arraycopy(
				this.filenames,
				0,
				(this.filenames = new String[length + filesCount]),
				0,
				filesCount);
			System.arraycopy(
				this.encodings,
				0,
				(this.encodings = new String[length + filesCount]),
				0,
				filesCount);
			System.arraycopy(
				this.destinationPaths,
				0,
				(this.destinationPaths = new String[length + filesCount]),
				0,
				filesCount);
			System.arraycopy(result, 0, this.filenames, filesCount, length);
			for (int i = 0; i < length; i++) {
				this.encodings[filesCount + i] = customEncoding;
				this.destinationPaths[filesCount + i] = customDestinationPath;
			}
			filesCount += length;
			customEncoding = null;
			customDestinationPath = null;
			currentSourceDirectory = null;
		} else {
			this.filenames = result;
			filesCount = this.filenames.length;
			this.encodings = new String[filesCount];
			this.destinationPaths = new String[filesCount];
			for (int i = 0; i < filesCount; i++) {
				this.encodings[i] = customEncoding;
				this.destinationPaths[i] = customDestinationPath;
			}
			customEncoding = null;
			customDestinationPath = null;
			currentSourceDirectory = null;
		}
		mode = DEFAULT;
		continue;
	}
	if (this.enablePreview) {
		this.options.put(
				CompilerOptions.OPTION_EnablePreviews,
				CompilerOptions.ENABLED);
	}

	// set DocCommentSupport, with appropriate side effects on defaults if
	// javadoc is not enabled
	if (this.enableJavadocOn) {
		this.options.put(
			CompilerOptions.OPTION_DocCommentSupport,
			CompilerOptions.ENABLED);
	} else if (this.warnJavadocOn || this.warnAllJavadocOn) {
		this.options.put(
			CompilerOptions.OPTION_DocCommentSupport,
			CompilerOptions.ENABLED);
		// override defaults: references that are embedded in javadoc are ignored
		// from the perspective of parameters and thrown exceptions usage
		this.options.put(
			CompilerOptions.OPTION_ReportUnusedParameterIncludeDocCommentReference,
			CompilerOptions.DISABLED);
		this.options.put(
			CompilerOptions.OPTION_ReportUnusedDeclaredThrownExceptionIncludeDocCommentReference,
			CompilerOptions.DISABLED);
	}
	// configure warnings for javadoc contents
	if (this.warnJavadocOn) {
		this.options.put(
			CompilerOptions.OPTION_ReportInvalidJavadocTags,
			CompilerOptions.ENABLED);
		this.options.put(
			CompilerOptions.OPTION_ReportInvalidJavadocTagsDeprecatedRef,
			CompilerOptions.ENABLED);
		this.options.put(
			CompilerOptions.OPTION_ReportInvalidJavadocTagsNotVisibleRef,
			CompilerOptions.ENABLED);
		this.options.put(
			CompilerOptions.OPTION_ReportMissingJavadocTagsVisibility,
			CompilerOptions.PRIVATE);
	}
	if (printUsageRequired || (filesCount == 0 && classCount == 0)) {
		if (usageSection ==  null) {
			printUsage(); // default
		} else {
			printUsage(usageSection);
		}
		this.proceed = false;
		return;
	}

	if (this.log != null) {
		this.logger.setLog(this.log);
	} else {
		this.showProgress = false;
	}
	this.logger.logVersion(printVersionRequired);

	validateOptions(didSpecifyCompliance);

	// Enable annotation processing by default in batch mode when compliance is at least 1.6
	// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=185768
	if (!didSpecifyDisabledAnnotationProcessing
			&& CompilerOptions.versionToJdkLevel(this.options.get(CompilerOptions.OPTION_Compliance)) >= ClassFileConstants.JDK1_6) {
		this.options.put(CompilerOptions.OPTION_Process_Annotations, CompilerOptions.ENABLED);
	}

	this.logger.logCommandLineArguments(newCommandLineArgs);
	this.logger.logOptions(this.options);

	if (this.maxRepetition == 0) {
		this.maxRepetition = 1;
	}
	if (this.maxRepetition >= 3 && (this.timing & TIMING_ENABLED) != 0) {
		this.compilerStats = new CompilerStats[this.maxRepetition];
	}

	if (filesCount != 0) {
		System.arraycopy(
			this.filenames,
			0,
			(this.filenames = new String[filesCount]),
			0,
			filesCount);

		this.modNames = new String[filesCount];
	}

	if (classCount != 0) {
		System.arraycopy(
			this.classNames,
			0,
			(this.classNames = new String[classCount]),
			0,
			classCount);
	}

	if (moduleSourcepathArg == null) {
		handleSingleModuleCompilation();
	}

	setPaths(bootclasspaths,
			sourcepathClasspathArg,
			sourcepathClasspaths,
			classpaths,
			modulepathArg,
			moduleSourcepathArg,
			extdirsClasspaths,
			endorsedDirClasspaths,
			customEncoding);

	if (specifiedEncodings != null && specifiedEncodings.size() > 1) {
		this.logger.logWarning(this.bind("configure.multipleencodings", //$NON-NLS-1$
				this.options.get(CompilerOptions.OPTION_Encoding),
				getAllEncodings(specifiedEncodings)));
	}
	if (this.pendingErrors != null) {
		for (Iterator<String> iterator = this.pendingErrors.iterator(); iterator.hasNext(); ) {
			String message = iterator.next();
			this.logger.logPendingError(message);
		}
		this.pendingErrors = null;
	}
}
/** Translates any supported standard version starting at 1.3 up-to latest into the corresponding constant from CompilerOptions */
private String optionStringToVersion(String currentArg) {
	switch (currentArg) {
		case "1.3": return CompilerOptions.VERSION_1_3; //$NON-NLS-1$
		case "1.4": return CompilerOptions.VERSION_1_4; //$NON-NLS-1$
		case "1.5": //$NON-NLS-1$
		case "5": //$NON-NLS-1$
		case "5.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_1_5;
		case "1.6": //$NON-NLS-1$
		case "6": //$NON-NLS-1$
		case "6.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_1_6;
		case "1.7": //$NON-NLS-1$
		case "7": //$NON-NLS-1$
		case "7.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_1_7;
		case "1.8": //$NON-NLS-1$
		case "8": //$NON-NLS-1$
		case "8.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_1_8;
		case "1.9": //$NON-NLS-1$
		case "9": //$NON-NLS-1$
		case "9.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_9;
		case "10": //$NON-NLS-1$
		case "10.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_10;
		case "11": //$NON-NLS-1$
		case "11.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_11;
		case "12": //$NON-NLS-1$
		case "12.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_12;
		case "13": //$NON-NLS-1$
		case "13.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_13;
		case "14": //$NON-NLS-1$
		case "14.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_14;
		case "15": //$NON-NLS-1$
		case "15.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_15;
		case "16": //$NON-NLS-1$
		case "16.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_16;
		case "17": //$NON-NLS-1$
		case "17.0": //$NON-NLS-1$
			return CompilerOptions.VERSION_17;
		default:
			return null;
	}
}
private String validateModuleVersion(String versionString) {
	try {
		Class<?> versionClass = Class.forName("java.lang.module.ModuleDescriptor$Version"); //$NON-NLS-1$
		Method method = versionClass.getMethod("parse", String.class); //$NON-NLS-1$
		try {
			method.invoke(null, versionString);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof IllegalArgumentException)
				throw (IllegalArgumentException) e.getCause();
		}
	} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException e) {
		this.logger.logWarning(this.bind("configure.no.ModuleDescriptorVersionparse")); //$NON-NLS-1$
	}
	return versionString;
}

private Parser getNewParser() {
	return new Parser(new ProblemReporter(getHandlingPolicy(),
			new CompilerOptions(this.options), getProblemFactory()), false);
}
private IModule extractModuleDesc(String fileName) {
	IModule mod = null;
	if (fileName.toLowerCase().endsWith(IModule.MODULE_INFO_JAVA)) {
		// this.options may not be completely populated yet, and definitely not
		// validated. Make sure the source level is set for the parser
		Map<String,String> opts = new HashMap<String, String>(this.options);
		opts.put(CompilerOptions.OPTION_Source, this.options.get(CompilerOptions.OPTION_Compliance));
		Parser parser = new Parser(new ProblemReporter(getHandlingPolicy(),
				new CompilerOptions(opts), getProblemFactory()), false);

		ICompilationUnit cu = new CompilationUnit(null, fileName, null);
		CompilationResult compilationResult = new CompilationResult(cu, 0, 1, 10);
		CompilationUnitDeclaration unit = parser.parse(cu, compilationResult);
		if (unit.isModuleInfo() && unit.moduleDeclaration != null) {
			mod = new BasicModule(unit.moduleDeclaration, null);
		}
	} else if (fileName.toLowerCase().endsWith(IModule.MODULE_INFO_CLASS)) {
		try {
			ClassFileReader reader = ClassFileReader.read(fileName); // Check the absolute path?
			mod = reader.getModuleDeclaration();
		} catch (ClassFormatException | IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(
					this.bind("configure.invalidModuleDescriptor", fileName)); //$NON-NLS-1$
		}
	}
	return mod;
}

private static char[][] decodeIgnoreOptionalProblemsFromFolders(String folders) {
	StringTokenizer tokenizer = new StringTokenizer(folders, File.pathSeparator);
	char[][] result = new char[2 * tokenizer.countTokens()][];
	int count = 0;
	while (tokenizer.hasMoreTokens()) {
		String fileName = tokenizer.nextToken();
		// relative folder names are created relative to the current user dir
		File file = new File(fileName);
		if (file.exists()) {
			String absolutePath = file.getAbsolutePath();
			result[count++] = absolutePath.toCharArray();
			// if the file exists, we should try to use its canonical path
			try {
				String canonicalPath = file.getCanonicalPath();
				if (!absolutePath.equals(canonicalPath)) {
					result[count++] = canonicalPath.toCharArray();
				}
			} catch (IOException e) {
				// ignore
			}
		} else {
			// if the file does not exist, use the name that was specified
			result[count++] = fileName.toCharArray();
		}
	}
	if (count < result.length) {
		char[][] shortened = new char[count][];
		System.arraycopy(result, 0, shortened, 0, count);
		result = shortened;
	}
	return result;
}

private static String getAllEncodings(Set<String> encodings) {
	int size = encodings.size();
	String[] allEncodings = new String[size];
	encodings.toArray(allEncodings);
	Arrays.sort(allEncodings);
	StringBuffer buffer = new StringBuffer();
	for (int i = 0; i < size; i++) {
		if (i > 0) {
			buffer.append(", "); //$NON-NLS-1$
		}
		buffer.append(allEncodings[i]);
	}
	return String.valueOf(buffer);
}
@SuppressWarnings("rawtypes")
private void initializeWarnings(String propertiesFile) {
	File file = new File(propertiesFile);
	if (!file.exists()) {
		throw new IllegalArgumentException(this.bind("configure.missingwarningspropertiesfile", propertiesFile)); //$NON-NLS-1$
	}
	BufferedInputStream stream = null;
	Properties properties = null;
	try {
		stream = new BufferedInputStream(new FileInputStream(propertiesFile));
		properties = new Properties();
		properties.load(stream);
	} catch(IOException e) {
		e.printStackTrace();
		throw new IllegalArgumentException(this.bind("configure.ioexceptionwarningspropertiesfile", propertiesFile)); //$NON-NLS-1$
	} finally {
		if (stream != null) {
			try {
				stream.close();
			} catch(IOException e) {
				// ignore
			}
		}
	}
	for(Iterator iterator = properties.entrySet().iterator(); iterator.hasNext(); ) {
		Map.Entry entry = (Map.Entry) iterator.next();
		final String key = entry.getKey().toString();
		if (key.startsWith("org.eclipse.jdt.core.compiler.")) { //$NON-NLS-1$
			this.options.put(key, entry.getValue().toString());
		}
	}
	// when using a properties file mimic relevant defaults from JavaCorePreferenceInitializer:
	if (!properties.containsKey(CompilerOptions.OPTION_LocalVariableAttribute)) {
		this.options.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
	}
	if (!properties.containsKey(CompilerOptions.OPTION_PreserveUnusedLocal)) {
		this.options.put(CompilerOptions.OPTION_PreserveUnusedLocal, CompilerOptions.PRESERVE);
	}
	if (!properties.containsKey(CompilerOptions.OPTION_DocCommentSupport)) {
		this.options.put(CompilerOptions.OPTION_DocCommentSupport, CompilerOptions.ENABLED);
	}
	if (!properties.containsKey(CompilerOptions.OPTION_ReportForbiddenReference)) {
		this.options.put(CompilerOptions.OPTION_ReportForbiddenReference, CompilerOptions.ERROR);
	}
}
protected void enableAll(int severity) {
	String newValue = null;
	switch(severity) {
		case ProblemSeverities.Error :
			newValue = CompilerOptions.ERROR;
			break;
		case ProblemSeverities.Warning :
			newValue = CompilerOptions.WARNING;
			break;
	}
	Map.Entry<String, String>[] entries = this.options.entrySet().toArray(new Map.Entry[this.options.size()]);
	for (int i = 0, max = entries.length; i < max; i++) {
		Map.Entry<String, String> entry = entries[i];
		if (entry.getValue().equals(CompilerOptions.IGNORE)) {
			this.options.put(entry.getKey(), newValue);
		}
	}
	this.options.put(CompilerOptions.OPTION_TaskTags, Util.EMPTY_STRING);
	if (newValue != null) {
		this.options.remove(newValue);
	}
}
protected void disableAll(int severity) {
	String checkedValue = null;
	switch(severity) {
		case ProblemSeverities.Error :
			checkedValue = CompilerOptions.ERROR;
			break;
		case ProblemSeverities.Warning :
			checkedValue = CompilerOptions.WARNING;
			break;
		case ProblemSeverities.Info :
			checkedValue = CompilerOptions.INFO;
			break;
	}
	Set<Entry<String, String>> entrySet = this.options.entrySet();
	for (Entry<String, String> entry : entrySet) {
		if (entry.getValue().equals(checkedValue)) {
			this.options.put(entry.getKey(), CompilerOptions.IGNORE);
		}
	}
	if (checkedValue != null) {
		this.options.put(checkedValue, CompilerOptions.IGNORE);
	}
	if (severity == ProblemSeverities.Warning) {
		disableAll(ProblemSeverities.Info);
	}
}
public String extractDestinationPathFromSourceFile(CompilationResult result) {
	ICompilationUnit compilationUnit = result.compilationUnit;
	if (compilationUnit != null) {
		char[] fileName = compilationUnit.getFileName();
		int lastIndex = CharOperation.lastIndexOf(java.io.File.separatorChar, fileName);
		if (lastIndex != -1) {
			final String outputPathName = new String(fileName, 0, lastIndex);
			final File output = new File(outputPathName);
			if (output.exists() && output.isDirectory()) {
				return outputPathName;
			}
		}
	}
	return System.getProperty("user.dir"); //$NON-NLS-1$
}
/*
 * Answer the component to which will be handed back compilation results from the compiler
 */
public ICompilerRequestor getBatchRequestor() {
    return new BatchCompilerRequestor(this);
}
/*
 *  Build the set of compilation source units
 */
public CompilationUnit[] getCompilationUnits() {
	int fileCount = this.filenames.length;
	CompilationUnit[] units = new CompilationUnit[fileCount];
	HashtableOfObject knownFileNames = new HashtableOfObject(fileCount);

	String defaultEncoding = this.options.get(CompilerOptions.OPTION_Encoding);
	if (Util.EMPTY_STRING.equals(defaultEncoding))
		defaultEncoding = null;

	for (int round = 0; round < 2; round++) {
		for (int i = 0; i < fileCount; i++) {
			char[] charName = this.filenames[i].toCharArray();
			boolean isModuleInfo = CharOperation.endsWith(charName, TypeConstants.MODULE_INFO_FILE_NAME);
			if (isModuleInfo == (round==0)) { // 1st round: modules, 2nd round others (to ensure populating pathToModCU well in time)
				if (knownFileNames.get(charName) != null)
					throw new IllegalArgumentException(this.bind("unit.more", this.filenames[i])); //$NON-NLS-1$
				knownFileNames.put(charName, charName);
				File file = new File(this.filenames[i]);
				if (!file.exists())
					throw new IllegalArgumentException(this.bind("unit.missing", this.filenames[i])); //$NON-NLS-1$
				String encoding = this.encodings[i];
				if (encoding == null)
					encoding = defaultEncoding;
				String fileName;
				try {
					fileName = file.getCanonicalPath();
				} catch (IOException e) {
					// if we got exception during canonicalization, fall back to the name that was specified
					fileName = this.filenames[i];
				}
				Function<String,String> annotationPathProvider = null;
				if (this.annotationsFromClasspath) {
					annotationPathProvider = (String qualifiedTypeName) -> {
						for (Classpath classpathEntry : this.checkedClasspaths) {
							if (classpathEntry.hasAnnotationFileFor(qualifiedTypeName.replace('.', '/')))
								return classpathEntry.getPath();
						}
						return null;
					};
				} else if (this.annotationPaths != null) {
					annotationPathProvider = (String qualifiedTypeName) -> {
						String eeaFileName = '/'+qualifiedTypeName.replace('.', '/')+ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX;
						for (String annotationPath : this.annotationPaths) {
							if (new File(annotationPath+eeaFileName).exists())
								return annotationPath;
						}
						return null;
					};
				}
				units[i] = new CompilationUnit(null, fileName, encoding, this.destinationPaths[i],
						shouldIgnoreOptionalProblems(this.ignoreOptionalProblemsFromFolders, fileName.toCharArray()),
						this.modNames[i], annotationPathProvider);
			}
		}
	}
	return units;
}

/*
 *  Low-level API performing the actual compilation
 */
public IErrorHandlingPolicy getHandlingPolicy() {

	// passes the initial set of files to the batch oracle (to avoid finding more than once the same units when case insensitive match)
	return new IErrorHandlingPolicy() {
		@Override
		public boolean proceedOnErrors() {
			return Main.this.proceedOnError; // stop if there are some errors
		}
		@Override
		public boolean stopOnFirstError() {
			return false;
		}
		@Override
		public boolean ignoreAllErrors() {
			return false;
		}
	};
}
private void setJavaHome(String javaHome) {
	File release = new File(javaHome, "release"); //$NON-NLS-1$
	Properties prop = new Properties();
	try (FileReader reader = new FileReader(release)) {
		prop.load(reader);
		String ver = prop.getProperty("JAVA_VERSION"); //$NON-NLS-1$
		if (ver != null)
			ver = ver.replace("\"", "");  //$NON-NLS-1$//$NON-NLS-2$
		this.javaHomeCache = new File(javaHome);
		this.javaHomeChecked = true;
	} catch (IOException e) {
		throw new IllegalArgumentException(this.bind("configure.invalidSystem", javaHome)); //$NON-NLS-1$
	}
}
/*
 * External API
 */
public File getJavaHome() {
	if (!this.javaHomeChecked) {
		this.javaHomeChecked = true;
		this.javaHomeCache = Util.getJavaHome();
	}
	return this.javaHomeCache;
}

public FileSystem getLibraryAccess() {
	FileSystem nameEnvironment = new FileSystem(this.checkedClasspaths, this.filenames,
					this.annotationsFromClasspath && CompilerOptions.ENABLED.equals(this.options.get(CompilerOptions.OPTION_AnnotationBasedNullAnalysis)),
					this.limitedModules);
	nameEnvironment.module = this.module;
	processAddonModuleOptions(nameEnvironment);
	return nameEnvironment;
}

/*
 *  Low-level API performing the actual compilation
 */
public IProblemFactory getProblemFactory() {
	return new DefaultProblemFactory(this.compilerLocale);
}

/*
 * External API
 */
protected ArrayList<Classpath> handleBootclasspath(ArrayList<String> bootclasspaths, String customEncoding) {
 	final int bootclasspathsSize;
 	ArrayList<Classpath> result = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
	if ((bootclasspaths != null)
		&& ((bootclasspathsSize = bootclasspaths.size()) != 0)) {
		result = new ArrayList<>(bootclasspathsSize);
		for (String path : bootclasspaths) {
			processPathEntries(DEFAULT_SIZE_CLASSPATH, result, path, customEncoding, false, true);
		}
	} else {
		try {
			Util.collectVMBootclasspath(result, this.javaHomeCache);
		} catch(IllegalStateException e) {
			throw new IllegalArgumentException(this.bind("configure.invalidSystem", this.javaHomeCache.toString())); //$NON-NLS-1$
		}
	}
	return result;
}
private void processAddonModuleOptions(FileSystem env) {
	Map<String, IPackageExport[]> exports = new HashMap<>();
	for (String option : this.addonExports) {
		AddExport addExport = ModuleFinder.extractAddonExport(option);
		if (addExport != null) {
			String modName = addExport.sourceModuleName;
			IPackageExport export = addExport.export;
			IPackageExport[] existing = exports.get(modName);
			if (existing == null) {
				existing = new IPackageExport[1];
				existing[0] = export;
				exports.put(modName, existing);
			} else {
				for (IPackageExport iPackageExport : existing) {
					if (CharOperation.equals(iPackageExport.name(), export.name())) {
						throw new IllegalArgumentException(this.bind("configure.duplicateExport")); //$NON-NLS-1$
					}
				}
				IPackageExport[] updated = new IPackageExport[existing.length + 1];
				System.arraycopy(existing, 0, updated, 0, existing.length);
				updated[existing.length] = export;
				exports.put(modName, updated);
			}
			env.addModuleUpdate(modName, m -> m.addExports(export.name(), export.targets()), UpdateKind.PACKAGE);
		} else {
			throw new IllegalArgumentException(this.bind("configure.invalidModuleOption", "--add-exports " + option)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	for (String option : this.addonReads) {
		String[] result = ModuleFinder.extractAddonRead(option);
		if (result != null && result.length == 2) {
			env.addModuleUpdate(result[0], m -> m.addReads(result[1].toCharArray()), UpdateKind.MODULE);
		} else {
			throw new IllegalArgumentException(this.bind("configure.invalidModuleOption", "--add-reads " + option)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
protected ArrayList<FileSystem.Classpath> handleModulepath(String arg) {
	ArrayList<String> modulePaths = processModulePathEntries(arg);
	ArrayList<Classpath> result = new ArrayList<>();
	if ((modulePaths != null && modulePaths.size() > 0)) {
		for (String path : modulePaths) {
			File file = new File(path);
			if (file.isDirectory()) {
				result.addAll(
					ModuleFinder.findModules(file, null, getNewParser(), this.options, true, this.releaseVersion));
			} else {
				Classpath modulePath = ModuleFinder.findModule(file, null, getNewParser(), this.options, true, this.releaseVersion);
				if (modulePath != null)
					result.add(modulePath);
			}
		}
	}
	// TODO: What about chained jars from MANIFEST.MF? Check with spec
	return result;
}
protected ArrayList<FileSystem.Classpath> handleModuleSourcepath(String arg) {
	ArrayList<String> modulePaths = processModulePathEntries(arg);
	ArrayList<FileSystem.Classpath> result = new ArrayList<>();
	if ((modulePaths != null)
		&& (modulePaths.size() != 0)) {
		if (this.destinationPath == null) {
			addPendingErrors(this.bind("configure.missingDestinationPath"));//$NON-NLS-1$
		}
		String[] paths = new String[modulePaths.size()];
		modulePaths.toArray(paths);
		for (int i = 0; i < paths.length; i++) {
			File dir = new File(paths[i]);
			if (dir.isDirectory()) {
				// 1. Create FileSystem.Classpath for each module
				// 2. Iterator each module in case of directory for source files and add to this.fileNames

				List<Classpath> modules = ModuleFinder.findModules(dir, this.destinationPath, getNewParser(), this.options, false, this.releaseVersion);
				for (Classpath classpath : modules) {
					result.add(classpath);
					Path modLocation = Paths.get(classpath.getPath()).toAbsolutePath();
					String destPath = classpath.getDestinationPath();
					IModule mod = classpath.getModule();
					String moduleName = mod == null ? null : new String(mod.name());
					for(int j = 0; j < this.filenames.length; j++) {
						Path filePath;
						try {
							// Get canonical path just as the classpath location is stored with the same.
							// To avoid mismatch of /USER_JAY and /USE~1 in windows systems.
							filePath = new File(this.filenames[j]).getCanonicalFile().toPath();
							if (filePath.startsWith(modLocation)) {
								this.modNames[j] = moduleName;
								this.destinationPaths[j] = destPath;
							}
						} catch (IOException e) {
							// Files doesn't exist and perhaps doesn't belong in a module, move on to other files
							// Use empty module name to distinguish from missing module case
							this.modNames[j] = ""; //$NON-NLS-1$
						}
					}
				}
			}
		}
		for(int j = 0; j < this.filenames.length; j++) {
			if (this.modNames[j] == null) {
				throw new IllegalArgumentException(this.bind("configure.notOnModuleSourcePath", new String[] {this.filenames[j]})); //$NON-NLS-1$
			}
		}
	}
	return result;
}
private void handleSingleModuleCompilation() {
	if (this.filenames == null) {
		return;
	}
	IModule singleMod = null;
	for (String filename : this.filenames) {
		IModule mod = extractModuleDesc(filename);
		if (mod != null) {
			if (singleMod == null) {
				singleMod = mod;
			} else {
				addPendingErrors(this.bind("configure.duplicateModuleInfo", filename)); //$NON-NLS-1$
			}
		}
	}
	if (singleMod != null) {
		String moduleName = new String(singleMod.name());
		for (int i = 0; i < this.modNames.length; i++) {
			this.modNames[i] = moduleName;
		}
		this.module = singleMod;
	}
}
/*
 * External API
 */
protected ArrayList<FileSystem.Classpath> handleClasspath(ArrayList<String> classpaths, String customEncoding) {
	ArrayList<FileSystem.Classpath> initial = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
	if (classpaths != null && classpaths.size() > 0) {
		for (String path : classpaths) {
			processPathEntries(DEFAULT_SIZE_CLASSPATH, initial, path, customEncoding, false, true);
		}
	} else {
		// no user classpath specified.
		String classProp = System.getProperty("java.class.path"); //$NON-NLS-1$
		if ((classProp == null) || (classProp.length() == 0)) {
			addPendingErrors(this.bind("configure.noClasspath")); //$NON-NLS-1$
			final Classpath classpath = FileSystem.getClasspath(System.getProperty("user.dir"), customEncoding, null, this.options, this.releaseVersion);//$NON-NLS-1$
			if (classpath != null) {
				initial.add(classpath);
			}
		} else {
			StringTokenizer tokenizer = new StringTokenizer(classProp, File.pathSeparator);
			String token;
			while (tokenizer.hasMoreTokens()) {
				token = tokenizer.nextToken();
				FileSystem.Classpath currentClasspath = FileSystem
						.getClasspath(token, customEncoding, null, this.options, this.releaseVersion);
				if (currentClasspath != null) {
					initial.add(currentClasspath);
				} else if (token.length() != 0) {
					addPendingErrors(this.bind("configure.incorrectClasspath", token));//$NON-NLS-1$
				}
			}
		}
	}
	ArrayList<Classpath> result = new ArrayList<>();
	HashMap<String, Classpath> knownNames = new HashMap<>();
	FileSystem.ClasspathSectionProblemReporter problemReporter =
		new FileSystem.ClasspathSectionProblemReporter() {
			@Override
			public void invalidClasspathSection(String jarFilePath) {
				addPendingErrors(bind("configure.invalidClasspathSection", jarFilePath)); //$NON-NLS-1$
			}
			@Override
			public void multipleClasspathSections(String jarFilePath) {
				addPendingErrors(bind("configure.multipleClasspathSections", jarFilePath)); //$NON-NLS-1$
			}
		};
	while (! initial.isEmpty()) {
		Classpath current = initial.remove(0);
		String currentPath = current.getPath();
		if (knownNames.get(currentPath) == null) {
			knownNames.put(currentPath, current);
			result.add(current);
			List<Classpath> linkedJars = current.fetchLinkedJars(problemReporter);
			if (linkedJars != null) {
				initial.addAll(0, linkedJars);
			}
		}
	}
	return result;
}
/*
 * External API
 */
protected ArrayList<FileSystem.Classpath> handleEndorseddirs(ArrayList<String> endorsedDirClasspaths) {
 	final File javaHome = getJavaHome();
	/*
	 * Feed endorsedDirClasspath according to:
	 * - -endorseddirs first if present;
	 * - else java.endorsed.dirs if defined;
	 * - else default extensions directory for the platform. (/lib/endorsed)
	 */
	if (endorsedDirClasspaths == null) {
		endorsedDirClasspaths = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
		String endorsedDirsStr = System.getProperty("java.endorsed.dirs"); //$NON-NLS-1$
		if (endorsedDirsStr == null) {
			if (javaHome != null) {
				endorsedDirClasspaths.add(javaHome.getAbsolutePath() + "/lib/endorsed"); //$NON-NLS-1$
			}
		} else {
			StringTokenizer tokenizer = new StringTokenizer(endorsedDirsStr, File.pathSeparator);
			while (tokenizer.hasMoreTokens()) {
				endorsedDirClasspaths.add(tokenizer.nextToken());
			}
		}
	}

	/*
	 * Feed extdirsClasspath with the entries found into the directories listed by
	 * extdirsNames.
	 */
	if (endorsedDirClasspaths.size() != 0) {
		ArrayList<FileSystem.Classpath> result = new ArrayList<>();
		File[] directoriesToCheck = new File[endorsedDirClasspaths.size()];
		for (int i = 0; i < directoriesToCheck.length; i++)
			directoriesToCheck[i] = new File(endorsedDirClasspaths.get(i));
		File[][] endorsedDirsJars = getLibrariesFiles(directoriesToCheck);
		if (endorsedDirsJars != null) {
			for (int i = 0, max = endorsedDirsJars.length; i < max; i++) {
				File[] current = endorsedDirsJars[i];
				if (current != null) {
					for (int j = 0, max2 = current.length; j < max2; j++) {
						FileSystem.Classpath classpath =
							FileSystem.getClasspath(
									current[j].getAbsolutePath(),
									null, null, this.options, this.releaseVersion);
						if (classpath != null) {
							result.add(classpath);
						}
					}
				} else if (directoriesToCheck[i].isFile()) {
					addPendingErrors(
						this.bind(
							"configure.incorrectEndorsedDirsEntry", //$NON-NLS-1$
							directoriesToCheck[i].getAbsolutePath()));
				}
			}
		}
		return result;
	}
	return FileSystem.EMPTY_CLASSPATH;
}

/*
 * External API
 * Handle extdirs processing
 */
protected ArrayList<FileSystem.Classpath> handleExtdirs(ArrayList<String> extdirsClasspaths) {
 	final File javaHome = getJavaHome();

	/*
	 * Feed extDirClasspath according to:
	 * - -extdirs first if present;
	 * - else java.ext.dirs if defined;
	 * - else default extensions directory for the platform.
	 */
	if (extdirsClasspaths == null) {
		extdirsClasspaths = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
		String extdirsStr = System.getProperty("java.ext.dirs"); //$NON-NLS-1$
		if (extdirsStr == null) {
			extdirsClasspaths.add(javaHome.getAbsolutePath() + "/lib/ext"); //$NON-NLS-1$
		} else {
			StringTokenizer tokenizer = new StringTokenizer(extdirsStr, File.pathSeparator);
			while (tokenizer.hasMoreTokens())
				extdirsClasspaths.add(tokenizer.nextToken());
		}
	}

	/*
	 * Feed extdirsClasspath with the entries found into the directories listed by
	 * extdirsNames.
	 */
	if (extdirsClasspaths.size() != 0) {
		ArrayList<FileSystem.Classpath> result = new ArrayList<>();
		File[] directoriesToCheck = new File[extdirsClasspaths.size()];
		for (int i = 0; i < directoriesToCheck.length; i++)
			directoriesToCheck[i] = new File(extdirsClasspaths.get(i));
		File[][] extdirsJars = getLibrariesFiles(directoriesToCheck);
		if (extdirsJars != null) {
			for (int i = 0, max = extdirsJars.length; i < max; i++) {
				File[] current = extdirsJars[i];
				if (current != null) {
					for (int j = 0, max2 = current.length; j < max2; j++) {
						FileSystem.Classpath classpath =
							FileSystem.getClasspath(
									current[j].getAbsolutePath(),
									null, null, this.options, this.releaseVersion);
						if (classpath != null) {
							result.add(classpath);
						}
					}
				} else if (directoriesToCheck[i].isFile()) {
					addPendingErrors(this.bind(
							"configure.incorrectExtDirsEntry", //$NON-NLS-1$
							directoriesToCheck[i].getAbsolutePath()));
				}
			}
		}
		return result;
	}

	return FileSystem.EMPTY_CLASSPATH;
}

protected boolean isIgnored(IProblem problem) {
	if (problem == null) {
		return true;
	}
	if (problem.isError()) {
		return false;
	}
	String key = problem.isInfo() ? CompilerOptions.INFO : CompilerOptions.WARNING;
	if (CompilerOptions.IGNORE.equals(this.options.get(key))) {
		return true;
	}
	if (this.ignoreOptionalProblemsFromFolders != null) {
		char[] fileName = problem.getOriginatingFileName();
		if (fileName != null) {
			return shouldIgnoreOptionalProblems(this.ignoreOptionalProblemsFromFolders, fileName);
		}
	}
	return false;
}

/*
 * External API
 * Handle a single warning token.
*/
protected void handleInfoToken(String token, boolean isEnabling) {
	handleErrorOrWarningToken(token, isEnabling, ProblemSeverities.Info);
}
protected void handleWarningToken(String token, boolean isEnabling) {
	handleErrorOrWarningToken(token, isEnabling, ProblemSeverities.Warning);
}
protected void handleErrorToken(String token, boolean isEnabling) {
	handleErrorOrWarningToken(token, isEnabling, ProblemSeverities.Error);
}
private void setSeverity(String compilerOptions, int severity, boolean isEnabling) {
	if (isEnabling) {
		switch(severity) {
			case ProblemSeverities.Error :
				this.options.put(compilerOptions, CompilerOptions.ERROR);
				break;
			case ProblemSeverities.Warning :
				this.options.put(compilerOptions, CompilerOptions.WARNING);
				break;
			case ProblemSeverities.Info :
				this.options.put(compilerOptions, CompilerOptions.INFO);
				break;
			default:
				this.options.put(compilerOptions, CompilerOptions.IGNORE);
		}
	} else {
		switch(severity) {
			case ProblemSeverities.Error :
				String currentValue = this.options.get(compilerOptions);
				if (CompilerOptions.ERROR.equals(currentValue)) {
					this.options.put(compilerOptions, CompilerOptions.IGNORE);
				}
				break;
			case ProblemSeverities.Warning :
				currentValue = this.options.get(compilerOptions);
				if (CompilerOptions.WARNING.equals(currentValue)) {
					this.options.put(compilerOptions, CompilerOptions.IGNORE);
				}
				break;
			case ProblemSeverities.Info :
				currentValue = this.options.get(compilerOptions);
				if (CompilerOptions.INFO.equals(currentValue)) {
					this.options.put(compilerOptions, CompilerOptions.IGNORE);
				}
				break;
			default:
				this.options.put(compilerOptions, CompilerOptions.IGNORE);
		}
	}
}
private void handleErrorOrWarningToken(String token, boolean isEnabling, int severity) {
	if (token.length() == 0) return;
	switch(token.charAt(0)) {
		case 'a' :
			if (token.equals("allDeprecation")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportDeprecation, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportTerminalDeprecation, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				this.options.put(
					CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("allJavadoc")) { //$NON-NLS-1$
				this.warnAllJavadocOn = this.warnJavadocOn = isEnabling;
				setSeverity(CompilerOptions.OPTION_ReportInvalidJavadoc, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportMissingJavadocTags, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportMissingJavadocComments, severity, isEnabling);
				return;
			} else if (token.equals("assertIdentifier")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportAssertIdentifier, severity, isEnabling);
				return;
			} else if (token.equals("allDeadCode")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportDeadCode, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportDeadCodeInTrivialIfStatement,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("allOver-ann")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingOverrideAnnotation, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportMissingOverrideAnnotationForInterfaceMethodImplementation,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("all-static-method")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMethodCanBeStatic, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportMethodCanBePotentiallyStatic, severity, isEnabling);
				return;
			} else if (token.equals("all")) { //$NON-NLS-1$
				if (isEnabling) {
					enableAll(severity);
				} else {
					disableAll(severity);
				}
				return;
			}
			break;
		case 'b' :
			if (token.equals("boxing")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportAutoboxing, severity, isEnabling);
				return;
			}
			break;
		case 'c' :
			if (token.equals("constructorName")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMethodWithConstructorName, severity, isEnabling);
				return;
			} else if (token.equals("conditionAssign")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportPossibleAccidentalBooleanAssignment, severity, isEnabling);
				return;
			} else if (token.equals("compareIdentical")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportComparingIdentical, severity, isEnabling);
				return;
			} else if (token.equals("charConcat") /*|| token.equals("noImplicitStringConversion")/*backward compatible*/) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNoImplicitStringConversion, severity, isEnabling);
				return;
			}
			break;
		case 'd' :
			if (token.equals("deprecation")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportDeprecation, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode,
					CompilerOptions.DISABLED);
				this.options.put(
					CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod,
					CompilerOptions.DISABLED);
				return;
			} else if (token.equals("dep-ann")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingDeprecatedAnnotation, severity, isEnabling);
				return;
			} else if (token.equals("discouraged")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportDiscouragedReference, severity, isEnabling);
				return;
			} else if (token.equals("deadCode")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportDeadCode, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportDeadCodeInTrivialIfStatement,
					CompilerOptions.DISABLED);
				return;
			}
			break;
		case 'e' :
			if (token.equals("enumSwitch")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportIncompleteEnumSwitch, severity, isEnabling);
				return;
			} else if (token.equals("enumSwitchPedantic")) { //$NON-NLS-1$
				if (isEnabling) {
					switch (severity) {
						case ProblemSeverities.Error:
							setSeverity(CompilerOptions.OPTION_ReportIncompleteEnumSwitch, severity, isEnabling);
							break;
						case ProblemSeverities.Warning:
							if (CompilerOptions.IGNORE.equals(this.options.get(CompilerOptions.OPTION_ReportIncompleteEnumSwitch))) {
								setSeverity(CompilerOptions.OPTION_ReportIncompleteEnumSwitch, severity, isEnabling);
							}
							break;
						default: // no severity update
					}
				}
				this.options.put(CompilerOptions.OPTION_ReportMissingEnumCaseDespiteDefault,
								 isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("emptyBlock")) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUndocumentedEmptyBlock, severity, isEnabling);
				return;
			} else if (token.equals("enumIdentifier")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportEnumIdentifier, severity, isEnabling);
				return;
			} else if (token.equals("exports")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportAPILeak, severity, isEnabling);
				return;
			}
			break;
		case 'f' :
			if (token.equals("fieldHiding")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportFieldHiding, severity, isEnabling);
				return;
			} else if (token.equals("finalBound")) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportFinalParameterBound, severity, isEnabling);
				return;
			} else if (token.equals("finally")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportFinallyBlockNotCompletingNormally, severity, isEnabling);
				return;
			} else if (token.equals("forbidden")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportForbiddenReference, severity, isEnabling);
				return;
			} else if (token.equals("fallthrough")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportFallthroughCase, severity, isEnabling);
				return;
			}
			break;
		case 'h' :
			if (token.equals("hiding")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportHiddenCatchBlock, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportLocalVariableHiding, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportFieldHiding, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportTypeParameterHiding, severity, isEnabling);
				return;
			} else if (token.equals("hashCode")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingHashCodeMethod, severity, isEnabling);
				return;
			}
			break;
		case 'i' :
			if (token.equals("indirectStatic")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportIndirectStaticAccess, severity, isEnabling);
				return;
			} else if (token.equals("inheritNullAnnot")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_InheritNullAnnotations,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("intfNonInherited") || token.equals("interfaceNonInherited")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
				setSeverity(CompilerOptions.OPTION_ReportIncompatibleNonInheritedInterfaceMethod, severity, isEnabling);
				return;
			} else if (token.equals("intfAnnotation")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportAnnotationSuperInterface, severity, isEnabling);
				return;
			} else if (token.equals("intfRedundant") /*|| token.equals("redundantSuperinterface")*/) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportRedundantSuperinterface, severity, isEnabling);
				return;
			} else if (token.equals("includeAssertNull")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_IncludeNullInfoFromAsserts,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("invalidJavadoc")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportInvalidJavadoc, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportInvalidJavadocTags,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				this.options.put(
					CompilerOptions.OPTION_ReportInvalidJavadocTagsDeprecatedRef,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				this.options.put(
					CompilerOptions.OPTION_ReportInvalidJavadocTagsNotVisibleRef,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				if (isEnabling) {
					this.options.put(
							CompilerOptions.OPTION_DocCommentSupport,
							CompilerOptions.ENABLED);
					this.options.put(
						CompilerOptions.OPTION_ReportInvalidJavadocTagsVisibility,
						CompilerOptions.PRIVATE);
				}
				return;
			} else if (token.equals("invalidJavadocTag")) { //$NON-NLS-1$
				this.options.put(
					CompilerOptions.OPTION_ReportInvalidJavadocTags,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("invalidJavadocTagDep")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_ReportInvalidJavadocTagsDeprecatedRef,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("invalidJavadocTagNotVisible")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_ReportInvalidJavadocTagsNotVisibleRef,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.startsWith("invalidJavadocTagVisibility")) { //$NON-NLS-1$
				int start = token.indexOf('(');
				int end = token.indexOf(')');
				String visibility = null;
				if (isEnabling && start >= 0 && end >= 0 && start < end){
					visibility = token.substring(start+1, end).trim();
				}
				if (visibility != null && visibility.equals(CompilerOptions.PUBLIC)
						|| visibility.equals(CompilerOptions.PRIVATE)
						|| visibility.equals(CompilerOptions.PROTECTED)
						|| visibility.equals(CompilerOptions.DEFAULT)) {
					this.options.put(
							CompilerOptions.OPTION_ReportInvalidJavadocTagsVisibility,
							visibility);
					return;
				} else {
					throw new IllegalArgumentException(this.bind("configure.invalidJavadocTagVisibility", token)); //$NON-NLS-1$
				}
			}
			break;
		case 'j' :
			if (token.equals("javadoc")) {//$NON-NLS-1$
				this.warnJavadocOn = isEnabling;
				setSeverity(CompilerOptions.OPTION_ReportInvalidJavadoc, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportMissingJavadocTags, severity, isEnabling);
				return;
			}
			break;
		case 'l' :
			if (token.equals("localHiding")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportLocalVariableHiding, severity, isEnabling);
				return;
			}
			break;
		case 'm' :
			if (token.equals("maskedCatchBlock") || token.equals("maskedCatchBlocks")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
				setSeverity(CompilerOptions.OPTION_ReportHiddenCatchBlock, severity, isEnabling);
				return;
			} else if (token.equals("missingJavadocTags")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingJavadocTags, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportMissingJavadocTagsOverriding,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				this.options.put(
					CompilerOptions.OPTION_ReportMissingJavadocTagsMethodTypeParameters,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				if (isEnabling) {
					this.options.put(
							CompilerOptions.OPTION_DocCommentSupport,
							CompilerOptions.ENABLED);
					this.options.put(
						CompilerOptions.OPTION_ReportMissingJavadocTagsVisibility,
						CompilerOptions.PRIVATE);
				}
				return;
			} else if (token.equals("missingJavadocTagsOverriding")) { //$NON-NLS-1$
				this.options.put(
					CompilerOptions.OPTION_ReportMissingJavadocTagsOverriding,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("missingJavadocTagsMethod")) { //$NON-NLS-1$
				this.options.put(
					CompilerOptions.OPTION_ReportMissingJavadocTagsMethodTypeParameters,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.startsWith("missingJavadocTagsVisibility")) { //$NON-NLS-1$
				int start = token.indexOf('(');
				int end = token.indexOf(')');
				String visibility = null;
				if (isEnabling && start >= 0 && end >= 0 && start < end){
					visibility = token.substring(start+1, end).trim();
				}
				if (visibility != null && visibility.equals(CompilerOptions.PUBLIC)
						|| visibility.equals(CompilerOptions.PRIVATE)
						|| visibility.equals(CompilerOptions.PROTECTED)
						|| visibility.equals(CompilerOptions.DEFAULT)) {
					this.options.put(
							CompilerOptions.OPTION_ReportMissingJavadocTagsVisibility,
							visibility);
					return;
				} else {
					throw new IllegalArgumentException(this.bind("configure.missingJavadocTagsVisibility", token)); //$NON-NLS-1$
				}
			} else if (token.equals("missingJavadocComments")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingJavadocComments, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportMissingJavadocCommentsOverriding,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				if (isEnabling) {
					this.options.put(
							CompilerOptions.OPTION_DocCommentSupport,
							CompilerOptions.ENABLED);
					this.options.put(
						CompilerOptions.OPTION_ReportMissingJavadocCommentsVisibility,
						CompilerOptions.PRIVATE);
				}
				return;
			} else if (token.equals("missingJavadocCommentsOverriding")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingJavadocComments, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportMissingJavadocCommentsOverriding,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.startsWith("missingJavadocCommentsVisibility")) { //$NON-NLS-1$
				int start = token.indexOf('(');
				int end = token.indexOf(')');
				String visibility = null;
				if (isEnabling && start >= 0 && end >= 0 && start < end){
					visibility = token.substring(start+1, end).trim();
				}
				if (visibility != null && visibility.equals(CompilerOptions.PUBLIC)
						|| visibility.equals(CompilerOptions.PRIVATE)
						|| visibility.equals(CompilerOptions.PROTECTED)
						|| visibility.equals(CompilerOptions.DEFAULT)) {
					this.options.put(
							CompilerOptions.OPTION_ReportMissingJavadocCommentsVisibility,
							visibility);
					return;
				} else {
					throw new IllegalArgumentException(this.bind("configure.missingJavadocCommentsVisibility", token)); //$NON-NLS-1$
				}
			} else if (token.equals("module")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnstableAutoModuleName, severity, isEnabling);
				return;
			}
			break;
		case 'n' :
			if (token.equals("nls")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNonExternalizedStringLiteral, severity, isEnabling);
				return;
			} else if (token.equals("noEffectAssign")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNoEffectAssignment, severity, isEnabling);
				return;
			} else if (/*token.equals("charConcat") ||*/ token.equals("noImplicitStringConversion")/*backward compatible*/) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNoImplicitStringConversion, severity, isEnabling);
				return;
			} else if (token.equals("null")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNullReference, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportPotentialNullReference, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportRedundantNullCheck, severity, isEnabling);
				return;
			} else if (token.equals("nullDereference")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNullReference, severity, isEnabling);
				if (!isEnabling) {
					setSeverity(CompilerOptions.OPTION_ReportPotentialNullReference, ProblemSeverities.Ignore, isEnabling);
					setSeverity(CompilerOptions.OPTION_ReportRedundantNullCheck, ProblemSeverities.Ignore, isEnabling);
				}
				return;
			}else if (token.equals("nullAnnotConflict")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNullAnnotationInferenceConflict, severity, isEnabling);
				return;
			} else if (token.equals("nullAnnotRedundant")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportRedundantNullAnnotation, severity, isEnabling);
				return;
			} else if (token.startsWith("nullAnnot")) { //$NON-NLS-1$
				String annotationNames = Util.EMPTY_STRING;
				int start = token.indexOf('(');
				int end = token.indexOf(')');
				String nonNullAnnotName = null, nullableAnnotName = null, nonNullByDefaultAnnotName = null;
				if (isEnabling && start >= 0 && end >= 0 && start < end){
					boolean isPrimarySet = !this.primaryNullAnnotationsSeen;
					annotationNames = token.substring(start+1, end).trim();
					int separator1 = annotationNames.indexOf('|');
					if (separator1 == -1) throw new IllegalArgumentException(this.bind("configure.invalidNullAnnot", token)); //$NON-NLS-1$
					nullableAnnotName = annotationNames.substring(0, separator1).trim();
					if (isPrimarySet && nullableAnnotName.length() == 0) throw new IllegalArgumentException(this.bind("configure.invalidNullAnnot", token)); //$NON-NLS-1$
					int separator2 = annotationNames.indexOf('|', separator1 + 1);
					if (separator2 == -1) throw new IllegalArgumentException(this.bind("configure.invalidNullAnnot", token)); //$NON-NLS-1$
					nonNullAnnotName = annotationNames.substring(separator1 + 1, separator2).trim();
					if (isPrimarySet && nonNullAnnotName.length() == 0) throw new IllegalArgumentException(this.bind("configure.invalidNullAnnot", token)); //$NON-NLS-1$
					nonNullByDefaultAnnotName = annotationNames.substring(separator2 + 1).trim();
					if (isPrimarySet && nonNullByDefaultAnnotName.length() == 0) throw new IllegalArgumentException(this.bind("configure.invalidNullAnnot", token)); //$NON-NLS-1$
					if (isPrimarySet) {
						this.primaryNullAnnotationsSeen = true;
						this.options.put(CompilerOptions.OPTION_NullableAnnotationName, nullableAnnotName);
						this.options.put(CompilerOptions.OPTION_NonNullAnnotationName, nonNullAnnotName);
						this.options.put(CompilerOptions.OPTION_NonNullByDefaultAnnotationName, nonNullByDefaultAnnotName);
					} else {
						if (nullableAnnotName.length() > 0) {
							String nullableList = this.options.get(CompilerOptions.OPTION_NullableAnnotationSecondaryNames);
							nullableList = nullableList.isEmpty() ? nullableAnnotName : nullableList + ',' + nullableAnnotName;
							this.options.put(CompilerOptions.OPTION_NullableAnnotationSecondaryNames, nullableList);
						}
						if (nonNullAnnotName.length() > 0) {
							String nonnullList = this.options.get(CompilerOptions.OPTION_NonNullAnnotationSecondaryNames);
							nonnullList = nonnullList.isEmpty() ? nonNullAnnotName : nonnullList + ',' + nonNullAnnotName;
							this.options.put(CompilerOptions.OPTION_NonNullAnnotationSecondaryNames, nonnullList);
						}
						if (nonNullByDefaultAnnotName.length() > 0) {
							String nnbdList = this.options.get(CompilerOptions.OPTION_NonNullByDefaultAnnotationSecondaryNames);
							nnbdList = nnbdList.isEmpty() ? nonNullByDefaultAnnotName : nnbdList + ',' + nonNullByDefaultAnnotName;
							this.options.put(CompilerOptions.OPTION_NonNullByDefaultAnnotationSecondaryNames, nnbdList);
						}
					}
				}
				this.options.put(
						CompilerOptions.OPTION_AnnotationBasedNullAnalysis,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				setSeverity(CompilerOptions.OPTION_ReportNullSpecViolation, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportNullAnnotationInferenceConflict, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportNullUncheckedConversion, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportRedundantNullAnnotation, severity, isEnabling);
				return;
			} else if (token.equals("nullUncheckedConversion")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNullUncheckedConversion, severity, isEnabling);
				return;
			} else if (token.equals("nonnullNotRepeated")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNonnullParameterAnnotationDropped, severity, isEnabling);
				return;
			}

			break;
		case 'o' :
			if (token.equals("over-sync") /*|| token.equals("syncOverride")*/) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingSynchronizedOnInheritedMethod, severity, isEnabling);
				return;
			} else if (token.equals("over-ann")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingOverrideAnnotation, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportMissingOverrideAnnotationForInterfaceMethodImplementation,
					CompilerOptions.DISABLED);
				return;
			}
			break;
		case 'p' :
			if (token.equals("pkgDefaultMethod") || token.equals("packageDefaultMethod")/*backward compatible*/ ) { //$NON-NLS-1$ //$NON-NLS-2$
				setSeverity(CompilerOptions.OPTION_ReportOverridingPackageDefaultMethod, severity, isEnabling);
				return;
			} else if (token.equals("paramAssign")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportParameterAssignment, severity, isEnabling);
				return;
			}
			break;
		case 'r' :
			if (token.equals("raw")) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportRawTypeReference, severity, isEnabling);
				return;
			} else if (/*token.equals("intfRedundant") ||*/ token.equals("redundantSuperinterface")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportRedundantSuperinterface, severity, isEnabling);
				return;
			} else if (token.equals("resource")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnclosedCloseable, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportPotentiallyUnclosedCloseable, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportExplicitlyClosedAutoCloseable, severity, isEnabling);
				return;
			} else if (token.equals("removal")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportTerminalDeprecation, severity, isEnabling);
				this.options.put(
					CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode,
					CompilerOptions.DISABLED);
				this.options.put(
					CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod,
					CompilerOptions.DISABLED);
				return;
			}
			break;
		case 's' :
			if (token.equals("specialParamHiding")) { //$NON-NLS-1$
				this.options.put(
					CompilerOptions.OPTION_ReportSpecialParameterHidingField,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("syntheticAccess") || token.equals("synthetic-access")) { //$NON-NLS-1$ //$NON-NLS-2$
				setSeverity(CompilerOptions.OPTION_ReportSyntheticAccessEmulation, severity, isEnabling);
				return;
			} else if (token.equals("staticReceiver")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNonStaticAccessToStatic, severity, isEnabling);
				return;
			} else 	if (/*token.equals("over-sync") ||*/ token.equals("syncOverride")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingSynchronizedOnInheritedMethod, severity, isEnabling);
				return;
			} else if (token.equals("semicolon")) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportEmptyStatement, severity, isEnabling);
				return;
			} else if (token.equals("serial")) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingSerialVersion, severity, isEnabling);
				return;
			} else if (token.equals("suppress")) {//$NON-NLS-1$
				switch(severity) {
					case ProblemSeverities.Warning :
						this.options.put(
								CompilerOptions.OPTION_SuppressWarnings,
								isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
						this.options.put(
								CompilerOptions.OPTION_SuppressOptionalErrors,
								CompilerOptions.DISABLED);
						break;
					case ProblemSeverities.Error :
						this.options.put(
								CompilerOptions.OPTION_SuppressWarnings,
								isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
						this.options.put(
							CompilerOptions.OPTION_SuppressOptionalErrors,
							isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				}
				return;
			} else if (token.equals("static-access")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportNonStaticAccessToStatic, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportIndirectStaticAccess, severity, isEnabling);
				return;
			} else if (token.equals("super")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportOverridingMethodWithoutSuperInvocation, severity, isEnabling);
				return;
			} else if (token.equals("static-method")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMethodCanBeStatic, severity, isEnabling);
				return;
			} else if (token.equals("switchDefault")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportMissingDefaultCase, severity, isEnabling);
				return;
			} else if (token.equals("syntacticAnalysis")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_SyntacticNullAnalysisForFields,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			}
			break;
		case 't' :
			if (token.startsWith("tasks")) { //$NON-NLS-1$
				String taskTags = Util.EMPTY_STRING;
				int start = token.indexOf('(');
				int end = token.indexOf(')');
				if (start >= 0 && end >= 0 && start < end){
					taskTags = token.substring(start+1, end).trim();
					taskTags = taskTags.replace('|',',');
				}
				if (taskTags.length() == 0){
					throw new IllegalArgumentException(this.bind("configure.invalidTaskTag", token)); //$NON-NLS-1$
				}
				this.options.put(
					CompilerOptions.OPTION_TaskTags,
					isEnabling ? taskTags : Util.EMPTY_STRING);

				setSeverity(CompilerOptions.OPTION_ReportTasks, severity, isEnabling);
				return;
			} else if (token.equals("typeHiding")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportTypeParameterHiding, severity, isEnabling);
				return;
			}
			break;
		case 'u' :
			if (token.equals("unusedLocal") || token.equals("unusedLocals")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
				setSeverity(CompilerOptions.OPTION_ReportUnusedLocal, severity, isEnabling);
				return;
			} else if (token.equals("unusedArgument") || token.equals("unusedArguments")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
				setSeverity(CompilerOptions.OPTION_ReportUnusedParameter, severity, isEnabling);
				return;
			} else if (token.equals("unusedExceptionParam")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnusedExceptionParameter, severity, isEnabling);
				return;
			} else if (token.equals("unusedImport") || token.equals("unusedImports")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
				setSeverity(CompilerOptions.OPTION_ReportUnusedImport, severity, isEnabling);
				return;
			} else if (token.equals("unusedAllocation")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnusedObjectAllocation, severity, isEnabling);
				return;
			} else if (token.equals("unusedPrivate")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnusedPrivateMember, severity, isEnabling);
				return;
			} else if (token.equals("unusedLabel")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnusedLabel, severity, isEnabling);
				return;
			} else if (token.equals("uselessTypeCheck")) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnnecessaryTypeCheck, severity, isEnabling);
				return;
			} else if (token.equals("unchecked") || token.equals("unsafe")) {//$NON-NLS-1$ //$NON-NLS-2$
				setSeverity(CompilerOptions.OPTION_ReportUncheckedTypeOperation, severity, isEnabling);
				return;
			} else if (token.equals("unlikelyCollectionMethodArgumentType")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnlikelyCollectionMethodArgumentType, severity, isEnabling);
				return;
			} else if (token.equals("unlikelyEqualsArgumentType")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnlikelyEqualsArgumentType, severity, isEnabling);
				return;
			} else if (token.equals("unnecessaryElse")) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnnecessaryElse, severity, isEnabling);
				return;
			} else if (token.equals("unusedThrown")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnusedDeclaredThrownException, severity, isEnabling);
				return;
			} else if (token.equals("unusedThrownWhenOverriding")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_ReportUnusedDeclaredThrownExceptionWhenOverriding,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("unusedThrownIncludeDocComment")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_ReportUnusedDeclaredThrownExceptionIncludeDocCommentReference,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("unusedThrownExemptExceptionThrowable")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_ReportUnusedDeclaredThrownExceptionExemptExceptionAndThrowable,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("unqualifiedField") || token.equals("unqualified-field-access")) { //$NON-NLS-1$ //$NON-NLS-2$
				setSeverity(CompilerOptions.OPTION_ReportUnqualifiedFieldAccess, severity, isEnabling);
				return;
			} else if (token.equals("unused")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportDeadCode, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportRedundantSuperinterface, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportRedundantSpecificationOfTypeArguments, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedDeclaredThrownException, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedExceptionParameter, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedImport, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedLabel, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedLocal, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedObjectAllocation, severity,isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedParameter, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedPrivateMember, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedTypeArgumentsForMethodInvocation, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedTypeParameter, severity,isEnabling);
				return;
			} else if (token.equals("unusedParam")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnusedParameter, severity, isEnabling);
				return;
			} else if (token.equals("unusedTypeParameter")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnusedTypeParameter, severity, isEnabling);
				return;
			} else if (token.equals("unusedParamIncludeDoc")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_ReportUnusedParameterIncludeDocCommentReference,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("unusedParamOverriding")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_ReportUnusedParameterWhenOverridingConcrete,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			} else if (token.equals("unusedParamImplementing")) { //$NON-NLS-1$
				this.options.put(
						CompilerOptions.OPTION_ReportUnusedParameterWhenImplementingAbstract,
						isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			}  else if (token.equals("unusedTypeArgs")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnusedTypeArgumentsForMethodInvocation, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportRedundantSpecificationOfTypeArguments, severity, isEnabling);
				return;
			} else if (token.equals("unavoidableGenericProblems")) { //$NON-NLS-1$
				this.options.put(
					CompilerOptions.OPTION_ReportUnavoidableGenericTypeProblems,
					isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
				return;
			}
			break;
		case 'v' :
			if (token.equals("varargsCast")) { //$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportVarargsArgumentNeedCast, severity, isEnabling);
				return;
			}
			break;
		case 'w' :
			if (token.equals("warningToken")) {//$NON-NLS-1$
				setSeverity(CompilerOptions.OPTION_ReportUnhandledWarningToken, severity, isEnabling);
				setSeverity(CompilerOptions.OPTION_ReportUnusedWarningToken, severity, isEnabling);
				return;
			}
			break;
	}
	String message = null;
	switch(severity) {
		case ProblemSeverities.Info:
			message = this.bind("configure.invalidInfo", token); //$NON-NLS-1$
			break;
		case ProblemSeverities.Warning :
			message = this.bind("configure.invalidWarning", token); //$NON-NLS-1$
			break;
		case ProblemSeverities.Error :
			message = this.bind("configure.invalidError", token); //$NON-NLS-1$
	}
	addPendingErrors(message);
}
/**
 * @deprecated - use {@link #initialize(PrintWriter, PrintWriter, boolean, Map, CompilationProgress)} instead
 *                       e.g. initialize(outWriter, errWriter, systemExit, null, null)
 */
protected void initialize(PrintWriter outWriter, PrintWriter errWriter, boolean systemExit) {
	this.initialize(outWriter, errWriter, systemExit, null /* options */, null /* progress */);
}
/**
 * @deprecated - use {@link #initialize(PrintWriter, PrintWriter, boolean, Map, CompilationProgress)} instead
 *                       e.g. initialize(outWriter, errWriter, systemExit, customDefaultOptions, null)
 */
protected void initialize(PrintWriter outWriter, PrintWriter errWriter, boolean systemExit, Map<String, String> customDefaultOptions) {
	this.initialize(outWriter, errWriter, systemExit, customDefaultOptions, null /* progress */);
}
protected void initialize(PrintWriter outWriter, PrintWriter errWriter, boolean systemExit, Map<String, String> customDefaultOptions, CompilationProgress compilationProgress) {
	this.logger = new Logger(this, outWriter, errWriter);
	this.proceed = true;
	this.out = outWriter;
	this.err = errWriter;
	this.systemExitWhenFinished = systemExit;
	this.options = new CompilerOptions().getMap();
	this.ignoreOptionalProblemsFromFolders = null;

	this.progress = compilationProgress;
	if (customDefaultOptions != null) {
		this.didSpecifySource = customDefaultOptions.get(CompilerOptions.OPTION_Source) != null;
		this.didSpecifyTarget = customDefaultOptions.get(CompilerOptions.OPTION_TargetPlatform) != null;
		for (Iterator<Map.Entry<String, String>> iter = customDefaultOptions.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<String, String> entry = iter.next();
			this.options.put(entry.getKey(), entry.getValue());
		}
	} else {
		this.didSpecifySource = false;
		this.didSpecifyTarget = false;
	}
	this.classNames = null;
}
protected void initializeAnnotationProcessorManager() {
	String className = "org.eclipse.jdt.internal.compiler.apt.dispatch.BatchAnnotationProcessorManager"; //$NON-NLS-1$
	try {
		Class<?> c = Class.forName(className);
		AbstractAnnotationProcessorManager annotationManager = (AbstractAnnotationProcessorManager) c.getDeclaredConstructor().newInstance();
		annotationManager.configure(this, this.expandedCommandLine);
		annotationManager.setErr(this.err);
		annotationManager.setOut(this.out);
		this.batchCompiler.annotationProcessorManager = annotationManager;
	} catch (ClassNotFoundException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
		this.logger.logUnavaibleAPT(className);
		throw new org.eclipse.jdt.internal.compiler.problem.AbortCompilation();
	} catch (IllegalAccessException e) {
		// should not happen
		throw new org.eclipse.jdt.internal.compiler.problem.AbortCompilation();
	} catch(UnsupportedClassVersionError e) {
		// report a warning
		this.logger.logIncorrectVMVersionForAnnotationProcessing();
	}
}
private static boolean isParentOf(char[] folderName, char[] fileName) {
	if (folderName.length >= fileName.length) {
		return false;
	}
	if (fileName[folderName.length] != '\\' && fileName[folderName.length] != '/') {
		return false;
	}
	for (int i = folderName.length - 1; i >= 0; i--) {
		if (folderName[i] != fileName[i]) {
			return false;
		}
	}
	return true;
}
// Dump classfiles onto disk for all compilation units that where successful
// and do not carry a -d none spec, either directly or inherited from Main.
public void outputClassFiles(CompilationResult unitResult) {
	if (!((unitResult == null) || (unitResult.hasErrors() && !this.proceedOnError))) {
		ClassFile[] classFiles = unitResult.getClassFiles();
		String currentDestinationPath = null;
		boolean generateClasspathStructure = false;
		CompilationUnit compilationUnit =
			(CompilationUnit) unitResult.compilationUnit;
		if (compilationUnit.destinationPath == null) {
			if (this.destinationPath == null) {
				currentDestinationPath =
					extractDestinationPathFromSourceFile(unitResult);
			} else if (this.destinationPath != NONE) {
				currentDestinationPath = this.destinationPath;
				generateClasspathStructure = true;
			} // else leave currentDestinationPath null
		} else if (compilationUnit.destinationPath != NONE) {
			currentDestinationPath = compilationUnit.destinationPath;
			generateClasspathStructure = true;
		} // else leave currentDestinationPath null
		if (currentDestinationPath != null) {
			for (int i = 0, fileCount = classFiles.length; i < fileCount; i++) {
				// retrieve the key and the corresponding classfile
				ClassFile classFile = classFiles[i];
				char[] filename = classFile.fileName();
				int length = filename.length;
				char[] relativeName = new char[length + 6];
				System.arraycopy(filename, 0, relativeName, 0, length);
				System.arraycopy(SuffixConstants.SUFFIX_class, 0, relativeName, length, 6);
				CharOperation.replace(relativeName, '/', File.separatorChar);
				String relativeStringName = new String(relativeName);
				try {
					if (this.compilerOptions.verbose)
						this.out.println(
							Messages.bind(
								Messages.compilation_write,
								new String[] {
									String.valueOf(this.exportedClassFilesCounter+1),
									relativeStringName
								}));
					Util.writeToDisk(
						generateClasspathStructure,
						currentDestinationPath,
						relativeStringName,
						classFile);
					this.logger.logClassFile(
						generateClasspathStructure,
						currentDestinationPath,
						relativeStringName);
					this.exportedClassFilesCounter++;
				} catch (IOException e) {
					this.logger.logNoClassFileCreated(currentDestinationPath, relativeStringName, e);
				}
			}
			this.batchCompiler.lookupEnvironment.releaseClassFiles(classFiles);
		}
	}
}
/*
 *  Low-level API performing the actual compilation
 */
public void performCompilation() {
	this.startTime = System.currentTimeMillis();

	FileSystem environment = getLibraryAccess();
	try {
		this.compilerOptions = new CompilerOptions(this.options);
		this.compilerOptions.performMethodsFullRecovery = false;
		this.compilerOptions.performStatementsRecovery = false;
		this.batchCompiler =
				new Compiler(
						environment,
						getHandlingPolicy(),
						this.compilerOptions,
						getBatchRequestor(),
						getProblemFactory(),
						this.out,
						this.progress);
		this.batchCompiler.remainingIterations = this.maxRepetition-this.currentRepetition/*remaining iterations including this one*/;
		// temporary code to allow the compiler to revert to a single thread
		String setting = System.getProperty("jdt.compiler.useSingleThread"); //$NON-NLS-1$
		this.batchCompiler.useSingleThread = setting != null && setting.equals("true"); //$NON-NLS-1$

		if (this.compilerOptions.complianceLevel >= ClassFileConstants.JDK1_6
				&& this.compilerOptions.processAnnotations) {
			if (checkVMVersion(ClassFileConstants.JDK1_6)) {
				initializeAnnotationProcessorManager();
				if (this.classNames != null) {
					this.batchCompiler.setBinaryTypes(processClassNames(this.batchCompiler.lookupEnvironment));
				}
			} else {
				// report a warning
				this.logger.logIncorrectVMVersionForAnnotationProcessing();
			}
			if (checkVMVersion(ClassFileConstants.JDK9)) {
				initRootModules(this.batchCompiler.lookupEnvironment, environment);
			}
		}

		// set the non-externally configurable options.
		this.compilerOptions.verbose = this.verbose;
		this.compilerOptions.produceReferenceInfo = this.produceRefInfo;
		try {
			this.logger.startLoggingSources();
			this.batchCompiler.compile(getCompilationUnits());
		} finally {
			this.logger.endLoggingSources();
		}

		if (this.extraProblems != null) {
			loggingExtraProblems();
			this.extraProblems = null;
		}
		if (this.compilerStats != null) {
			this.compilerStats[this.currentRepetition] = this.batchCompiler.stats;
		}
		this.logger.printStats();
	}
	finally {
	// cleanup
		environment.cleanup();
	}
}
protected void loggingExtraProblems() {
	this.logger.loggingExtraProblems(this);
}
public void printUsage() {
	printUsage("misc.usage"); //$NON-NLS-1$
}
private void printUsage(String sectionID) {
	this.logger.logUsage(
		this.bind(
			sectionID,
			new String[] {
				System.getProperty("path.separator"), //$NON-NLS-1$
				this.bind("compiler.name"), //$NON-NLS-1$
				this.bind("compiler.version"), //$NON-NLS-1$
				this.bind("compiler.copyright") //$NON-NLS-1$
			}));
	this.logger.flush();
}
private void initRootModules(LookupEnvironment environment, FileSystem fileSystem) {
	Map<String, String> map = new HashMap<>();
	for (String m : this.rootModules) {
		ModuleBinding mod = environment.getModule(m.toCharArray());
		if (mod == null) {
			throw new IllegalArgumentException(this.bind("configure.invalidModuleName", m)); //$NON-NLS-1$
		}
		PackageBinding[] exports = mod.getExports();
		for (PackageBinding packageBinding : exports) {
			String qName = CharOperation.toString(packageBinding.compoundName);
			String existing = map.get(qName);
			if (existing != null) {
				throw new IllegalArgumentException(this.bind("configure.packageConflict", new String[] {qName, existing, m})); //$NON-NLS-1$
				// report an error and bail out
			}
			map.put(qName, m);
		}
	}
	if (this.limitedModules != null) {
		for (String m : this.limitedModules) {
			ModuleBinding mod = environment.getModule(m.toCharArray());
			if (mod == null) {
				throw new IllegalArgumentException(this.bind("configure.invalidModuleName", m)); //$NON-NLS-1$
			}
		}
	}
	environment.moduleVersion = this.moduleVersion;
}
private ReferenceBinding[] processClassNames(LookupEnvironment environment) {
	// check for .class file presence in case of apt processing
	int length = this.classNames.length;
	ReferenceBinding[] referenceBindings = new ReferenceBinding[length];
	ModuleBinding[] modules = new ModuleBinding[length];
	Set<ModuleBinding> modSet = new HashSet<>();
	String[] typeNames = new String[length];
	if (this.complianceLevel <= ClassFileConstants.JDK1_8) {
		typeNames = this.classNames;
	} else {
		for (int i = 0; i < length; i++) {
			String currentName = this.classNames[i];
			int idx = currentName.indexOf('/');
			ModuleBinding mod = null;
			if (idx > 0) {
				String m = currentName.substring(0, idx);
				mod = environment.getModule(m.toCharArray());
				if (mod == null) {
					throw new IllegalArgumentException(this.bind("configure.invalidModuleName", m)); //$NON-NLS-1$
				}
				modules[i] = mod;
				modSet.add(mod);
				currentName = currentName.substring(idx + 1);
			}
			typeNames[i] = currentName;
		}
	}

	for (int i = 0; i < length; i++) {
		char[][] compoundName = null;
		String cls = typeNames[i];
		if (cls.indexOf('.') != -1) {
			// consider names with '.' as fully qualified names
			char[] typeName = cls.toCharArray();
			compoundName = CharOperation.splitOn('.', typeName);
		} else {
			compoundName = new char[][] { cls.toCharArray() };
		}
		ModuleBinding mod = modules[i];
		ReferenceBinding type = mod != null ? environment.getType(compoundName, mod) : environment.getType(compoundName);
		if (type != null && type.isValidBinding()) {
			if (type.isBinaryBinding()) {
				referenceBindings[i] = type;
				type.superclass();
			}
		} else {
			throw new IllegalArgumentException(
					this.bind("configure.invalidClassName", this.classNames[i]));//$NON-NLS-1$
		}
	}
	return referenceBindings;
}
private ArrayList<String> processModulePathEntries(String arg) {
	ArrayList<String> paths = new ArrayList<>();
	if (arg == null)
		return paths;
	StringTokenizer tokenizer = new StringTokenizer(arg, File.pathSeparator, false);
	while (tokenizer.hasMoreTokens()) {
		paths.add(tokenizer.nextToken());
	}
	return paths;
}
/*
 * External API
 */
public void processPathEntries(final int defaultSize, final ArrayList<FileSystem.Classpath> paths,
			final String currentPath, String customEncoding, boolean isSourceOnly,
			boolean rejectDestinationPathOnJars) {
	String currentClasspathName = null;
	String currentDestinationPath = null;
	ArrayList<String> currentRuleSpecs = new ArrayList<>(defaultSize);
	StringTokenizer tokenizer = new StringTokenizer(currentPath,
			File.pathSeparator + "[]", true); //$NON-NLS-1$
	ArrayList<String> tokens = new ArrayList<>();
	while (tokenizer.hasMoreTokens()) {
		tokens.add(tokenizer.nextToken());
	}
	// state machine
	final int start = 0;
	final int readyToClose = 1;
	// 'path' 'path1[rule];path2'
	final int readyToCloseEndingWithRules = 2;
	// 'path[rule]' 'path1;path2[rule]'
	final int readyToCloseOrOtherEntry = 3;
	// 'path[rule];' 'path;' 'path1;path2;'
	final int rulesNeedAnotherRule = 4;
	// 'path[rule1;'
	final int rulesStart = 5;
	// 'path[' 'path1;path2['
	final int rulesReadyToClose = 6;
	// 'path[rule' 'path[rule1;rule2'
	final int destinationPathReadyToClose = 7;
	// 'path[-d bin'
	final int readyToCloseEndingWithDestinationPath = 8;
	// 'path[-d bin]' 'path[rule][-d bin]'
	final int destinationPathStart = 9;
	// 'path[rule]['
	final int bracketOpened = 10;
	// '.*[.*'
	final int bracketClosed = 11;
	// '.*([.*])+'

	final int error = 99;
	int state = start;
	String token = null;
	int cursor = 0, tokensNb = tokens.size(), bracket = -1;
	while (cursor < tokensNb && state != error) {
		token = tokens.get(cursor++);
		if (token.equals(File.pathSeparator)) {
			switch (state) {
			case start:
			case readyToCloseOrOtherEntry:
			case bracketOpened:
				break;
			case readyToClose:
			case readyToCloseEndingWithRules:
			case readyToCloseEndingWithDestinationPath:
				state = readyToCloseOrOtherEntry;
				addNewEntry(paths, currentClasspathName, currentRuleSpecs,
						customEncoding, currentDestinationPath, isSourceOnly,
						rejectDestinationPathOnJars);
				currentRuleSpecs.clear();
				break;
			case rulesReadyToClose:
				state = rulesNeedAnotherRule;
				break;
			case destinationPathReadyToClose:
				throw new IllegalArgumentException(
					this.bind("configure.incorrectDestinationPathEntry", //$NON-NLS-1$
						currentPath));
			case bracketClosed:
				cursor = bracket + 1;
				state = rulesStart;
				break;
			default:
				state = error;
			}
		} else if (token.equals("[")) { //$NON-NLS-1$
			switch (state) {
			case start:
				currentClasspathName = ""; //$NON-NLS-1$
				//$FALL-THROUGH$
				case readyToClose:
				bracket = cursor - 1;
				//$FALL-THROUGH$
				case bracketClosed:
				state = bracketOpened;
				break;
			case readyToCloseEndingWithRules:
				state = destinationPathStart;
				break;
			case readyToCloseEndingWithDestinationPath:
				state = rulesStart;
				break;
			case bracketOpened:
			default:
				state = error;
			}
		} else if (token.equals("]")) { //$NON-NLS-1$
			switch (state) {
			case rulesReadyToClose:
				state = readyToCloseEndingWithRules;
				break;
			case destinationPathReadyToClose:
				state = readyToCloseEndingWithDestinationPath;
				break;
			case bracketOpened:
				state = bracketClosed;
				break;
			case bracketClosed:
			default:
				state = error;
			}
		} else {
			// regular word
			switch (state) {
			case start:
			case readyToCloseOrOtherEntry:
				state = readyToClose;
				currentClasspathName = token;
				break;
			case rulesStart:
				if (token.startsWith("-d ")) { //$NON-NLS-1$
					if (currentDestinationPath != null) {
						throw new IllegalArgumentException(
								this.bind("configure.duplicateDestinationPathEntry", //$NON-NLS-1$
										currentPath));
					}
					currentDestinationPath = token.substring(3).trim();
					state = destinationPathReadyToClose;
					break;
				} // else we proceed with a rule
				//$FALL-THROUGH$
			case rulesNeedAnotherRule:
				if (currentDestinationPath != null) {
					throw new IllegalArgumentException(
							this.bind("configure.accessRuleAfterDestinationPath", //$NON-NLS-1$
								currentPath));
				}
				state = rulesReadyToClose;
				currentRuleSpecs.add(token);
				break;
			case destinationPathStart:
				if (!token.startsWith("-d ")) { //$NON-NLS-1$
					state = error;
				} else {
					currentDestinationPath = token.substring(3).trim();
					state = destinationPathReadyToClose;
				}
				break;
			case bracketClosed:
				for (int i = bracket; i < cursor ; i++) {
					currentClasspathName += tokens.get(i);
				}
				state = readyToClose;
				break;
			case bracketOpened:
				break;
			default:
				state = error;
			}
		}
		if (state == bracketClosed && cursor == tokensNb) {
			cursor = bracket + 1;
			state = rulesStart;
		}
	}
	switch(state) {
		case readyToCloseOrOtherEntry:
			break;
		case readyToClose:
		case readyToCloseEndingWithRules:
		case readyToCloseEndingWithDestinationPath:
			addNewEntry(paths, currentClasspathName, currentRuleSpecs,
				customEncoding, currentDestinationPath, isSourceOnly,
				rejectDestinationPathOnJars);
			break;
		case bracketOpened:
		case bracketClosed:
		default :
			// we go on anyway
			if (currentPath.length() != 0) {
				addPendingErrors(this.bind("configure.incorrectClasspath", currentPath));//$NON-NLS-1$
			}
	}
}

private int processPaths(String[] args, int index, String currentArg, ArrayList<String> paths) {
	int localIndex = index;
	int count = 0;
	for (int i = 0, max = currentArg.length(); i < max; i++) {
		switch(currentArg.charAt(i)) {
			case '[' :
				count++;
				break;
			case ']' :
				count--;
				break;
		}
	}
	if (count == 0) {
		paths.add(currentArg);
	} else if (count > 1) {
		throw new IllegalArgumentException(
				this.bind("configure.unexpectedBracket", //$NON-NLS-1$
							currentArg));
	} else {
		StringBuilder currentPath = new StringBuilder(currentArg);
		while (true) {
			if (localIndex >= args.length) {
				throw new IllegalArgumentException(
						this.bind("configure.unexpectedBracket", //$NON-NLS-1$
								currentArg));
			}
			localIndex++;
			String nextArg = args[localIndex];
			for (int i = 0, max = nextArg.length(); i < max; i++) {
				switch(nextArg.charAt(i)) {
					case '[' :
						if (count > 1) {
							throw new IllegalArgumentException(
									this.bind("configure.unexpectedBracket", //$NON-NLS-1$
											nextArg));
						}
						count++;
						break;
					case ']' :
						count--;
						break;
				}
			}
			if (count == 0) {
				currentPath.append(' ');
				currentPath.append(nextArg);
				paths.add(currentPath.toString());
				return localIndex - index;
			} else if (count < 0) {
				throw new IllegalArgumentException(
						this.bind("configure.unexpectedBracket", //$NON-NLS-1$
									nextArg));
			} else {
				currentPath.append(' ');
				currentPath.append(nextArg);
			}
		}

	}
	return localIndex - index;
}
private int processPaths(String[] args, int index, String currentArg, String[] paths) {
	int localIndex = index;
	int count = 0;
	for (int i = 0, max = currentArg.length(); i < max; i++) {
		switch(currentArg.charAt(i)) {
			case '[' :
				count++;
				break;
			case ']' :
				count--;
				break;
		}
	}
	if (count == 0) {
		paths[0] = currentArg;
	} else {
		StringBuilder currentPath = new StringBuilder(currentArg);
		while (true) {
			localIndex++;
			if (localIndex >= args.length) {
				throw new IllegalArgumentException(
						this.bind("configure.unexpectedBracket", //$NON-NLS-1$
								currentArg));
			}
			String nextArg = args[localIndex];
			for (int i = 0, max = nextArg.length(); i < max; i++) {
				switch(nextArg.charAt(i)) {
					case '[' :
						if (count > 1) {
							throw new IllegalArgumentException(
									this.bind("configure.unexpectedBracket", //$NON-NLS-1$
											currentArg));
						}
						count++;
						break;
					case ']' :
						count--;
						break;
				}
			}
			if (count == 0) {
				currentPath.append(' ');
				currentPath.append(nextArg);
				paths[0] = currentPath.toString();
				return localIndex - index;
			} else if (count < 0) {
				throw new IllegalArgumentException(
						this.bind("configure.unexpectedBracket", //$NON-NLS-1$
									currentArg));
			} else {
				currentPath.append(' ');
				currentPath.append(nextArg);
			}
		}

	}
	return localIndex - index;
}
/**
 * Creates a NLS catalog for the given locale.
 */
public void relocalize() {
	relocalize(Locale.getDefault());
}

private void relocalize(Locale locale) {
	this.compilerLocale = locale;
	try {
		this.bundle = ResourceBundleFactory.getBundle(locale);
	} catch(MissingResourceException e) {
		System.out.println("Missing resource : " + Main.bundleName.replace('.', '/') + ".properties for locale " + locale); //$NON-NLS-1$//$NON-NLS-2$
		throw e;
	}
}
/*
 * External API
 */
public void setDestinationPath(String dest) {
	this.destinationPath = dest;
}
/*
 * External API
 */
public void setLocale(Locale locale) {
	relocalize(locale);
}
/*
 * External API
 */
protected void setPaths(ArrayList<String> bootclasspaths,
		String sourcepathClasspathArg,
		ArrayList<String> sourcepathClasspaths,
		ArrayList<String> classpaths,
		String modulePath,
		String moduleSourcepath,
		ArrayList<String> extdirsClasspaths,
		ArrayList<String> endorsedDirClasspaths,
		String customEncoding) {

	if (this.complianceLevel == 0) {
		String version = this.options.get(CompilerOptions.OPTION_Compliance);
		this.complianceLevel = CompilerOptions.versionToJdkLevel(version);
	}
	// process bootclasspath, classpath and sourcepaths
	ArrayList<Classpath> allPaths = null;
	long jdkLevel = validateClasspathOptions(bootclasspaths, endorsedDirClasspaths, extdirsClasspaths);

	if (this.releaseVersion != null && this.complianceLevel < jdkLevel) {
		// TODO: Revisit for access rules
		allPaths = new ArrayList<Classpath>();
		allPaths.add(
				FileSystem.getOlderSystemRelease(this.javaHomeCache.getAbsolutePath(), this.releaseVersion, null));
	} else {
		allPaths = handleBootclasspath(bootclasspaths, customEncoding);
	}

	List<FileSystem.Classpath> cp = handleClasspath(classpaths, customEncoding);

	List<FileSystem.Classpath> mp = handleModulepath(modulePath);

	List<FileSystem.Classpath> msp = handleModuleSourcepath(moduleSourcepath);

	ArrayList<FileSystem.Classpath> sourcepaths = new ArrayList<>();
	if (sourcepathClasspathArg != null) {
		processPathEntries(DEFAULT_SIZE_CLASSPATH, sourcepaths,
			sourcepathClasspathArg, customEncoding, true, false);
	}

	/*
	 * Feed endorsedDirClasspath according to:
	 * - -extdirs first if present;
	 * - else java.ext.dirs if defined;
	 * - else default extensions directory for the platform.
	 */
	List<FileSystem.Classpath> extdirs = handleExtdirs(extdirsClasspaths);

	List<FileSystem.Classpath> endorsed = handleEndorseddirs(endorsedDirClasspaths);

	/*
	 * Concatenate classpath entries
	 * We put the bootclasspath at the beginning of the classpath
	 * entries, followed by the extension libraries, followed by
	 * the sourcepath followed by the classpath.  All classpath
	 * entries are searched for both sources and binaries except
	 * the sourcepath entries which are searched for sources only.
	 */
	allPaths.addAll(0, endorsed);
	allPaths.addAll(extdirs);
	allPaths.addAll(sourcepaths);
	allPaths.addAll(cp);
	allPaths.addAll(mp);
	allPaths.addAll(msp);
	allPaths = FileSystem.ClasspathNormalizer.normalize(allPaths);
	this.checkedClasspaths = new FileSystem.Classpath[allPaths.size()];
	allPaths.toArray(this.checkedClasspaths);
	this.logger.logClasspath(this.checkedClasspaths);

	if (this.annotationPaths != null && CompilerOptions.ENABLED.equals(this.options.get(CompilerOptions.OPTION_AnnotationBasedNullAnalysis))) {
		for (FileSystem.Classpath c : this.checkedClasspaths) {
			if (c instanceof ClasspathJar)
				((ClasspathJar) c).annotationPaths = this.annotationPaths;
			else if (c instanceof ClasspathJrt)
				((ClasspathJrt) c).annotationPaths = this.annotationPaths;
		}
	}
}
public final static boolean shouldIgnoreOptionalProblems(char[][] folderNames, char[] fileName) {
	if (folderNames == null || fileName == null) {
		return false;
	}
	for (int i = 0, max = folderNames.length; i < max; i++) {
		char[] folderName = folderNames[i];
		if (isParentOf(folderName, fileName)) {
			return true;
		}
	}
	return false;
}
protected long validateClasspathOptions(ArrayList<String> bootclasspaths, ArrayList<String> endorsedDirClasspaths, ArrayList<String> extdirsClasspaths) {
	if (this.complianceLevel > ClassFileConstants.JDK1_8) {
		if (bootclasspaths != null && bootclasspaths.size() > 0)
			throw new IllegalArgumentException(
				this.bind("configure.unsupportedOption", "-bootclasspath")); //$NON-NLS-1$ //$NON-NLS-2$
		if (extdirsClasspaths != null && extdirsClasspaths.size() > 0)
			throw new IllegalArgumentException(
				this.bind("configure.unsupportedOption", "-extdirs")); //$NON-NLS-1$ //$NON-NLS-2$
		if (endorsedDirClasspaths != null && endorsedDirClasspaths.size() > 0)
			throw new IllegalArgumentException(
				this.bind("configure.unsupportedOption", "-endorseddirs")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	long jdkLevel = Util.getJDKLevel(getJavaHome());
	if (jdkLevel < ClassFileConstants.JDK9 && this.releaseVersion != null) {
		throw new IllegalArgumentException(
				this.bind("configure.unsupportedReleaseOption")); //$NON-NLS-1$
	}
	return jdkLevel;
}
protected void validateOptions(boolean didSpecifyCompliance) {
	if (didSpecifyCompliance) {
		String version = this.options.get(CompilerOptions.OPTION_Compliance);
		if (this.releaseVersion != null) {
			throw new IllegalArgumentException(
					this.bind("configure.unsupportedWithRelease", version));//$NON-NLS-1$
		}
		if (CompilerOptions.VERSION_1_3.equals(version)) {
			if (!this.didSpecifySource) this.options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_3);
			if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_1);
		} else if (CompilerOptions.VERSION_1_4.equals(version)) {
			if (this.didSpecifySource) {
				Object source = this.options.get(CompilerOptions.OPTION_Source);
				if (CompilerOptions.VERSION_1_3.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_2);
				} else if (CompilerOptions.VERSION_1_4.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
				}
			} else {
				this.options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_3);
				if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_2);
			}
		} else if (CompilerOptions.VERSION_1_5.equals(version)) {
			if (this.didSpecifySource) {
				Object source = this.options.get(CompilerOptions.OPTION_Source);
				if (CompilerOptions.VERSION_1_3.equals(source)
						|| CompilerOptions.VERSION_1_4.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
				} else if (CompilerOptions.VERSION_1_5.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_5);
				}
			} else {
				this.options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_5);
				if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_5);
			}
		} else if (CompilerOptions.VERSION_1_6.equals(version)) {
			if (this.didSpecifySource) {
				Object source = this.options.get(CompilerOptions.OPTION_Source);
				if (CompilerOptions.VERSION_1_3.equals(source)
						|| CompilerOptions.VERSION_1_4.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
				} else if (CompilerOptions.VERSION_1_5.equals(source)
						|| CompilerOptions.VERSION_1_6.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
				}
			} else {
				this.options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_6);
				if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
			}
		} else if (CompilerOptions.VERSION_1_7.equals(version)) {
			if (this.didSpecifySource) {
				Object source = this.options.get(CompilerOptions.OPTION_Source);
				if (CompilerOptions.VERSION_1_3.equals(source)
						|| CompilerOptions.VERSION_1_4.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
				} else if (CompilerOptions.VERSION_1_5.equals(source)
						|| CompilerOptions.VERSION_1_6.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
				} else if (CompilerOptions.VERSION_1_7.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_7);
				}
			} else {
				this.options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7);
				if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_7);
			}
		} else if (CompilerOptions.VERSION_1_8.equals(version)) {
			if (this.didSpecifySource) {
				Object source = this.options.get(CompilerOptions.OPTION_Source);
				if (CompilerOptions.VERSION_1_3.equals(source)
						|| CompilerOptions.VERSION_1_4.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
				} else if (CompilerOptions.VERSION_1_5.equals(source)
						|| CompilerOptions.VERSION_1_6.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
				} else if (CompilerOptions.VERSION_1_7.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_7);
				} else if (CompilerOptions.VERSION_1_8.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_8);
				}
			} else {
				this.options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_8);
				if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_8);
			}
		} else if (CompilerOptions.VERSION_9.equals(version)) {
			if (this.didSpecifySource) {
				Object source = this.options.get(CompilerOptions.OPTION_Source);
				if (CompilerOptions.VERSION_1_3.equals(source)
						|| CompilerOptions.VERSION_1_4.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
				} else if (CompilerOptions.VERSION_1_5.equals(source)
						|| CompilerOptions.VERSION_1_6.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
				} else if (CompilerOptions.VERSION_1_7.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_7);
				} else if (CompilerOptions.VERSION_1_8.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_8);
				} else if (CompilerOptions.VERSION_9.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_9);
				}
			} else {
				this.options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_9);
				if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_9);
			}
		} else if (CompilerOptions.VERSION_10.equals(version)) {
			if (this.didSpecifySource) {
				Object source = this.options.get(CompilerOptions.OPTION_Source);
				if (CompilerOptions.VERSION_1_3.equals(source)
						|| CompilerOptions.VERSION_1_4.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
				} else if (CompilerOptions.VERSION_1_5.equals(source)
						|| CompilerOptions.VERSION_1_6.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
				} else if (CompilerOptions.VERSION_1_7.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_7);
				} else if (CompilerOptions.VERSION_1_8.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_8);
				} else if (CompilerOptions.VERSION_9.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_9);
				} else if (CompilerOptions.VERSION_10.equals(source)) {
					if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_10);
				}
			} else {
				this.options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_10);
				if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_10);
			}
		} else {
			if (!this.didSpecifyTarget) {
				if (this.didSpecifySource) {
					String source = this.options.get(CompilerOptions.OPTION_Source);
					if (CompilerOptions.VERSION_1_3.equals(source)
							|| CompilerOptions.VERSION_1_4.equals(source)) {
						if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
					} else if (CompilerOptions.VERSION_1_5.equals(source)
							|| CompilerOptions.VERSION_1_6.equals(source)) {
						this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
					} else {
						// 1.3 is the lowest version that can be specified as -source
						// The following check will ensure '0' is ignored.
						if (CompilerOptions.versionToJdkLevel(source) >= ClassFileConstants.JDK1_7)
							this.options.put(CompilerOptions.OPTION_TargetPlatform, source);
					}
				} else {
					if (CompilerOptions.versionToJdkLevel(version) > ClassFileConstants.JDK10) {
						this.options.put(CompilerOptions.OPTION_Source, version);
						this.options.put(CompilerOptions.OPTION_TargetPlatform, version);
					}
				}
			}
		}

	} else if (this.didSpecifySource) {
		String version = this.options.get(CompilerOptions.OPTION_Source);
		// default is source 1.3 target 1.2 and compliance 1.4
		if (CompilerOptions.VERSION_1_4.equals(version)) {
			if (!didSpecifyCompliance) this.options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_4);
			if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
		} else if (CompilerOptions.VERSION_1_5.equals(version)) {
			if (!didSpecifyCompliance) this.options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_5);
			if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_5);
		} else if (CompilerOptions.VERSION_1_6.equals(version)) {
			if (!didSpecifyCompliance) this.options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_6);
			if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
		} else if (CompilerOptions.VERSION_1_7.equals(version)) {
			if (!didSpecifyCompliance) this.options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_7);
			if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_7);
		} else if (CompilerOptions.VERSION_1_8.equals(version)) {
			if (!didSpecifyCompliance) this.options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_8);
			if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_8);
		} else if (CompilerOptions.VERSION_9.equals(version)) {
			if (!didSpecifyCompliance) this.options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_9);
			if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_9);
		} else if (CompilerOptions.VERSION_10.equals(version)) {
			if (!didSpecifyCompliance) this.options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_10);
			if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_10);
		} else {
			if (CompilerOptions.versionToJdkLevel(version) > ClassFileConstants.JDK10) {
				if (!didSpecifyCompliance) this.options.put(CompilerOptions.OPTION_Compliance, version);
				if (!this.didSpecifyTarget) this.options.put(CompilerOptions.OPTION_TargetPlatform, version);
			}
		}
	}

	final String sourceVersion = this.options.get(CompilerOptions.OPTION_Source);
	if (this.complianceLevel == 0) {
		final String compliance = this.options.get(CompilerOptions.OPTION_Compliance);
		this.complianceLevel = CompilerOptions.versionToJdkLevel(compliance);
	}
	if (sourceVersion.equals(CompilerOptions.VERSION_10)
			&& this.complianceLevel < ClassFileConstants.JDK10) {
		// compliance must be 10 if source is 10
		throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForSource", this.options.get(CompilerOptions.OPTION_Compliance), CompilerOptions.VERSION_10)); //$NON-NLS-1$
	} else if (sourceVersion.equals(CompilerOptions.VERSION_9)
			&& this.complianceLevel < ClassFileConstants.JDK9) {
		// compliance must be 9 if source is 9
		throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForSource", this.options.get(CompilerOptions.OPTION_Compliance), CompilerOptions.VERSION_9)); //$NON-NLS-1$
	} else if (sourceVersion.equals(CompilerOptions.VERSION_1_8)
			&& this.complianceLevel < ClassFileConstants.JDK1_8) {
		// compliance must be 1.8 if source is 1.8
		throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForSource", this.options.get(CompilerOptions.OPTION_Compliance), CompilerOptions.VERSION_1_8)); //$NON-NLS-1$
	} else if (sourceVersion.equals(CompilerOptions.VERSION_1_7)
			&& this.complianceLevel < ClassFileConstants.JDK1_7) {
		// compliance must be 1.7 if source is 1.7
		throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForSource", this.options.get(CompilerOptions.OPTION_Compliance), CompilerOptions.VERSION_1_7)); //$NON-NLS-1$
	} else if (sourceVersion.equals(CompilerOptions.VERSION_1_6)
			&& this.complianceLevel < ClassFileConstants.JDK1_6) {
		// compliance must be 1.6 if source is 1.6
		throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForSource", this.options.get(CompilerOptions.OPTION_Compliance), CompilerOptions.VERSION_1_6)); //$NON-NLS-1$
	} else if (sourceVersion.equals(CompilerOptions.VERSION_1_5)
			&& this.complianceLevel < ClassFileConstants.JDK1_5) {
		// compliance must be 1.5 if source is 1.5
		throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForSource", this.options.get(CompilerOptions.OPTION_Compliance), CompilerOptions.VERSION_1_5)); //$NON-NLS-1$
	} else if (sourceVersion.equals(CompilerOptions.VERSION_1_4)
			&& this.complianceLevel < ClassFileConstants.JDK1_4) {
		// compliance must be 1.4 if source is 1.4
		throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForSource", this.options.get(CompilerOptions.OPTION_Compliance), CompilerOptions.VERSION_1_4)); //$NON-NLS-1$
	} else {
		long ver = CompilerOptions.versionToJdkLevel(sourceVersion);
		if(this.complianceLevel < ver)
			throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForSource", this.options.get(CompilerOptions.OPTION_Compliance), sourceVersion)); //$NON-NLS-1$
	}
	if (this.enablePreview && this.complianceLevel != ClassFileConstants.getLatestJDKLevel()) {
		throw new IllegalArgumentException(this.bind("configure.unsupportedPreview")); //$NON-NLS-1$
	}

	// check and set compliance/source/target compatibilities
	if (this.didSpecifyTarget) {
		final String targetVersion = this.options.get(CompilerOptions.OPTION_TargetPlatform);
		// tolerate jsr14 target
		if (CompilerOptions.VERSION_JSR14.equals(targetVersion)) {
			// expecting source >= 1.5
			if (CompilerOptions.versionToJdkLevel(sourceVersion) < ClassFileConstants.JDK1_5) {
				throw new IllegalArgumentException(this.bind("configure.incompatibleTargetForGenericSource", targetVersion, sourceVersion)); //$NON-NLS-1$
			}
		} else if (CompilerOptions.VERSION_CLDC1_1.equals(targetVersion)) {
			if (this.didSpecifySource && CompilerOptions.versionToJdkLevel(sourceVersion) >= ClassFileConstants.JDK1_4) {
				throw new IllegalArgumentException(this.bind("configure.incompatibleSourceForCldcTarget", targetVersion, sourceVersion)); //$NON-NLS-1$
			}
			if (this.complianceLevel >= ClassFileConstants.JDK1_5) {
				throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForCldcTarget", targetVersion, sourceVersion)); //$NON-NLS-1$
			}
		} else {
			// target must be 1.8 if source is 1.8
			if (CompilerOptions.versionToJdkLevel(sourceVersion) >= ClassFileConstants.JDK1_8
					&& CompilerOptions.versionToJdkLevel(targetVersion) < ClassFileConstants.JDK1_8){
				throw new IllegalArgumentException(this.bind("configure.incompatibleTargetForSource", targetVersion, CompilerOptions.VERSION_1_8)); //$NON-NLS-1$
			}
			// target must be 1.7 if source is 1.7
			if (CompilerOptions.versionToJdkLevel(sourceVersion) >= ClassFileConstants.JDK1_7
					&& CompilerOptions.versionToJdkLevel(targetVersion) < ClassFileConstants.JDK1_7){
				throw new IllegalArgumentException(this.bind("configure.incompatibleTargetForSource", targetVersion, CompilerOptions.VERSION_1_7)); //$NON-NLS-1$
			}
			// target must be 1.6 if source is 1.6
			if (CompilerOptions.versionToJdkLevel(sourceVersion) >= ClassFileConstants.JDK1_6
					&& CompilerOptions.versionToJdkLevel(targetVersion) < ClassFileConstants.JDK1_6){
				throw new IllegalArgumentException(this.bind("configure.incompatibleTargetForSource", targetVersion, CompilerOptions.VERSION_1_6)); //$NON-NLS-1$
			}
			// target must be 1.5 if source is 1.5
			if (CompilerOptions.versionToJdkLevel(sourceVersion) >= ClassFileConstants.JDK1_5
					&& CompilerOptions.versionToJdkLevel(targetVersion) < ClassFileConstants.JDK1_5){
				throw new IllegalArgumentException(this.bind("configure.incompatibleTargetForSource", targetVersion, CompilerOptions.VERSION_1_5)); //$NON-NLS-1$
			}
			// target must be 1.4 if source is 1.4
			if (CompilerOptions.versionToJdkLevel(sourceVersion) >= ClassFileConstants.JDK1_4
					&& CompilerOptions.versionToJdkLevel(targetVersion) < ClassFileConstants.JDK1_4){
				throw new IllegalArgumentException(this.bind("configure.incompatibleTargetForSource", targetVersion, CompilerOptions.VERSION_1_4)); //$NON-NLS-1$
			}
			// target cannot be greater than compliance level
			if (this.complianceLevel < CompilerOptions.versionToJdkLevel(targetVersion)){
				throw new IllegalArgumentException(this.bind("configure.incompatibleComplianceForTarget", this.options.get(CompilerOptions.OPTION_Compliance), targetVersion)); //$NON-NLS-1$
			}
		}
	}
}
}
