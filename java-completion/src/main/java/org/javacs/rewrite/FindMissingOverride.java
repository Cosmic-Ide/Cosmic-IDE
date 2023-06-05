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

package org.javacs.rewrite;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

class FindMissingOverride extends TreePathScanner<Void, List<TreePath>> {
    private static final Logger LOG = Logger.getLogger("main");
    private final Trees trees;
    private final Elements elements;
    private final Types types;

    FindMissingOverride(JavacTask task) {
        this.trees = Trees.instance(task);
        this.elements = task.getElements();
        this.types = task.getTypes();
    }

    @Override
    public Void visitMethod(MethodTree t, List<TreePath> missing) {
        var method = (ExecutableElement) trees.getElement(getCurrentPath());
        var supers = overrides(method);
        if (!supers.isEmpty() && !hasOverrideAnnotation(method)) {
            var overridesMethod = supers.get(0);
            var overridesClass = overridesMethod.getEnclosingElement();
            LOG.info(
                    String.format(
                            "...`%s` has no @Override annotation but overrides `%s.%s`",
                            method, overridesClass, overridesMethod));
            missing.add(getCurrentPath());
        }
        return super.visitMethod(t, null);
    }

    private boolean hasOverrideAnnotation(ExecutableElement method) {
        for (var ann : method.getAnnotationMirrors()) {
            var type = ann.getAnnotationType();
            var el = type.asElement();
            var name = el.toString();
            if (name.equals("java.lang.Override")) {
                return true;
            }
        }
        return false;
    }

    private List<Element> overrides(ExecutableElement method) {
        var missing = new ArrayList<Element>();
        var enclosingClass = (TypeElement) method.getEnclosingElement();
        var enclosingType = enclosingClass.asType();
        for (var superClass : types.directSupertypes(enclosingType)) {
            var e = (TypeElement) types.asElement(superClass);
            for (var other : e.getEnclosedElements()) {
                if (!(other instanceof ExecutableElement)) continue;
                if (elements.overrides(method, (ExecutableElement) other, enclosingClass)) {
                    missing.add(other);
                }
            }
        }
        return missing;
    }
}
