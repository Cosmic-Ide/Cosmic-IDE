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