package com.tyron.javacompletion.file;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_TextRange extends TextRange {

  private final TextPosition start;

  private final TextPosition end;

  AutoValue_TextRange(
      TextPosition start,
      TextPosition end) {
    if (start == null) {
      throw new NullPointerException("Null start");
    }
    this.start = start;
    if (end == null) {
      throw new NullPointerException("Null end");
    }
    this.end = end;
  }

  @Override
  public TextPosition getStart() {
    return start;
  }

  @Override
  public TextPosition getEnd() {
    return end;
  }

  @Override
  public String toString() {
    return "TextRange{"
        + "start=" + start + ", "
        + "end=" + end
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof TextRange) {
      TextRange that = (TextRange) o;
      return this.start.equals(that.getStart())
          && this.end.equals(that.getEnd());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= start.hashCode();
    h$ *= 1000003;
    h$ ^= end.hashCode();
    return h$;
  }

}
