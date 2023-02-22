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

import com.google.auto.value.AutoValue;

/** A solved type that is referencing to a package. */
@AutoValue
public abstract class SolvedPackageType extends SolvedEntityType {
    @Override
    public abstract PackageEntity getEntity();

    public static SolvedPackageType create(PackageEntity packageEntity) {
        return new AutoValue_SolvedPackageType(packageEntity);
    }

    @Override
    public TypeReference toTypeReference() {
        throw new RuntimeException(
                String.format(
                        "Cannot convert package type %s to type reference.", getEntity().getQualifiedName()));
    }
}