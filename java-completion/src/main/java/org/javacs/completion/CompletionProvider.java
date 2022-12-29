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

package org.javacs.completion;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.javacs.CompileTask;
import org.javacs.CompilerProvider;
import org.javacs.CompletionData;
import org.javacs.FileStore;
import org.javacs.JsonHelper;
import org.javacs.ParseTask;
import org.javacs.SourceFileObject;
import org.javacs.StringSearch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeVariable;

public class CompletionProvider {

    public static final CompletionList NOT_SUPPORTED = new CompletionList(false, List.of());
    public static final int MAX_COMPLETION_ITEMS = 50;
    private static final String[] TOP_LEVEL_KEYWORDS = {
            "package",
            "import",
            "public",
            "private",
            "protected",
            "abstract",
            "class",
            "interface",
            "@interface",
            "extends",
            "implements",
    };
    private static final String[] CLASS_BODY_KEYWORDS = {
            "public",
            "private",
            "protected",
            "static",
            "final",
            "native",
            "synchronized",
            "abstract",
            "default",
            "class",
            "interface",
            "void",
            "boolean",
            "int",
            "long",
            "float",
            "double",
    };
    private static final String[] METHOD_BODY_KEYWORDS = {
            "new",
            "assert",
            "try",
            "catch",
            "finally",
            "throw",
            "return",
            "break",
            "case",
            "continue",
            "default",
            "do",
            "while",
            "for",
            "switch",
            "if",
            "else",
            "instanceof",
            "var",
            "final",
            "class",
            "void",
            "boolean",
            "int",
            "long",
            "float",
            "double",
    };
    private static final CompletionList EMPTY = new CompletionList(false, List.of());
    private static final Logger LOG = Logger.getLogger("main");
    private final CompilerProvider compiler;
    private CancelChecker checker;

    public CompletionProvider(CompilerProvider compiler) {
        this.compiler = compiler;
    }

    public Either<List<CompletionItem>, CompletionList> complete(CancelChecker checker, Path file, int line, int column) {
        this.checker = checker;
        // Check if the request was cancelled
        checker.checkCanceled();

        LOG.info("Complete at " + file.getFileName() + "(" + line + "," + column + ")...");
        var started = Instant.now();
        var task = compiler.parse(file);
        var cursor = task.root.getLineMap().getPosition(line, column);
        var contents = new PruneMethodBodies(task.task).scan(task.root, cursor);
        var endOfLine = endOfLine(contents, (int) cursor);

        checker.checkCanceled();

        contents.insert(endOfLine, ';');
        var list = compileAndComplete(file, contents.toString(), cursor);
        addTopLevelSnippets(task, list);
        logCompletionTiming(started, list.getItems(), list.isIncomplete());
        return Either.forRight(list);
    }

    private int endOfLine(CharSequence contents, int cursor) {
        while (cursor < contents.length()) {
            var c = contents.charAt(cursor);
            if (c == '\r' || c == '\n') break;
            cursor++;
        }
        return cursor;
    }

    private CompletionList compileAndComplete(Path file, String contents, long cursor) {
        var started = Instant.now();
        var source = new SourceFileObject(file, contents, Instant.now());
        var partial = partialIdentifier(contents, (int) cursor);
        var endsWithParen = endsWithParen(contents, (int) cursor);
        LOG.info("Partial identifier: " + partial + " endsWithParen: " + endsWithParen);
        checker.checkCanceled();
        try (var task = compiler.compile(List.of(source))) {
            LOG.info("...compiled in " + Duration.between(started, Instant.now()).toMillis() + "ms");
            var path = new FindCompletionsAt(task.task).scan(task.root(), cursor);
            switch (path.getLeaf().getKind()) {
                case IDENTIFIER:
                    return completeIdentifier(task, path, partial, endsWithParen, file);
                case MEMBER_SELECT:
                    return completeMemberSelect(task, path, partial, endsWithParen);
                case MEMBER_REFERENCE:
                    return completeMemberReference(task, path, partial);
                case SWITCH:
                    return completeSwitchConstant(task, path, partial);
                case IMPORT:
                    return completeImport(qualifiedPartialIdentifier(contents, (int) cursor), file);
                default:
                    var list = new CompletionList();
                    list.setItems(new ArrayList<CompletionItem>());
                    addKeywords(path, partial, list);
                    return list;
            }
        }
    }

