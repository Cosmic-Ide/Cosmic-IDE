package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantUtf8Info extends ConstantPoolInfo.ConstantUtf8Info {

  private final String value;

  AutoValue_ConstantPoolInfo_ConstantUtf8Info(
      String value) {
    if (value == null) {
      throw new NullPointerException("Null value");
    }
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "ConstantUtf8Info{"
        + "value=" + value
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantUtf8Info) {
      ConstantPoolInfo.ConstantUtf8Info that = (ConstantPoolInfo.ConstantUtf8Info) o;
      return this.value.equals(that.getValue());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= value.hashCode();
    return h$;
  }

}
