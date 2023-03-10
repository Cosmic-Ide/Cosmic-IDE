package com.tyron.javacompletion.parser.classfile;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_FieldInfo extends FieldInfo {

  private final EnumSet<FieldInfo.AccessFlag> accessFlags;

  private final int nameIndex;

  private final int descriptorIndex;

  private final ImmutableList<AttributeInfo> attributeInfos;

  AutoValue_FieldInfo(
      EnumSet<FieldInfo.AccessFlag> accessFlags,
      int nameIndex,
      int descriptorIndex,
      ImmutableList<AttributeInfo> attributeInfos) {
    if (accessFlags == null) {
      throw new NullPointerException("Null accessFlags");
    }
    this.accessFlags = accessFlags;
    this.nameIndex = nameIndex;
    this.descriptorIndex = descriptorIndex;
    if (attributeInfos == null) {
      throw new NullPointerException("Null attributeInfos");
    }
    this.attributeInfos = attributeInfos;
  }

  @Override
  public EnumSet<FieldInfo.AccessFlag> getAccessFlags() {
    return accessFlags;
  }

  @Override
  public int getNameIndex() {
    return nameIndex;
  }

  @Override
  public int getDescriptorIndex() {
    return descriptorIndex;
  }

  @Override
  public ImmutableList<AttributeInfo> getAttributeInfos() {
    return attributeInfos;
  }

  @Override
  public String toString() {
    return "FieldInfo{"
        + "accessFlags=" + accessFlags + ", "
        + "nameIndex=" + nameIndex + ", "
        + "descriptorIndex=" + descriptorIndex + ", "
        + "attributeInfos=" + attributeInfos
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof FieldInfo) {
      FieldInfo that = (FieldInfo) o;
      return this.accessFlags.equals(that.getAccessFlags())
          && this.nameIndex == that.getNameIndex()
          && this.descriptorIndex == that.getDescriptorIndex()
          && this.attributeInfos.equals(that.getAttributeInfos());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= accessFlags.hashCode();
    h$ *= 1000003;
    h$ ^= nameIndex;
    h$ *= 1000003;
    h$ ^= descriptorIndex;
    h$ *= 1000003;
    h$ ^= attributeInfos.hashCode();
    return h$;
  }

}
