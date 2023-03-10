package com.tyron.javacompletion.file;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_TextPosition extends TextPosition {

  private final int line;

  private final int character;

  AutoValue_TextPosition(
      int line,
      int character) {
    this.line = line;
    this.character = character;
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public int getCharacter() {
    return character;
  }

  @Override
  public String toString() {
    return "TextPosition{"
        + "line=" + line + ", "
        + "character=" + character
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof TextPosition) {
      TextPosition that = (TextPosition) o;
      return this.line == that.getLine()
          && this.character == that.getCharacter();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= line;
    h$ *= 1000003;
    h$ ^= character;
    return h$;
  }

}