    private void addTopLevelSnippets(ParseTask task, CompletionList list) {
        checker.checkCanceled();
        var file = Paths.get(task.root.getSourceFile().toUri());
        if (!hasTypeDeclaration(task.root)) {
            list.getItems().add(classSnippet(file));
            if (task.root.getPackage() == null) {
                list.getItems().add(packageSnippet(file));
            }
        }
    }

    private boolean hasTypeDeclaration(CompilationUnitTree root) {
        checker.checkCanceled();
        for (var tree : root.getTypeDecls()) {
            if (tree.getKind() != Tree.Kind.ERRONEOUS) {
                return true;
            }
        }
        return false;
    }

    private CompletionItem packageSnippet(Path file) {
        checker.checkCanceled();
        var name = FileStore.suggestedPackageName(file);
        return snippetItem("package " + name, "package " + name + ";\n\n");
    }

    private CompletionItem classSnippet(Path file) {
        checker.checkCanceled();
        var name = file.getFileName().toString();
        name = name.substring(0, name.length() - ".java".length());
        return snippetItem("class " + name, "class " + name + " {\n    $0\n}");
    }

    private String partialIdentifier(String contents, int end) {
        checker.checkCanceled();
        var start = end;
        while (start > 0 && Character.isJavaIdentifierPart(contents.charAt(start - 1))) {
            start--;
        }
        return contents.substring(start, end);
    }

    private boolean endsWithParen(String contents, int cursor) {
        checker.checkCanceled();
        for (var i = cursor; i < contents.length(); i++) {
            if (!Character.isJavaIdentifierPart(contents.charAt(i))) {
                return contents.charAt(i) == '(';
            }
        }
        return false;
    }

    private String qualifiedPartialIdentifier(String contents, int end) {
        checker.checkCanceled();
        var start = end;
        while (start > 0 && isQualifiedIdentifierChar(contents.charAt(start - 1))) {
            start--;
        }
        return contents.substring(start, end);
    }

    private boolean isQualifiedIdentifierChar(char c) {
        checker.checkCanceled();
        return c == '.' || Character.isJavaIdentifierPart(c);
    }

    private CompletionList completeIdentifier(CompileTask task, TreePath path, String partial, boolean endsWithParen, Path file) {
        checker.checkCanceled();
        LOG.info("...complete identifiers");
        var list = new CompletionList();
        list.setItems(completeUsingScope(task, path, partial, endsWithParen));
        addStaticImports(task, path.getCompilationUnit(), partial, endsWithParen, list);
        if (!list.isIncomplete() && partial.length() > 0 && Character.isUpperCase(partial.charAt(0))) {
            addClassNames(path.getCompilationUnit(), partial, list, file);
        }
        addKeywords(path, partial, list);
        return list;
    }

    private void addKeywords(TreePath path, String partial, CompletionList list) {
        checker.checkCanceled();
        var level = findTreeLevel(path);
        String[] keywords = {};
        if (level instanceof CompilationUnitTree) {
            keywords = TOP_LEVEL_KEYWORDS;
        } else if (level instanceof ClassTree) {
            keywords = CLASS_BODY_KEYWORDS;
        } else if (level instanceof MethodTree) {
            keywords = METHOD_BODY_KEYWORDS;
        }
        for (var k : keywords) {
            if (StringSearch.matchesPartialName(k, partial)) {
                list.getItems().add(keyword(k));
            }
        }
    }

