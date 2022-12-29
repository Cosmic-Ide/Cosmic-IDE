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

import org.javacs.CompileTask;
import org.javacs.FindNameAt;

import java.nio.file.Path;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

class NavigationHelper {

    static Element findElement(CompileTask task, Path file, int line, int column) {
        for (var root : task.roots) {
            if (root.getSourceFile().toUri().equals(file.toUri())) {
                var trees = Trees.instance(task.task);
                var cursor = root.getLineMap().getPosition(line, column);
                var path = new FindNameAt(task).scan(root, cursor);
                if (path == null) return null;
                return trees.getElement(path);
            }
        }
        throw new RuntimeException("file not found");
    }

    static boolean isLocal(Element element) {
        if (element.getModifiers().contains(Modifier.PRIVATE)) {
            return true;
        }
        switch (element.getKind()) {
            case EXCEPTION_PARAMETER:
            case LOCAL_VARIABLE:
            case PARAMETER:
            case TYPE_PARAMETER:
                return true;
            default:
                return false;
        }
    }

    static boolean isMember(Element element) {
        switch (element.getKind()) {
            case ENUM_CONSTANT:
            case FIELD:
            case METHOD:
            case CONSTRUCTOR:
                return true;
            default:
                return false;
        }
    }

    static boolean isType(Element element) {
        switch (element.getKind()) {
            case ANNOTATION_TYPE:
            case CLASS:
            case ENUM:
            case INTERFACE:
                return true;
            default:
                return false;
        }
    }
}
