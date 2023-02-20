package com.tyron.javacompletion.parser.classfile;

import com.google.common.collect.ImmutableList;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantClassInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantDoubleInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantFieldrefInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantFloatInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantIntegerInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantInterfaceMethodrefInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantInvokeDynamicInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantLongInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantMethodHandleInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantMethodTypeInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantMethodrefInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantNameAndTypeInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantStringInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantUtf8Info;

/**
 * A parser for Java .class files.
 *
 * <p>Format spec is defined at https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
 */
public class ClassFileParser {
    private static final int CLASS_MAGIC = 0xCAFEBABE;
    // Constant pool tags:
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_FIELDREF = 9;
    private static final int CONSTANT_METHODREF = 10;
    private static final int CONSTANT_INTERFACE_METHODREF = 11;
    private static final int CONSTANT_STRING = 8;
    private static final int CONSTANT_INTEGER = 3;
    private static final int CONSTANT_FLOAT = 4;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;
    private static final int CONSTANT_NAME_AND_TYPE = 12;
    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_METHOD_HANDLE = 15;
    private static final int CONSTANT_METHOD_TYPE = 16;
    private static final int CONSTANT_INVOKE_DYNAMIC = 18;

    private ImmutableList<ConstantPoolInfo> constantPool;

    public ClassFileParser() {}

    public ClassFileInfo parse(Path filePath) throws IOException {
        try (DataInputStream inStream = new DataInputStream(Files.newInputStream(filePath))) {
            return parseClass(inStream);
        }
    }

    // See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.1
    private ClassFileInfo parseClass(DataInputStream inStream) throws IOException {
        ClassFileInfo.Builder builder = ClassFileInfo.builder();
        int magic = inStream.readInt();
        if (magic != CLASS_MAGIC) {
            throw new ClassFileParserError(
                    "Invalid magic of the class file. Expected %x, actual: %x", CLASS_MAGIC, magic);
        }

        int majorVersion = inStream.readUnsignedShort();
        int minorVersion = inStream.readUnsignedShort();

        constantPool = parseConstantPools(inStream);
        builder.setConstantPool(constantPool);

        builder.setAccessFlags(parseClassAccessFlags(inStream));
        builder.setThisClassIndex(inStream.readUnsignedShort());
        builder.setSuperClassIndex(inStream.readUnsignedShort());

        builder.setInterfaceIndeces(parseInterfaces(inStream));
        builder.setFields(parseFields(inStream));
        builder.setMethods(parseMethods(inStream));
        builder.setAttributes(parseAttributes(inStream));
        return builder.build();
    }

    private ImmutableList<ConstantPoolInfo> parseConstantPools(DataInputStream inStream)
            throws IOException {
        int constantPoolCount = inStream.readUnsignedShort();
        // constantPoolCount is the number of constantPool + 1
        ImmutableList.Builder<ConstantPoolInfo> builder = new ImmutableList.Builder<>();

        // The first element is a place holder. We cannot use null because ImmutableList doesn't allow
        // null to be its element.
        builder.add(new ConstantPoolInfo());
        for (int i = 1; i < constantPoolCount; i++) {
            // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4
            byte tag = inStream.readByte();
            ConstantPoolInfo info;
            switch (tag) {
                case CONSTANT_CLASS:
                    info = parseConstantClass(inStream);
                    break;
                case CONSTANT_FIELDREF:
                    info = parseConstantFieldref(inStream);
                    break;
                case CONSTANT_METHODREF:
                    info = parseConstantMethodref(inStream);
                    break;
                case CONSTANT_INTERFACE_METHODREF:
                    info = parseConstantInterfaceMethodref(inStream);
                    break;
                case CONSTANT_STRING:
                    info = parseConstantString(inStream);
                    break;
                case CONSTANT_INTEGER:
                    info = parseConstantInteger(inStream);
                    break;
                case CONSTANT_FLOAT:
                    info = parseConstantFloat(inStream);
                    break;
                case CONSTANT_LONG:
                    info = parseConstantLong(inStream);
                    break;
                case CONSTANT_DOUBLE:
                    info = parseConstantDouble(inStream);
                    break;
                case CONSTANT_NAME_AND_TYPE:
                    info = parseConstantNameAndType(inStream);
                    break;
                case CONSTANT_UTF8:
                    info = parseConstantUtf8(inStream);
                    break;
                case CONSTANT_METHOD_HANDLE:
                    info = parseConstantMethodHandle(inStream);
                    break;
                case CONSTANT_METHOD_TYPE:
                    info = parseConstantMethodTypeInfo(inStream);
                    break;
                case CONSTANT_INVOKE_DYNAMIC:
                    info = parseConstantInvokeDynamicInfo(inStream);
                    break;
                default:
                    throw new ClassFileParserError("Unknown constant pool tag %s", tag);
            }
            builder.add(info);

            if (tag == CONSTANT_DOUBLE || tag == CONSTANT_LONG) {
                // 8-byte constants take up two entries in the constant_pool table.
                // See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.5
                builder.add(info);
                i++;
            }
        }
        return builder.build();
    }