    private Tree findTreeLevel(TreePath path) {
        checker.checkCanceled();
        while (path != null) {
            if (path.getLeaf() instanceof CompilationUnitTree
                    || path.getLeaf() instanceof ClassTree
                    || path.getLeaf() instanceof MethodTree) {
                return path.getLeaf();
            }
            path = path.getParentPath();
        }
        throw new RuntimeException("empty path");
    }

    private List<CompletionItem> completeUsingScope(
            CompileTask task, TreePath path, String partial, boolean endsWithParen) {
        checker.checkCanceled();
        var trees = Trees.instance(task.task);
        var list = new ArrayList<CompletionItem>();
        var methods = new HashMap<String, List<ExecutableElement>>();
        var scope = trees.getScope(path);
        Predicate<CharSequence> filter = name -> StringSearch.matchesPartialName(name, partial);
        for (var member : ScopeHelper.scopeMembers(task, scope, filter)) {
            if (member.getKind() == ElementKind.METHOD) {
                putMethod((ExecutableElement) member, methods);
            } else {
                list.add(item(task, member));
            }
        }
        for (var overloads : methods.values()) {
            list.add(method(task, overloads, !endsWithParen));
        }
        LOG.info("...found " + list.size() + " scope members");
        return list;
    }

    private void addStaticImports(
            CompileTask task, CompilationUnitTree root, String partial, boolean endsWithParen, CompletionList list) {
        checker.checkCanceled();
        var trees = Trees.instance(task.task);
        var methods = new HashMap<String, List<ExecutableElement>>();
        var previousSize = list.getItems().size();
        outer:
        for (var i : root.getImports()) {
            if (!i.isStatic()) continue;
            var id = (MemberSelectTree) i.getQualifiedIdentifier();
            if (!importMatchesPartial(id.getIdentifier(), partial)) continue;
            var path = trees.getPath(root, id.getExpression());
            var type = (TypeElement) trees.getElement(path);
            for (var member : type.getEnclosedElements()) {
                if (!member.getModifiers().contains(Modifier.STATIC)) continue;
                if (!memberMatchesImport(id.getIdentifier(), member)) continue;
                if (!StringSearch.matchesPartialName(member.getSimpleName(), partial)) continue;
                if (member.getKind() == ElementKind.METHOD) {
                    putMethod((ExecutableElement) member, methods);
                } else {
                    list.getItems().add(item(task, member));
                }
                if (list.getItems().size() + methods.size() > MAX_COMPLETION_ITEMS) {
                    list.setIsIncomplete(true);
                    break outer;
                }
            }
        }
        for (var overloads : methods.values()) {
            list.getItems().add(method(task, overloads, !endsWithParen));
        }
        LOG.info("...found " + (list.getItems().size() - previousSize) + " static imports");
    }

    private boolean importMatchesPartial(Name staticImport, String partial) {
        checker.checkCanceled();
        return staticImport.contentEquals("*") || StringSearch.matchesPartialName(staticImport, partial);
    }

    private boolean memberMatchesImport(Name staticImport, Element member) {
        checker.checkCanceled();
        return staticImport.contentEquals("*") || staticImport.contentEquals(member.getSimpleName());
    }

    private void addClassNames(CompilationUnitTree root, String partial, CompletionList list, Path file) {
        checker.checkCanceled();
        var packageName = Objects.toString(root.getPackageName(), "");
        var uniques = new HashSet<String>();
        var previousSize = list.getItems().size();
        for (var className : compiler.packagePrivateTopLevelTypes(packageName)) {
            if (!StringSearch.matchesPartialName(className, partial)) continue;
            list.getItems().add(classItem(className, file));
            uniques.add(className);
        }
        for (var className : compiler.publicTopLevelTypes()) {
            if (!StringSearch.matchesPartialName(simpleName(className), partial)) continue;
            if (uniques.contains(className)) continue;
            if (list.getItems().size() > MAX_COMPLETION_ITEMS) {
                list.setIsIncomplete(true);
                break;
            }
            list.getItems().add(classItem(className, file));
            uniques.add(className);
        }
        LOG.info("...found " + (list.getItems().size() - previousSize) + " class names");
    }

