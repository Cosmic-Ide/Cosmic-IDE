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

package org.javacs.hover;

import com.google.gson.JsonNull;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.javacs.CompileTask;
import org.javacs.CompilerProvider;
import org.javacs.CompletionData;
import org.javacs.FindHelper;
import org.javacs.JsonHelper;
import org.javacs.MarkdownHelper;
import org.javacs.ParseTask;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@SuppressWarnings("deprecation")
public class HoverProvider {

    public static final List<Either<String, MarkedString>> NOT_SUPPORTED = List.of();
    private static final Logger LOG = Logger.getLogger("main");
    final CompilerProvider compiler;

    public HoverProvider(CompilerProvider compiler) {
        this.compiler = compiler;
    }

    public List<Either<String, MarkedString>> hover(CancelChecker checker, Path file, int line, int column) {
        try (var task = compiler.compile(file)) {
            checker.checkCanceled();
            var position = task.root().getLineMap().getPosition(line, column);
            var element = new FindHoverElement(task.task).scan(task.root(), position);
            checker.checkCanceled();
            if (element == null) return NOT_SUPPORTED;
            var list = new ArrayList<Either<String, MarkedString>>();
            var code = printType(element);
            list.add(Either.forRight(new MarkedString("java", code)));
            var docs = docs(task, element);
            if (!docs.isEmpty()) {
                list.add(Either.forRight(new MarkedString("java", docs)));
            }
            return list;
        }
    }

    public void resolveCompletionItem(CancelChecker checker, CompletionItem item) {
        if (item.getData() == null || item.getData() == JsonNull.INSTANCE) return;
        var data = JsonHelper.GSON.fromJson(JsonHelper.GSON.toJsonTree(item.getData()), CompletionData.class);
        checker.checkCanceled();
        var source = compiler.findAnywhere(data.className);
        checker.checkCanceled();
        if (source.isEmpty()) return;
        var task = compiler.parse(source.get());
        checker.checkCanceled();
        var tree = findItem(task, data);
        checker.checkCanceled();
        resolveDetail(item, data, tree);
        checker.checkCanceled();
        var path = Trees.instance(task.task).getPath(task.root, tree);
        checker.checkCanceled();
        var docTree = DocTrees.instance(task.task).getDocCommentTree(path);
        if (docTree == null) return;
        item.setDocumentation(MarkdownHelper.asMarkupContent(docTree));
    }

    // TODO consider showing actual source code instead of just types and names
    private void resolveDetail(CompletionItem item, CompletionData data, Tree tree) {
        if (tree instanceof MethodTree) {
            var method = (MethodTree) tree;
            var parameters = new StringJoiner(", ");
            for (var p : method.getParameters()) {
                parameters.add(p.getType() + " " + p.getName());
            }
            item.setDetail(method.getReturnType() + " " + method.getName() + "(" + parameters + ")");
            if (!method.getThrows().isEmpty()) {
                var exceptions = new StringJoiner(", ");
                for (var e : method.getThrows()) {
                    exceptions.add(e.toString());
                }
                item.setDetail(item.getDetail() + " throws " + exceptions);
            }
            if (data.plusOverloads != 0) {
                item.setDetail(" (+" + data.plusOverloads + " overloads)");
            }
        }
    }

    private Tree findItem(ParseTask task, CompletionData data) {
        if (data.erasedParameterTypes != null) {
            return FindHelper.findMethod(task, data.className, data.memberName, data.erasedParameterTypes);
        }
        if (data.memberName != null) {
            return FindHelper.findField(task, data.className, data.memberName);
        }
        if (data.className != null) {
            return FindHelper.findType(task, data.className);
        }
        throw new RuntimeException("no className");
    }

    private String docs(CompileTask task, Element element) {
        if (element instanceof TypeElement) {
            var type = (TypeElement) element;
            var className = type.getQualifiedName().toString();
            var file = compiler.findAnywhere(className);
            if (file.isEmpty()) return "";
            var parse = compiler.parse(file.get());
            var tree = FindHelper.findType(parse, className);
            return docs(parse, tree);
        } else if (element.getKind() == ElementKind.FIELD) {
            var field = (VariableElement) element;
            var type = (TypeElement) field.getEnclosingElement();
            var className = type.getQualifiedName().toString();
            var file = compiler.findAnywhere(className);
            if (file.isEmpty()) return "";
            var parse = compiler.parse(file.get());
            var tree = FindHelper.findType(parse, className);
            return docs(parse, tree);
        } else if (element instanceof ExecutableElement) {
            var method = (ExecutableElement) element;
            var type = (TypeElement) method.getEnclosingElement();
            var className = type.getQualifiedName().toString();
            var methodName = method.getSimpleName().toString();
            var erasedParameterTypes = FindHelper.erasedParameterTypes(task, method);
            var file = compiler.findAnywhere(className);
            if (file.isEmpty()) return "";
            var parse = compiler.parse(file.get());
            var tree = FindHelper.findMethod(parse, className, methodName, erasedParameterTypes);
            return docs(parse, tree);
        } else {
            return "";
        }
    }

    private String docs(ParseTask task, Tree tree) {
        var path = Trees.instance(task.task).getPath(task.root, tree);
        var docTree = DocTrees.instance(task.task).getDocCommentTree(path);
        if (docTree == null) return "";
        return MarkdownHelper.asMarkdown(docTree);
    }

    // TODO this should be merged with logic in CompletionProvider
    // TODO this should parameterize the type
    // TODO show more information about declarations---was this a parameter, a field? What were the modifiers?
    private String printType(Element e) {
        if (e instanceof ExecutableElement) {
            var m = (ExecutableElement) e;
            return ShortTypePrinter.DEFAULT.printMethod(m);
        } else if (e instanceof VariableElement) {
            var v = (VariableElement) e;
            return ShortTypePrinter.DEFAULT.print(v.asType()) + " " + v;
        } else if (e instanceof TypeElement) {
            var t = (TypeElement) e;
            var lines = new StringJoiner("\n");
            lines.add(hoverTypeDeclaration(t) + " {");
            for (var member : t.getEnclosedElements()) {
                // TODO check accessibility
                if (member instanceof ExecutableElement || member instanceof VariableElement) {
                    lines.add("  " + printType(member) + ";");
                } else if (member instanceof TypeElement) {
                    lines.add("  " + hoverTypeDeclaration((TypeElement) member) + " { /* removed */ }");
                }
            }
            lines.add("}");
            return lines.toString();
        } else {
            return e.toString();
        }
    }

    private String hoverTypeDeclaration(TypeElement t) {
        var result = new StringBuilder();
        switch (t.getKind()) {
            case ANNOTATION_TYPE:
                result.append("@interface");
                break;
            case INTERFACE:
                result.append("interface");
                break;
            case CLASS:
                result.append("class");
                break;
            case ENUM:
                result.append("enum");
                break;
            default:
                LOG.warning("Don't know what to call type element " + t);
                result.append("_");
        }
        result.append(" ").append(ShortTypePrinter.DEFAULT.print(t.asType()));
        var superType = ShortTypePrinter.DEFAULT.print(t.getSuperclass());
        switch (superType) {
            case "Object":
            case "none":
                break;
            default:
                result.append(" extends ").append(superType);
        }
        return result.toString();
    }
}
