/************************************************************************************
 * This file is part of Java Language Server (https://github.com/itsaky/java-language-server)
 *
 * Copyright (C) 2021 Akash Yadav
 *
 * Java Language Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Language Server.  If not, see <https://www.gnu.org/licenses/>.
 *
 **************************************************************************************/

package org.javacs.action;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.javacs.CompileTask;
import org.javacs.CompilerProvider;
import org.javacs.FindTypeDeclarationAt;
import org.javacs.rewrite.AddException;
import org.javacs.rewrite.AddImport;
import org.javacs.rewrite.AddSuppressWarningAnnotation;
import org.javacs.rewrite.ConvertFieldToBlock;
import org.javacs.rewrite.ConvertVariableToStatement;
import org.javacs.rewrite.CreateMissingMethod;
import org.javacs.rewrite.GenerateRecordConstructor;
import org.javacs.rewrite.ImplementAbstractMethods;
import org.javacs.rewrite.OverrideInheritedMethod;
import org.javacs.rewrite.RemoveClass;
import org.javacs.rewrite.RemoveException;
import org.javacs.rewrite.RemoveMethod;
import org.javacs.rewrite.Rewrite;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class CodeActionProvider {

    private static final Pattern NOT_THROWN_EXCEPTION = Pattern.compile("^'((\\w+\\.)*\\w+)' is not thrown");
    private static final Pattern UNREPORTED_EXCEPTION = Pattern.compile("unreported exception ((\\w+\\.)*\\w+)");
    private static final Logger LOG = Logger.getLogger("main");
    private final CompilerProvider compiler;
    private final CancelChecker checker;

    public CodeActionProvider(CompilerProvider compiler, CancelChecker checker) {
        this.compiler = compiler;
        this.checker = checker;
    }

    public List<Either<Command, CodeAction>> codeActionsForCursor(CodeActionParams params) {
        var pathUri = fromParams(params);
        LOG.info(String.format("Find code actions at %s(%d)...", pathUri.getPath(), params.getRange().getStart().getLine() + 1));
        var started = Instant.now();
        var file = Paths.get(pathUri);
        // TODO this get-map / convert-to-CodeAction split is an ugly workaround of the fact that we need a new compile
        // task to generate the code actions
        // If we switch to resolving code actions asynchronously using Command, that will fix this problem.
        var rewrites = new TreeMap<String, Rewrite>();
        try (var task = compiler.compile(file)) {
            checker.checkCanceled();
            var elapsed = Duration.between(started, Instant.now()).toMillis();
            LOG.info(String.format("...compiled in %d ms", elapsed));
            var lines = task.root().getLineMap();
            var cursor = lines.getPosition(params.getRange().getStart().getLine() + 1, params.getRange().getStart().getCharacter() + 1);
            rewrites.putAll(overrideInheritedMethods(task, file, cursor));
        }
        var actions = new ArrayList<Either<Command, CodeAction>>();
        for (var title : rewrites.keySet()) {
            // TODO are these all quick fixes?
            actions.addAll(createQuickFix(title, rewrites.get(title), null));
        }
        var elapsed = Duration.between(started, Instant.now()).toMillis();
        LOG.info(String.format("...created %d actions in %d ms", actions.size(), elapsed));
        return actions;
    }

    private URI fromParams(CodeActionParams params) {
        checker.checkCanceled();
        return URI.create(params.getTextDocument().getUri());
    }

    private Map<String, Rewrite> overrideInheritedMethods(CompileTask task, Path file, long cursor) {
        if (!isBlankLine(task.root(), cursor)) return Map.of();
        if (isInMethod(task, cursor)) return Map.of();
        checker.checkCanceled();
        var methodTree = new FindMethodDeclarationAt(task.task).scan(task.root(), cursor);
        if (methodTree != null) return Map.of();
        checker.checkCanceled();
        var actions = new TreeMap<String, Rewrite>();
        var trees = Trees.instance(task.task);
        var classTree = new FindTypeDeclarationAt(task.task).scan(task.root(), cursor);
        if (classTree == null) return Map.of();
        var classPath = trees.getPath(task.root(), classTree);
        var elements = task.task.getElements();
        var classElement = (TypeElement) trees.getElement(classPath);
        for (var member : elements.getAllMembers(classElement)) {
            checker.checkCanceled();
            if (member.getModifiers().contains(Modifier.FINAL)) continue;
            if (member.getKind() != ElementKind.METHOD) continue;
            var method = member;
            var methodSource = (TypeElement) member.getEnclosingElement();
            if (methodSource.getQualifiedName().contentEquals("java.lang.Object")) continue;
            if (methodSource.equals(classElement)) continue;
            var ptr = new MethodPtr(task.task, method);
            checker.checkCanceled();
            var rewrite =
                    new OverrideInheritedMethod(
                            ptr.className, ptr.methodName, ptr.erasedParameterTypes, file, (int) cursor);
            var title = "Override '" + method.getSimpleName() + "' from " + ptr.className;
            checker.checkCanceled();
            actions.put(title, rewrite);
        }
        return actions;
    }

    private boolean isInMethod(CompileTask task, long cursor) {
        var method = new FindMethodDeclarationAt(task.task).scan(task.root(), cursor);
        return method != null;
    }

    private boolean isBlankLine(CompilationUnitTree root, long cursor) {
        checker.checkCanceled();
        var lines = root.getLineMap();
        var line = lines.getLineNumber(cursor);
        var start = lines.getStartPosition(line);
        CharSequence contents;
        try {
            contents = root.getSourceFile().getCharContent(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (var i = start; i < cursor; i++) {
            if (!Character.isWhitespace(contents.charAt((int) i))) {
                return false;
            }
        }
        return true;
    }

    public List<Either<Command, CodeAction>> codeActionForDiagnostics(CodeActionParams params) {
        checker.checkCanceled();
        LOG.info(String.format("Check %d diagnostics for quick fixes...", params.getContext().getDiagnostics().size()));
        var started = Instant.now();
        var file = Paths.get(fromParams(params));
        try (var task = compiler.compile(file)) {
            var actions = new ArrayList<Either<Command, CodeAction>>();
            for (var d : params.getContext().getDiagnostics()) {
                var newActions = codeActionForDiagnostic(task, file, d);
                actions.addAll(newActions);
            }
            var elapsed = Duration.between(started, Instant.now()).toMillis();
            LOG.info(String.format("...created %d quick fixes in %d ms", actions.size(), elapsed));
            return actions;
        }
    }

    private List<Either<Command, CodeAction>> codeActionForDiagnostic(CompileTask task, Path file, Diagnostic d) {
        // TODO this should be done asynchronously using executeCommand
        switch (d.getCode().getLeft()) {
            case "unused_local":
                checker.checkCanceled();
                var toStatement = new ConvertVariableToStatement(file, findPosition(task, d.getRange().getStart()));
                checker.checkCanceled();
                return createQuickFix("Convert to statement", toStatement, d);
            case "unused_field":
                var toBlock = new ConvertFieldToBlock(file, findPosition(task, d.getRange().getStart()));
                checker.checkCanceled();
                return createQuickFix("Convert to block", toBlock, d);
            case "unused_class":
                var removeClass = new RemoveClass(file, findPosition(task, d.getRange().getStart()));
                checker.checkCanceled();
                return createQuickFix("Remove class", removeClass, d);
            case "unused_method":
                var unusedMethod = findMethod(task, d.getRange());
                var removeMethod =
                        new RemoveMethod(
                                unusedMethod.className, unusedMethod.methodName, unusedMethod.erasedParameterTypes);
                checker.checkCanceled();
                return createQuickFix("Remove method", removeMethod, d);
            case "unused_throws":
                var shortExceptionName = extractRange(task, d.getRange());
                var notThrown = extractNotThrownExceptionName(d.getMessage());
                var methodWithExtraThrow = findMethod(task, d.getRange());
                var removeThrow =
                        new RemoveException(
                                methodWithExtraThrow.className,
                                methodWithExtraThrow.methodName,
                                methodWithExtraThrow.erasedParameterTypes,
                                notThrown);
                checker.checkCanceled();
                return createQuickFix("Remove '" + shortExceptionName + "'", removeThrow, d);
            case "compiler.warn.unchecked.call.mbr.of.raw.type":
                var warnedMethod = findMethod(task, d.getRange());
                var suppressWarning =
                        new AddSuppressWarningAnnotation(
                                warnedMethod.className, warnedMethod.methodName, warnedMethod.erasedParameterTypes);
                checker.checkCanceled();
                return createQuickFix("Suppress 'unchecked' warning", suppressWarning, d);
            case "compiler.err.unreported.exception.need.to.catch.or.throw":
                var needsThrow = findMethod(task, d.getRange());
                var exceptionName = extractExceptionName(d.getMessage());
                var addThrows =
                        new AddException(
                                needsThrow.className,
                                needsThrow.methodName,
                                needsThrow.erasedParameterTypes,
                                exceptionName);
                checker.checkCanceled();
                return createQuickFix("Add 'throws'", addThrows, d);
            case "compiler.err.cant.resolve.location":
                var simpleName = extractRange(task, d.getRange());
                var allImports = new ArrayList<Either<Command, CodeAction>>();
                for (var qualifiedName : compiler.publicTopLevelTypes()) {
                    if (qualifiedName.endsWith("." + simpleName)) {
                        var title = "Import '" + qualifiedName + "'";
                        var addImport = new AddImport(file, qualifiedName);
                        allImports.addAll(createQuickFix(title, addImport, d));
                    }
                }
                return allImports;
            case "compiler.err.var.not.initialized.in.default.constructor":
                var needsConstructor = findClassNeedingConstructor(task, d.getRange());
                if (needsConstructor == null) return List.of();
                var generateConstructor = new GenerateRecordConstructor(needsConstructor);
                return createQuickFix("Generate constructor", generateConstructor, d);
            case "compiler.err.does.not.override.abstract":
                final var root = task.root();
                final var lines = root.getLineMap();
                final var treeFinder = newClassFinder(task);
                final var range = d.getRange();
                final var position = lines.getPosition(range.getStart().getLine() + 1, range.getStart().getCharacter() + 1);
                final var tree = treeFinder.scan(root, position);
                final var implementAbstracts = new ImplementAbstractMethods(file, tree, treeFinder.getStoredTreePath());
                return createQuickFix("Implement abstract methods", implementAbstracts, d);
            case "compiler.err.cant.resolve.location.args":
                var missingMethod = new CreateMissingMethod(file, findPosition(task, d.getRange().getStart()));
                return createQuickFix("Create missing method", missingMethod, d);
            default:
                return List.of();
        }
    }

    private int findPosition(CompileTask task, Position position) {
        var lines = task.root().getLineMap();
        return (int) lines.getPosition(position.getLine() + 1, position.getCharacter() + 1);
    }

    private String findClassNeedingConstructor(CompileTask task, Range range) {
        var type = findClassTree(task, range);
        if (type == null || hasConstructor(task, type)) return null;
        return qualifiedName(task, type);
    }

    private String findClass(CompileTask task, Range range) {
        var type = findClassTree(task, range);
        if (type == null) return null;
        return qualifiedName(task, type);
    }

    private FindTypeDeclarationAt newClassFinder(CompileTask task) {
        return new FindTypeDeclarationAt(task.task);
    }

    private ClassTree findClassTree(CompileTask task, Range range) {
        var position = task.root().getLineMap().getPosition(range.getStart().getLine() + 1, range.getStart().getCharacter() + 1);
        return newClassFinder(task).scan(task.root(), position);
    }

    private String qualifiedName(CompileTask task, ClassTree tree) {
        var trees = Trees.instance(task.task);
        var path = trees.getPath(task.root(), tree);
        var type = (TypeElement) trees.getElement(path);
        return type.getQualifiedName().toString();
    }

    private boolean hasConstructor(CompileTask task, ClassTree type) {
        for (var member : type.getMembers()) {
            if (member instanceof MethodTree) {
                var method = member;
                if (isConstructor(task, method)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isConstructor(CompileTask task, MethodTree method) {
        return method.getName().contentEquals("<init>") && !synthentic(task, method);
    }

    private boolean synthentic(CompileTask task, MethodTree method) {
        return Trees.instance(task.task).getSourcePositions().getStartPosition(task.root(), method) != -1;
    }

    private MethodPtr findMethod(CompileTask task, Range range) {
        var trees = Trees.instance(task.task);
        var position = task.root().getLineMap().getPosition(range.getStart().getLine() + 1, range.getStart().getCharacter() + 1);
        var tree = new FindMethodDeclarationAt(task.task).scan(task.root(), position);
        var path = trees.getPath(task.root(), tree);
        var method = (ExecutableElement) trees.getElement(path);
        return new MethodPtr(task.task, method);
    }

    private String extractNotThrownExceptionName(String message) {
        var matcher = NOT_THROWN_EXCEPTION.matcher(message);
        if (!matcher.find()) {
            LOG.warning(String.format("`%s` doesn't match `%s`", message, NOT_THROWN_EXCEPTION));
            return "";
        }
        return matcher.group(1);
    }

    private String extractExceptionName(String message) {
        var matcher = UNREPORTED_EXCEPTION.matcher(message);
        if (!matcher.find()) {
            LOG.warning(String.format("`%s` doesn't match `%s`", message, UNREPORTED_EXCEPTION));
            return "";
        }
        return matcher.group(1);
    }

    private CharSequence extractRange(CompileTask task, Range range) {
        CharSequence contents;
        try {
            contents = task.root().getSourceFile().getCharContent(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var start = (int) task.root().getLineMap().getPosition(range.getStart().getLine() + 1, range.getStart().getCharacter() + 1);
        var end = (int) task.root().getLineMap().getPosition(range.getEnd().getLine() + 1, range.getEnd().getCharacter() + 1);
        return contents.subSequence(start, end);
    }

    private List<Either<Command, CodeAction>> createQuickFix(String title, Rewrite rewrite, org.eclipse.lsp4j.Diagnostic d) {
        checker.checkCanceled();
        var edits = rewrite.rewrite(compiler);
        checker.checkCanceled();
        if (edits == Rewrite.CANCELLED) {
            return List.of();
        }
        var a = new CodeAction();
        a.setKind(CodeActionKind.QuickFix);
        a.setTitle(title);
        a.setEdit(new WorkspaceEdit());
        for (var file : edits.keySet()) {
            a.getEdit().getChanges().put(file.toUri().toString(), List.of(edits.get(file)));
        }
        return List.of(Either.forRight(a));
    }

    class MethodPtr {
        String className, methodName;
        String[] erasedParameterTypes;

        MethodPtr(JavacTask task, ExecutableElement method) {
            var types = task.getTypes();
            var parent = (TypeElement) method.getEnclosingElement();
            className = parent.getQualifiedName().toString();
            methodName = method.getSimpleName().toString();
            erasedParameterTypes = new String[method.getParameters().size()];
            for (var i = 0; i < erasedParameterTypes.length; i++) {
                var param = method.getParameters().get(i);
                var type = param.asType();
                var erased = types.erasure(type);
                erasedParameterTypes[i] = erased.toString();
            }
        }
    }
}
