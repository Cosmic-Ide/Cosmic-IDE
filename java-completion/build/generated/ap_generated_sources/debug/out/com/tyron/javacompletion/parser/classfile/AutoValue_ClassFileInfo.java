package com.tyron.javacompletion.parser.classfile;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ClassFileInfo extends ClassFileInfo {

  private final ImmutableList<ConstantPoolInfo> constantPool;

  private final EnumSet<ClassAccessFlag> accessFlags;

  private final int thisClassIndex;

  private final int superClassIndex;

  private final ImmutableList<Integer> interfaceIndeces;

  private final ImmutableList<FieldInfo> fields;

  private final ImmutableList<MethodInfo> methods;

  private final ImmutableList<AttributeInfo> attributes;

  private AutoValue_ClassFileInfo(
      ImmutableList<ConstantPoolInfo> constantPool,
      EnumSet<ClassAccessFlag> accessFlags,
      int thisClassIndex,
      int superClassIndex,
      ImmutableList<Integer> interfaceIndeces,
      ImmutableList<FieldInfo> fields,
      ImmutableList<MethodInfo> methods,
      ImmutableList<AttributeInfo> attributes) {
    this.constantPool = constantPool;
    this.accessFlags = accessFlags;
    this.thisClassIndex = thisClassIndex;
    this.superClassIndex = superClassIndex;
    this.interfaceIndeces = interfaceIndeces;
    this.fields = fields;
    this.methods = methods;
    this.attributes = attributes;
  }

  @Override
  public ImmutableList<ConstantPoolInfo> getConstantPool() {
    return constantPool;
  }

  @Override
  public EnumSet<ClassAccessFlag> getAccessFlags() {
    return accessFlags;
  }

  @Override
  public int getThisClassIndex() {
    return thisClassIndex;
  }

  @Override
  public int getSuperClassIndex() {
    return superClassIndex;
  }

  @Override
  public ImmutableList<Integer> getInterfaceIndeces() {
    return interfaceIndeces;
  }

  @Override
  public ImmutableList<FieldInfo> getFields() {
    return fields;
  }

  @Override
  public ImmutableList<MethodInfo> getMethods() {
    return methods;
  }

  @Override
  public ImmutableList<AttributeInfo> getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    return "ClassFileInfo{"
        + "constantPool=" + constantPool + ", "
        + "accessFlags=" + accessFlags + ", "
        + "thisClassIndex=" + thisClassIndex + ", "
        + "superClassIndex=" + superClassIndex + ", "
        + "interfaceIndeces=" + interfaceIndeces + ", "
        + "fields=" + fields + ", "
        + "methods=" + methods + ", "
        + "attributes=" + attributes
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ClassFileInfo) {
      ClassFileInfo that = (ClassFileInfo) o;
      return this.constantPool.equals(that.getConstantPool())
          && this.accessFlags.equals(that.getAccessFlags())
          && this.thisClassIndex == that.getThisClassIndex()
          && this.superClassIndex == that.getSuperClassIndex()
          && this.interfaceIndeces.equals(that.getInterfaceIndeces())
          && this.fields.equals(that.getFields())
          && this.methods.equals(that.getMethods())
          && this.attributes.equals(that.getAttributes());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= constantPool.hashCode();
    h$ *= 1000003;
    h$ ^= accessFlags.hashCode();
    h$ *= 1000003;
    h$ ^= thisClassIndex;
    h$ *= 1000003;
    h$ ^= superClassIndex;
    h$ *= 1000003;
    h$ ^= interfaceIndeces.hashCode();
    h$ *= 1000003;
    h$ ^= fields.hashCode();
    h$ *= 1000003;
    h$ ^= methods.hashCode();
    h$ *= 1000003;
    h$ ^= attributes.hashCode();
    return h$;
  }

  static final class Builder extends ClassFileInfo.Builder {
    private ImmutableList<ConstantPoolInfo> constantPool;
    private EnumSet<ClassAccessFlag> accessFlags;
    private int thisClassIndex;
    private int superClassIndex;
    private ImmutableList<Integer> interfaceIndeces;
    private ImmutableList<FieldInfo> fields;
    private ImmutableList<MethodInfo> methods;
    private ImmutableList<AttributeInfo> attributes;
    private byte set$0;
    Builder() {
    }
    @Override
    public ClassFileInfo.Builder setConstantPool(ImmutableList<ConstantPoolInfo> constantPool) {
      if (constantPool == null) {
        throw new NullPointerException("Null constantPool");
      }
      this.constantPool = constantPool;
      return this;
    }
    @Override
    public ClassFileInfo.Builder setAccessFlags(EnumSet<ClassAccessFlag> accessFlags) {
      if (accessFlags == null) {
        throw new NullPointerException("Null accessFlags");
      }
      this.accessFlags = accessFlags;
      return this;
    }
    @Override
    public ClassFileInfo.Builder setThisClassIndex(int thisClassIndex) {
      this.thisClassIndex = thisClassIndex;
      set$0 |= 1;
      return this;
    }
    @Override
    public ClassFileInfo.Builder setSuperClassIndex(int superClassIndex) {
      this.superClassIndex = superClassIndex;
      set$0 |= 2;
      return this;
    }
    @Override
    public ClassFileInfo.Builder setInterfaceIndeces(ImmutableList<Integer> interfaceIndeces) {
      if (interfaceIndeces == null) {
        throw new NullPointerException("Null interfaceIndeces");
      }
      this.interfaceIndeces = interfaceIndeces;
      return this;
    }
    @Override
    public ClassFileInfo.Builder setFields(ImmutableList<FieldInfo> fields) {
      if (fields == null) {
        throw new NullPointerException("Null fields");
      }
      this.fields = fields;
      return this;
    }
    @Override
    public ClassFileInfo.Builder setMethods(ImmutableList<MethodInfo> methods) {
      if (methods == null) {
        throw new NullPointerException("Null methods");
      }
      this.methods = methods;
      return this;
    }
    @Override
    public ClassFileInfo.Builder setAttributes(ImmutableList<AttributeInfo> attributes) {
      if (attributes == null) {
        throw new NullPointerException("Null attributes");
      }
      this.attributes = attributes;
      return this;
    }
    @Override
    public ClassFileInfo build() {
      if (set$0 != 3
          || this.constantPool == null
          || this.accessFlags == null
          || this.interfaceIndeces == null
          || this.fields == null
          || this.methods == null
          || this.attributes == null) {
        StringBuilder missing = new StringBuilder();
        if (this.constantPool == null) {
          missing.append(" constantPool");
        }
        if (this.accessFlags == null) {
          missing.append(" accessFlags");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" thisClassIndex");
        }
        if ((set$0 & 2) == 0) {
          missing.append(" superClassIndex");
        }
        if (this.interfaceIndeces == null) {
          missing.append(" interfaceIndeces");
        }
        if (this.fields == null) {
          missing.append(" fields");
        }
        if (this.methods == null) {
          missing.append(" methods");
        }
        if (this.attributes == null) {
          missing.append(" attributes");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_ClassFileInfo(
          this.constantPool,
          this.accessFlags,
          this.thisClassIndex,
          this.superClassIndex,
          this.interfaceIndeces,
          this.fields,
          this.methods,
          this.attributes);
    }
  }

}