    private CompletionList completeMemberSelect(
            CompileTask task, TreePath path, String partial, boolean endsWithParen) {
        checker.checkCanceled();
        var trees = Trees.instance(task.task);
        var select = (MemberSelectTree) path.getLeaf();
        LOG.info("...complete members of " + select.getExpression());
        path = new TreePath(path, select.getExpression());
        var isStatic = trees.getElement(path) instanceof TypeElement;
        var scope = trees.getScope(path);
        var type = trees.getTypeMirror(path);
        if (type instanceof ArrayType) {
            return completeArrayMemberSelect(isStatic);
        } else if (type instanceof TypeVariable) {
            return completeTypeVariableMemberSelect(task, scope, (TypeVariable) type, isStatic, partial, endsWithParen);
        } else if (type instanceof DeclaredType) {
            return completeDeclaredTypeMemberSelect(task, scope, (DeclaredType) type, isStatic, partial, endsWithParen);
        } else {
            return NOT_SUPPORTED;
        }
    }

    private CompletionList completeArrayMemberSelect(boolean isStatic) {
        checker.checkCanceled();
        if (isStatic) {
            return EMPTY;
        } else {
            var list = new CompletionList();
            list.setItems(new ArrayList<CompletionItem>());
            list.getItems().add(keyword("length"));
            return list;
        }
    }

    private CompletionList completeTypeVariableMemberSelect(
            CompileTask task, Scope scope, TypeVariable type, boolean isStatic, String partial, boolean endsWithParen) {
        checker.checkCanceled();
        if (type.getUpperBound() instanceof DeclaredType) {
            return completeDeclaredTypeMemberSelect(
                    task, scope, (DeclaredType) type.getUpperBound(), isStatic, partial, endsWithParen);
        } else if (type.getUpperBound() instanceof TypeVariable) {
            return completeTypeVariableMemberSelect(
                    task, scope, (TypeVariable) type.getUpperBound(), isStatic, partial, endsWithParen);
        } else {
            return NOT_SUPPORTED;
        }
    }

    private CompletionList completeDeclaredTypeMemberSelect(
            CompileTask task, Scope scope, DeclaredType type, boolean isStatic, String partial, boolean endsWithParen) {
        checker.checkCanceled();
        var trees = Trees.instance(task.task);
        var typeElement = (TypeElement) type.asElement();
        var list = new ArrayList<CompletionItem>();
        var methods = new HashMap<String, List<ExecutableElement>>();
        for (var member : task.task.getElements().getAllMembers(typeElement)) {
            if (member.getKind() == ElementKind.CONSTRUCTOR) continue;
            if (!StringSearch.matchesPartialName(member.getSimpleName(), partial)) continue;
            if (!trees.isAccessible(scope, member, type)) continue;
            if (isStatic != member.getModifiers().contains(Modifier.STATIC)) continue;
            if (member.getKind() == ElementKind.METHOD) {
                putMethod((ExecutableElement) member, methods);
            } else {
                list.add(item(task, member));
            }
        }
        for (var overloads : methods.values()) {
            list.add(method(task, overloads, !endsWithParen));
        }
        if (isStatic) {
            list.add(keyword("class"));
        }
        if (isStatic && isEnclosingClass(type, scope)) {
            list.add(keyword("this"));
            list.add(keyword("super"));
        }
        return new CompletionList(false, list);
    }

    private boolean isEnclosingClass(DeclaredType type, Scope start) {
        checker.checkCanceled();
        for (var s : ScopeHelper.fastScopes(start)) {
            // If we reach a static method, stop looking
            var method = s.getEnclosingMethod();
            if (method != null && method.getModifiers().contains(Modifier.STATIC)) {
                return false;
            }
            // If we find the enclosing class
            var thisElement = s.getEnclosingClass();
            if (thisElement != null && thisElement.asType().equals(type)) {
                return true;
            }
            // If the enclosing class is static, stop looking
            if (thisElement != null && thisElement.getModifiers().contains(Modifier.STATIC)) {
                return false;
            }
        }
        return false;
    }

