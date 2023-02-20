package com.tyron.javacompletion.parser.classfile;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.TypeReference;

/** Information about the parsed .class file */
@AutoValue
public abstract class ParsedClassFile {
    /**
     * foo/bar/EnclosingClass$SimpleName
     *
     * <p>See JVMS 4.2.1: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.2.1
     */
    public abstract String getClassBinaryName();

    public abstract String getSimpleName();

    public abstract ImmutableList<String> getClassQualifiers();

    public abstract Optional<String> getOuterClassBinaryName();

    public abstract ClassSignature getClassSignature();

    public abstract ImmutableList<ParsedMethod> getMethods();

    public abstract ImmutableList<ParsedField> getFields();

    public abstract Entity.Kind getEntityKind();

    public abstract boolean isStatic();

    public static Builder builder() {
        return new AutoValue_ParsedClassFile.Builder();
    }

    @AutoValue
    public abstract static class ParsedMethod {
        public abstract String getSimpleName();

        public abstract MethodSignature getMethodSignature();

        public abstract boolean isStatic();

        public static ParsedMethod create(
                String simpleName, MethodSignature signature, boolean isStatic) {
            return new AutoValue_ParsedClassFile_ParsedMethod(simpleName, signature, isStatic);
        }
    }

    @AutoValue
    public abstract static class ParsedField {
        public abstract String getSimpleName();

        public abstract TypeReference getFieldType();

        public abstract boolean isStatic();

        public static ParsedField create(String simpleName, TypeReference fieldType, boolean isStatic) {
            return new AutoValue_ParsedClassFile_ParsedField(simpleName, fieldType, isStatic);
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setClassBinaryName(String value);

        public abstract Builder setSimpleName(String value);

        public abstract Builder setClassQualifiers(ImmutableList<String> value);

        public abstract Builder setOuterClassBinaryName(Optional<String> value);

        public abstract Builder setClassSignature(ClassSignature value);

        public abstract ImmutableList.Builder<ParsedMethod> methodsBuilder();

        public Builder addMethod(ParsedMethod method) {
            methodsBuilder().add(method);
            return this;
        }

        public abstract ImmutableList.Builder<ParsedField> fieldsBuilder();

        public Builder addField(ParsedField field) {
            fieldsBuilder().add(field);
            return this;
        }

        public abstract Builder setEntityKind(Entity.Kind value);

        public abstract Builder setStatic(boolean value);

        public abstract ParsedClassFile build();
    }
}