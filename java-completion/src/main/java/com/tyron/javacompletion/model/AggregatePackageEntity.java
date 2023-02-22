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

import java.util.List;
import java.util.Optional;

/** A {@link PackageEntity} that associates to {@link AggregatePackageScope}. */
public class AggregatePackageEntity extends PackageEntity {
    public AggregatePackageEntity(String simpleName, List<String> qualifiers) {
        super(simpleName, qualifiers, new AggregatePackageScope());
    }

    @Override
    public AggregatePackageScope getScope() {
        return (AggregatePackageScope) super.getScope();
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.empty();
    }
}