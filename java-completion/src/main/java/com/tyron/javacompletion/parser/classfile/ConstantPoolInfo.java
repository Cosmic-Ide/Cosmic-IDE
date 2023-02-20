package com.tyron.javacompletion.parser.classfile;

import com.google.auto.value.AutoValue;

/**
 * cp_info structure in a .class file.
 *
 * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4
 */
public class ConstantPoolInfo {

    /**
     * CONSTANT_Class_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.1
     */
    @AutoValue
    public abstract static class ConstantClassInfo extends ConstantPoolInfo {
        /**
         * An index into the constant pool table for the class name.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantUtf8Info} representing a
         * valid binary class or interface name encoded in internal form.
         */
        public abstract int getNameIndex();

        public static ConstantClassInfo create(int nameIndex) {
            return new AutoValue_ConstantPoolInfo_ConstantClassInfo(nameIndex);
        }
    }

    /**
     * CONSTANT_Fieldref_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.2
     */
    @AutoValue
    public abstract static class ConstantFieldrefInfo extends ConstantPoolInfo {
        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantClassInfo}.
         */
        public abstract int getClassIndex();
        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantNameAndTypeInfo}.
         */
        public abstract int getNameAndTypeIndex();

        public static ConstantFieldrefInfo create(int classIndex, int nameAndTypeIndex) {
            return new AutoValue_ConstantPoolInfo_ConstantFieldrefInfo(classIndex, nameAndTypeIndex);
        }
    }

    /**
     * CONSTANT_Methodref_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.2
     */
    @AutoValue
    public abstract static class ConstantMethodrefInfo extends ConstantPoolInfo {
        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantClassInfo}.
         */
        public abstract int getClassIndex();
        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantNameAndTypeInfo}.
         */
        public abstract int getNameAndTypeIndex();

        public static ConstantMethodrefInfo create(int classIndex, int nameAndTypeIndex) {
            return new AutoValue_ConstantPoolInfo_ConstantMethodrefInfo(classIndex, nameAndTypeIndex);
        }
    }

    /**
     * CONSTANT_InterfaceMethodref_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.2
     */
    @AutoValue
    public abstract static class ConstantInterfaceMethodrefInfo extends ConstantPoolInfo {
        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantClassInfo}.
         */
        public abstract int getClassIndex();
        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantNameAndTypeInfo}.
         */
        public abstract int getNameAndTypeIndex();

        public static ConstantInterfaceMethodrefInfo create(int classIndex, int nameAndTypeIndex) {
            return new AutoValue_ConstantPoolInfo_ConstantInterfaceMethodrefInfo(
                    classIndex, nameAndTypeIndex);
        }
    }

    /**
     * CONSTANT_String_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.3
     */
    @AutoValue
    public abstract static class ConstantStringInfo extends ConstantPoolInfo {
        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantUtf8Info}.
         */
        public abstract int getStringIndex();

        public static ConstantStringInfo create(int stringIndex) {
            return new AutoValue_ConstantPoolInfo_ConstantStringInfo(stringIndex);
        }
    }

    /**
     * CONSTANT_Integer_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.4
     */
    @AutoValue
    public abstract static class ConstantIntegerInfo extends ConstantPoolInfo {
        /** The value of the integer. */
        public abstract int getValue();

        public static ConstantIntegerInfo create(int value) {
            return new AutoValue_ConstantPoolInfo_ConstantIntegerInfo(value);
        }
    }

    /**
     * CONSTANT_Float_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.4
     */
    @AutoValue
    public abstract static class ConstantFloatInfo extends ConstantPoolInfo {
        /** The value of the float. */
        public abstract float getValue();

        public static ConstantFloatInfo create(float value) {
            return new AutoValue_ConstantPoolInfo_ConstantFloatInfo(value);
        }
    }

    /**
     * CONSTANT_Long_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.5
     */
    @AutoValue
    public abstract static class ConstantLongInfo extends ConstantPoolInfo {
        /** The value of the long. */
        public abstract long getValue();

        public static ConstantLongInfo create(long value) {
            return new AutoValue_ConstantPoolInfo_ConstantLongInfo(value);
        }
    }

    /**
     * CONSTANT_Double_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.5
     */
    @AutoValue
    public abstract static class ConstantDoubleInfo extends ConstantPoolInfo {
        /** The value of the double. */
        public abstract double getValue();

        public static ConstantDoubleInfo create(double value) {
            return new AutoValue_ConstantPoolInfo_ConstantDoubleInfo(value);
        }
    }

    /**
     * CONSTANT_NameAndType_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.6
     */
    @AutoValue
    public abstract static class ConstantNameAndTypeInfo extends ConstantPoolInfo {
        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantUtf8Info}. It's either a
         * special method name {@code <init>}, or a valid unqualified name denoting a field or method.
         */
        public abstract int getNameIndex();

        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantUtf8Info} representing a
         * valid field descriptor or method descriptor.
         */
        public abstract int getDescriptorIndex();

        public static ConstantNameAndTypeInfo create(int nameIndex, int descriptorIndex) {
            return new AutoValue_ConstantPoolInfo_ConstantNameAndTypeInfo(nameIndex, descriptorIndex);
        }
    }

    /**
     * CONSTANT_Utf8_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.7
     */
    @AutoValue
    public abstract static class ConstantUtf8Info extends ConstantPoolInfo {
        /** The value of the UTF-8 string. */
        public abstract String getValue();

        public static ConstantUtf8Info create(String value) {
            return new AutoValue_ConstantPoolInfo_ConstantUtf8Info(value);
        }
    }

    /**
     * CONSTANT_MethodHandle_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.8
     */
    @AutoValue
    public abstract static class ConstantMethodHandleInfo extends ConstantPoolInfo {
        /** The kind of this method handle, which characterizes its bytecode behavior. */
        public abstract byte getReferenceKind();

        /**
         * An index into the constant pool table.
         *
         * <p>The constant pool entry at that index must be as follows:
         *
         * <ol>
         *   <li>If the value of the {@link #getReferenceKind()} is 1 (REF_getField), 2 (REF_getStatic),
         *       3 (REF_putField), or 4 (REF_putStatic), then the constant pool entry at that index must
         *       be a {@link ConstantFieldrefInfo} structure representing a field for which a method
         *       handle is to be created.
         *   <li>If the value of the {@link #getReferenceKind()} is 5 (REF_invokeVirtual) or 8
         *       (REF_newInvokeSpecial), then the constant pool entry at that index must be a {@link
         *       ConstantMethodrefInfo} structure (§4.4.2) representing a class's method or constructor
         *       (§2.9) for which a method handle is to be created.
         *   <li>If the value of the {@link #getReferenceKind()} is 6 (REF_invokeStatic) or 7
         *       (REF_invokeSpecial), then if the class file version number is less than 52.0, the
         *       constant pool entry at that index must be a {@link ConstantMethodrefInfo} structure
         *       representing a class's method for which a method handle is to be created; if the class
         *       file version number is 52.0 or above, the constant_pool entry at that index must be
         *       either a {@link ConstantMethodrefInfo} structure or a {@link
         *       ConstantInterfaceMethodrefInfo} structure (§4.4.2) representing a class's or
         *       interface's method for which a method handle is to be created.
         *   <li>If the value of the {@link #getReferenceKind()} is 9 (REF_invokeInterface), then the
         *       constant_pool entry at that index must be a {@link ConstantInterfaceMethodrefInfo}
         *       structure representing an interface's method for which a method handle is to be
         *       created.
         * </ol>
         *
         * <p>If the value of the {@link #getReferenceKind()} is 5 (REF_invokeVirtual), 6
         * (REF_invokeStatic), 7 (REF_invokeSpecial), or 9 (REF_invokeInterface), the name of the method
         * represented by a {@link ConstantMethodrefInfo} structure or a {@link
         * ConstantInterfaceMethodrefInfo} structure must not be {@code <init>} or {@code <clinit>}.
         *
         * <p>If the value is 8 (REF_newInvokeSpecial), the name of the method represented by a {@link
         * ConstantMethodrefInfo} structure must be <init>.
         */
        public abstract int getReferenceIndex();

        public static ConstantMethodHandleInfo create(byte referenceKind, int referenceIndex) {
            return new AutoValue_ConstantPoolInfo_ConstantMethodHandleInfo(referenceKind, referenceIndex);
        }
    }

    /**
     * CONSTANT_MethodType_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.9
     */
    @AutoValue
    public abstract static class ConstantMethodTypeInfo extends ConstantPoolInfo {
        /**
         * An index into the constant pool table for the class name.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantUtf8Info}.
         */
        public abstract int getDescriptorIndex();

        public static ConstantMethodTypeInfo create(int descriptorIndex) {
            return new AutoValue_ConstantPoolInfo_ConstantMethodTypeInfo(descriptorIndex);
        }
    }

    /**
     * CONSTANT_InvokeDynamic_info structure.
     *
     * <p>See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.9
     */
    @AutoValue
    public abstract static class ConstantInvokeDynamicInfo extends ConstantPoolInfo {
        /** A valid index into the bootstrap methods array of the bootstrap method table. */
        public abstract int getBootstrapMethodAttrIndex();
        /**
         * An index into the constant pool table for the class name.
         *
         * <p>The constant pool entry at the index must be a {@link ConstantNameAndTypeInfo}.
         */
        public abstract int getNameAndTypeIndex();

        public static ConstantInvokeDynamicInfo create(
                int boostrapMethodAttrIndex, int nameAndTypeIndex) {
            return new AutoValue_ConstantPoolInfo_ConstantInvokeDynamicInfo(
                    boostrapMethodAttrIndex, nameAndTypeIndex);
        }
    }
}