    private CompletionList completeMemberReference(CompileTask task, TreePath path, String partial) {
        checker.checkCanceled();
        var trees = Trees.instance(task.task);
        var select = (MemberReferenceTree) path.getLeaf();
        LOG.info("...complete methods of " + select.getQualifierExpression());
        path = new TreePath(path, select.getQualifierExpression());
        var element = trees.getElement(path);
        var isStatic = element instanceof TypeElement;
        var scope = trees.getScope(path);
        var type = trees.getTypeMirror(path);
        if (type instanceof ArrayType) {
            return completeArrayMemberReference(isStatic);
        } else if (type instanceof TypeVariable) {
            return completeTypeVariableMemberReference(task, scope, (TypeVariable) type, isStatic, partial);
        } else if (type instanceof DeclaredType) {
            return completeDeclaredTypeMemberReference(task, scope, (DeclaredType) type, isStatic, partial);
        } else {
            return NOT_SUPPORTED;
        }
    }

    private CompletionList completeArrayMemberReference(boolean isStatic) {
        checker.checkCanceled();
        if (isStatic) {
            var list = new CompletionList();
            list.setItems(new ArrayList<CompletionItem>());
            list.getItems().add(keyword("new"));
            return list;
        } else {
            return EMPTY;
        }
    }

    private CompletionList completeTypeVariableMemberReference(
            CompileTask task, Scope scope, TypeVariable type, boolean isStatic, String partial) {
        checker.checkCanceled();
        if (type.getUpperBound() instanceof DeclaredType) {
            return completeDeclaredTypeMemberReference(
                    task, scope, (DeclaredType) type.getUpperBound(), isStatic, partial);
        } else if (type.getUpperBound() instanceof TypeVariable) {
            return completeTypeVariableMemberReference(
                    task, scope, (TypeVariable) type.getUpperBound(), isStatic, partial);
        } else {
            return NOT_SUPPORTED;
        }
    }

    private CompletionList completeDeclaredTypeMemberReference(
            CompileTask task, Scope scope, DeclaredType type, boolean isStatic, String partial) {

        checker.checkCanceled();
        var trees = Trees.instance(task.task);
        var typeElement = (TypeElement) type.asElement();
        var list = new ArrayList<CompletionItem>();
        var methods = new HashMap<String, List<ExecutableElement>>();
        for (var member : task.task.getElements().getAllMembers(typeElement)) {
            if (!StringSearch.matchesPartialName(member.getSimpleName(), partial)) continue;
            if (member.getKind() != ElementKind.METHOD) continue;
            if (!trees.isAccessible(scope, member, type)) continue;
            if (!isStatic && member.getModifiers().contains(Modifier.STATIC)) continue;
            if (member.getKind() == ElementKind.METHOD) {

                putMethod((ExecutableElement) member, methods);
            } else {
                list.add(item(task, member));
            }
        }
        for (var overloads : methods.values()) {
            list.add(method(task, overloads, false));
        }
        if (isStatic) {
            list.add(keyword("new"));
        }
        return new CompletionList(false, list);
    }

    private void putMethod(ExecutableElement method, Map<String, List<ExecutableElement>> methods) {
        checker.checkCanceled();
        var name = method.getSimpleName().toString();
        if (!methods.containsKey(name)) {
            methods.put(name, new ArrayList<>());
        }
        methods.get(name).add(method);
    }

    private CompletionList completeSwitchConstant(CompileTask task, TreePath path, String partial) {
        checker.checkCanceled();
        var switchTree = (SwitchTree) path.getLeaf();
        path = new TreePath(path, switchTree.getExpression());
        var type = Trees.instance(task.task).getTypeMirror(path);
        LOG.info("...complete constants of type " + type);
        if (!(type instanceof DeclaredType declared)) {
            return NOT_SUPPORTED;
        }
        var element = (TypeElement) declared.asElement();
        var list = new ArrayList<CompletionItem>();
        for (var member : task.task.getElements().getAllMembers(element)) {
            if (member.getKind() != ElementKind.ENUM_CONSTANT) continue;
            if (!StringSearch.matchesPartialName(member.getSimpleName(), partial)) continue;
            list.add(item(task, member));
        }
        return new CompletionList(false, list);
    }

