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

import com.sun.source.tree.Tree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreeScanner;
import java.util.Optional;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.TypeArgument;
import com.tyron.javacompletion.model.WildcardTypeArgument;

/** Converts a Java source tree to a {@link TypeArgument}. */
public class TypeArgumentScanner extends TreeScanner<TypeArgument, Void> {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    public TypeArgument getTypeArgument(Tree node) {
        return scan(node, null);
    }

    @Override
    public TypeArgument scan(Tree node, Void unused) {
        if (node instanceof WildcardTree) {
            return createWildcardTypeArgument((WildcardTree) node);
        }
        return new TypeReferenceScanner(this).getTypeReference(node);
    }

    private WildcardTypeArgument createWildcardTypeArgument(WildcardTree node) {
        Optional<WildcardTypeArgument.Bound> bound;
        switch (node.getKind()) {
            case SUPER_WILDCARD:
                bound =
                        Optional.of(
                                WildcardTypeArgument.Bound.create(
                                        WildcardTypeArgument.Bound.Kind.SUPER,
                                        new TypeReferenceScanner().getTypeReference(node.getBound())));
                break;
            case EXTENDS_WILDCARD:
                bound =
                        Optional.of(
                                WildcardTypeArgument.Bound.create(
                                        WildcardTypeArgument.Bound.Kind.EXTENDS,
                                        new TypeReferenceScanner().getTypeReference(node.getBound())));
                break;
            case UNBOUNDED_WILDCARD:
                bound = Optional.empty();
                break;
            default:
                logger.warning("Unknown wildcard type varialbe kind: %s", node.getKind());
                bound = Optional.empty();
        }
        return WildcardTypeArgument.create(bound);
    }
}