package com.tyron.javacompletion.parser.classfile;

import com.tyron.javacompletion.model.TypeReference;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ParsedClassFile_ParsedField extends ParsedClassFile.ParsedField {

  private final String simpleName;

  private final TypeReference fieldType;

  private final boolean static0;

  AutoValue_ParsedClassFile_ParsedField(
      String simpleName,
      TypeReference fieldType,
      boolean static0) {
    if (simpleName == null) {
      throw new NullPointerException("Null simpleName");
    }
    this.simpleName = simpleName;
    if (fieldType == null) {
      throw new NullPointerException("Null fieldType");
    }
    this.fieldType = fieldType;
    this.static0 = static0;
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  @Override
  public TypeReference getFieldType() {
    return fieldType;
  }

  @Override
  public boolean isStatic() {
    return static0;
  }

  @Override
  public String toString() {
    return "ParsedField{"
        + "simpleName=" + simpleName + ", "
        + "fieldType=" + fieldType + ", "
        + "static=" + static0
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ParsedClassFile.ParsedField) {
      ParsedClassFile.ParsedField that = (ParsedClassFile.ParsedField) o;
      return this.simpleName.equals(that.getSimpleName())
          && this.fieldType.equals(that.getFieldType())
          && this.static0 == that.isStatic();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= simpleName.hashCode();
    h$ *= 1000003;
    h$ ^= fieldType.hashCode();
    h$ *= 1000003;
    h$ ^= static0 ? 1231 : 1237;
    return h$;
  }

}
