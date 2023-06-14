/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

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

/**
 * attribute_info structure in a .class file.
 *
 * <p>See <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7">...</a>
 */
public abstract class AttributeInfo {

    /**
     * See <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.6">...</a>
     */
    @AutoValue
    public abstract static class InnerClass extends AttributeInfo {
        public static final String NAME = "InnerClasses";

        public static InnerClass create(ImmutableList<ClassInfo> classes) {
            return new AutoValue_AttributeInfo_InnerClass(classes);
        }

        public abstract ImmutableList<ClassInfo> getClasses();

        @AutoValue
        public abstract static class ClassInfo {
            public static ClassInfo create(
                    int innerClassInfoIndex,
                    int outerClassInfoIndex,
                    int innerNameIndex,
                    EnumSet<ClassAccessFlag> accessFlags) {
                return new AutoValue_AttributeInfo_InnerClass_ClassInfo(
                        innerClassInfoIndex, outerClassInfoIndex, innerNameIndex, accessFlags);
            }

            /**
             * A valid index into the constant_pool table.
             *
             * <p>The constant pool entry at that index must be a  structure representing the class. The remaining items
             * in the classes array entry give information about the class.
             */
            public abstract int getInnerClassInfoIndex();

            /**
             * If the class is not a member of a class or an interface (that is, if the class is a
             * top-level class or interface or a local class or an anonymous class), its value must be
             * zero.
             *
             * <p>Otherwise, the value must be a valid index into the constant pool table, and the entry
             * at that index must be representing the class
             * or interface of which this class is a member.
             */
            public abstract int getOuterClassInfoIndex();

            /**
             * If the class is anonymous, the value must be zero.
             *
             * <p>Otherwise, the value must be a valid index into the constant pool table, and the entry
             * at that index must represent the
             * original simple name of the class, as given in the source code from which this class file
             * was compiled.
             */
            public abstract int getInnerNameIndex();

            public abstract EnumSet<ClassAccessFlag> getAccessFlags();
        }
    }

    @AutoValue
    public abstract static class Signature extends AttributeInfo {
        public static final String NAME = "Signature";

        public static Signature create(int signatureIndex) {
            return new AutoValue_AttributeInfo_Signature(signatureIndex);
        }

        public abstract int getSignatureIndex();
    }
}