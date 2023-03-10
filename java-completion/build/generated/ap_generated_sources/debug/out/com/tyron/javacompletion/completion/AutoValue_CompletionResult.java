package com.tyron.javacompletion.completion;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_CompletionResult extends CompletionResult {

  private final Path filePath;

  private final int line;

  private final int column;

  private final String prefix;

  private final ImmutableList<CompletionCandidate> completionCandidates;

  private final TextEditOptions textEditOptions;

  private AutoValue_CompletionResult(
      Path filePath,
      int line,
      int column,
      String prefix,
      ImmutableList<CompletionCandidate> completionCandidates,
      TextEditOptions textEditOptions) {
    this.filePath = filePath;
    this.line = line;
    this.column = column;
    this.prefix = prefix;
    this.completionCandidates = completionCandidates;
    this.textEditOptions = textEditOptions;
  }

  @Override
  public Path getFilePath() {
    return filePath;
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public int getColumn() {
    return column;
  }

  @Override
  public String getPrefix() {
    return prefix;
  }

  @Override
  public ImmutableList<CompletionCandidate> getCompletionCandidates() {
    return completionCandidates;
  }

  @Override
  public TextEditOptions getTextEditOptions() {
    return textEditOptions;
  }

  @Override
  public String toString() {
    return "CompletionResult{"
        + "filePath=" + filePath + ", "
        + "line=" + line + ", "
        + "column=" + column + ", "
        + "prefix=" + prefix + ", "
        + "completionCandidates=" + completionCandidates + ", "
        + "textEditOptions=" + textEditOptions
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof CompletionResult) {
      CompletionResult that = (CompletionResult) o;
      return this.filePath.equals(that.getFilePath())
          && this.line == that.getLine()
          && this.column == that.getColumn()
          && this.prefix.equals(that.getPrefix())
          && this.completionCandidates.equals(that.getCompletionCandidates())
          && this.textEditOptions.equals(that.getTextEditOptions());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= filePath.hashCode();
    h$ *= 1000003;
    h$ ^= line;
    h$ *= 1000003;
    h$ ^= column;
    h$ *= 1000003;
    h$ ^= prefix.hashCode();
    h$ *= 1000003;
    h$ ^= completionCandidates.hashCode();
    h$ *= 1000003;
    h$ ^= textEditOptions.hashCode();
    return h$;
  }

  @Override
  public CompletionResult.Builder toBuilder() {
    return new Builder(this);
  }

  static final class Builder extends CompletionResult.Builder {
    private Path filePath;
    private int line;
    private int column;
    private String prefix;
    private ImmutableList<CompletionCandidate> completionCandidates;
    private TextEditOptions textEditOptions;
    private byte set$0;
    Builder() {
    }
    private Builder(CompletionResult source) {
      this.filePath = source.getFilePath();
      this.line = source.getLine();
      this.column = source.getColumn();
      this.prefix = source.getPrefix();
      this.completionCandidates = source.getCompletionCandidates();
      this.textEditOptions = source.getTextEditOptions();
      set$0 = (byte) 3;
    }
    @Override
    public CompletionResult.Builder setFilePath(Path filePath) {
      if (filePath == null) {
        throw new NullPointerException("Null filePath");
      }
      this.filePath = filePath;
      return this;
    }
    @Override
    public CompletionResult.Builder setLine(int line) {
      this.line = line;
      set$0 |= 1;
      return this;
    }
    @Override
    public CompletionResult.Builder setColumn(int column) {
      this.column = column;
      set$0 |= 2;
      return this;
    }
    @Override
    public CompletionResult.Builder setPrefix(String prefix) {
      if (prefix == null) {
        throw new NullPointerException("Null prefix");
      }
      this.prefix = prefix;
      return this;
    }
    @Override
    public CompletionResult.Builder setCompletionCandidates(ImmutableList<CompletionCandidate> completionCandidates) {
      if (completionCandidates == null) {
        throw new NullPointerException("Null completionCandidates");
      }
      this.completionCandidates = completionCandidates;
      return this;
    }
    @Override
    public CompletionResult.Builder setTextEditOptions(TextEditOptions textEditOptions) {
      if (textEditOptions == null) {
        throw new NullPointerException("Null textEditOptions");
      }
      this.textEditOptions = textEditOptions;
      return this;
    }
    @Override
    public CompletionResult build() {
      if (set$0 != 3
          || this.filePath == null
          || this.prefix == null
          || this.completionCandidates == null
          || this.textEditOptions == null) {
        StringBuilder missing = new StringBuilder();
        if (this.filePath == null) {
          missing.append(" filePath");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" line");
        }
        if ((set$0 & 2) == 0) {
          missing.append(" column");
        }
        if (this.prefix == null) {
          missing.append(" prefix");
        }
        if (this.completionCandidates == null) {
          missing.append(" completionCandidates");
        }
        if (this.textEditOptions == null) {
          missing.append(" textEditOptions");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_CompletionResult(
          this.filePath,
          this.line,
          this.column,
          this.prefix,
          this.completionCandidates,
          this.textEditOptions);
    }
  }

}
