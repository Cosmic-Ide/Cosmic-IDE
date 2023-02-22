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
package com.tyron.javacompletion.parser.classfile;

import com.google.auto.value.AutoValue;
import java.util.EnumSet;

@AutoValue
public abstract class InnerClassEntry {
    /** Binary name of the outer class, e.g. foo/bar/OuterClass1$OuterClass2. */
    public abstract String getOuterClassName();
    /** Simple name of the inner class, e.g. InnerClass. */
    public abstract String getInnerName();

    public abstract EnumSet<ClassAccessFlag> getAccessFlags();

    public static InnerClassEntry create(
            String outerClassName, String innerName, EnumSet<ClassAccessFlag> accessFlags) {
        return new AutoValue_InnerClassEntry(outerClassName, innerName, accessFlags);
    }
}