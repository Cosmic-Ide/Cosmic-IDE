package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_AttributeInfo_Signature extends AttributeInfo.Signature {

  private final int signatureIndex;

  AutoValue_AttributeInfo_Signature(
      int signatureIndex) {
    this.signatureIndex = signatureIndex;
  }

  @Override
  public int getSignatureIndex() {
    return signatureIndex;
  }

  @Override
  public String toString() {
    return "Signature{"
        + "signatureIndex=" + signatureIndex
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof AttributeInfo.Signature) {
      AttributeInfo.Signature that = (AttributeInfo.Signature) o;
      return this.signatureIndex == that.getSignatureIndex();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= signatureIndex;
    return h$;
  }

}
