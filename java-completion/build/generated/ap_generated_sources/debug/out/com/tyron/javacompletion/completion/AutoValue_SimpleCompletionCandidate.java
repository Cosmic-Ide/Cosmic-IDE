package com.tyron.javacompletion.completion;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_SimpleCompletionCandidate extends SimpleCompletionCandidate {

  private final String name;

  private final CompletionCandidate.Kind kind;

  private AutoValue_SimpleCompletionCandidate(
      String name,
      CompletionCandidate.Kind kind) {
    this.name = name;
    this.kind = kind;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public CompletionCandidate.Kind getKind() {
    return kind;
  }

  @Override
  public String toString() {
    return "SimpleCompletionCandidate{"
        + "name=" + name + ", "
        + "kind=" + kind
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SimpleCompletionCandidate) {
      SimpleCompletionCandidate that = (SimpleCompletionCandidate) o;
      return this.name.equals(that.getName())
          && this.kind.equals(that.getKind());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= kind.hashCode();
    return h$;
  }

  static final class Builder extends SimpleCompletionCandidate.Builder {
    private String name;
    private CompletionCandidate.Kind kind;
    Builder() {
    }
    @Override
    public SimpleCompletionCandidate.Builder setName(String name) {
      if (name == null) {
        throw new NullPointerException("Null name");
      }
      this.name = name;
      return this;
    }
    @Override
    public SimpleCompletionCandidate.Builder setKind(CompletionCandidate.Kind kind) {
      if (kind == null) {
        throw new NullPointerException("Null kind");
      }
      this.kind = kind;
      return this;
    }
    @Override
    public SimpleCompletionCandidate build() {
      if (this.name == null
          || this.kind == null) {
        StringBuilder missing = new StringBuilder();
        if (this.name == null) {
          missing.append(" name");
        }
        if (this.kind == null) {
          missing.append(" kind");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_SimpleCompletionCandidate(
          this.name,
          this.kind);
    }
  }

}
