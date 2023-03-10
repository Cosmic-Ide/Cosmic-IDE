package com.tyron.javacompletion.file;

import com.google.common.collect.ImmutableList;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_EditHistory extends EditHistory {

  private final String originalContent;

  private final ImmutableList<EditHistory.AppliedEdit> appliedEdits;

  AutoValue_EditHistory(
      String originalContent,
      ImmutableList<EditHistory.AppliedEdit> appliedEdits) {
    if (originalContent == null) {
      throw new NullPointerException("Null originalContent");
    }
    this.originalContent = originalContent;
    if (appliedEdits == null) {
      throw new NullPointerException("Null appliedEdits");
    }
    this.appliedEdits = appliedEdits;
  }

  @Override
  public String getOriginalContent() {
    return originalContent;
  }

  @Override
  public ImmutableList<EditHistory.AppliedEdit> getAppliedEdits() {
    return appliedEdits;
  }

  @Override
  public String toString() {
    return "EditHistory{"
        + "originalContent=" + originalContent + ", "
        + "appliedEdits=" + appliedEdits
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof EditHistory) {
      EditHistory that = (EditHistory) o;
      return this.originalContent.equals(that.getOriginalContent())
          && this.appliedEdits.equals(that.getAppliedEdits());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= originalContent.hashCode();
    h$ *= 1000003;
    h$ ^= appliedEdits.hashCode();
    return h$;
  }

}