    private EnumSet<ClassAccessFlag> parseClassAccessFlags(DataInputStream inStream)
            throws IOException {
        int accessFlagsInt = inStream.readUnsignedShort();

        EnumSet<ClassAccessFlag> accessFlags = EnumSet.noneOf(ClassAccessFlag.class);
        for (ClassAccessFlag accessFlag : ClassAccessFlag.values()) {
            if ((accessFlagsInt & accessFlag.getValue()) != 0) {
                accessFlags.add(accessFlag);
            }
        }
        return accessFlags;
    }

    private ConstantClassInfo parseConstantClass(DataInputStream inStream) throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.1
        int nameIndex = inStream.readUnsignedShort();
        return ConstantClassInfo.create(nameIndex);
    }

    private ConstantFieldrefInfo parseConstantFieldref(DataInputStream inStream) throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.2
        int classIndex = inStream.readUnsignedShort();
        int nameAndTypeIndex = inStream.readUnsignedShort();
        return ConstantFieldrefInfo.create(classIndex, nameAndTypeIndex);
    }

    private ConstantMethodrefInfo parseConstantMethodref(DataInputStream inStream)
            throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.2
        int classIndex = inStream.readUnsignedShort();
        int nameAndTypeIndex = inStream.readUnsignedShort();
        return ConstantMethodrefInfo.create(classIndex, nameAndTypeIndex);
    }

    private ConstantInterfaceMethodrefInfo parseConstantInterfaceMethodref(DataInputStream inStream)
            throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.2
        int classIndex = inStream.readUnsignedShort();
        int nameAndTypeIndex = inStream.readUnsignedShort();
        return ConstantInterfaceMethodrefInfo.create(classIndex, nameAndTypeIndex);
    }

    private ConstantStringInfo parseConstantString(DataInputStream inStream) throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.3
        int stringIndex = inStream.readUnsignedShort();
        return ConstantStringInfo.create(stringIndex);
    }

    private ConstantIntegerInfo parseConstantInteger(DataInputStream inStream) throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.4
        int value = inStream.readInt();
        return ConstantIntegerInfo.create(value);
    }

    private ConstantFloatInfo parseConstantFloat(DataInputStream inStream) throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.4
        float value = inStream.readFloat();
        return ConstantFloatInfo.create(value);
    }

    private ConstantLongInfo parseConstantLong(DataInputStream inStream) throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.5
        long value = inStream.readLong();
        return ConstantLongInfo.create(value);
    }

    private ConstantDoubleInfo parseConstantDouble(DataInputStream inStream) throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.5
        double value = inStream.readDouble();
        return ConstantDoubleInfo.create(value);
    }

    private ConstantNameAndTypeInfo parseConstantNameAndType(DataInputStream inStream)
            throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.6
        int nameIndex = inStream.readUnsignedShort();
        int descriptorIndex = inStream.readUnsignedShort();
        return ConstantNameAndTypeInfo.create(nameIndex, descriptorIndex);
    }

    private ConstantUtf8Info parseConstantUtf8(DataInputStream inStream) throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.7
        String value = inStream.readUTF();
        return ConstantUtf8Info.create(value);
    }

    private ConstantMethodHandleInfo parseConstantMethodHandle(DataInputStream inStream)
            throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.8
        byte referenceKind = inStream.readByte();
        int referenceIndex = inStream.readUnsignedShort();
        return ConstantMethodHandleInfo.create(referenceKind, referenceIndex);
    }

    private ConstantMethodTypeInfo parseConstantMethodTypeInfo(DataInputStream inStream)
            throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.9
        int descriptorIndex = inStream.readUnsignedShort();
        return ConstantMethodTypeInfo.create(descriptorIndex);
    }

    private ConstantInvokeDynamicInfo parseConstantInvokeDynamicInfo(DataInputStream inStream)
            throws IOException {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.10
        int bootstrapMethodAttrIndex = inStream.readUnsignedShort();
        int nameAndTypeIndex = inStream.readUnsignedShort();
        return ConstantInvokeDynamicInfo.create(bootstrapMethodAttrIndex, nameAndTypeIndex);
    }

    private ImmutableList<Integer> parseInterfaces(DataInputStream inStream) throws IOException {
        int interfacesCount = inStream.readUnsignedShort();
        ImmutableList.Builder<Integer> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < interfacesCount; i++) {
            builder.add(inStream.readUnsignedShort());
        }
        return builder.build();
    }

    private ImmutableList<FieldInfo> parseFields(DataInputStream inStream) throws IOException {
        int fieldsCount = inStream.readUnsignedShort();
        ImmutableList.Builder<FieldInfo> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < fieldsCount; i++) {
            // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.5
            int accessFlagsInt = inStream.readUnsignedShort();
            EnumSet<FieldInfo.AccessFlag> accessFlags = EnumSet.noneOf(FieldInfo.AccessFlag.class);
            for (FieldInfo.AccessFlag flag : FieldInfo.AccessFlag.values()) {
                if ((accessFlagsInt & flag.getValue()) != 0) {
                    accessFlags.add(flag);
                }
            }

            int nameIndex = inStream.readUnsignedShort();
            int descriptorIndex = inStream.readUnsignedShort();
            ImmutableList<AttributeInfo> attributeInfos = parseAttributes(inStream);
            builder.add(FieldInfo.create(accessFlags, nameIndex, descriptorIndex, attributeInfos));
        }
        return builder.build();
    }

    private ImmutableList<MethodInfo> parseMethods(DataInputStream inStream) throws IOException {
        int methodsCount = inStream.readUnsignedShort();
        ImmutableList.Builder<MethodInfo> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < methodsCount; i++) {
            // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.6
            int accessFlagsInt = inStream.readUnsignedShort();
            EnumSet<MethodInfo.AccessFlag> accessFlags = EnumSet.noneOf(MethodInfo.AccessFlag.class);
            for (MethodInfo.AccessFlag flag : MethodInfo.AccessFlag.values()) {
                if ((accessFlagsInt & flag.getValue()) != 0) {
                    accessFlags.add(flag);
                }
            }

            int nameIndex = inStream.readUnsignedShort();
            int descriptorIndex = inStream.readUnsignedShort();
            ImmutableList<AttributeInfo> attributeInfos = parseAttributes(inStream);
            builder.add(MethodInfo.create(accessFlags, nameIndex, descriptorIndex, attributeInfos));
        }
        return builder.build();
    }

    private ImmutableList<AttributeInfo> parseAttributes(DataInputStream inStream)
            throws IOException {
        ImmutableList.Builder<AttributeInfo> builder = new ImmutableList.Builder<>();
        int attributesCount = inStream.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7
            int nameIndex = inStream.readUnsignedShort();
            int length = inStream.readInt();
            String name = getUtf8Constant(nameIndex);
            if (AttributeInfo.InnerClass.NAME.equals(name)) {
                // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.6
                int numClasses = inStream.readUnsignedShort();
                ImmutableList.Builder<AttributeInfo.InnerClass.ClassInfo> classInfoBuilder =
                        new ImmutableList.Builder<>();

                // Each class has 4 u16 fields, plus a u16 for numClasses.
                if (length != 2 * 4 * numClasses + 2) {
                    throw new ClassFileParserError(
                            "Attribute length %s dosn't match the length of InnerClass attribute with %s classes",
                            length, numClasses);
                }

                for (int j = 0; j < numClasses; j++) {
                    int innerClassIndex = inStream.readUnsignedShort();
                    int outerClassIndex = inStream.readUnsignedShort();
                    int innerNameIndex = inStream.readUnsignedShort();
                    EnumSet<ClassAccessFlag> accessFlags = parseClassAccessFlags(inStream);
                    classInfoBuilder.add(
                            AttributeInfo.InnerClass.ClassInfo.create(
                                    innerClassIndex, outerClassIndex, innerNameIndex, accessFlags));
                }
                builder.add(AttributeInfo.InnerClass.create(classInfoBuilder.build()));
            } else if (AttributeInfo.Signature.NAME.equals(name)) {
                // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.9
                if (length != 2) {
                    throw new ClassFileParserError(
                            "Attribute length %s doesn't match the size of Signature attribute", length);
                }
                int signatureIndex = inStream.readUnsignedShort();
                builder.add(AttributeInfo.Signature.create(signatureIndex));
            } else {
                inStream.skipBytes(length);
            }
        }
        return builder.build();
    }

    private String getUtf8Constant(int constantPoolIndex) {
        if (constantPoolIndex < 0 || constantPoolIndex >= constantPool.size()) {
            throw new ClassFileParserError(
                    "Constant pool index %s out of range. Constant pool size is %s",
                    constantPoolIndex, constantPool.size());
        }

        ConstantPoolInfo constant = constantPool.get(constantPoolIndex);

        if (!(constant instanceof ConstantUtf8Info)) {
            throw new ClassFileParserError(
                    "Constant %s is not a ConstantUtf8Info instance. It's %s",
                    constantPoolIndex, constant.getClass().getSimpleName());
        }
        return ((ConstantUtf8Info) constant).getValue();
    }

    public static class ClassFileParserError extends RuntimeException {
        public ClassFileParserError(String fmt, Object... args) {
            super(String.format(fmt, args));
        }
    }
}