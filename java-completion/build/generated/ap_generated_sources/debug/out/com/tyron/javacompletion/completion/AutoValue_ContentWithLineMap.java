package com.tyron.javacompletion.completion;

import com.sun.source.tree.LineMap;
import java.nio.file.Path;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ContentWithLineMap extends ContentWithLineMap {

  private final CharSequence content;

  private final LineMap lineMap;

  private final Path filePath;

  AutoValue_ContentWithLineMap(
      CharSequence content,
      LineMap lineMap,
      Path filePath) {
    if (content == null) {
      throw new NullPointerException("Null content");
    }
    this.content = content;
    if (lineMap == null) {
      throw new NullPointerException("Null lineMap");
    }
    this.lineMap = lineMap;
    if (filePath == null) {
      throw new NullPointerException("Null filePath");
    }
    this.filePath = filePath;
  }

  @Override
  CharSequence getContent() {
    return content;
  }

  @Override
  LineMap getLineMap() {
    return lineMap;
  }

  @Override
  Path getFilePath() {
    return filePath;
  }

  @Override
  public String toString() {
    return "ContentWithLineMap{"
        + "content=" + content + ", "
        + "lineMap=" + lineMap + ", "
        + "filePath=" + filePath
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ContentWithLineMap) {
      ContentWithLineMap that = (ContentWithLineMap) o;
      return this.content.equals(that.getContent())
          && this.lineMap.equals(that.getLineMap())
          && this.filePath.equals(that.getFilePath());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= content.hashCode();
    h$ *= 1000003;
    h$ ^= lineMap.hashCode();
    h$ *= 1000003;
    h$ ^= filePath.hashCode();
    return h$;
  }

}
