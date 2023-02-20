package com.tyron.javacompletion.parser.classfile;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.TypeReference;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantClassInfo;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo.ConstantUtf8Info;

/** A converter that converts a {@link ClassFileInfo} to a {@link ParsedClassFile}. */
public class ClassInfoConverter {
    public ParsedClassFile convert(ClassFileInfo classFileInfo) {
        ClassInfoReader reader = new ClassInfoReader(classFileInfo);

        ParsedClassFile.Builder builder = ParsedClassFile.builder();

        convertBaseClassInfo(builder, reader);
        convertClassSignature(builder, reader);
        convertMethods(builder, reader);
        convertFields(builder, reader);
        return builder.build();
    }

    private void convertBaseClassInfo(ParsedClassFile.Builder builder, ClassInfoReader reader) {
        String classBinaryName = reader.getClassName(reader.getClassFileInfo().getThisClassIndex());
        TypeReference className =
                new SignatureParser(classBinaryName, reader.getInnerClassMap()).parseClassBinaryName();
        EnumSet<ClassAccessFlag> accessFlags;
        builder.setClassBinaryName(classBinaryName);
        builder.setSimpleName(className.getSimpleName());
        builder.setClassQualifiers(className.getQualifiers());
        if (reader.getInnerClassMap().containsKey(classBinaryName)) {
            InnerClassEntry innerClassEntry = reader.getInnerClassMap().get(classBinaryName);
            builder.setOuterClassBinaryName(Optional.of(innerClassEntry.getOuterClassName()));
            accessFlags = innerClassEntry.getAccessFlags();
            builder.setStatic(accessFlags.contains(ClassAccessFlag.STATIC));
        } else {
            builder.setOuterClassBinaryName(Optional.empty());
            accessFlags = reader.getClassFileInfo().getAccessFlags();
            builder.setStatic(false);
        }

        Entity.Kind entityKind;
        if (accessFlags.contains(ClassAccessFlag.ANNOTATION)) {
            // An annotation is also an interface, so annotation must be checked before interface.
            entityKind = Entity.Kind.ANNOTATION;
        } else if (accessFlags.contains(ClassAccessFlag.INTERFACE)) {
            entityKind = Entity.Kind.INTERFACE;
        } else if (accessFlags.contains(ClassAccessFlag.ENUM)) {
            entityKind = Entity.Kind.ENUM;
        } else {
            entityKind = Entity.Kind.CLASS;
        }
        builder.setEntityKind(entityKind);
    }

    private void convertClassSignature(ParsedClassFile.Builder builder, ClassInfoReader reader) {
        Optional<AttributeInfo.Signature> signature =
                reader.getSignature(reader.getClassFileInfo().getAttributes());
        if (signature.isPresent()) {
            convertClassEntityFromSignature(builder, reader, signature.get());
            return;
        }

        ClassSignature.Builder classSignatureBuilder = ClassSignature.builder();
        int superClassIndex = reader.getClassFileInfo().getSuperClassIndex();
        if (superClassIndex > 0) {
            String superClassName = reader.getClassName(superClassIndex);
            classSignatureBuilder.setSuperClass(
                    new SignatureParser(superClassName, reader.getInnerClassMap()).parseClassBinaryName());
        } else {
            classSignatureBuilder.setSuperClass(TypeReference.JAVA_LANG_OBJECT);
        }

        for (int interfaceIndex : reader.getClassFileInfo().getInterfaceIndeces()) {
            String interfaceName = reader.getClassName(interfaceIndex);
            classSignatureBuilder.addInterface(
                    new SignatureParser(interfaceName, reader.getInnerClassMap()).parseClassBinaryName());
        }

        classSignatureBuilder.setTypeParameters(ImmutableList.of());

        builder.setClassSignature(classSignatureBuilder.build());
    }

    private void convertClassEntityFromSignature(
            ParsedClassFile.Builder builder, ClassInfoReader reader, AttributeInfo.Signature signature) {
        SignatureParser parser =
                new SignatureParser(
                        reader.getUtf8(signature.getSignatureIndex()), reader.getInnerClassMap());
        builder.setClassSignature(parser.parseClassSignature());
    }

    private void convertMethods(ParsedClassFile.Builder builder, ClassInfoReader reader) {
        for (MethodInfo methodInfo : reader.getClassFileInfo().getMethods()) {
            String simpleName = reader.getUtf8(methodInfo.getNameIndex());
            Optional<AttributeInfo.Signature> signature =
                    reader.getSignature(methodInfo.getAttributeInfos());
            int signatureIndex;
            if (signature.isPresent()) {
                signatureIndex = signature.get().getSignatureIndex();
            } else {
                signatureIndex = methodInfo.getDescriptorIndex();
            }
            MethodSignature methodSignature =
                    new SignatureParser(reader.getUtf8(signatureIndex), reader.getInnerClassMap())
                            .parseMethodSignature();
            boolean isStatic = methodInfo.getAccessFlags().contains(MethodInfo.AccessFlag.STATIC);
            builder.addMethod(ParsedClassFile.ParsedMethod.create(simpleName, methodSignature, isStatic));
        }
    }

