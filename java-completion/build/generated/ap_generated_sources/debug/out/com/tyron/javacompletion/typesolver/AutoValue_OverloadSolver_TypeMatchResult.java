package com.tyron.javacompletion.typesolver;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_OverloadSolver_TypeMatchResult extends OverloadSolver.TypeMatchResult {

  private final OverloadSolver.TypeMatchLevel typeMatchLevel;

  private final boolean hasPrimitiveWidening;

  private AutoValue_OverloadSolver_TypeMatchResult(
      OverloadSolver.TypeMatchLevel typeMatchLevel,
      boolean hasPrimitiveWidening) {
    this.typeMatchLevel = typeMatchLevel;
    this.hasPrimitiveWidening = hasPrimitiveWidening;
  }

  @Override
  OverloadSolver.TypeMatchLevel getTypeMatchLevel() {
    return typeMatchLevel;
  }

  @Override
  boolean getHasPrimitiveWidening() {
    return hasPrimitiveWidening;
  }

  @Override
  public String toString() {
    return "TypeMatchResult{"
        + "typeMatchLevel=" + typeMatchLevel + ", "
        + "hasPrimitiveWidening=" + hasPrimitiveWidening
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof OverloadSolver.TypeMatchResult) {
      OverloadSolver.TypeMatchResult that = (OverloadSolver.TypeMatchResult) o;
      return this.typeMatchLevel.equals(that.getTypeMatchLevel())
          && this.hasPrimitiveWidening == that.getHasPrimitiveWidening();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= typeMatchLevel.hashCode();
    h$ *= 1000003;
    h$ ^= hasPrimitiveWidening ? 1231 : 1237;
    return h$;
  }

  static final class Builder extends OverloadSolver.TypeMatchResult.Builder {
    private OverloadSolver.TypeMatchLevel typeMatchLevel;
    private boolean hasPrimitiveWidening;
    private byte set$0;
    Builder() {
    }
    @Override
    OverloadSolver.TypeMatchResult.Builder setTypeMatchLevel(OverloadSolver.TypeMatchLevel typeMatchLevel) {
      if (typeMatchLevel == null) {
        throw new NullPointerException("Null typeMatchLevel");
      }
      this.typeMatchLevel = typeMatchLevel;
      return this;
    }
    @Override
    OverloadSolver.TypeMatchResult.Builder setHasPrimitiveWidening(boolean hasPrimitiveWidening) {
      this.hasPrimitiveWidening = hasPrimitiveWidening;
      set$0 |= 1;
      return this;
    }
    @Override
    OverloadSolver.TypeMatchResult build() {
      if (set$0 != 1
          || this.typeMatchLevel == null) {
        StringBuilder missing = new StringBuilder();
        if (this.typeMatchLevel == null) {
          missing.append(" typeMatchLevel");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" hasPrimitiveWidening");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_OverloadSolver_TypeMatchResult(
          this.typeMatchLevel,
          this.hasPrimitiveWidening);
    }
  }

}
