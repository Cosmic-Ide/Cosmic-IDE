package com.tyron.javacompletion.completion;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_TextEditOptions extends TextEditOptions {

  private final boolean appendMethodArgumentSnippets;

  private AutoValue_TextEditOptions(
      boolean appendMethodArgumentSnippets) {
    this.appendMethodArgumentSnippets = appendMethodArgumentSnippets;
  }

  @Override
  public boolean getAppendMethodArgumentSnippets() {
    return appendMethodArgumentSnippets;
  }

  @Override
  public String toString() {
    return "TextEditOptions{"
        + "appendMethodArgumentSnippets=" + appendMethodArgumentSnippets
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof TextEditOptions) {
      TextEditOptions that = (TextEditOptions) o;
      return this.appendMethodArgumentSnippets == that.getAppendMethodArgumentSnippets();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= appendMethodArgumentSnippets ? 1231 : 1237;
    return h$;
  }

  static final class Builder extends TextEditOptions.Builder {
    private boolean appendMethodArgumentSnippets;
    private byte set$0;
    Builder() {
    }
    @Override
    public TextEditOptions.Builder setAppendMethodArgumentSnippets(boolean appendMethodArgumentSnippets) {
      this.appendMethodArgumentSnippets = appendMethodArgumentSnippets;
      set$0 |= 1;
      return this;
    }
    @Override
    public TextEditOptions build() {
      if (set$0 != 1) {
        String missing = " appendMethodArgumentSnippets";
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_TextEditOptions(
          this.appendMethodArgumentSnippets);
    }
  }

}
