package com.tyron.javacompletion.parser;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_Insertion extends Insertion {

  private final int pos;

  private final String text;

  AutoValue_Insertion(
      int pos,
      String text) {
    this.pos = pos;
    if (text == null) {
      throw new NullPointerException("Null text");
    }
    this.text = text;
  }

  @Override
  public int getPos() {
    return pos;
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  public String toString() {
    return "Insertion{"
        + "pos=" + pos + ", "
        + "text=" + text
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Insertion) {
      Insertion that = (Insertion) o;
      return this.pos == that.getPos()
          && this.text.equals(that.getText());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= pos;
    h$ *= 1000003;
    h$ ^= text.hashCode();
    return h$;
  }

}