    private CompletionList completeImport(String path, Path file) {
        checker.checkCanceled();
        LOG.info("...complete import");
        var names = new HashSet<String>();
        var list = new CompletionList();
        list.setItems(new ArrayList<CompletionItem>());
        for (var className : compiler.publicTopLevelTypes()) {
            if (className.startsWith(path)) {
                var start = path.lastIndexOf('.');
                var end = className.indexOf('.', path.length());
                if (end == -1) end = className.length();
                var segment = className.substring(start + 1, end);
                if (names.contains(segment)) continue;
                names.add(segment);
                var isClass = end == path.length();
                if (isClass) {
                    list.getItems().add(classItem(className, file));
                } else {
                    list.getItems().add(packageItem(segment));
                }
                if (list.getItems().size() > MAX_COMPLETION_ITEMS) {
                    list.setIsIncomplete(true);
                    return list;
                }
            }
        }
        return list;
    }

    private CompletionItem packageItem(String name) {
        checker.checkCanceled();
        var i = new CompletionItem();
        i.setLabel(name);
        i.setKind(CompletionItemKind.Module);
        return i;
    }

    private CompletionItem classItem(String className, Path file) {
        checker.checkCanceled();
        var i = new CompletionItem();

        i.setAdditionalTextEdits(new ArrayList<>());

        i.setLabel(simpleName(className).toString());
        i.setKind(CompletionItemKind.Class);
        i.setDetail(className);
        var data = new CompletionData();
        data.className = className;
        i.setData(data);
        return i;
    }

    private CompletionItem snippetItem(String label, String snippet) {
        checker.checkCanceled();
        var i = new CompletionItem();
        i.setLabel(label);
        i.setKind(CompletionItemKind.Snippet);
        i.setInsertText(snippet);
        i.setInsertTextFormat(InsertTextFormat.Snippet);
        i.setSortText(String.format("%02d%s", Priority.SNIPPET, i.getLabel()));
        return i;
    }

    private CompletionItem item(CompileTask task, Element element) {
        checker.checkCanceled();
        if (element.getKind() == ElementKind.METHOD) throw new RuntimeException("method");
        var i = new CompletionItem();
        i.setLabel(element.getSimpleName().toString());
        i.setKind(kind(element));
        i.setDetail(element.toString());
        i.setData(data(task, element, 1));
        return i;
    }

    private CompletionItem method(CompileTask task, List<ExecutableElement> overloads, boolean addParens) {
        checker.checkCanceled();
        var first = overloads.get(0);
        var i = new CompletionItem();
        i.setLabel(first.getSimpleName().toString());
        i.setKind(CompletionItemKind.Method);
        i.setDetail(first.getReturnType() + " " + first);
        var data = data(task, first, overloads.size());
        i.setData(JsonHelper.GSON.toJsonTree(data));

        if (addParens) {
            if (overloads.size() == 1 && first.getParameters().isEmpty()) {
                i.setInsertText(first.getSimpleName() + "()$0");
            } else {
                i.setInsertText(first.getSimpleName() + "($0)");
                // Activate signatureHelp
                // Remove this if VSCode ever fixes https://github.com/microsoft/vscode/issues/78806
                i.setCommand(new Command());
                i.getCommand().setCommand("editor.action.triggerParameterHints");
                i.getCommand().setTitle("Trigger Parameter Hints");
            }
            i.setInsertTextFormat(InsertTextFormat.Snippet); // Snippet
        }
        return i;
    }

