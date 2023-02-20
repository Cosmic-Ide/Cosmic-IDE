package com.tyron.javacompletion.project;

import com.google.auto.value.AutoValue;
import java.nio.file.Path;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.Module;

/** Information about a parsed file in a project. */
@AutoValue
public abstract class FileItem {
    public abstract Module getModule();

    public abstract FileScope getFileScope();

    public abstract Path getPath();

    public static Builder newBuilder() {
        return new AutoValue_FileItem.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setModule(Module module);

        public abstract Builder setFileScope(FileScope fileScope);

        public abstract Builder setPath(Path path);

        public abstract FileItem build();
    }
}