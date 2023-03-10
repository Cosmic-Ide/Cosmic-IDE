package com.tyron.javacompletion.file;

import java.nio.file.Path;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_FileTextLocation extends FileTextLocation {

  private final Path filePath;

  private final TextRange range;

  AutoValue_FileTextLocation(
      Path filePath,
      TextRange range) {
    if (filePath == null) {
      throw new NullPointerException("Null filePath");
    }
    this.filePath = filePath;
    if (range == null) {
      throw new NullPointerException("Null range");
    }
    this.range = range;
  }

  @Override
  public Path getFilePath() {
    return filePath;
  }

  @Override
  public TextRange getRange() {
    return range;
  }

  @Override
  public String toString() {
    return "FileTextLocation{"
        + "filePath=" + filePath + ", "
        + "range=" + range
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof FileTextLocation) {
      FileTextLocation that = (FileTextLocation) o;
      return this.filePath.equals(that.getFilePath())
          && this.range.equals(that.getRange());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= filePath.hashCode();
    h$ *= 1000003;
    h$ ^= range.hashCode();
    return h$;
  }

}
