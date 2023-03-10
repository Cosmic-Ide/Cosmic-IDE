package com.tyron.javacompletion.parser.classfile;

import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.model.Entity;
import java.util.Optional;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ParsedClassFile extends ParsedClassFile {

  private final String classBinaryName;

  private final String simpleName;

  private final ImmutableList<String> classQualifiers;

  private final Optional<String> outerClassBinaryName;

  private final ClassSignature classSignature;

  private final ImmutableList<ParsedClassFile.ParsedMethod> methods;

  private final ImmutableList<ParsedClassFile.ParsedField> fields;

  private final Entity.Kind entityKind;

  private final boolean static0;

  private AutoValue_ParsedClassFile(
      String classBinaryName,
      String simpleName,
      ImmutableList<String> classQualifiers,
      Optional<String> outerClassBinaryName,
      ClassSignature classSignature,
      ImmutableList<ParsedClassFile.ParsedMethod> methods,
      ImmutableList<ParsedClassFile.ParsedField> fields,
      Entity.Kind entityKind,
      boolean static0) {
    this.classBinaryName = classBinaryName;
    this.simpleName = simpleName;
    this.classQualifiers = classQualifiers;
    this.outerClassBinaryName = outerClassBinaryName;
    this.classSignature = classSignature;
    this.methods = methods;
    this.fields = fields;
    this.entityKind = entityKind;
    this.static0 = static0;
  }

  @Override
  public String getClassBinaryName() {
    return classBinaryName;
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  @Override
  public ImmutableList<String> getClassQualifiers() {
    return classQualifiers;
  }

  @Override
  public Optional<String> getOuterClassBinaryName() {
    return outerClassBinaryName;
  }

  @Override
  public ClassSignature getClassSignature() {
    return classSignature;
  }

  @Override
  public ImmutableList<ParsedClassFile.ParsedMethod> getMethods() {
    return methods;
  }

  @Override
  public ImmutableList<ParsedClassFile.ParsedField> getFields() {
    return fields;
  }

  @Override
  public Entity.Kind getEntityKind() {
    return entityKind;
  }

  @Override
  public boolean isStatic() {
    return static0;
  }

  @Override
  public String toString() {
    return "ParsedClassFile{"
        + "classBinaryName=" + classBinaryName + ", "
        + "simpleName=" + simpleName + ", "
        + "classQualifiers=" + classQualifiers + ", "
        + "outerClassBinaryName=" + outerClassBinaryName + ", "
        + "classSignature=" + classSignature + ", "
        + "methods=" + methods + ", "
        + "fields=" + fields + ", "
        + "entityKind=" + entityKind + ", "
        + "static=" + static0
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ParsedClassFile) {
      ParsedClassFile that = (ParsedClassFile) o;
      return this.classBinaryName.equals(that.getClassBinaryName())
          && this.simpleName.equals(that.getSimpleName())
          && this.classQualifiers.equals(that.getClassQualifiers())
          && this.outerClassBinaryName.equals(that.getOuterClassBinaryName())
          && this.classSignature.equals(that.getClassSignature())
          && this.methods.equals(that.getMethods())
          && this.fields.equals(that.getFields())
          && this.entityKind.equals(that.getEntityKind())
          && this.static0 == that.isStatic();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= classBinaryName.hashCode();
    h$ *= 1000003;
    h$ ^= simpleName.hashCode();
    h$ *= 1000003;
    h$ ^= classQualifiers.hashCode();
    h$ *= 1000003;
    h$ ^= outerClassBinaryName.hashCode();
    h$ *= 1000003;
    h$ ^= classSignature.hashCode();
    h$ *= 1000003;
    h$ ^= methods.hashCode();
    h$ *= 1000003;
    h$ ^= fields.hashCode();
    h$ *= 1000003;
    h$ ^= entityKind.hashCode();
    h$ *= 1000003;
    h$ ^= static0 ? 1231 : 1237;
    return h$;
  }

  static final class Builder extends ParsedClassFile.Builder {
    private String classBinaryName;
    private String simpleName;
    private ImmutableList<String> classQualifiers;
    private Optional<String> outerClassBinaryName = Optional.empty();
    private ClassSignature classSignature;
    private ImmutableList.Builder<ParsedClassFile.ParsedMethod> methodsBuilder$;
    private ImmutableList<ParsedClassFile.ParsedMethod> methods;
    private ImmutableList.Builder<ParsedClassFile.ParsedField> fieldsBuilder$;
    private ImmutableList<ParsedClassFile.ParsedField> fields;
    private Entity.Kind entityKind;
    private boolean static0;
    private byte set$0;
    Builder() {
    }
    @Override
    public ParsedClassFile.Builder setClassBinaryName(String classBinaryName) {
      if (classBinaryName == null) {
        throw new NullPointerException("Null classBinaryName");
      }
      this.classBinaryName = classBinaryName;
      return this;
    }
    @Override
    public ParsedClassFile.Builder setSimpleName(String simpleName) {
      if (simpleName == null) {
        throw new NullPointerException("Null simpleName");
      }
      this.simpleName = simpleName;
      return this;
    }
    @Override
    public ParsedClassFile.Builder setClassQualifiers(ImmutableList<String> classQualifiers) {
      if (classQualifiers == null) {
        throw new NullPointerException("Null classQualifiers");
      }
      this.classQualifiers = classQualifiers;
      return this;
    }
    @Override
    public ParsedClassFile.Builder setOuterClassBinaryName(Optional<String> outerClassBinaryName) {
      if (outerClassBinaryName == null) {
        throw new NullPointerException("Null outerClassBinaryName");
      }
      this.outerClassBinaryName = outerClassBinaryName;
      return this;
    }
    @Override
    public ParsedClassFile.Builder setClassSignature(ClassSignature classSignature) {
      if (classSignature == null) {
        throw new NullPointerException("Null classSignature");
      }
      this.classSignature = classSignature;
      return this;
    }
    @Override
    public ImmutableList.Builder<ParsedClassFile.ParsedMethod> methodsBuilder() {
      if (methodsBuilder$ == null) {
        methodsBuilder$ = ImmutableList.builder();
      }
      return methodsBuilder$;
    }
    @Override
    public ImmutableList.Builder<ParsedClassFile.ParsedField> fieldsBuilder() {
      if (fieldsBuilder$ == null) {
        fieldsBuilder$ = ImmutableList.builder();
      }
      return fieldsBuilder$;
    }
    @Override
    public ParsedClassFile.Builder setEntityKind(Entity.Kind entityKind) {
      if (entityKind == null) {
        throw new NullPointerException("Null entityKind");
      }
      this.entityKind = entityKind;
      return this;
    }
    @Override
    public ParsedClassFile.Builder setStatic(boolean static0) {
      this.static0 = static0;
      set$0 |= 1;
      return this;
    }
    @Override
    public ParsedClassFile build() {
      if (methodsBuilder$ != null) {
        this.methods = methodsBuilder$.build();
      } else if (this.methods == null) {
        this.methods = ImmutableList.of();
      }
      if (fieldsBuilder$ != null) {
        this.fields = fieldsBuilder$.build();
      } else if (this.fields == null) {
        this.fields = ImmutableList.of();
      }
      if (set$0 != 1
          || this.classBinaryName == null
          || this.simpleName == null
          || this.classQualifiers == null
          || this.classSignature == null
          || this.entityKind == null) {
        StringBuilder missing = new StringBuilder();
        if (this.classBinaryName == null) {
          missing.append(" classBinaryName");
        }
        if (this.simpleName == null) {
          missing.append(" simpleName");
        }
        if (this.classQualifiers == null) {
          missing.append(" classQualifiers");
        }
        if (this.classSignature == null) {
          missing.append(" classSignature");
        }
        if (this.entityKind == null) {
          missing.append(" entityKind");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" static");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_ParsedClassFile(
          this.classBinaryName,
          this.simpleName,
          this.classQualifiers,
          this.outerClassBinaryName,
          this.classSignature,
          this.methods,
          this.fields,
          this.entityKind,
          this.static0);
    }
  }

}
