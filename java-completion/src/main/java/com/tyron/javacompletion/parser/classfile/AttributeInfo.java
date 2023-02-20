package com.tyron.javacompletion.parser.classfile;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.EnumSet;

/**
 * attribute_info structure in a .class file.
 *
 * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7
 */
public abstract class AttributeInfo {

    /** See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.6 */
    @AutoValue
    public abstract static class InnerClass extends AttributeInfo {
        public static final String NAME = "InnerClasses";

        @AutoValue
        public abstract static class ClassInfo {
            /**
             * A valid index into the constant_pool table.
             *
             * <p>The constant pool entry at that index must be a {@link
             * ConstantPoolInfo#ConstantClassInfo} structure representing the class. The remaining items
             * in the classes array entry give information about the class.
             */
            public abstract int getInnerClassInfoIndex();

            /**
             * If the class is not a member of a class or an interface (that is, if the class is a
             * top-level class or interface or a local class or an anonymous class), its value must be
             * zero.
             *
             * <p>Otherwise, the value must be a valid index into the constant pool table, and the entry
             * at that index must be a {@link ConstantPoolInfo#ConstantClassInfo} representing the class
             * or interface of which this class is a member.
             */
            public abstract int getOuterClassInfoIndex();

            /**
             * If the class is anonymous, the value must be zero.
             *
             * <p>Otherwise, the value must be a valid index into the constant pool table, and the entry
             * at that index must be a {@link ConstantPoolInfo#ConstantUtf8Info} that represents the
             * original simple name of the class, as given in the source code from which this class file
             * was compiled.
             */
            public abstract int getInnerNameIndex();

            public abstract EnumSet<ClassAccessFlag> getAccessFlags();

            public static ClassInfo create(
                    int innerClassInfoIndex,
                    int outerClassInfoIndex,
                    int innerNameIndex,
                    EnumSet<ClassAccessFlag> accessFlags) {
                return new AutoValue_AttributeInfo_InnerClass_ClassInfo(
                        innerClassInfoIndex, outerClassInfoIndex, innerNameIndex, accessFlags);
            }
        }

        public abstract ImmutableList<ClassInfo> getClasses();

        public static InnerClass create(ImmutableList<ClassInfo> classes) {
            return new AutoValue_AttributeInfo_InnerClass(classes);
        }
    }

    @AutoValue
    public abstract static class Signature extends AttributeInfo {
        public static final String NAME = "Signature";

        public abstract int getSignatureIndex();

        public static Signature create(int signatureIndex) {
            return new AutoValue_AttributeInfo_Signature(signatureIndex);
        }
    }
}