    private CompletionData data(CompileTask task, Element element, int overloads) {
        checker.checkCanceled();
        var data = new CompletionData();
        if (element instanceof TypeElement type) {
            data.className = type.getQualifiedName().toString();
        } else if (element.getKind() == ElementKind.FIELD) {
            var field = (VariableElement) element;
            var type = (TypeElement) field.getEnclosingElement();
            data.className = type.getQualifiedName().toString();
            data.memberName = field.getSimpleName().toString();
        } else if (element instanceof ExecutableElement method) {
            var types = task.task.getTypes();
            var type = (TypeElement) method.getEnclosingElement();
            data.className = type.getQualifiedName().toString();
            data.memberName = method.getSimpleName().toString();
            data.erasedParameterTypes = new String[method.getParameters().size()];
            for (var i = 0; i < data.erasedParameterTypes.length; i++) {
                var p = method.getParameters().get(i).asType();
                data.erasedParameterTypes[i] = types.erasure(p).toString();
            }
            data.plusOverloads = overloads - 1;
        } else {
            return null;
        }
        return data;
    }

    private CompletionItemKind kind(Element e) {
        checker.checkCanceled();
        switch (e.getKind()) {
            case ANNOTATION_TYPE:
                return CompletionItemKind.Interface;
            case CLASS:
                return CompletionItemKind.Class;
            case CONSTRUCTOR:
                return CompletionItemKind.Constructor;
            case ENUM:
                return CompletionItemKind.Enum;
            case ENUM_CONSTANT:
                return CompletionItemKind.EnumMember;
            case EXCEPTION_PARAMETER:
                return CompletionItemKind.Property;
            case FIELD:
                return CompletionItemKind.Field;
            case STATIC_INIT:
            case INSTANCE_INIT:
                return CompletionItemKind.Function;
            case INTERFACE:
                return CompletionItemKind.Interface;
            case LOCAL_VARIABLE:
                return CompletionItemKind.Variable;
            case METHOD:
                return CompletionItemKind.Method;
            case PACKAGE:
                return CompletionItemKind.Module;
            case PARAMETER:
                return CompletionItemKind.Property;
            case RESOURCE_VARIABLE:
                return CompletionItemKind.Variable;
            case TYPE_PARAMETER:
                return CompletionItemKind.TypeParameter;
            case OTHER:
            default:
                return null;
        }
    }

    private CompletionItem keyword(String keyword) {
        checker.checkCanceled();
        var i = new CompletionItem();
        i.setLabel(keyword);
        i.setKind(CompletionItemKind.Keyword);
        i.setDetail("keyword");
        i.setSortText(String.format("%02d%s", Priority.KEYWORD, i.getLabel()));
        i.setInsertText(keyword.concat(" "));
        return i;
    }

    private void logCompletionTiming(Instant started, List<?> list, boolean isIncomplete) {
        checker.checkCanceled();
        var elapsedMs = Duration.between(started, Instant.now()).toMillis();
        if (isIncomplete)
            LOG.info(String.format("Found %d items (incomplete) in %,d ms", list.size(), elapsedMs));
        else LOG.info(String.format("...found %d items in %,d ms", list.size(), elapsedMs));
    }

    private CharSequence simpleName(String className) {
        checker.checkCanceled();
        var dot = className.lastIndexOf('.');
        if (dot == -1) return className;
        return className.subSequence(dot + 1, className.length());
    }

    private static class Priority {
        static int iota = 0;
        static final int SNIPPET = iota;
        static final int LOCAL = iota++;
        static final int FIELD = iota++;
        static final int INHERITED_FIELD = iota++;
        static final int METHOD = iota++;
        static final int INHERITED_METHOD = iota++;
        static final int OBJECT_METHOD = iota++;
        static final int INNER_CLASS = iota++;
        static final int INHERITED_INNER_CLASS = iota++;
        static final int IMPORTED_CLASS = iota++;
        static final int NOT_IMPORTED_CLASS = iota++;
        static final int KEYWORD = iota++;
        static final int PACKAGE_MEMBER = iota++;
        static final int CASE_LABEL = iota++;
    }
}
