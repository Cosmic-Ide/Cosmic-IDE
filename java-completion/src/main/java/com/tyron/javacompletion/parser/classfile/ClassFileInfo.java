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
import com.google.common.collect.ImmutableList;
import java.util.EnumSet;

@AutoValue
public abstract class ClassFileInfo {
    public abstract ImmutableList<ConstantPoolInfo> getConstantPool();

    public abstract EnumSet<ClassAccessFlag> getAccessFlags();

    public abstract int getThisClassIndex();

    public abstract int getSuperClassIndex();

    public abstract ImmutableList<Integer> getInterfaceIndeces();

    public abstract ImmutableList<FieldInfo> getFields();

    public abstract ImmutableList<MethodInfo> getMethods();

    public abstract ImmutableList<AttributeInfo> getAttributes();

    public static Builder builder() {
        return new AutoValue_ClassFileInfo.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setConstantPool(ImmutableList<ConstantPoolInfo> value);

        public abstract Builder setAccessFlags(EnumSet<ClassAccessFlag> value);

        public abstract Builder setThisClassIndex(int value);

        public abstract Builder setSuperClassIndex(int value);

        public abstract Builder setInterfaceIndeces(ImmutableList<Integer> value);

        public abstract Builder setFields(ImmutableList<FieldInfo> value);

        public abstract Builder setMethods(ImmutableList<MethodInfo> value);

        public abstract Builder setAttributes(ImmutableList<AttributeInfo> value);

        public abstract ClassFileInfo build();
    }
}