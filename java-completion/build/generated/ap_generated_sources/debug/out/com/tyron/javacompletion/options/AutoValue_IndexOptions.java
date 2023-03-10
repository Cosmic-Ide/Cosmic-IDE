package com.tyron.javacompletion.options;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_IndexOptions extends IndexOptions {

  private final boolean shouldIndexPrivate;

  private final boolean shouldIndexMethodContent;

  private AutoValue_IndexOptions(
      boolean shouldIndexPrivate,
      boolean shouldIndexMethodContent) {
    this.shouldIndexPrivate = shouldIndexPrivate;
    this.shouldIndexMethodContent = shouldIndexMethodContent;
  }

  @Override
  public boolean shouldIndexPrivate() {
    return shouldIndexPrivate;
  }

  @Override
  public boolean shouldIndexMethodContent() {
    return shouldIndexMethodContent;
  }

  @Override
  public String toString() {
    return "IndexOptions{"
        + "shouldIndexPrivate=" + shouldIndexPrivate + ", "
        + "shouldIndexMethodContent=" + shouldIndexMethodContent
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof IndexOptions) {
      IndexOptions that = (IndexOptions) o;
      return this.shouldIndexPrivate == that.shouldIndexPrivate()
          && this.shouldIndexMethodContent == that.shouldIndexMethodContent();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= shouldIndexPrivate ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= shouldIndexMethodContent ? 1231 : 1237;
    return h$;
  }

  static final class Builder extends IndexOptions.Builder {
    private boolean shouldIndexPrivate;
    private boolean shouldIndexMethodContent;
    private byte set$0;
    Builder() {
    }
    @Override
    public IndexOptions.Builder setShouldIndexPrivate(boolean shouldIndexPrivate) {
      this.shouldIndexPrivate = shouldIndexPrivate;
      set$0 |= 1;
      return this;
    }
    @Override
    public IndexOptions.Builder setShouldIndexMethodContent(boolean shouldIndexMethodContent) {
      this.shouldIndexMethodContent = shouldIndexMethodContent;
      set$0 |= 2;
      return this;
    }
    @Override
    public IndexOptions build() {
      if (set$0 != 3) {
        StringBuilder missing = new StringBuilder();
        if ((set$0 & 1) == 0) {
          missing.append(" shouldIndexPrivate");
        }
        if ((set$0 & 2) == 0) {
          missing.append(" shouldIndexMethodContent");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_IndexOptions(
          this.shouldIndexPrivate,
          this.shouldIndexMethodContent);
    }
  }

}
