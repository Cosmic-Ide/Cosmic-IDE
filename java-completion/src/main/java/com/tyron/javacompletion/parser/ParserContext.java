/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tyron.javacompletion.parser;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import static com.google.common.base.Charsets.UTF_8;
import static com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

/**
 * Environment for using Javac Parser
 */
public class ParserContext {
    private final Context javacContext;
    private final JavacFileManager javacFileManager;

    public ParserContext() {
        javacContext = new Context();
        javacFileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    }

    /**
     * Set source file of the log.
     *
     * <p>This method should be called before parsing or lexing. If not set, IllegalArgumentException
     * will be thrown if the parser enconters errors.
     */
    public void setupLoggingSource(String filename) {
        SourceFileObject sourceFileObject = new SourceFileObject(filename);
        Log javacLog = Log.instance(javacContext);
        javacLog.useSource(sourceFileObject);
    }

    /**
     * Parses the content of a Java file.
     *
     * @param filename the filename of the Java file
     * @param content the content of the Java file
     */
    public JCCompilationUnit parse(String filename, CharSequence content) {
        setupLoggingSource(filename);

        // Create a parser and start parsing.
        JavacParser parser =
                ParserFactory.instance(javacContext)
                        .newParser(
                                content, true /* keepDocComments */, true /* keepEndPos */, true /* keepLineMap */);
        return parser.parseCompilationUnit();
    }

    public Scanner tokenize(CharSequence content, boolean keepDocComments) {
        return ScannerFactory.instance(javacContext).newScanner(content, keepDocComments);
    }
}
