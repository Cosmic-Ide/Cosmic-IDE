package com.tyron.javacompletion.file;

import java.util.Optional;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_EditHistory_AppliedEdit extends EditHistory.AppliedEdit {

  private final TextRange textRange;

  private final Optional<Integer> rangeLength;

  private final String newText;

  AutoValue_EditHistory_AppliedEdit(
      TextRange textRange,
      Optional<Integer> rangeLength,
      String newText) {
    if (textRange == null) {
      throw new NullPointerException("Null textRange");
    }
    this.textRange = textRange;
    if (rangeLength == null) {
      throw new NullPointerException("Null rangeLength");
    }
    this.rangeLength = rangeLength;
    if (newText == null) {
      throw new NullPointerException("Null newText");
    }
    this.newText = newText;
  }

  @Override
  public TextRange getTextRange() {
    return textRange;
  }

  @Override
  public Optional<Integer> getRangeLength() {
    return rangeLength;
  }

  @Override
  public String getNewText() {
    return newText;
  }

  @Override
  public String toString() {
    return "AppliedEdit{"
        + "textRange=" + textRange + ", "
        + "rangeLength=" + rangeLength + ", "
        + "newText=" + newText
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof EditHistory.AppliedEdit) {
      EditHistory.AppliedEdit that = (EditHistory.AppliedEdit) o;
      return this.textRange.equals(that.getTextRange())
          && this.rangeLength.equals(that.getRangeLength())
          && this.newText.equals(that.getNewText());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= textRange.hashCode();
    h$ *= 1000003;
    h$ ^= rangeLength.hashCode();
    h$ *= 1000003;
    h$ ^= newText.hashCode();
    return h$;
  }

}
