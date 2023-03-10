package com.tyron.javacompletion.project;

import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.Module;
import java.nio.file.Path;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_FileItem extends FileItem {

  private final Module module;

  private final FileScope fileScope;

  private final Path path;

  private AutoValue_FileItem(
      Module module,
      FileScope fileScope,
      Path path) {
    this.module = module;
    this.fileScope = fileScope;
    this.path = path;
  }

  @Override
  public Module getModule() {
    return module;
  }

  @Override
  public FileScope getFileScope() {
    return fileScope;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public String toString() {
    return "FileItem{"
        + "module=" + module + ", "
        + "fileScope=" + fileScope + ", "
        + "path=" + path
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof FileItem) {
      FileItem that = (FileItem) o;
      return this.module.equals(that.getModule())
          && this.fileScope.equals(that.getFileScope())
          && this.path.equals(that.getPath());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= module.hashCode();
    h$ *= 1000003;
    h$ ^= fileScope.hashCode();
    h$ *= 1000003;
    h$ ^= path.hashCode();
    return h$;
  }

  static final class Builder extends FileItem.Builder {
    private Module module;
    private FileScope fileScope;
    private Path path;
    Builder() {
    }
    @Override
    public FileItem.Builder setModule(Module module) {
      if (module == null) {
        throw new NullPointerException("Null module");
      }
      this.module = module;
      return this;
    }
    @Override
    public FileItem.Builder setFileScope(FileScope fileScope) {
      if (fileScope == null) {
        throw new NullPointerException("Null fileScope");
      }
      this.fileScope = fileScope;
      return this;
    }
    @Override
    public FileItem.Builder setPath(Path path) {
      if (path == null) {
        throw new NullPointerException("Null path");
      }
      this.path = path;
      return this;
    }
    @Override
    public FileItem build() {
      if (this.module == null
          || this.fileScope == null
          || this.path == null) {
        StringBuilder missing = new StringBuilder();
        if (this.module == null) {
          missing.append(" module");
        }
        if (this.fileScope == null) {
          missing.append(" fileScope");
        }
        if (this.path == null) {
          missing.append(" path");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_FileItem(
          this.module,
          this.fileScope,
          this.path);
    }
  }

}
