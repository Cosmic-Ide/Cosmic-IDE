/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gauthier JACQUES - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.tool;

import java.io.File;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

public class EclipseDiagnostic implements Diagnostic<EclipseFileObject> {

    private Kind kind;
    private final int problemId;
    private final String[] problemArguments;
    private final char[] originatingFileName;
    private final int lineNumber;
    private final int columnNumber;
    private final int startPosition;
    private final int endPosition;
    private final DefaultProblemFactory problemFactory;

    private EclipseDiagnostic(Kind kind,
                              int problemId,
                              String[] problemArguments,
                              char[] originatingFileName,
                              DefaultProblemFactory problemFactory,
                              int lineNumber,
                              int columnNumber,
                              int startPosition,
                              int endPosition) {
        this.kind = kind;
        this.problemId = problemId;
        this.problemArguments = problemArguments;
        this.originatingFileName = originatingFileName;
        this.problemFactory = problemFactory;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    private EclipseDiagnostic(Kind kind,
                              int problemId,
                              String[] problemArguments,
                              char[] originatingFileName,
                              DefaultProblemFactory problemFactory) {
        this(kind, problemId, problemArguments, originatingFileName, problemFactory, (int)Diagnostic.NOPOS, (int)Diagnostic.NOPOS, (int)Diagnostic.NOPOS, (int)Diagnostic.NOPOS);
    }

    public static EclipseDiagnostic newInstance(CategorizedProblem problem, DefaultProblemFactory factory) {
        if(problem instanceof DefaultProblem) return newInstanceFromDefaultProblem((DefaultProblem) problem, factory);
        return new EclipseDiagnostic(getKind(problem),
              problem.getID(),
              problem.getArguments(),
              problem.getOriginatingFileName(),
              factory);
    }

    private static EclipseDiagnostic newInstanceFromDefaultProblem(DefaultProblem problem, DefaultProblemFactory factory) {
        return new EclipseDiagnostic(getKind(problem),
              problem.getID(),
              problem.getArguments(),
              problem.getOriginatingFileName(),
              factory,
              problem.getSourceLineNumber(),
              problem.getSourceColumnNumber(),
              problem.getSourceStart(),
              problem.getSourceEnd()
        );
    }

    private static Kind getKind(CategorizedProblem problem) {
        Kind kind = Kind.OTHER;
        if(problem.isError()) {
            kind = Kind.ERROR;
        } else if(problem.isWarning()) {
            kind = Kind.WARNING;
        } else if (problem instanceof DefaultProblem && ((DefaultProblem) problem).isInfo()) {
        	kind = Kind.NOTE;
        }
        return kind;
    }

    @Override
    public Kind getKind() {
        return this.kind;
    }

    @Override
    public EclipseFileObject getSource() {
        File f = new File(new String(this.originatingFileName));
        if (f.exists()) {
            return new EclipseFileObject(null, f.toURI(), JavaFileObject.Kind.SOURCE, null);
        }
        return null;
    }

    @Override
    public long getPosition() {
        return this.startPosition;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getStartPosition() {
        return this.startPosition;
    }

    @Override
    public long getEndPosition() {
        return this.endPosition;
    }

    @Override
    public long getLineNumber() {
        return this.lineNumber;
    }

    @Override
    public long getColumnNumber() {
        return this.columnNumber;
    }

    @Override
    public String getCode() {
        return Integer.toString(this.problemId);
    }

    @Override
    public String getMessage(Locale locale) {
        if (locale != null) {
        	this.problemFactory.setLocale(locale);
        }
        return this.problemFactory.getLocalizedMessage(this.problemId, this.problemArguments);
    }
}