    private void convertFields(ParsedClassFile.Builder builder, ClassInfoReader reader) {
        for (FieldInfo fieldInfo : reader.getClassFileInfo().getFields()) {
            String simpleName = reader.getUtf8(fieldInfo.getNameIndex());
            Optional<AttributeInfo.Signature> signature =
                    reader.getSignature(fieldInfo.getAttributeInfos());
            TypeReference fieldType;
            if (signature.isPresent()) {
                String signatureString = reader.getUtf8(signature.get().getSignatureIndex());
                // Signature can only be a reference type.
                fieldType =
                        new SignatureParser(signatureString, reader.getInnerClassMap()).parseFieldReference();
            } else {
                String descriptor = reader.getUtf8(fieldInfo.getDescriptorIndex());
                // Descriptor can be either a reference type or a base type.
                fieldType =
                        new SignatureParser(descriptor, reader.getInnerClassMap()).parseJavaTypeSignature();
            }
            boolean isStatic = fieldInfo.getAccessFlags().contains(FieldInfo.AccessFlag.STATIC);
            builder.addField(ParsedClassFile.ParsedField.create(simpleName, fieldType, isStatic));
        }
    }

    private static class ClassInfoReader {
        private final ClassFileInfo classFileInfo;
        private ImmutableMap<String, InnerClassEntry> innerClassMap;

        private ClassInfoReader(ClassFileInfo classFileInfo) {
            this.classFileInfo = classFileInfo;
            this.innerClassMap = null;
        }

        private ClassFileInfo getClassFileInfo() {
            return classFileInfo;
        }

        private ImmutableMap<String, InnerClassEntry> getInnerClassMap() {
            if (innerClassMap == null) {
                innerClassMap = buildInnerClassMap();
            }
            return innerClassMap;
        }

        private ImmutableMap<String, InnerClassEntry> buildInnerClassMap() {
            Optional<AttributeInfo.InnerClass> innerClass =
                    getAttributeOfType(classFileInfo.getAttributes(), AttributeInfo.InnerClass.class);
            if (!innerClass.isPresent()) {
                return ImmutableMap.of();
            }

            ImmutableMap.Builder<String, InnerClassEntry> builder = new ImmutableMap.Builder<>();
            for (AttributeInfo.InnerClass.ClassInfo classInfo : innerClass.get().getClasses()) {
                int innerNameIndex = classInfo.getInnerNameIndex();
                if (innerNameIndex == 0) {
                    // This is an annonymous class. Ignore it since it has no name.
                    continue;
                }
                int outerClassInfoIndex = classInfo.getOuterClassInfoIndex();
                if (outerClassInfoIndex == 0) {
                    // The class is a top-level class. Ignore it.
                    continue;
                }

                String innerClassName = getClassName(classInfo.getInnerClassInfoIndex());
                String outerClassName = getClassName(outerClassInfoIndex);
                String innerName = getUtf8(innerNameIndex);
                builder.put(
                        innerClassName,
                        InnerClassEntry.create(outerClassName, innerName, classInfo.getAccessFlags()));
            }
            return builder.build();
        }

        private String getUtf8(int index) {
            return getConstant(index, ConstantUtf8Info.class).getValue();
        }

        private String getClassName(int classInfoIndex) {
            ConstantClassInfo classInfo = getConstant(classInfoIndex, ConstantClassInfo.class);
            return getUtf8(classInfo.getNameIndex());
        }

        private <T extends ConstantPoolInfo> T getConstant(int index, Class<T> constantClass) {
            checkArgument(
                    index > 0 && index < classFileInfo.getConstantPool().size(),
                    "Constant index %s out of range [1, %s]",
                    index,
                    classFileInfo.getConstantPool().size());
            ConstantPoolInfo constant = classFileInfo.getConstantPool().get(index);
            checkArgument(
                    constantClass.isAssignableFrom(constant.getClass()),
                    "The class of constant %s is %s, not %s",
                    index,
                    constant.getClass().getSimpleName(),
                    constantClass.getSimpleName());
            @SuppressWarnings("unchecked")
            T ret = (T) constant;
            return ret;
        }

        private Optional<AttributeInfo.Signature> getSignature(List<AttributeInfo> attributes) {
            return getAttributeOfType(attributes, AttributeInfo.Signature.class);
        }

        private <T extends AttributeInfo> Optional<T> getAttributeOfType(
                List<AttributeInfo> attributes, Class<T> attributeType) {
            for (AttributeInfo attribute : attributes) {
                if (attributeType.isAssignableFrom(attribute.getClass())) {
                    @SuppressWarnings("unchecked")
                    T ret = (T) attribute;
                    return Optional.of(ret);
                }
            }
            return Optional.empty();
        }
    }
}