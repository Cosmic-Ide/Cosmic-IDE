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

package org.javacs.navigation;

import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.javacs.CompileTask;
import org.javacs.CompilerProvider;
import org.javacs.FindHelper;
import org.javacs.SourceFileObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaFileObject;

public class DefinitionProvider {
    public static final List<Location> NOT_SUPPORTED = List.of();
    private final CompilerProvider compiler;
    private final Path file;
    private final int line, column;

    public DefinitionProvider(CompilerProvider compiler, Path file, int line, int column) {
        this.compiler = compiler;
        this.file = file;
        this.line = line;
        this.column = column;
    }

    public List<Location> find(CancelChecker checker) {
        try (var task = compiler.compile(file)) {
            checker.checkCanceled();
            var element = NavigationHelper.findElement(task, file, line, column);
            if (element == null) return NOT_SUPPORTED;
            if (element.asType().getKind() == TypeKind.ERROR) {
                task.close();
                checker.checkCanceled();
                return findError(element);
            }
            // TODO instead of checking isLocal, just try to resolve the location, fall back to searching
            if (NavigationHelper.isLocal(element)) {
                checker.checkCanceled();
                return findDefinitions(task, element);
            }
            var className = className(element);
            if (className.isEmpty()) return NOT_SUPPORTED;
            var otherFile = compiler.findAnywhere(className);
            if (otherFile.isEmpty()) return List.of();
            if (otherFile.get().toUri().equals(file.toUri())) {
                checker.checkCanceled();
                return findDefinitions(task, element);
            }
            task.close();
            checker.checkCanceled();
            return findRemoteDefinitions(otherFile.get());
        }
    }

    private List<Location> findError(Element element) {
        var name = element.getSimpleName();
        if (name == null) return NOT_SUPPORTED;
        var parent = element.getEnclosingElement();
        if (!(parent instanceof TypeElement)) return NOT_SUPPORTED;
        var type = parent;
        var className = type.getQualifiedName().toString();
        var memberName = name.toString();
        return findAllMembers(className, memberName);
    }

    private List<Location> findAllMembers(String className, String memberName) {
        var otherFile = compiler.findAnywhere(className);
        if (otherFile.isEmpty()) return List.of();
        var fileAsSource = new SourceFileObject(file);
        var sources = List.of(fileAsSource, otherFile.get());
        if (otherFile.get().toString().equals(file.toUri())) {
            sources = List.of(fileAsSource);
        }
        var locations = new ArrayList<Location>();
        try (var task = compiler.compile(sources)) {
            var trees = Trees.instance(task.task);
            var elements = task.task.getElements();
            var parentClass = elements.getTypeElement(className);
            for (var member : elements.getAllMembers(parentClass)) {
                if (!member.getSimpleName().contentEquals(memberName)) continue;
                var path = trees.getPath(member);
                if (path == null) continue;
                var location = FindHelper.location(task, path, memberName);
                locations.add(location);
            }
        }
        return locations;
    }

    private String className(Element element) {
        while (element != null) {
            if (element instanceof TypeElement) {
                var type = (TypeElement) element;
                return type.getQualifiedName().toString();
            }
            element = element.getEnclosingElement();
        }
        return "";
    }

    private List<Location> findRemoteDefinitions(JavaFileObject otherFile) {
        try (var task = compiler.compile(List.of(new SourceFileObject(file), otherFile))) {
            var element = NavigationHelper.findElement(task, file, line, column);
            return findDefinitions(task, element);
        }
    }

    private List<Location> findDefinitions(CompileTask task, Element element) {
        var trees = Trees.instance(task.task);
        var path = trees.getPath(element);
        if (path == null) {
            return List.of();
        }
        var name = element.getSimpleName();
        if (name.contentEquals("<init>")) name = element.getEnclosingElement().getSimpleName();
        return List.of(FindHelper.location(task, path, name));
    }
}
