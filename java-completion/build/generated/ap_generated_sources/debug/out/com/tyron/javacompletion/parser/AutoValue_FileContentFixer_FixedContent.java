package com.tyron.javacompletion.parser;

import com.sun.source.tree.LineMap;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_FileContentFixer_FixedContent extends FileContentFixer.FixedContent {

  private final String content;

  private final LineMap adjustedLineMap;

  AutoValue_FileContentFixer_FixedContent(
      String content,
      LineMap adjustedLineMap) {
    if (content == null) {
      throw new NullPointerException("Null content");
    }
    this.content = content;
    if (adjustedLineMap == null) {
      throw new NullPointerException("Null adjustedLineMap");
    }
    this.adjustedLineMap = adjustedLineMap;
  }

  @Override
  public String getContent() {
    return content;
  }

  @Override
  public LineMap getAdjustedLineMap() {
    return adjustedLineMap;
  }

  @Override
  public String toString() {
    return "FixedContent{"
        + "content=" + content + ", "
        + "adjustedLineMap=" + adjustedLineMap
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof FileContentFixer.FixedContent) {
      FileContentFixer.FixedContent that = (FileContentFixer.FixedContent) o;
      return this.content.equals(that.getContent())
          && this.adjustedLineMap.equals(that.getAdjustedLineMap());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= content.hashCode();
    h$ *= 1000003;
    h$ ^= adjustedLineMap.hashCode();
    return h$;
  }

}
