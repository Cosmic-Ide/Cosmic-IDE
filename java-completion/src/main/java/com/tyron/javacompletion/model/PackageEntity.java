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
package com.tyron.javacompletion.model;

import com.google.common.collect.Range;
import com.tyron.javacompletion.model.util.QualifiedNames;

import java.util.List;
import java.util.Optional;

/** Represents a package. */
public class PackageEntity extends Entity {
    private final PackageScope packageScope;

    public PackageEntity(String simpleName, List<String> qualifiers, PackageScope packageScope) {
        super(
                simpleName,
                Entity.Kind.QUALIFIER,
                qualifiers,
                true /* isStatic */,
                Optional.empty() /* javadoc */,
                Range.closedOpen(0, 0));
        this.packageScope = packageScope;
    }

    @Override
    public PackageScope getScope() {
        return packageScope;
    }

    @Override
    public String toString() {
        return "PackageEntity<"
                + QualifiedNames.formatQualifiedName(getQualifiers(), getSimpleName())
                + ">";
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.empty();
    }
}