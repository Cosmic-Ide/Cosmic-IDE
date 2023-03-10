package com.tyron.javacompletion.parser;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_AdjustedLineMap_Adjustment extends AdjustedLineMap.Adjustment {

  private final long original;

  private final long adjusted;

  AutoValue_AdjustedLineMap_Adjustment(
      long original,
      long adjusted) {
    this.original = original;
    this.adjusted = adjusted;
  }

  @Override
  long getOriginal() {
    return original;
  }

  @Override
  long getAdjusted() {
    return adjusted;
  }

  @Override
  public String toString() {
    return "Adjustment{"
        + "original=" + original + ", "
        + "adjusted=" + adjusted
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof AdjustedLineMap.Adjustment) {
      AdjustedLineMap.Adjustment that = (AdjustedLineMap.Adjustment) o;
      return this.original == that.getOriginal()
          && this.adjusted == that.getAdjusted();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (int) ((original >>> 32) ^ original);
    h$ *= 1000003;
    h$ ^= (int) ((adjusted >>> 32) ^ adjusted);
    return h$;
  }

}
